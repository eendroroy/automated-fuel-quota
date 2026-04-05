package io.github.eendroroy.fuelquota.dto.response;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a quota configuration set.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Quota configuration set response")
public class QuotaConfigSetResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Configuration set UUID")
    private String id;

    @Schema(description = "Human-readable name for this config set", example = "Private Cars")
    private String name;

    @Schema(description = "Fuel limit per quota period in litres", example = "30.0")
    private BigDecimal limitLitres;

    @Schema(description = "Quota reset period", example = "WEEKLY")
    private QuotaPeriod quotaPeriod;

    @Schema(description = "Description or notes")
    private String description;

    @Schema(description = "Registration codes covered by this set", example = "[\"GA\", \"KHA\"]")
    private List<String> registrationCodes;

    @Schema(description = "Registration codes with descriptions")
    private List<RegistrationCodeInfo> registrationCodeDetails;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Registration code with its description")
    public static class RegistrationCodeInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Registration code", example = "GA")
        private String code;

        @Schema(description = "Description", example = "Private Cars (1301 to 2000 cc)")
        private String description;
    }
}

