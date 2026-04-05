package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Quota status snapshot for a specific vehicle.
 *
 * <p>Returned by:
 * <ul>
 *   <li>{@code GET /api/customer/quota} – customer's own quota (BRD FR-02)</li>
 *   <li>{@code GET /api/admin/quotas} – paginated quota list for admin</li>
 *   <li>{@code PUT /api/admin/quotas/{vehicleId}/adjust} – after admin adjustment</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Periodic fuel quota status for a vehicle")
public class QuotaResponse {

    /** String-serialised UUID of the quota record. */
    @Schema(description = "Quota record UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    /** String-serialised UUID of the associated vehicle. */
    @Schema(description = "Vehicle UUID", example = "a3f1c2b4-...")
    private String vehicleId;

    /** Registration plate number of the vehicle. */
    @Schema(description = "Vehicle registration number", example = "DHK-1234")
    private String registrationNumber;

    /** Full name of the vehicle owner. */
    @Schema(description = "Vehicle owner's full name", example = "John Doe")
    private String ownerName;

    /** Maximum fuel (litres) the vehicle may receive per quota period. */
    @Schema(description = "Fuel quota limit in litres per period", example = "24.00")
    private BigDecimal limitLiters;

    /** Cumulative litres consumed since the last quota reset. */
    @Schema(description = "Litres consumed this period", example = "10.50")
    private BigDecimal usedLiters;

    /** Litres still available before the next quota reset. */
    @Schema(description = "Litres remaining this period", example = "13.50")
    private BigDecimal remainingLiters;

    /** The configured reset period: DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY. */
    @Schema(description = "Quota reset period", example = "WEEKLY")
    private String period;

    /** Date/time of the next scheduled quota reset. */
    @Schema(description = "Timestamp of the next quota reset")
    private LocalDateTime resetTimestamp;

    /** Timestamp of the most recent fuel transaction against this quota. */
    @Schema(description = "Timestamp of the last fuel transaction")
    private LocalDateTime lastTransactionTimestamp;

    /** Operational status of the quota: {@code ACTIVE}, {@code SUSPENDED}, or {@code EXPIRED}. */
    @Schema(description = "Quota status: ACTIVE, SUSPENDED, or EXPIRED", example = "ACTIVE")
    private String status;

    /** True when an admin manually adjusted this quota — excluded from bulk config-set sync. */
    @Schema(description = "Whether this quota was individually overridden by an admin")
    private boolean individuallyOverridden;
}
