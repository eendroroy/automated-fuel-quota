package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Driver email is required")
    @Email(message = "Please provide a valid email address")
    @Schema(description = "Email address of the driver (must be a registered customer)", example = "driver@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String driverEmail;
}

