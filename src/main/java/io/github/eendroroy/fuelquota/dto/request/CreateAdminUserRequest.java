package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for creating a new ADMIN user account.
 */
@Getter
@Setter
@Schema(description = "Payload for creating a new admin user")
public class CreateAdminUserRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    @Schema(description = "Admin's full name", example = "Jane Smith")
    private String name;

    @NotBlank
    @Email
    @Size(max = 100)
    @Schema(description = "Admin's unique email address", example = "jane@fuelquota.gov")
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(description = "Account password (min 8 characters)", example = "S3cur3P@ss!")
    private String password;
}

