package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.AddVehicleRequest;
import io.github.eendroroy.fuelquota.dto.response.VehicleResponse;
import io.github.eendroroy.fuelquota.dto.response.QuotaResponse;
import io.github.eendroroy.fuelquota.dto.response.QrTokenResponse;
import io.github.eendroroy.fuelquota.dto.response.TransactionResponse;
import io.github.eendroroy.fuelquota.entity.Quota;
import io.github.eendroroy.fuelquota.entity.User;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.repository.QuotaRepository;
import io.github.eendroroy.fuelquota.repository.UserRepository;
import io.github.eendroroy.fuelquota.repository.VehicleRepository;
import io.github.eendroroy.fuelquota.repository.TransactionRepository;
import io.github.eendroroy.fuelquota.repository.RegistrationCodeRepository;
import io.github.eendroroy.fuelquota.security.JwtTokenProvider;
import io.github.eendroroy.fuelquota.mapper.VehicleMapper;
import io.github.eendroroy.fuelquota.mapper.QuotaMapper;
import io.github.eendroroy.fuelquota.mapper.TransactionMapper;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;
import io.github.eendroroy.fuelquota.exception.BadRequestException;
import io.github.eendroroy.fuelquota.config.AppProperties;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final QuotaRepository quotaRepository;
    private final TransactionRepository transactionRepository;
    private final QuotaService quotaService;
    private final QuotaConfigService quotaConfigService;
    private final RegistrationCodeRepository registrationCodeRepository;
    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;
    private final VehicleMapper vehicleMapper;
    private final QuotaMapper quotaMapper;
    private final TransactionMapper transactionMapper;

    // ── Multi-vehicle listing ──────────────────────────────────────────────────

    /**
     * Returns all vehicles belonging to the authenticated customer (non-paginated).
     */
    public List<VehicleResponse> getVehiclesByUserId(UUID userId) {
        List<Vehicle> vehicles = vehicleRepository.findByUserId(userId);
        if (vehicles.isEmpty()) {
            throw new ResourceNotFoundException("No vehicles found for user");
        }
        return vehicles.stream().map(vehicleMapper::toResponse).toList();
    }

    /**
     * Returns paginated vehicles belonging to the authenticated customer.
     */
    public Page<VehicleResponse> getVehiclesByUserId(UUID userId, Pageable pageable) {
        Page<Vehicle> vehicles = vehicleRepository.findPageByUserId(userId, pageable);
        return vehicles.map(vehicleMapper::toResponse);
    }

    /**
     * Returns the first (or only) vehicle for the customer — kept for backward compatibility.
     */
    // ...existing code...
    public VehicleResponse getVehicleByUserId(UUID userId) {
        return vehicleRepository.findByUserId(userId).stream()
                .findFirst()
                .map(vehicleMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found for user"));
    }

    // ── Add / Remove vehicle ───────────────────────────────────────────────────

    /**
     * Adds a new vehicle to the authenticated customer's account.
     * The vehicle is immediately set to VERIFIED status with an ACTIVE quota.
     *
     * <p>If {@code ownerNid} or {@code ownerMobile} are not supplied in the request
     * they are derived from the user's account: mobile from {@link User#getMobileNumber()},
     * NID from the user's first registered vehicle (established at sign-up).
     *
     * <p><strong>Future scope:</strong> BRTA API verification of ownership
     * documents will be required before setting VERIFIED status.
     */
    @Transactional
    // ...existing code...
    public VehicleResponse addVehicle(UUID userId, AddVehicleRequest request) {
        String registrationNumber = request.assembleRegistrationNumber();

        if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new BadRequestException("Vehicle registration number already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Resolve ownerNid — fall back to the NID from the user's first registered vehicle
        String ownerNid = (request.getOwnerNid() != null && !request.getOwnerNid().isBlank())
                ? request.getOwnerNid()
                : vehicleRepository.findByUserId(userId).stream()
                        .map(Vehicle::getOwnerNid)
                        .filter(nid -> nid != null && !nid.isBlank())
                        .findFirst()
                        .orElse("");

        // Resolve ownerMobile — fall back to the mobile stored on the user account
        String ownerMobile = (request.getOwnerMobile() != null && !request.getOwnerMobile().isBlank())
                ? request.getOwnerMobile()
                : (user.getMobileNumber() != null ? user.getMobileNumber() : "");

        // Resolve vehicle class description from registration code lookup
        String vehicleClass = registrationCodeRepository
                .findByCode(request.getVehicleRegistrationCode().toUpperCase().trim())
                .map(rc -> rc.getDescription())
                .orElse(request.getVehicleRegistrationCode());

        Vehicle vehicle = new Vehicle(
                registrationNumber,
                request.getBrtaOfficeCode().toUpperCase().trim(),
                request.getVehicleRegistrationCode().toUpperCase().trim(),
                user.getName(),
                ownerNid,
                ownerMobile,
                user.getEmail(),
                request.getVehicleMake(),
                request.getVehicleColor(),
                vehicleClass,
                request.getFuelType(),
                LocalDate.parse(request.getRegistrationDate())
        );
        vehicle.setStatus(Vehicle.VehicleStatus.VERIFIED);
        vehicle.setUser(user);
        if (request.getEngineDisplacement() != null) vehicle.setEngineDisplacement(request.getEngineDisplacement());
        vehicle = vehicleRepository.save(vehicle);

        // Create quota immediately as ACTIVE
        BigDecimal limit = quotaConfigService.getDefaultLimitLitres();
        Quota quota = new Quota(vehicle, limit, quotaConfigService.getDefaultPeriod());
        quota.setStatus(Quota.QuotaStatus.ACTIVE);
        quotaRepository.save(quota);

        return vehicleMapper.toResponse(vehicle);
    }

    /**
     * Removes a vehicle from the customer's account.
     * <ul>
     *   <li>VERIFIED / UNVERIFIED with no transactions → deleted</li>
     *   <li>With transaction history → marked DEREGISTERED</li>
     * </ul>
     */
    @Transactional
    // ...existing code...
    public void removeVehicle(UUID userId, UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (vehicle.getUser() == null || !vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("Vehicle does not belong to this user");
        }

        boolean hasTransactions = transactionRepository.existsByVehicleId(vehicleId);

        if (!hasTransactions) {
            // Hard delete — no history
            vehicleRepository.delete(vehicle);
        } else {
            // Soft deregister — preserve transaction history
            vehicle.setStatus(Vehicle.VehicleStatus.DEREGISTERED);
            vehicleRepository.save(vehicle);
            // Suspend quota if present
            quotaRepository.findByVehicleId(vehicleId).ifPresent(q -> {
                q.setStatus(Quota.QuotaStatus.SUSPENDED);
                quotaRepository.save(q);
            });
        }
    }

    // ── Quota ─────────────────────────────────────────────────────────────────

    /**
     * Quota for the first vehicle (backward-compat endpoint).
     */
    public QuotaResponse getQuotaByUserId(UUID userId) {
        VehicleResponse vehicleResponse = getVehicleByUserId(userId);
        Quota quota = quotaService.getQuotaByVehicleId(UUID.fromString(vehicleResponse.getId()));
        return quotaMapper.toResponse(quota);
    }

    /**
     * Quota for a specific vehicle owned by the customer.
     */
    public QuotaResponse getQuotaByVehicleId(UUID userId, UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (!vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("Vehicle does not belong to this user");
        }
        Quota quota = quotaService.getQuotaByVehicleId(vehicleId);
        return quotaMapper.toResponse(quota);
    }

    // ── QR Code ───────────────────────────────────────────────────────────────

    /**
     * Generates a QR token for the first VERIFIED vehicle (backward-compat).
     */
    public QrTokenResponse generateQrToken(UUID userId) {
        VehicleResponse vehicleResponse = vehicleRepository.findByUserId(userId).stream()
                .filter(v -> v.getStatus() == Vehicle.VehicleStatus.VERIFIED)
                .findFirst()
                .map(vehicleMapper::toResponse)
                .orElseThrow(() -> new BadRequestException("No verified vehicle found. Cannot generate QR code."));

        return buildQrResponse(vehicleResponse);
    }

    /**
     * Generates a QR token for a specific vehicle.
     * Can be called by the vehicle owner or an assigned driver.
     */
    public QrTokenResponse generateQrTokenForVehicle(UUID userId, UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        // Allow both owner and assigned driver to generate QR code
        boolean isOwner = vehicle.getUser().getId().equals(userId);
        boolean isDriver = vehicle.getDriver() != null && vehicle.getDriver().getId().equals(userId);

        if (!isOwner && !isDriver) {
            throw new BadRequestException("You are not authorized to generate QR code for this vehicle");
        }

        if (vehicle.getStatus() != Vehicle.VehicleStatus.VERIFIED) {
            throw new BadRequestException("Vehicle is not verified. Cannot generate QR code.");
        }
        return buildQrResponse(vehicleMapper.toResponse(vehicle));
    }

    private QrTokenResponse buildQrResponse(VehicleResponse vehicleResponse) {
        UUID vehicleId = UUID.fromString(vehicleResponse.getId());
        String token = tokenProvider.generateQrToken(vehicleId, vehicleResponse.getRegistrationNumber());
        long expirationMs = appProperties.getJwt().getQrExpirationMs();
        return QrTokenResponse.builder()
                .token(token)
                .vehicleId(vehicleId.toString())
                .registrationNumber(vehicleResponse.getRegistrationNumber())
                .expiresInSeconds(expirationMs / 1000)
                .build();
    }

    public String regenerateQrToken(UUID userId) {
        return generateQrToken(userId).getToken();
    }

    public String regenerateQrTokenForVehicle(UUID userId, UUID vehicleId) {
        return generateQrTokenForVehicle(userId, vehicleId).getToken();
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    /**
     * Transactions across ALL vehicles of the authenticated customer.
     */
    public Page<TransactionResponse> getTransactionsByUserId(UUID userId, Pageable pageable) {
        return transactionRepository.findByVehicleUserId(userId, pageable)
                .map(transactionMapper::toResponse);
    }

    /**
     * Transactions for a specific vehicle owned by the customer.
     */
    public Page<TransactionResponse> getTransactionsByVehicleId(UUID userId, UUID vehicleId, Pageable pageable) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (!vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("Vehicle does not belong to this user");
        }
        return transactionRepository
                .findByVehicleOrderByTransactionTimestampDesc(vehicle, pageable)
                .map(transactionMapper::toResponse);
    }

    // ── Driver Assignment ─────────────────────────────────────────────────────

    /**
     * Assigns a driver to a vehicle owned by the authenticated customer.
     * The driver must have a registered customer account.
     *
     * @param userId       owner's user ID
     * @param vehicleId    vehicle ID
     * @param driverMobile mobile number of the driver to assign
     * @return updated vehicle response
     * @throws ResourceNotFoundException if vehicle or driver not found
     * @throws BadRequestException if vehicle doesn't belong to user or driver is not a customer
     */
    @Transactional
    public VehicleResponse assignDriver(UUID userId, UUID vehicleId, String driverMobile) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (!vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("Vehicle does not belong to this user");
        }

        User driver = userRepository.findByMobileNumber(driverMobile)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with mobile: " + driverMobile));

        if (driver.getRole() != User.UserRole.CUSTOMER) {
            throw new BadRequestException("Driver must have a customer account");
        }

        if (driver.getId().equals(userId)) {
            throw new BadRequestException("Cannot assign yourself as driver");
        }

        vehicle.setDriver(driver);
        vehicle = vehicleRepository.save(vehicle);

        return vehicleMapper.toResponse(vehicle);
    }

    /**
     * Removes the assigned driver from a vehicle.
     *
     * @param userId    owner's user ID
     * @param vehicleId vehicle ID
     * @return updated vehicle response
     * @throws ResourceNotFoundException if vehicle not found
     * @throws BadRequestException if vehicle doesn't belong to user
     */
    @Transactional
    public VehicleResponse removeDriver(UUID userId, UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (!vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("Vehicle does not belong to this user");
        }

        vehicle.setDriver(null);
        vehicle = vehicleRepository.save(vehicle);

        return vehicleMapper.toResponse(vehicle);
    }

    /**
     * Returns all vehicles where the authenticated user is assigned as driver.
     *
     * @param userId driver's user ID
     * @return list of vehicles where user is the driver
     */
    public List<VehicleResponse> getVehiclesWhereDriver(UUID userId) {
        // This requires a new repository method
        return vehicleRepository.findByDriverId(userId).stream()
                .map(vehicleMapper::toResponse)
                .toList();
    }
}
