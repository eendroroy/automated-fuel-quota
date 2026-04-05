package io.github.eendroroy.fuelquota.dto.request;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating or updating a quota configuration set.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Quota configuration set — applies a shared limit and period to multiple registration codes")
public class QuotaConfigSetRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Config set name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    @Schema(description = "Human-readable name for this config set", example = "Private Cars", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "Limit litres is required")
    @DecimalMin(value = "0.01", message = "Limit must be greater than zero")
    @DecimalMax(value = "10000.0", message = "Limit cannot exceed 10000 litres")
    @Schema(description = "Fuel limit per quota period in litres", example = "30.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal limitLitres;

    @NotNull(message = "Quota period is required")
    @Schema(description = "Quota reset period", example = "WEEKLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private QuotaPeriod quotaPeriod;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Optional description or notes", example = "Standard weekly quota for private cars above 1300cc")
    private String description;

    @NotEmpty(message = "At least one registration code is required")
    @Schema(description = "Vehicle registration codes covered by this set", example = "[\"GA\", \"KHA\", \"BHA\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotBlank @Size(max = 10) String> registrationCodes;
}

