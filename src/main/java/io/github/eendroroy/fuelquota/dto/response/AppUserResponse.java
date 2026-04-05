package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Full user representation returned by the admin user-management API.
 *
 * <p>Intentionally omits the password hash and all internal Spring Security
 * fields. The {@code id} is serialised as a {@code String} for JSON
 * compatibility with front-end UUID handling.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User details for admin management")
public class AppUserResponse {

    @Schema(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "User's full name", example = "John Doe")
    private String name;

    @Schema(description = "User's mobile number", example = "01711123456")
    private String mobileNumber;

    @Schema(description = "User's email address", example = "john@example.com")
    private String email;

    @Schema(description = "National Identity Document number", example = "199012345678")
    private String nid;

    @Schema(description = "User role", example = "CUSTOMER")
    private String role;

    @Schema(description = "Account status", example = "ACTIVE")
    private String status;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of most recent successful login")
    private LocalDateTime lastLoginTimestamp;

    @Schema(description = "Number of vehicles registered to this user", example = "2")
    private Long vehicleCount;
}

