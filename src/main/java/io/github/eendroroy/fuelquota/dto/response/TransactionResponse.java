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
 * Summary of a single fuel dispense transaction.
 *
 * <p>Returned by:
 * <ul>
 *   <li>{@code GET /api/customer/transactions} – customer's own history (BRD FR-03)</li>
 *   <li>{@code GET /api/admin/transactions} – admin-level transaction report</li>
 * </ul>
 * Sensitive fields such as the raw QR token are intentionally excluded.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Summary of a completed fuel dispense transaction")
public class TransactionResponse {

    /** String-serialised UUID of the transaction record. */
    @Schema(description = "Transaction UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    /** String-serialised UUID of the vehicle that received fuel. */
    @Schema(description = "Vehicle UUID", example = "a3f1c2b4-...")
    private String vehicleId;

    /** Registration plate number of the vehicle. */
    @Schema(description = "Vehicle registration number", example = "DHK-1234")
    private String registrationNumber;

    /** String-serialised UUID of the fuel station. */
    @Schema(description = "Fuel station UUID", example = "b5e2d3a1-...")
    private String stationId;

    /** Human-readable name of the fuel station. */
    @Schema(description = "Fuel station name", example = "ABC Fuel Station Dhanmondi")
    private String stationName;

    /** String-serialised UUID of the pump representative who dispensed the fuel. */
    @Schema(description = "Pump representative UUID (null if unknown)")
    private String pumpRepresentativeId;

    /** Volume of fuel dispensed in litres. */
    @Schema(description = "Fuel volume dispensed in litres", example = "8.50")
    private BigDecimal amountDispensedLiters;

    /** Type of fuel dispensed (e.g. Petrol, Diesel, Octane). */
    @Schema(description = "Type of fuel dispensed", example = "Petrol")
    private String fuelTypeDispensed;

    /** Exact date/time the transaction was recorded. */
    @Schema(description = "Transaction timestamp")
    private LocalDateTime transactionTimestamp;

    /** Vehicle's remaining quota immediately after this transaction. */
    @Schema(description = "Remaining quota in litres after this transaction", example = "13.50")
    private BigDecimal remainingQuotaAfter;

    /** Whether the pump representative's GPS position was within the station geofence. */
    @Schema(description = "True if GPS position was within the station geofence", example = "true")
    private Boolean geofenceVerified;

    /** Transaction status: {@code COMPLETED}, {@code CANCELLED}, or {@code FAILED}. */
    @Schema(description = "Transaction status", example = "COMPLETED")
    private String status;
}

