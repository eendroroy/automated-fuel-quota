package io.github.eendroroy.fuelquota.dto.response;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for the system quota configuration.
 *
 * <p>Returned by {@code GET /api/admin/quota-config} and
 * {@code PUT /api/admin/quota-config}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Current quota configuration")
public class QuotaConfigResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Configuration UUID")
    private String id;

    @Schema(description = "Fuel limit per quota period in litres", example = "24.0")
    private BigDecimal limitLitres;

    @Schema(description = "Geofence radius in metres", example = "100")
    private Integer geofenceRadiusMeters;

    @Schema(description = "Quota reset period", example = "WEEKLY")
    private QuotaPeriod quotaPeriod;

    @Schema(description = "Spring cron expression for the reset scheduler", example = "0 0 0 ? * SUN")
    private String resetCronExpression;

    @Schema(description = "Admin change notes / description")
    private String description;

    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last modification timestamp")
    private LocalDateTime updatedAt;
}

