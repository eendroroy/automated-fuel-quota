package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for bulk vehicle approval operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk vehicle approval request containing a list of vehicle IDs")
public class BulkApproveRequest {

    @NotEmpty(message = "At least one vehicle ID is required")
    @Schema(description = "List of vehicle UUIDs to approve", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UUID> vehicleIds;
}

