package io.github.eendroroy.fuelquota.mapper;

import io.github.eendroroy.fuelquota.dto.response.QuotaResponse;
import io.github.eendroroy.fuelquota.entity.Quota;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Quota} entities to {@link QuotaResponse} DTOs.
 *
 * <p>Navigates the {@code quota → vehicle} association (EAGER-loaded) to populate
 * the registration number and owner name fields in the response.
 */
@Component
public class QuotaMapper {

    /**
     * Converts a {@link Quota} entity to a {@link QuotaResponse}.
     *
     * @param quota the source entity (must not be {@code null})
     * @return a populated {@link QuotaResponse}
     */
    public QuotaResponse toResponse(Quota quota) {
        return QuotaResponse.builder()
                .id(quota.getId().toString())
                .vehicleId(quota.getVehicle().getId().toString())
                .registrationNumber(quota.getVehicle().getRegistrationNumber())
                .ownerName(quota.getVehicle().getOwnerName())
                .limitLiters(quota.getLimitLiters())
                .usedLiters(quota.getUsedLiters())
                .remainingLiters(quota.getRemainingLiters())
                .period(quota.getPeriod() != null ? quota.getPeriod().name() : "WEEKLY")
                .resetTimestamp(quota.getResetTimestamp())
                .lastTransactionTimestamp(quota.getLastTransactionTimestamp())
                .status(quota.getStatus().name())
                .individuallyOverridden(quota.isIndividuallyOverridden())
                .build();
    }
}

