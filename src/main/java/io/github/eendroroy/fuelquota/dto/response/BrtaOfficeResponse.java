package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Response DTO for a BRTA office / region lookup entry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "BRTA office entry")
public class BrtaOfficeResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "BRTA office / region code", example = "DHAKA METRO")
    private String brtaCode;

    @Schema(description = "Region description", example = "Dhaka Metropolitan Area")
    private String description;
}

