package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.response.AuditLogResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.github.eendroroy.fuelquota.entity.AuditLog;
import io.github.eendroroy.fuelquota.repository.AuditLogRepository;

import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Record an admin action in the audit trail.
     */
    @Transactional
    public void log(UUID adminUserId, String adminName, AuditLog.AuditAction actionType,
                    String targetEntity, String targetEntityId,
                    Object oldValue, Object newValue, String reasonNotes) {
        try {
            String oldJson = oldValue != null ? objectMapper.writeValueAsString(oldValue) : null;
            String newJson = newValue != null ? objectMapper.writeValueAsString(newValue) : null;

            AuditLog entry = new AuditLog(adminUserId, adminName, actionType,
                    targetEntity, targetEntityId, oldJson, newJson, reasonNotes);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            logger.error("Failed to save audit log for action {}: {}", actionType, e.getMessage());
        }
    }

    /**
     * Get paginated audit logs with optional filters.
     * Uses Specification to build the query dynamically — avoids PostgreSQL
     * type-inference failures caused by the {@code ? IS NULL} pattern.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(String actionTypeStr,
                                               LocalDateTime startDate,
                                               LocalDateTime endDate,
                                               String adminSearch,
                                               String targetEntity,
                                               Pageable pageable) {
        AuditLog.AuditAction actionType = null;
        if (actionTypeStr != null && !actionTypeStr.isBlank()) {
            try {
                actionType = AuditLog.AuditAction.valueOf(actionTypeStr);
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown audit action type filter: {}", actionTypeStr);
            }
        }

        final AuditLog.AuditAction finalActionType = actionType;
        final LocalDateTime finalStartDate = startDate;
        final LocalDateTime finalEndDate = endDate;

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (finalActionType != null) {
                predicates.add(cb.equal(root.get("actionType"), finalActionType));
            }
            if (finalStartDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("actionTimestamp"), finalStartDate));
            }
            if (finalEndDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("actionTimestamp"), finalEndDate));
            }
            if (adminSearch != null && !adminSearch.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("adminName")),
                        "%" + adminSearch.toLowerCase() + "%"));
            }
            if (targetEntity != null && !targetEntity.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("targetEntity")),
                        "%" + targetEntity.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Apply default sort if the caller did not supply one
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "actionTimestamp"));
        }

        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        Map<String, Object> parsedOld = parseJson(log.getOldValue());
        Map<String, Object> parsedNew = parseJson(log.getNewValue());
        return AuditLogResponse.builder()
                .id(log.getId() != null ? log.getId().toString() : null)
                .adminUserId(log.getAdminUserId() != null ? log.getAdminUserId().toString() : null)
                .adminName(log.getAdminName())
                .actionType(log.getActionType() != null ? log.getActionType().name() : null)
                .targetEntity(log.getTargetEntity())
                .targetEntityId(log.getTargetEntityId())
                .oldValue(parsedOld)
                .newValue(parsedNew)
                .reasonNotes(log.getReasonNotes())
                .actionTimestamp(log.getActionTimestamp())
                .build();
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
