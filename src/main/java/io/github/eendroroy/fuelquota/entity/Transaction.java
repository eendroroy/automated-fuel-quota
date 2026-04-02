package io.github.eendroroy.fuelquota.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a single fuel dispense event.
 *
 * <p>Created when a pump representative confirms fuel dispensing via
 * {@code POST /api/pump/confirm} (BRD FR-13). Immutable once created —
 * status changes are the only permitted mutation.
 *
 * <p>Key integrity guarantees:
 * <ul>
 *   <li>The {@code qrTokenUsed} field ensures idempotency (one transaction per QR token).</li>
 *   <li>{@code geofenceVerified} records whether the representative's GPS was within
 *       the station radius at the time of dispensing.</li>
 *   <li>{@code remainingQuotaAfter} is a snapshot taken immediately after consumption
 *       for audit/receipt purposes.</li>
 * </ul>
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_vehicle_timestamp", columnList = "vehicle_id, transaction_timestamp"),
    @Index(name = "idx_station_timestamp", columnList = "station_id, transaction_timestamp"),
    @Index(name = "idx_timestamp",         columnList = "transaction_timestamp")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"vehicle", "station", "pumpRepresentative"})
public class Transaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Vehicle that received the fuel. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /** Station at which the fuel was dispensed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private FuelStation station;

    /** Identifier of the physical pump/nozzle used. */
    @Column(name = "pump_id", length = 30)
    private String pumpId;

    /** Volume of fuel actually dispensed (litres). */
    @Column(name = "amount_dispensed_liters", nullable = false, precision = 5, scale = 2)
    private BigDecimal amountDispensedLiters;

    /** Type of fuel dispensed (e.g. Petrol, Diesel, Octane). */
    @Column(name = "fuel_type_dispensed", nullable = false, length = 30)
    private String fuelTypeDispensed;

    /** Exact timestamp when the transaction was recorded. */
    @Column(name = "transaction_timestamp", nullable = false)
    private LocalDateTime transactionTimestamp;

    /** The {@link User} (pump representative) who performed the dispense. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pump_representative_id", nullable = false)
    private User pumpRepresentative;

    /** GPS latitude of the pump representative's device at dispense time. */
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    /** GPS longitude of the pump representative's device at dispense time. */
    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    /**
     * Whether the representative's GPS was within the station's geofence radius.
     * Calculated at transaction creation using the Haversine formula.
     */
    @Column(name = "geofence_verified", nullable = false)
    private Boolean geofenceVerified = false;

    /**
     * Raw JWT QR token consumed during this transaction.
     * Stored to enforce idempotency (duplicate confirmation prevention).
     */
    @Column(name = "qr_token_used", nullable = false, length = 500)
    private String qrTokenUsed;

    /**
     * Snapshot of the vehicle's remaining quota immediately after this transaction.
     * Stored for receipt and audit purposes.
     */
    @Column(name = "remaining_quota_after", nullable = false, precision = 5, scale = 2)
    private BigDecimal remainingQuotaAfter;

    /** Transaction lifecycle status. Defaults to {@link TransactionStatus#COMPLETED}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    /** Optional notes attached to the transaction record. */
    @Column(name = "notes", length = 500)
    private String notes;

    /** Automatically populated by Spring Data JPA auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Defines the lifecycle states of a transaction.
     */
    public enum TransactionStatus {
        /** Fuel was successfully dispensed and quota consumed. */
        COMPLETED,
        /** Transaction was cancelled before fuel was dispensed. */
        CANCELLED,
        /** An error prevented the transaction from completing. */
        FAILED
    }

    /**
     * Primary constructor used when creating a transaction after dispensing.
     * Automatically sets {@link #transactionTimestamp} and computes
     * {@link #geofenceVerified} using the station's Haversine geofence check.
     *
     * @param vehicle              vehicle that received fuel
     * @param station              station where dispensing took place
     * @param amountDispensedLiters exact dispensed volume in litres
     * @param fuelTypeDispensed    type of fuel dispensed
     * @param pumpRepresentative   user who performed the dispense
     * @param latitude             representative's GPS latitude
     * @param longitude            representative's GPS longitude
     * @param qrTokenUsed          QR JWT token used for authorization
     * @param remainingQuotaAfter  vehicle's remaining quota after this transaction
     */
    public Transaction(Vehicle vehicle, FuelStation station, BigDecimal amountDispensedLiters,
                       String fuelTypeDispensed, User pumpRepresentative, BigDecimal latitude,
                       BigDecimal longitude, String qrTokenUsed, BigDecimal remainingQuotaAfter) {
        this.vehicle = vehicle;
        this.station = station;
        this.amountDispensedLiters = amountDispensedLiters;
        this.fuelTypeDispensed = fuelTypeDispensed;
        this.transactionTimestamp = LocalDateTime.now();
        this.pumpRepresentative = pumpRepresentative;
        this.latitude = latitude;
        this.longitude = longitude;
        this.qrTokenUsed = qrTokenUsed;
        this.remainingQuotaAfter = remainingQuotaAfter;
        this.geofenceVerified = (latitude != null && longitude != null)
                && station.isWithinGeofence(latitude, longitude);
    }

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the transaction completed successfully.
     *
     * @return {@code true} when {@link #status} is {@link TransactionStatus#COMPLETED}
     */
    public boolean isSuccessful() {
        return status == TransactionStatus.COMPLETED;
    }

    /**
     * Generates a short, human-readable transaction reference code.
     * Format: {@code TXN-XXXXXXXX} (first 8 chars of UUID, upper-cased).
     *
     * @return transaction reference string
     */
    public String getTransactionReference() {
        return "TXN-" + id.toString().substring(0, 8).toUpperCase();
    }
}
