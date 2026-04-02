package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Manual authorization request — used when the pump representative types the
 * vehicle registration number directly instead of scanning a QR code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Manual authorization request by vehicle registration number")
public class ManualAuthorizationRequest {

    @NotBlank(message = "Registration number is required")
    @Schema(description = "Vehicle registration plate number", example = "DHK-KA-11-1234",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String registrationNumber;

    @NotNull(message = "Station ID is required")
    @Schema(description = "UUID of the fuel station", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID stationId;

    @Schema(description = "Requested fuel quantity in litres (defaults to 50 if omitted)", example = "10.00")
    private BigDecimal requestedLiters;
}

