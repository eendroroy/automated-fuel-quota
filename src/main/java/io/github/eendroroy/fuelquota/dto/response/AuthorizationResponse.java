package io.github.eendroroy.fuelquota.dto.response;

import io.github.eendroroy.fuelquota.enums.AuthorizationDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Result of a fuel authorization request (BRD FR-05, FR-06).
 *
 * <p>Returned by {@code POST /api/pump/authorize}.  The pump representative app
 * must check {@link #decision} before proceeding:
 * <ul>
 *   <li>{@code APPROVED} – dispense exactly {@link #authorizedLiters}.</li>
 *   <li>{@code PARTIAL} – quota is insufficient for the full request; dispense
 *       only {@link #authorizedLiters} (less than requested).</li>
 *   <li>{@code DENIED} – do not dispense; display {@link #message} to the representative.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authorization decision returned after validating a QR token at the pump")
public class AuthorizationResponse {

    /**
     * The authorization decision.
     * See {@link AuthorizationDecision} for semantics.
     */
    @Schema(description = "Authorization decision: APPROVED, PARTIAL, or DENIED", example = "APPROVED")
    private AuthorizationDecision decision;

    /**
     * Maximum litres the system authorizes for this transaction.
     * Will be 0 if {@code decision} is {@code DENIED}.
     */
    @Schema(description = "Authorized fuel quantity in litres", example = "10.00")
    private BigDecimal authorizedLiters;

    /**
     * Vehicle's remaining quota at the time of authorization
     * (before any consumption).
     */
    @Schema(description = "Vehicle's remaining weekly quota in litres", example = "13.50")
    private BigDecimal remainingQuota;

    /**
     * Human-readable outcome message.
     * Contains the deny reason when {@code decision} is {@code DENIED}.
     */
    @Schema(description = "Outcome message (deny reason when DENIED)", example = "Authorization granted")
    private String message;

    /** Registration plate number of the vehicle being served. */
    @Schema(description = "Vehicle registration number", example = "DHK-1234")
    private String vehicleFound;

    /** Manufacturer / model of the vehicle. */
    @Schema(description = "Vehicle make / model", example = "Toyota Corolla")
    private String vehicleMake;

    /** Body colour of the vehicle. */
    @Schema(description = "Vehicle body colour", example = "White")
    private String vehicleColor;

    /** Full name of the vehicle owner. */
    @Schema(description = "Vehicle owner's full name", example = "John Doe")
    private String ownerName;
}

