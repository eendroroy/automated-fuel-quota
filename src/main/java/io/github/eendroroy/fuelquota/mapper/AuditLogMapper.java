package io.github.eendroroy.fuelquota.mapper;

import io.github.eendroroy.fuelquota.dto.response.AuditLogResponse;
import io.github.eendroroy.fuelquota.entity.AuditLog;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps {@link AuditLog} entities to {@link AuditLogResponse} DTOs.
 *
 * <p>The {@code oldValue} and {@code newValue} fields are stored as JSON strings
 * in the database and must be pre-parsed (by the caller) before this mapper is
 * invoked, so that the response carries structured {@link Map} objects.
 */
@Component
public class AuditLogMapper {

    /**
     * Converts an {@link AuditLog} entity and its pre-parsed JSON values to an
     * {@link AuditLogResponse}.
     *
     * @param log       the source entity (must not be {@code null})
     * @param parsedOld the deserialized {@code oldValue} map (may be {@code null})
     * @param parsedNew the deserialized {@code newValue} map (may be {@code null})
     * @return a populated {@link AuditLogResponse}
     */
    public AuditLogResponse toResponse(AuditLog log,
                                       Map<String, Object> parsedOld,
                                       Map<String, Object> parsedNew) {
        return AuditLogResponse.builder()
                .id(log.getId().toString())
                .adminUserId(log.getAdminUserId().toString())
                .adminName(log.getAdminName())
                .actionType(log.getActionType().name())
                .targetEntity(log.getTargetEntity())
                .targetEntityId(log.getTargetEntityId())
                .oldValue(parsedOld)
                .newValue(parsedNew)
                .reasonNotes(log.getReasonNotes())
                .actionTimestamp(log.getActionTimestamp())
                .build();
    }
}

