package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for a customer to claim ownership of an already-registered vehicle.
 *
 * <p>Used when a second-hand vehicle is purchased and the new owner wants to
 * transfer the vehicle registration to their account.
 *
 * <p><strong>Future scope:</strong> BRTA API integration will be used to
 * verify ownership before approving the claim automatically.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vehicle ownership claim request (e.g. for second-hand purchases)")
public class ClaimVehicleRequest {

    @NotBlank(message = "Registration number is required")
    @Size(max = 20, message = "Registration number cannot exceed 20 characters")
    @Schema(description = "Registration plate of the vehicle to claim",
            example = "DHK-1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registrationNumber;

    @NotBlank(message = "NID is required")
    @Size(max = 20, message = "NID cannot exceed 20 characters")
    @Schema(description = "NID of the claimant as proof of ownership",
            example = "199012345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String claimantNid;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Schema(description = "Reason for the claim (e.g. purchased second-hand on 2026-01-15)",
            example = "Purchased this vehicle second-hand from previous owner",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}

