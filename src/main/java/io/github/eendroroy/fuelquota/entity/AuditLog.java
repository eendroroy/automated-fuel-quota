package io.github.eendroroy.fuelquota.entity;

import io.github.eendroroy.fuelquota.service.AuditLogService;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit trail entry recording every administrative action.
 *
 * <p>Created by {@link AuditLogService#log} after
 * each significant admin operation (vehicle approval/rejection, quota adjustment,
 * station management, representative management, etc.).
 *
 * <p>{@code oldValue} and {@code newValue} are stored as JSON text columns
 * so that arbitrary state snapshots can be recorded without schema changes.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_action_type", columnList = "action_type"),
    @Index(name = "idx_audit_timestamp",   columnList = "action_timestamp"),
    @Index(name = "idx_audit_admin",       columnList = "admin_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "actionType", "targetEntity", "targetEntityId", "actionTimestamp"})
public class AuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID of the admin user who performed the action. */
    @Column(name = "admin_user_id", nullable = false)
    private UUID adminUserId;

    /** Display name of the admin user (denormalized for query convenience). */
    @Column(name = "admin_name", nullable = false, length = 100)
    private String adminName;

    /**
     * Type of administrative action performed.
     *
     * @see AuditAction
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AuditAction actionType;

    /** Entity type that was affected (e.g. {@code Vehicle}, {@code Quota}). */
    @Column(name = "target_entity", nullable = false, length = 50)
    private String targetEntity;

    /** String representation of the affected entity's identifier. */
    @Column(name = "target_entity_id", length = 100)
    private String targetEntityId;

    /**
     * JSON snapshot of the entity state <em>before</em> the action.
     * {@code null} for creation actions.
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * JSON snapshot of the entity state <em>after</em> the action.
     * {@code null} for deletion actions.
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Business justification provided by the admin at the time of the action. */
    @Column(name = "reason_notes", length = 500)
    private String reasonNotes;

    /** Exact timestamp when the action was recorded. */
    @Column(name = "action_timestamp", nullable = false)
    private LocalDateTime actionTimestamp;

    /**
     * Enumeration of all auditable administrative actions.
     */
    public enum AuditAction {
        /** Manual quota limit change via {@code PUT /api/admin/quotas/{id}/adjust}. */
        QUOTA_ADJUSTMENT,
        /** Weekly or manual quota reset. */
        QUOTA_RESET,
        /**
         * Bulk quota sync triggered from admin quota config UI.
         * A single entry is written for the entire batch operation.
         */
        QUOTA_SYNC,
        /**
         * Vehicle automatically transferred to a new customer after BRTA ownership
         * verification passed during the {@code POST /api/customer/v1/vehicles} flow.
         */
        VEHICLE_TRANSFERRED,
        /**
         * Admin triggered a BRTA re-verification for a vehicle.
         * Currently always succeeds (BRTA API integration is future scope).
         */
        VEHICLE_REVERIFIED,
        /** New fuel station created. */
        STATION_CREATED,
        /** Existing fuel station details updated. */
        STATION_UPDATED,
        /** Fuel station deactivated/deleted. */
        STATION_DEACTIVATED,
        /** User account suspended. */
        USER_SUSPENDED,
        /** Previously suspended user account activated. */
        USER_ACTIVATED,
        /** New pump representative account created. */
        REP_CREATED,
        /** Pump representative account details updated. */
        REP_UPDATED
    }

    /**
     * Full constructor used by {@link AuditLogService#log}.
     * Sets {@link #actionTimestamp} to the current local time.
     *
     * @param adminUserId    UUID of the admin performing the action
     * @param adminName      display name of the admin
     * @param actionType     type of action
     * @param targetEntity   entity type affected
     * @param targetEntityId identifier of the affected entity
     * @param oldValue       JSON state before the action (may be {@code null})
     * @param newValue       JSON state after the action (may be {@code null})
     * @param reasonNotes    optional justification notes
     */
    public AuditLog(UUID adminUserId, String adminName, AuditAction actionType,
                    String targetEntity, String targetEntityId,
                    String oldValue, String newValue, String reasonNotes) {
        this.adminUserId = adminUserId;
        this.adminName = adminName;
        this.actionType = actionType;
        this.targetEntity = targetEntity;
        this.targetEntityId = targetEntityId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reasonNotes = reasonNotes;
        this.actionTimestamp = LocalDateTime.now();
    }
}
