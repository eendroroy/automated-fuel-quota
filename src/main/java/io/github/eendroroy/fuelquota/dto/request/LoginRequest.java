package io.github.eendroroy.fuelquota.dto.request;

import io.github.eendroroy.fuelquota.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for user authentication (customer or admin login).
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
     * Registered e-mail address of the user.
     * Must be a well-formed e-mail according to RFC 5321.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    @Schema(description = "User's registered email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    /**
     * Plain-text password. Minimum 8 characters.
     * Compared against the BCrypt hash stored in the database.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "User's password (min 8 characters)", example = "securePass1!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}

