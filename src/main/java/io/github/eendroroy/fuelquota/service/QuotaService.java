package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.config.AppProperties;
import io.github.eendroroy.fuelquota.entity.Quota;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.enums.AuthorizationDecision;
import io.github.eendroroy.fuelquota.repository.QuotaRepository;
import io.github.eendroroy.fuelquota.repository.VehicleRepository;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;
import io.github.eendroroy.fuelquota.exception.BadRequestException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Core service for weekly fuel quota management.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Quota retrieval and caching (by vehicle ID and registration number).</li>
 *   <li>Authorization logic — decides whether dispensing is APPROVED, PARTIAL, or DENIED.</li>
 *   <li>Quota consumption after a confirmed dispense.</li>
 *   <li>Scheduled weekly reset every Sunday at 00:00.</li>
 *   <li>Admin operations: manual reset and limit adjustment.</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class QuotaService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaService.class);

    private final QuotaRepository quotaRepository;
    private final VehicleRepository vehicleRepository;
    private final AppProperties appProperties;
    private final QuotaConfigService quotaConfigService;

    /**
     * Retrieves the quota for a vehicle by its UUID, with Redis caching.
     *
     * @param vehicleId UUID of the vehicle
     * @return the associated {@link Quota}
     * @throws ResourceNotFoundException if no quota exists for the vehicle
     */
    @Cacheable(value = "quota", key = "#vehicleId")
    public Quota getQuotaByVehicleId(UUID vehicleId) {
        return quotaRepository.findByVehicleId(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Quota not found for vehicle"));
    }

    /**
     * Retrieves the quota for a vehicle by its registration number, with Redis caching.
     *
     * @param registrationNumber unique registration plate number
     * @return the associated {@link Quota}
     * @throws ResourceNotFoundException if no quota exists for the given registration number
     */
    @Cacheable(value = "quota", key = "#registrationNumber")
    public Quota getQuotaByRegistrationNumber(String registrationNumber) {
        return quotaRepository.findByVehicleRegistrationNumber(registrationNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Quota not found for vehicle: " + registrationNumber));
    }

    /**
     * Core quota authorization logic (BRD FR-11, FR-12).
     *
     * <p>Evaluates vehicle eligibility, quota availability, and computes
     * the maximum dispense amount (supporting partial dispense).
     *
     * @param registrationNumber vehicle registration plate
     * @param requestedLiters    fuel quantity requested by the customer
     * @return a {@link QuotaAuthorizationResult} with the decision and authorized amount
     */
    public QuotaAuthorizationResult authorizeQuota(String registrationNumber, BigDecimal requestedLiters) {
        Quota quota = getQuotaByRegistrationNumber(registrationNumber);
        Vehicle vehicle = quota.getVehicle();

        if (vehicle.getStatus() != Vehicle.VehicleStatus.VERIFIED) {
            return new QuotaAuthorizationResult(AuthorizationDecision.DENIED, BigDecimal.ZERO,
                    quota.getRemainingLiters(), "Vehicle is not verified");
        }

        if (!quota.hasAvailableQuota()) {
            return new QuotaAuthorizationResult(AuthorizationDecision.DENIED, BigDecimal.ZERO,
                    quota.getRemainingLiters(), "No quota available");
        }

        BigDecimal authorizedLiters = quota.getMaxDispensableAmount(requestedLiters);

        if (authorizedLiters.compareTo(BigDecimal.ZERO) == 0) {
            return new QuotaAuthorizationResult(AuthorizationDecision.DENIED, BigDecimal.ZERO,
                    quota.getRemainingLiters(), "Quota exhausted");
        }

        AuthorizationDecision decision = authorizedLiters.compareTo(requestedLiters) < 0
                ? AuthorizationDecision.PARTIAL : AuthorizationDecision.APPROVED;

        return new QuotaAuthorizationResult(decision, authorizedLiters, quota.getRemainingLiters(), null);
    }

    /**
     * Consumes the specified amount of quota for a vehicle (BRD FR-13).
     * Evicts the quota cache entry for the vehicle.
     *
     * @param vehicleId       UUID of the vehicle
     * @param litersDispensed actual fuel dispensed (must be positive and within available quota)
     * @return the updated {@link Quota}
     * @throws BadRequestException if the dispensed amount exceeds available quota
     */
    @CacheEvict(value = "quota", key = "#vehicleId")
    public Quota consumeQuota(UUID vehicleId, BigDecimal litersDispensed) {
        Quota quota = quotaRepository.findByVehicleId(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Quota not found"));

        if (!quota.canDispense(litersDispensed)) {
            throw new BadRequestException("Insufficient quota available");
        }

        quota.consumeQuota(litersDispensed);
        return quotaRepository.save(quota);
    }

    /**
     * Scheduled quota reset job. Triggered by cron expression from
     * {@code app.quota.reset-cron-expression}. The next reset timestamp
     * is computed based on {@code app.quota.period}.
     */
    @Scheduled(cron = "${app.quota.reset-cron-expression}")
    @CacheEvict(value = "quota", allEntries = true)
    public void resetWeeklyQuotas() {
        logger.info("Starting periodic quota reset job...");
        try {
            LocalDateTime now = LocalDateTime.now();
            QuotaPeriod period = quotaConfigService.getDefaultPeriod();
            LocalDateTime nextResetTime = Quota.calculateNextResetTime(period);
            int resetCount = quotaRepository.resetExpiredQuotas(now, nextResetTime, now);
            logger.info("Quota reset completed ({} period). {} quotas were reset.", period, resetCount);
        } catch (Exception e) {
            logger.error("Error occurred during quota reset", e);
            throw e;
        }
    }

    /**
     * Manually resets a single vehicle's quota (admin emergency function).
     * Evicts the quota cache entry for the vehicle.
     *
     * @param vehicleId UUID of the vehicle whose quota should be reset
     * @throws ResourceNotFoundException if no quota exists for the vehicle
     */
    @CacheEvict(value = "quota", key = "#vehicleId")
    public void manualResetQuota(UUID vehicleId) {
        Quota quota = quotaRepository.findByVehicleId(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Quota not found"));
        quota.resetQuota();
        quotaRepository.save(quota);
        logger.info("Manual quota reset performed for vehicle ID: {}", vehicleId);
    }

    /**
     * Adjusts the weekly fuel quota limit for a specific vehicle (admin function).
     * Evicts the quota cache entry for the vehicle.
     *
     * @param vehicleId       UUID of the vehicle
     * @param newLimitLiters  new weekly limit in litres (must be positive)
     * @param reason          business justification for the change (stored in audit log)
     * @return the updated {@link Quota}
     * @throws BadRequestException       if {@code newLimitLiters} is not positive
     * @throws ResourceNotFoundException if no quota exists for the vehicle
     */
    @CacheEvict(value = "quota", key = "#vehicleId")
    public Quota adjustQuotaLimit(UUID vehicleId, BigDecimal newLimitLiters, String reason) {
        if (newLimitLiters.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quota limit must be positive");
        }
        Quota quota = quotaRepository.findByVehicleId(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Quota not found"));
        quota.setLimitLiters(newLimitLiters);
        quota = quotaRepository.save(quota);
        logger.info("Quota limit adjusted for vehicle ID: {} to {} liters. Reason: {}",
                vehicleId, newLimitLiters, reason);
        return quota;
    }

    /**
     * Creates and persists a new quota for a vehicle that has just been approved.
     * The weekly limit is read from {@code app.quota.weekly-limit-litres}.
     *
     * @param vehicle the newly approved {@link Vehicle}
     * @return the newly created {@link Quota}
     * @throws BadRequestException if a quota already exists for the vehicle
     */
    public Quota createQuotaForVehicle(Vehicle vehicle) {
        if (quotaRepository.findByVehicle(vehicle).isPresent()) {
            throw new BadRequestException("Quota already exists for this vehicle");
        }
        BigDecimal limit = quotaConfigService.getDefaultLimitLitres();
        QuotaPeriod period = quotaConfigService.getDefaultPeriod();
        return quotaRepository.save(new Quota(vehicle, limit, period));
    }

    /**
     * Returns all quotas whose reset timestamp is in the past.
     *
     * @return list of expired quotas
     */
    public List<Quota> getQuotasToReset() {
        return quotaRepository.findQuotasToReset(LocalDateTime.now());
    }

    /**
     * Evicts all quota cache entries. Called when global cache invalidation is needed.
     */
    @CacheEvict(value = "quota", allEntries = true)
    public void clearQuotaCache() {
        logger.debug("Quota cache cleared");
    }

    /**
     * Activates a vehicle's quota after the vehicle is approved by an admin.
     * Evicts the quota cache entry for the vehicle.
     *
     * @param vehicleId UUID of the approved vehicle
     * @throws ResourceNotFoundException if no quota exists for the vehicle
     */
    @CacheEvict(value = "quota", key = "#vehicleId")
    public void activateQuota(UUID vehicleId) {
        Quota quota = quotaRepository.findByVehicleId(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Quota not found for vehicle"));
        quota.setStatus(Quota.QuotaStatus.ACTIVE);
        quotaRepository.save(quota);
        logger.info("Quota activated for vehicle ID: {}", vehicleId);
    }

    /**
     * Suspends a vehicle's quota when the vehicle is suspended by an admin.
     * Evicts the quota cache entry for the vehicle.
     *
     * @param vehicleId UUID of the suspended vehicle
     * @throws ResourceNotFoundException if no quota exists for the vehicle
     */
    @CacheEvict(value = "quota", key = "#vehicleId")
    public void suspendQuota(UUID vehicleId) {
        Quota quota = quotaRepository.findByVehicleId(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Quota not found for vehicle"));
        quota.setStatus(Quota.QuotaStatus.SUSPENDED);
        quotaRepository.save(quota);
        logger.info("Quota suspended for vehicle ID: {}", vehicleId);
    }

    /**
     * Returns a paginated list of all quotas, optionally filtered by registration number
     * or owner name.
     *
     * @param search   optional search string (matched against registration number / owner name)
     * @param pageable pagination parameters
     * @return paginated {@link Quota} results
     */
    @Transactional(readOnly = true)
    public Page<Quota> getAllQuotas(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return quotaRepository.findAll(pageable);
        }
        return quotaRepository.findQuotasWithSearch(search, pageable);
    }

    // ── Inner result type ─────────────────────────────────────────────────────

    /**
     * Carries the result of a quota authorization evaluation.
     * Used internally between {@link QuotaService} and {@link PumpService}.
     */
    @Getter
    public static class QuotaAuthorizationResult {

        /** The authorization decision. */
        private final AuthorizationDecision decision;

        /** The maximum litres authorized for dispensing. */
        private final BigDecimal authorizedLiters;

        /** The vehicle's remaining quota at the time of evaluation. */
        private final BigDecimal remainingQuota;

        /** Human-readable reason for denial ({@code null} when approved or partial). */
        private final String denyReason;

        /**
         * Constructs a quota authorization result.
         *
         * @param decision         the authorization decision
         * @param authorizedLiters authorized fuel quantity
         * @param remainingQuota   remaining quota before consumption
         * @param denyReason       denial reason, or {@code null}
         */
        public QuotaAuthorizationResult(AuthorizationDecision decision, BigDecimal authorizedLiters,
                                        BigDecimal remainingQuota, String denyReason) {
            this.decision = decision;
            this.authorizedLiters = authorizedLiters;
            this.remainingQuota = remainingQuota;
            this.denyReason = denyReason;
        }

        /** @return {@code true} if the decision is {@link AuthorizationDecision#APPROVED} */
        public boolean isApproved() { return decision == AuthorizationDecision.APPROVED; }

        /** @return {@code true} if the decision is {@link AuthorizationDecision#PARTIAL} */
        public boolean isPartial()  { return decision == AuthorizationDecision.PARTIAL; }

        /** @return {@code true} if the decision is {@link AuthorizationDecision#DENIED} */
        public boolean isDenied()   { return decision == AuthorizationDecision.DENIED; }
    }
}
