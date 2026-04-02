package io.github.eendroroy.fuelquota.dto.request;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for creating or updating the default quota configuration.
 *
 * <p>Submitted by admins via {@code PUT /api/admin/quota-config}.
 * Changes take effect immediately for all newly created quotas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Quota configuration update payload")
public class QuotaConfigRequest {

    @NotNull(message = "Limit litres is required")
    @DecimalMin(value = "1.0", message = "Limit must be at least 1 litre")
    @DecimalMax(value = "1000.0", message = "Limit cannot exceed 1000 litres")
    @Schema(description = "Fuel limit in litres per quota period", example = "24.0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal limitLitres;

    @NotNull(message = "Geofence radius is required")
    @Min(value = 10, message = "Geofence radius must be at least 10 metres")
    @Max(value = 10000, message = "Geofence radius cannot exceed 10 000 metres")
    @Schema(description = "Geofence radius in metres for pump proximity validation", example = "100",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer geofenceRadiusMeters;

    @NotNull(message = "Quota period is required")
    @Schema(description = "Quota reset period", example = "WEEKLY",
            allowableValues = {"DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private QuotaPeriod quotaPeriod;

    @NotBlank(message = "Reset cron expression is required")
    @Schema(description = "Spring cron expression for quota reset job", example = "0 0 0 ? * SUN",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String resetCronExpression;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Optional change notes or description", example = "Increased limit for Eid period")
    private String description;
}

