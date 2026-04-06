package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.AuthorizationRequest;
import io.github.eendroroy.fuelquota.dto.request.DispenseConfirmationRequest;
import io.github.eendroroy.fuelquota.dto.request.ManualAuthorizationRequest;
import io.github.eendroroy.fuelquota.dto.request.PumpRepLoginRequest;
import io.github.eendroroy.fuelquota.dto.response.AuthorizationResponse;
import io.github.eendroroy.fuelquota.dto.response.DispenseConfirmationResponse;
import io.github.eendroroy.fuelquota.dto.response.PumpRepLoginResponse;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import io.github.eendroroy.fuelquota.entity.PumpRepresentative;
import io.github.eendroroy.fuelquota.entity.Transaction;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.entity.Quota;
import io.github.eendroroy.fuelquota.enums.AuthorizationDecision;
import io.github.eendroroy.fuelquota.repository.FuelStationRepository;
import io.github.eendroroy.fuelquota.repository.PumpRepresentativeRepository;
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
import java.time.LocalDateTime;
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
    private final PumpRepresentativeRepository pumpRepRepository;
    private final QuotaService quotaService;

    /**
     * Authenticates a pump representative by mobile number (no password required for demo).
     *
     * @param request login request containing the mobile number
     * @return session details including representative info and assigned station
     */
    public PumpRepLoginResponse pumpRepLogin(PumpRepLoginRequest request) {
        PumpRepresentative rep = pumpRepRepository.findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() -> new BadRequestException("No representative found with mobile number: " + request.getMobileNumber()));

        if (rep.getStatus() != PumpRepresentative.RepStatus.ACTIVE) {
            throw new BadRequestException("Representative account is not active");
        }

        rep.setLastLoginTimestamp(LocalDateTime.now());
        pumpRepRepository.save(rep);

        FuelStation station = rep.getStation();
        return PumpRepLoginResponse.builder()
                .id(rep.getId().toString())
                .name(rep.getName())
                .employeeId(rep.getEmployeeId())
                .stationId(station.getId().toString())
                .stationName(station.getStationName())
                .stationCode(station.getStationCode())
                .build();
    }

    /**
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
            String qrFuelType = null;
            try {
                vehicleId = tokenProvider.getVehicleIdFromQrToken(request.getQrToken());
                registrationNumber = tokenProvider.getRegistrationNumberFromQrToken(request.getQrToken());
                // Extract the customer's intended fuel type from the QR token (may be null for legacy tokens)
                qrFuelType = tokenProvider.getFuelTypeFromQrToken(request.getQrToken());
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
                    .totalQuota(quotaResult.getLimitLiters())
                    .message(quotaResult.getDenyReason())
                    .vehicleFound(vehicle.getRegistrationNumber())
                    .vehicleMake(vehicle.getVehicleMake())
                    .vehicleColor(vehicle.getVehicleColor())
                    .ownerName(vehicle.getOwnerName())
                    .vehicleStatus(vehicle.getStatus().name())
                    // Use the fuel type the customer selected when generating the QR code;
                    // fall back to the vehicle's primary fuel type for legacy tokens.
                    .fuelType(qrFuelType != null ? qrFuelType : vehicle.getFuelType())
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
     * Authorizes fuel dispensing by vehicle registration number (manual entry alternative to QR scan).
     *
     * <p>Skips QR token validation; all other checks (vehicle status, station active,
     * geofence, quota) are identical to {@link #authorizeDispensing}.
     *
     * @param request manual authorization request with registration number and station ID
     * @return {@link AuthorizationResponse} with decision and vehicle info
     */
    public AuthorizationResponse authorizeByRegistration(ManualAuthorizationRequest request) {
        try {
            // Step 1: Verify vehicle exists
            Vehicle vehicle = vehicleRepository.findByRegistrationNumber(request.getRegistrationNumber()).orElse(null);
            if (vehicle == null) {
                return AuthorizationResponse.builder()
                        .decision(AuthorizationDecision.DENIED)
                        .authorizedLiters(BigDecimal.ZERO)
                        .remainingQuota(BigDecimal.ZERO)
                        .message("Vehicle not found: " + request.getRegistrationNumber())
                        .build();
            }

            // Step 2: Verify station exists and is active
            FuelStation station = stationRepository.findById(request.getStationId()).orElse(null);
            if (station == null || !station.isActive()) {
                return AuthorizationResponse.builder()
                        .decision(AuthorizationDecision.DENIED)
                        .authorizedLiters(BigDecimal.ZERO)
                        .remainingQuota(BigDecimal.ZERO)
                        .message("Station not found or inactive")
                        .vehicleFound(vehicle.getRegistrationNumber())
                        .vehicleStatus(vehicle.getStatus().name())
                        .fuelType(vehicle.getFuelType())
                        .build();
            }

            // Step 3: Quota authorization
            BigDecimal requestedLiters = request.getRequestedLiters() != null
                    ? request.getRequestedLiters() : BigDecimal.valueOf(50);

            QuotaAuthorizationResult quotaResult =
                    quotaService.authorizeQuota(vehicle.getRegistrationNumber(), requestedLiters);

            return AuthorizationResponse.builder()
                    .decision(quotaResult.getDecision())
                    .authorizedLiters(quotaResult.getAuthorizedLiters())
                    .remainingQuota(quotaResult.getRemainingQuota())
                    .totalQuota(quotaResult.getLimitLiters())
                    .message(quotaResult.getDenyReason())
                    .vehicleFound(vehicle.getRegistrationNumber())
                    .vehicleMake(vehicle.getVehicleMake())
                    .vehicleColor(vehicle.getVehicleColor())
                    .ownerName(vehicle.getOwnerName())
                    .vehicleStatus(vehicle.getStatus().name())
                    .fuelType(vehicle.getFuelType())
                    .build();

        } catch (Exception e) {
            logger.error("Error during manual authorization for: {}", request.getRegistrationNumber(), e);
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
        final boolean hasQrToken = request.getQrToken() != null && !request.getQrToken().isBlank();

        // Resolve vehicle: from QR token when available, otherwise by registration number embedded in request
        UUID vehicleId;
        if (hasQrToken) {
            vehicleId = tokenProvider.getVehicleIdFromQrToken(request.getQrToken());
        } else if (request.getRegistrationNumber() != null && !request.getRegistrationNumber().isBlank()) {
            Vehicle v = vehicleRepository.findByRegistrationNumber(request.getRegistrationNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
            vehicleId = v.getId();
        } else {
            throw new BadRequestException("Either a QR token or a registration number is required");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        FuelStation station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found"));

        PumpRepresentative pumpRep = pumpRepRepository.findById(request.getPumpRepresentativeId())
                .orElseThrow(() -> new ResourceNotFoundException("Pump representative not found"));

        // Idempotency check — only enforced when a QR token was supplied
        if (hasQrToken && !transactionRepository.findByQrToken(request.getQrToken()).isEmpty()) {
            throw new BadRequestException("This QR code has already been used");
        }

        BigDecimal dispensedLiters = request.getDispensedLiters();
        if (dispensedLiters.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Dispensed amount must be positive");
        }

        // Use the QR token as the transaction token; null for manual-path transactions
        String tokenForRecord = hasQrToken ? request.getQrToken() : null;

        // Consume quota and persist transaction
        Quota updatedQuota = quotaService.consumeQuota(vehicleId, dispensedLiters);

        Transaction transaction = new Transaction(
                vehicle, station, dispensedLiters, request.getFuelType(),
                pumpRep, request.getLatitude(), request.getLongitude(),
                tokenForRecord, updatedQuota.getRemainingLiters()
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
