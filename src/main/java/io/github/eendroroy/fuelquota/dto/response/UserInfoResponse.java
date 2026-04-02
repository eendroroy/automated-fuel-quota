package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compact user representation embedded inside {@link AuthResponse}.
 *
 * <p>Intentionally omits the password hash and all sensitive fields.
 * The {@code id} is serialised as a {@code String} for JSON compatibility with
 * front-end UUID handling.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Minimal user details returned as part of the authentication response")
public class UserInfoResponse {

    /** String-serialised UUID of the user. */
    @Schema(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    /** Registered e-mail address (also the login username). */
    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;

    /** Display name of the user. */
    @Schema(description = "User's full name", example = "John Doe")
    private String name;

    /**
     * Authorisation role: {@code CUSTOMER}, {@code ADMIN}, or
     * {@code PUMP_REPRESENTATIVE}.
     */
    @Schema(description = "User role", example = "CUSTOMER")
    private String role;
}

