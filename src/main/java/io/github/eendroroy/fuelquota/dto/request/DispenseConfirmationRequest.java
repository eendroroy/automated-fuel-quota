package io.github.eendroroy.fuelquota.dto.request;

import io.github.eendroroy.fuelquota.exception.BadRequestException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload confirming that fuel has been physically dispensed.
 *
 * <p>Implements BRD FR-07 / FR-13 flow:
 * <ol>
 *   <li>After authorization is obtained ({@code POST /api/pump/authorize}), the
 *       representative dispenses the actual fuel amount.</li>
 *   <li>The app posts this payload to {@code POST /api/pump/confirm}.</li>
 *   <li>Backend re-validates the QR token (idempotency check), consumes the quota,
 *       records the {@code Transaction}, and returns a confirmation receipt.</li>
 * </ol>
 *
 * <p>This endpoint is idempotent: submitting the same {@code qrToken} twice
 * will raise a {@link BadRequestException}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dispense confirmation sent by the pump representative app after physically dispensing fuel")
public class DispenseConfirmationRequest {

    /**
     * The same JWT QR token that was used in the preceding authorize call.
     * Re-validated to prevent replay/duplicate transactions.
     */
    @NotBlank(message = "QR token is required")
    @Schema(description = "JWT QR token from customer's mobile app (same as used in authorize)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qrToken;

    /** UUID of the fuel station where dispensing occurred. */
    @NotNull(message = "Station ID is required")
    @Schema(description = "UUID of the fuel station", example = "a3f1c2b4-...", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID stationId;

    /** Physical pump/nozzle identifier. */
    @Schema(description = "Physical pump/nozzle identifier", example = "PUMP-02")
    private String pumpId;

    /**
     * Exact fuel volume dispensed in litres.
     * Must be a positive value (≥ 0.01 L).
     * May be less than the authorized amount.
     */
    @NotNull(message = "Dispensed liters is required")
    @DecimalMin(value = "0.01", message = "Dispensed liters must be greater than 0")
    @Schema(description = "Actual fuel amount dispensed in litres", example = "8.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal dispensedLiters;

    /** Type of fuel dispensed (e.g. Petrol, Diesel, Octane). */
    @NotBlank(message = "Fuel type is required")
    @Schema(description = "Type of fuel dispensed", example = "Petrol", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fuelType;

    /** GPS latitude of the pump representative device at the time of dispense. */
    @Schema(description = "GPS latitude at time of dispensing", example = "23.7465")
    private BigDecimal latitude;

    /** GPS longitude of the pump representative device at the time of dispense. */
    @Schema(description = "GPS longitude at time of dispensing", example = "90.3700")
    private BigDecimal longitude;

    /**
     * UUID of the pump representative performing the dispense.
     * Must correspond to an active representative record in the system.
     */
    @NotNull(message = "Pump representative ID is required")
    @Schema(description = "UUID of the pump representative", example = "b5e2d3a1-...", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID pumpRepresentativeId;
}

