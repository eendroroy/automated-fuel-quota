package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for admin vehicle actions that require a reason statement.
 *
 * <p>Used by the following admin endpoints:
 * <ul>
 *   <li>{@code PUT /api/admin/vehicles/{id}/reject}</li>
 *   <li>{@code PUT /api/admin/vehicles/{id}/suspend}</li>
 * </ul>
 * The reason is stored in the audit log entry for traceability.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload carrying the reason for a vehicle administrative action")
public class VehicleActionRequest {

    /**
     * Business justification for rejecting or suspending a vehicle registration.
     * Maximum 500 characters.
     */
    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Schema(description = "Reason for the vehicle action (stored in audit log)", example = "Fraudulent registration documents detected", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}

