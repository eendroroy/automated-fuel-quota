package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request DTO for assigning a driver to a vehicle.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to assign a driver to a vehicle")
public class AssignDriverRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Driver mobile number is required")
    @Pattern(regexp = "^01[3-9]\\d{8}$", message = "Please provide a valid Bangladesh mobile number (e.g. 01711123456)")
    @Schema(description = "Mobile number of the driver (must be a registered customer)", example = "01711123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String driverMobile;
}
