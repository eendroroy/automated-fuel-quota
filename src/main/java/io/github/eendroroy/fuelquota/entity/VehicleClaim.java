package io.github.eendroroy.fuelquota.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a customer's request to claim ownership of an already-registered vehicle.
 *
 * <p>Use case: A customer purchases a second-hand vehicle that is already registered
 * in the system under the previous owner.  The new owner submits a claim so that
 * the system can transfer the vehicle to their account once ownership is verified.
 *
 * <p><strong>Future scope:</strong> BRTA API integration will be used to
 * automatically verify ownership before approving the claim.  Until then, an admin
 * manually reviews and approves or rejects each claim.
 *
 * <p>Claim lifecycle:
 * <pre>
 *   PENDING → APPROVED  (admin manually approves; vehicle ownership transferred)
 *   PENDING → REJECTED  (admin rejects; vehicle remains with original owner)
 * </pre>
 */
@Entity
@Table(name = "vehicle_claims")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleClaim implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The vehicle whose ownership is being claimed.
     * Multiple claims for the same vehicle are allowed (but only one can be APPROVED).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /**
     * The user submitting the ownership claim (the prospective new owner).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimant_user_id", nullable = false)
    private User claimant;

    /**
     * NID provided by the claimant as proof of ownership.
     * Should match the NID on the vehicle registration document.
     *
     * <p><strong>Future scope:</strong> This NID will be cross-validated against
     * BRTA records to confirm the claimant is the registered owner.
     */
    @Column(name = "claimant_nid", nullable = false, length = 20)
    private String claimantNid;

    /** The reason or context provided by the claimant (e.g. "purchased second-hand"). */
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    /** Current review status of the claim. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ClaimStatus status = ClaimStatus.PENDING;

    /** Notes added by the admin when approving or rejecting the claim. */
    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    /** Automatically populated by Spring Data JPA auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated by Spring Data JPA auditing on every save. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Lifecycle states for a vehicle ownership claim.
     */
    public enum ClaimStatus {
        /** Awaiting admin review. */
        PENDING,
        /** Admin approved; vehicle ownership transferred to claimant. */
        APPROVED,
        /** Admin rejected; vehicle remains with original owner. */
        REJECTED
    }
}

