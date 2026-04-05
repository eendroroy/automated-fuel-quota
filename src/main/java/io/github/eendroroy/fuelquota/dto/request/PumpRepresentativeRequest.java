package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating a pump representative account.
 *
 * <p>Used by admin endpoints:
 * <ul>
 *   <li>{@code POST /api/admin/pump-representatives}</li>
 *   <li>{@code PUT /api/admin/pump-representatives/{id}}</li>
 * </ul>
 * The {@code password} field is mandatory on creation but optional on update —
 * if left blank during update the existing password hash is preserved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for creating or updating a pump representative account")
public class PumpRepresentativeRequest {

    /** UUID of the fuel station the representative is assigned to. */
    @NotBlank
    @Schema(description = "UUID of the assigned fuel station", example = "a3f1c2b4-...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stationId;

    /** Full name of the pump representative. */
    @NotBlank
    @Size(max = 100)
    @Schema(description = "Full name", example = "Ali Hassan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /** Contact mobile number. */
    @NotBlank
    @Pattern(regexp = "^01[3-9]\\d{8}$", message = "Please provide a valid mobile number (e.g. 01711123456)")
    @Size(max = 20)
    @Schema(description = "Mobile phone number in local format (01XXXXXXXXX)", example = "01755000001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileNumber;

    /** Work e-mail address — must be unique across all representatives. */
    @NotBlank
    @Email
    @Size(max = 100)
    @Schema(description = "Work email address (unique)", example = "ali.hassan@station.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    /** Internal employee identifier — must be unique. */
    @NotBlank
    @Size(max = 50)
    @Schema(description = "Unique employee ID", example = "EMP-2025-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String employeeId;

    /** Login username for the pump representative mobile app — must be unique. */
    @NotBlank
    @Size(max = 50)
    @Schema(description = "Login username (unique)", example = "ali.hassan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    /**
     * Plain-text password.
     * Required on create; leave blank on update to keep the existing password.
     */
    @Schema(description = "Password (required on create, optional on update)", example = "P@ssw0rd!")
    private String password;
}

