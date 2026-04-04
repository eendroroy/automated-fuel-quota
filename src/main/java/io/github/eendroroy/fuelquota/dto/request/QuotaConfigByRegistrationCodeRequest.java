package io.github.eendroroy.fuelquota.dto.request;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Request DTO for creating or updating quota configuration by registration code.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Quota configuration for a specific vehicle registration code")
public class QuotaConfigByRegistrationCodeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Registration code is required")
    @Size(max = 10, message = "Registration code cannot exceed 10 characters")
    @Schema(description = "Vehicle registration code (e.g., GA, LA, KHA)", example = "GA", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registrationCode;

    @NotNull(message = "Limit litres is required")
    @DecimalMin(value = "0.01", message = "Limit must be greater than zero")
    @Schema(description = "Fuel limit per quota period in litres", example = "30.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal limitLitres;

    @NotNull(message = "Quota period is required")
    @Schema(description = "Quota reset period", example = "DAILY", requiredMode = Schema.RequiredMode.REQUIRED)
    private QuotaPeriod quotaPeriod;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Description or notes for this configuration", example = "Private cars daily quota")
    private String description;
}

