package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for adjusting a vehicle's weekly fuel quota limit.
 *
 * <p>Used by the admin endpoint {@code PUT /api/admin/quotas/{vehicleId}/adjust}.
 * Changes are recorded in the audit log, and the reason field is required for traceability.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for admin quota limit adjustment")
public class QuotaAdjustmentRequest {

    /**
     * New weekly fuel quota limit in litres.
     * Must be a positive value greater than 0.1 L.
     */
    @NotNull(message = "New limit is required")
    @DecimalMin(value = "0.1", message = "Quota limit must be positive")
    @Schema(description = "New weekly quota limit in litres", example = "30.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal newLimitLiters;

    /**
     * Business justification for the adjustment.
     * Stored verbatim in the audit log entry.
     */
    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Schema(description = "Reason for the quota adjustment (stored in audit log)", example = "Vehicle converted to commercial use", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}

