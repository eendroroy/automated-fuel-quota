package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Response DTO for a vehicle ownership claim.
 *
 * <p>Returned when a customer submits or an admin reviews a claim.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vehicle ownership claim details")
public class VehicleClaimResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Claim UUID")
    private String id;

    @Schema(description = "UUID of the claimed vehicle")
    private String vehicleId;

    @Schema(description = "Registration number of the claimed vehicle", example = "DHK-1234")
    private String registrationNumber;

    @Schema(description = "UUID of the user who submitted the claim")
    private String claimantUserId;

    @Schema(description = "Name of the claimant")
    private String claimantName;

    @Schema(description = "NID provided by the claimant")
    private String claimantNid;

    @Schema(description = "Reason provided by the claimant")
    private String reason;

    @Schema(description = "Claim status: PENDING, APPROVED, REJECTED", example = "PENDING")
    private String status;

    @Schema(description = "Admin review notes (present when approved or rejected)")
    private String adminNotes;

    @Schema(description = "Timestamp when the claim was submitted")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of last status update")
    private LocalDateTime updatedAt;
}

