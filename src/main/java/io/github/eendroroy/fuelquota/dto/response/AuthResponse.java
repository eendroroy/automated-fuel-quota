package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload returned by all authentication endpoints.
 *
 * <p>Contains a signed JWT access token valid for 24 hours and a minimal
 * representation of the authenticated user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authentication response containing JWT token and user details")
public class AuthResponse {

    /**
     * Signed JWT access token.
     * Include in subsequent requests as {@code Authorization: Bearer <token>}.
     */
    @Schema(description = "JWT access token (valid for 24 hours)")
    private String token;

    /** Minimal user information embedded in the response. */
    @Schema(description = "Authenticated user details")
    private UserInfoResponse user;
}

