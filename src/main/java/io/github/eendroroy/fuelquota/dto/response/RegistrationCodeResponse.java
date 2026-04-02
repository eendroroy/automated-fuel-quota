package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Response DTO for a vehicle registration code lookup entry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vehicle registration code entry")
public class RegistrationCodeResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Code prefix", example = "GA")
    private String code;

    @Schema(description = "Vehicle category description", example = "Private Cars (1301 to 2000 cc)")
    private String description;
}

