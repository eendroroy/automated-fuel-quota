package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login request for pump representative demo app.
 * The representative identifies themselves by their employee ID.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Pump representative login using employee ID")
public class PumpRepLoginRequest {

    @NotBlank(message = "Employee ID is required")
    @Schema(description = "Unique employee ID assigned by the station management", example = "EMP-001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String employeeId;
}

