package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating a user's account status.
 *
 * <p>Used by the admin user-management API to suspend or reactivate accounts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to change a user's account status")
public class UserStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Schema(description = "New account status (ACTIVE or SUSPENDED)", example = "SUSPENDED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "Optional reason for the status change", example = "Violation of terms of service")
    private String reason;
}

