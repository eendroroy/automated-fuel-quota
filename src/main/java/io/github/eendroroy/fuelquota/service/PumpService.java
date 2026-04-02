package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.AuthorizationRequest;
import io.github.eendroroy.fuelquota.dto.request.DispenseConfirmationRequest;
import io.github.eendroroy.fuelquota.dto.response.AuthorizationResponse;
import io.github.eendroroy.fuelquota.dto.response.DispenseConfirmationResponse;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import io.github.eendroroy.fuelquota.entity.Transaction;
import io.github.eendroroy.fuelquota.entity.User;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.entity.Quota;
import io.github.eendroroy.fuelquota.enums.AuthorizationDecision;
import io.github.eendroroy.fuelquota.repository.FuelStationRepository;
import io.github.eendroroy.fuelquota.repository.TransactionRepository;
import io.github.eendroroy.fuelquota.repository.VehicleRepository;
import io.github.eendroroy.fuelquota.security.JwtTokenProvider;
import io.github.eendroroy.fuelquota.service.QuotaService.QuotaAuthorizationResult;
import io.github.eendroroy.fuelquota.exception.BadRequestException;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service handling the pump representative authorization and dispense confirmation flows.
 *
 * <p>Implements BRD requirements:
 * <ul>
 *   <li>FR-04: QR token scanning and validation</li>
 *   <li>FR-05: Secure authorization web service call</li>
 *   <li>FR-06: Display authorization result</li>
 *   <li>FR-07 / FR-13: Confirm dispense and record transaction (idempotent)</li>
 *   <li>FR-08 / FR-11: JWT QR token validation</li>
 *   <li>FR-09: Vehicle status check</li>
 *   <li>FR-10: GPS geofence validation</li>
 *   <li>FR-12: Partial dispense support</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PumpService {

    private static final Logger logger = LoggerFactory.getLogger(PumpService.class);

    private final JwtTokenProvider tokenProvider;
    private final VehicleRepository vehicleRepository;
    private final FuelStationRepository stationRepository;
    private final TransactionRepository transactionRepository;
    private final QuotaService quotaService;
    private final AuthService authService;

    /**
     * Core authorization logic for fuel dispensing (BRD FR-04 to FR-12).
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Validate the JWT QR token (FR-08, FR-11).</li>
     *   <li>Verify the vehicle exists and has ACTIVE status (FR-09).</li>
     *   <li>Verify the station exists and is ACTIVE.</li>
     *   <li>Validate GPS geofence if coordinates are provided (FR-10).</li>
     *   <li>Calculate available quota; support partial dispense (FR-11, FR-12).</li>
     * </ol>
     *
     * @param request authorization request from the pump representative app
     * @return {@link AuthorizationResponse} with decision, authorized litres, and vehicle info
     */
    public AuthorizationResponse authorizeDispensing(AuthorizationRequest request) {
        try {
            // Step 1: Validate QR token
            UUID vehicleId;
            String registrationNumber;
            try {
                vehicleId = tokenProvider.getVehicleIdFromQrToken(request.getQrToken());
                registrationNumber = tokenProvider.getRegistrationNumberFromQrToken(request.getQrToken());
            } catch (Exception e) {
                logger.warn("Invalid QR token received: {}", e.getMessage());
                return AuthorizationResponse.builder()
                        .decision(AuthorizationDecision.DENIED)
                        .authorizedLiters(BigDecimal.ZERO)
                        .remainingQuota(BigDecimal.ZERO)
                        .message("Invalid or expired QR code")
                        .build();
            }

            // Step 2: Verify vehicle exists and is ACTIVE
            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
            if (vehicle == null) {
                logger.warn("Vehicle not found for ID: {}", vehicleId);
                return AuthorizationResponse.builder()
                        .decision(AuthorizationDecision.DENIED)
                        .authorizedLiters(BigDecimal.ZERO)
                        .remainingQuota(BigDecimal.ZERO)
                        .message("Vehicle not found")
                        .build();
            }

            // Step 3: Verify station exists and is ACTIVE
            FuelStation station = stationRepository.findById(request.getStationId()).orElse(null);
            if (station == null || !station.isActive()) {
                return AuthorizationResponse.builder()
                        .decision(AuthorizationDecision.DENIED)
                        .authorizedLiters(BigDecimal.ZERO)
                        .remainingQuota(BigDecimal.ZERO)
                        .message("Station not found or inactive")
                        .build();
            }

            // Step 4: GPS geofence validation
            if (request.getLatitude() != null && request.getLongitude() != null) {
                if (!station.isWithinGeofence(request.getLatitude(), request.getLongitude())) {
                    logger.warn("Geofence validation failed for station {} at coordinates {},{}",
                            station.getStationCode(), request.getLatitude(), request.getLongitude());
                    return AuthorizationResponse.builder()
                            .decision(AuthorizationDecision.DENIED)
                            .authorizedLiters(BigDecimal.ZERO)
                            .remainingQuota(BigDecimal.ZERO)
                            .message("Location verification failed")
                            .build();
                }
            }

            // Step 5: Quota authorization (with partial dispense support)
            BigDecimal requestedLiters = request.getRequestedLiters() != null
                    ? request.getRequestedLiters() : BigDecimal.valueOf(50);

            QuotaAuthorizationResult quotaResult = quotaService.authorizeQuota(registrationNumber, requestedLiters);

            AuthorizationResponse response = AuthorizationResponse.builder()
                    .decision(quotaResult.getDecision())
                    .authorizedLiters(quotaResult.getAuthorizedLiters())
                    .remainingQuota(quotaResult.getRemainingQuota())
                    .message(quotaResult.getDenyReason())
                    .vehicleFound(vehicle.getRegistrationNumber())
                    .vehicleMake(vehicle.getVehicleMake())
                    .vehicleColor(vehicle.getVehicleColor())
                    .ownerName(vehicle.getOwnerName())
                    .build();

            logger.info("Authorization result for vehicle {}: {} - {} liters authorized",
                    registrationNumber, quotaResult.getDecision(), quotaResult.getAuthorizedLiters());
            return response;

        } catch (Exception e) {
            logger.error("Error during authorization for request: {}", request, e);
            return AuthorizationResponse.builder()
                    .decision(AuthorizationDecision.DENIED)
                    .authorizedLiters(BigDecimal.ZERO)
                    .remainingQuota(BigDecimal.ZERO)
                    .message("System error occurred")
                    .build();
        }
    }

    /**
     * Confirms fuel dispensing, updates quota, and records the transaction (BRD FR-07, FR-13).
     *
     * <p>This method is idempotent: submitting the same QR token twice raises a
     * {@link BadRequestException} to prevent double quota consumption.
     *
     * @param request dispense confirmation from the pump representative app
     * @return {@link DispenseConfirmationResponse} with transaction reference and receipt details
     * @throws ResourceNotFoundException if the vehicle, station, or pump representative is not found
     * @throws BadRequestException       if the QR token has already been used or dispensed amount is invalid
     */
    public DispenseConfirmationResponse confirmDispensing(DispenseConfirmationRequest request) {
        // Re-validate QR token
        UUID vehicleId = tokenProvider.getVehicleIdFromQrToken(request.getQrToken());

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        FuelStation station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found"));

        User pumpRep = authService.findUserById(request.getPumpRepresentativeId());

        // Idempotency check: one transaction per QR token
        if (!transactionRepository.findByQrToken(request.getQrToken()).isEmpty()) {
            throw new BadRequestException("This QR code has already been used");
        }

        BigDecimal dispensedLiters = request.getDispensedLiters();
        if (dispensedLiters.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Dispensed amount must be positive");
        }

        // Consume quota and persist transaction
        Quota updatedQuota = quotaService.consumeQuota(vehicleId, dispensedLiters);

        Transaction transaction = new Transaction(
                vehicle, station, dispensedLiters, request.getFuelType(),
                pumpRep, request.getLatitude(), request.getLongitude(),
                request.getQrToken(), updatedQuota.getRemainingLiters()
        );
        transaction.setPumpId(request.getPumpId());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);

        logger.info("Transaction completed: {} liters dispensed to vehicle {} at station {}",
                dispensedLiters, vehicle.getRegistrationNumber(), station.getStationCode());

        return DispenseConfirmationResponse.builder()
                .transactionId(transaction.getId().toString())
                .transactionReference(transaction.getTransactionReference())
                .dispensedLiters(dispensedLiters)
                .remainingQuota(updatedQuota.getRemainingLiters())
                .timestamp(transaction.getTransactionTimestamp())
                .message("Transaction completed successfully")
                .build();
    }
}
