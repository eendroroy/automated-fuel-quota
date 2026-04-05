package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login request for pump representative app.
 * The representative identifies themselves by their mobile number.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Pump representative login using mobile number")
public class PumpRepLoginRequest {

    @NotBlank(message = "Mobile number is required")
    @Schema(description = "Pump representative's mobile number", example = "+8801711123456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileNumber;
}

