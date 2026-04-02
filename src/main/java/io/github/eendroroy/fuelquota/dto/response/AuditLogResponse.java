package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit log entry returned to admin callers.
 *
 * <p>Returned by {@code GET /api/admin/audit-logs}.
 * The {@code oldValue} and {@code newValue} fields are deserialized from the
 * JSON stored in the database and presented as structured maps for easy
 * front-end rendering.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Audit log entry recording an administrative action")
public class AuditLogResponse {

    /** String-serialised UUID of the audit log entry. */
    @Schema(description = "Audit log entry UUID")
    private String id;

    /** String-serialised UUID of the admin user who performed the action. */
    @Schema(description = "Admin user UUID")
    private String adminUserId;

    /** Display name of the admin user. */
    @Schema(description = "Admin user's full name", example = "System Administrator")
    private String adminName;

    /** Type of action performed (e.g. {@code VEHICLE_APPROVED}, {@code QUOTA_ADJUSTMENT}). */
    @Schema(description = "Action type enum value", example = "VEHICLE_APPROVED")
    private String actionType;

    /** Entity type that was affected (e.g. {@code Vehicle}, {@code Quota}). */
    @Schema(description = "Affected entity type", example = "Vehicle")
    private String targetEntity;

    /** String identifier of the affected entity instance. */
    @Schema(description = "Affected entity identifier")
    private String targetEntityId;

    /** State of the entity before the action (deserialized from JSON). */
    @Schema(description = "Entity state before the action")
    private Map<String, Object> oldValue;

    /** State of the entity after the action (deserialized from JSON). */
    @Schema(description = "Entity state after the action")
    private Map<String, Object> newValue;

    /** Business justification provided by the admin at the time of the action. */
    @Schema(description = "Reason / notes for the action", example = "Fraudulent registration detected")
    private String reasonNotes;

    /** Timestamp when the action was recorded. */
    @Schema(description = "Timestamp of the action")
    private LocalDateTime actionTimestamp;
}

