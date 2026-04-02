package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response containing the QR token and associated vehicle metadata.
 *
 * <p>Returned by {@code GET /api/customer/qr-code} (BRD FR-01).
 * The {@code token} is a short-lived JWT (default 1 hour) encoding the
 * vehicle ID and registration number. It is presented to the pump
 * representative's app as a QR code to initiate the authorization flow.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "QR token generated for the customer to present at a fuel pump")
public class QrTokenResponse {

    /**
     * Signed JWT QR token.
     * Encode this value as a QR code image on the front-end.
     */
    @Schema(description = "Signed JWT QR token (1-hour lifetime by default)")
    private String token;

    /** String-serialised UUID of the vehicle this token was issued for. */
    @Schema(description = "UUID of the vehicle", example = "550e8400-e29b-41d4-a716-446655440000")
    private String vehicleId;

    /** Registration plate number embedded in the token. */
    @Schema(description = "Vehicle registration number", example = "DHK-1234")
    private String registrationNumber;

    /**
     * Seconds until the token expires.
     * Derived from {@code app.jwt.qr-expiration-ms}.
     */
    @Schema(description = "Token expiry duration in seconds", example = "3600")
    private long expiresInSeconds;
}

