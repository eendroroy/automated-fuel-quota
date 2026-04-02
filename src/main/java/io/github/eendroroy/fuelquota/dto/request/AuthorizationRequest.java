package io.github.eendroroy.fuelquota.dto.request;

import io.github.eendroroy.fuelquota.dto.response.AuthorizationResponse;
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
 * Request payload sent by the pump representative mobile app to authorize fuel dispensing.
 *
 * <p>Implements BRD FR-04 / FR-05 flow:
 * <ol>
 *   <li>Representative scans the customer's QR code (the raw JWT string).</li>
 *   <li>Mobile app sends this payload to {@code POST /api/pump/authorize}.</li>
 *   <li>Backend validates the QR token, checks vehicle status, GPS geofence,
 *       and available quota before returning an {@link AuthorizationResponse}.</li>
 * </ol>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authorization request sent by the pump representative app after scanning a customer QR code")
public class AuthorizationRequest {

    /**
     * JWT QR token scanned from the customer's app.
     * Contains the vehicle ID and registration number, signed with the system secret.
     * Expires after 1 hour (configurable via {@code app.jwt.qr-expiration-ms}).
     */
    @NotBlank(message = "QR token is required")
    @Schema(description = "JWT QR token from customer's mobile app", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qrToken;

    /** UUID of the fuel station where dispensing is taking place. */
    @NotNull(message = "Station ID is required")
    @Schema(description = "UUID of the fuel station", example = "a3f1c2b4-...", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID stationId;

    /** Identifier of the physical pump / nozzle at the station. */
    @Schema(description = "Physical pump/nozzle identifier at the station", example = "PUMP-02")
    private String pumpId;

    /**
     * GPS latitude reported by the pump representative device.
     * Used for geofence validation (BRD FR-10).
     * Optional — if omitted, geofence check is skipped.
     */
    @Schema(description = "GPS latitude of the pump representative's device", example = "23.7465")
    private BigDecimal latitude;

    /**
     * GPS longitude reported by the pump representative device.
     * Used for geofence validation (BRD FR-10).
     */
    @Schema(description = "GPS longitude of the pump representative's device", example = "90.3700")
    private BigDecimal longitude;

    /**
     * Fuel quantity (litres) the customer wishes to receive.
     * If omitted or {@code null}, defaults to 50 L (system default).
     * Partial authorization may be returned if remaining quota is less than requested.
     */
    @Schema(description = "Requested fuel quantity in litres (defaults to 50 if omitted)", example = "10.00")
    private BigDecimal requestedLiters;
}

