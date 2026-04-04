package io.github.eendroroy.fuelquota.dto.response;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for quota configuration by registration code.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Quota configuration for a specific vehicle registration code")
public class QuotaConfigByRegistrationCodeResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Configuration UUID")
    private String id;

    @Schema(description = "Vehicle registration code", example = "GA")
    private String registrationCode;

    @Schema(description = "Vehicle class description", example = "Private Cars (1301 to 2000 cc)")
    private String registrationCodeDescription;

    @Schema(description = "Fuel limit per quota period in litres", example = "30.0")
    private BigDecimal limitLitres;

    @Schema(description = "Quota reset period", example = "DAILY")
    private QuotaPeriod quotaPeriod;

    @Schema(description = "Description or notes")
    private String description;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}

