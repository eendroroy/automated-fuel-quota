package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction receipt returned after a successful fuel dispense confirmation.
 *
 * <p>Returned by {@code POST /api/pump/confirm} (BRD FR-07, FR-13).
 * Provides a compact confirmation summary that the pump representative app
 * can display to both the customer and the representative.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Transaction receipt returned after fuel is confirmed as dispensed")
public class DispenseConfirmationResponse {

    /** String-serialised UUID of the newly created transaction record. */
    @Schema(description = "Transaction UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String transactionId;

    /**
     * Short human-readable reference code derived from the transaction UUID
     * (format: {@code TXN-XXXXXXXX}).
     */
    @Schema(description = "Short transaction reference code", example = "TXN-550E8400")
    private String transactionReference;

    /** Actual volume of fuel dispensed in litres as confirmed by the representative. */
    @Schema(description = "Fuel volume dispensed in litres", example = "8.50")
    private BigDecimal dispensedLiters;

    /** Vehicle's remaining weekly quota immediately after this transaction. */
    @Schema(description = "Remaining quota in litres after this transaction", example = "15.50")
    private BigDecimal remainingQuota;

    /** Exact date/time the transaction was recorded in the database. */
    @Schema(description = "Transaction timestamp")
    private LocalDateTime timestamp;

    /** Human-readable confirmation message. */
    @Schema(description = "Confirmation message", example = "Transaction completed successfully")
    private String message;
}

