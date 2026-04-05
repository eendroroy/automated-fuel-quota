package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for sending an OTP to a mobile number during customer registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send an OTP to a mobile number for verification")
public class SendOtpRequest {

    /** Mobile phone number in local Bangladeshi format (01XXXXXXXXX). */
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^01[3-9]\\d{8}$", message = "Please provide a valid mobile number (e.g. 01711123456)")
    @Schema(description = "Mobile number to send OTP to (01XXXXXXXXX)", example = "01711123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileNumber;
}

