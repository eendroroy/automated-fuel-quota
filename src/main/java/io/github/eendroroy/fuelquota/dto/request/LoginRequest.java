package io.github.eendroroy.fuelquota.dto.request;

import io.github.eendroroy.fuelquota.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for user authentication.
 *
 * <p><strong>Customer/Driver Login:</strong> Uses {@code mobileNumber} and {@code password}.
 * <p><strong>Admin Login:</strong> Uses {@code email} and {@code password}.
 *
 * <p>Validated by Jakarta Bean Validation before reaching the service layer.
 * On success, a JWT access token is returned in {@link AuthResponse}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Credentials required to authenticate a user")
public class LoginRequest {

    /**
     * Mobile phone number for customer/driver login or email address for admin login.
     * Depends on context (customer/admin endpoint).
     */
    @NotBlank(message = "Mobile number is required")
    @Schema(description = "User's mobile number (for customer/driver) or email (for admin)", example = "+8801711123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileNumber;

    /**
     * Plain-text password. Minimum 8 characters.
     * Compared against the BCrypt hash stored in the database.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "User's password (min 8 characters)", example = "securePass1!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}

