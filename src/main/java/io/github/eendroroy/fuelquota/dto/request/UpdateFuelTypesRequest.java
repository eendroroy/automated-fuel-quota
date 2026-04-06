package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for updating the secondary fuel types of a vehicle.
 *
 * <p>The primary fuel type is immutable after registration; only the secondary
 * (alternative) fuel types may be updated via this request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Update secondary fuel types for a vehicle")
public class UpdateFuelTypesRequest {

    /**
     * Replacement list of secondary fuel types.
     * Pass an empty list to remove all secondary fuel types.
     */
    @Schema(description = "Secondary fuel types (e.g. CNG for a Petrol vehicle)", example = "[\"CNG\"]")
    private List<String> secondaryFuelTypes;
}

