package io.github.eendroroy.fuelquota.controller.admin.v1;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.request.QuotaAdjustmentRequest;
import io.github.eendroroy.fuelquota.dto.request.QuotaConfigRequest;
import io.github.eendroroy.fuelquota.dto.request.QuotaConfigSetRequest;
import io.github.eendroroy.fuelquota.dto.response.QuotaConfigResponse;
import io.github.eendroroy.fuelquota.dto.response.QuotaConfigSetResponse;
import io.github.eendroroy.fuelquota.dto.response.QuotaResponse;
import io.github.eendroroy.fuelquota.entity.AuditLog;
import io.github.eendroroy.fuelquota.mapper.QuotaMapper;
import io.github.eendroroy.fuelquota.service.AuditLogService;
import io.github.eendroroy.fuelquota.service.QuotaConfigService;
import io.github.eendroroy.fuelquota.service.QuotaConfigSetService;
import io.github.eendroroy.fuelquota.service.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/v1")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin v1 - Quota", description = "Quota management and configuration")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class V1AdminQuotaController {

    private final QuotaService quotaService;
    private final QuotaMapper quotaMapper;
    private final QuotaConfigService quotaConfigService;
    private final QuotaConfigSetService quotaConfigSetService;
    private final AuditLogService auditLogService;

    // ── Quota Management ──────────────────────────────────────────────────────

    @GetMapping("/quotas")
    @Operation(summary = "Get all quotas")
    public ResponseEntity<Page<QuotaResponse>> getAllQuotas(
            @Parameter(description = "Search term for vehicle registration number or owner name")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by BRTA office code") @RequestParam(required = false) String brtaCode,
            @Parameter(description = "Filter by vehicle registration code") @RequestParam(required = false) String registrationCode,
            @Parameter(description = "Filter by quota status (ACTIVE, SUSPENDED, EXHAUSTED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "vehicle.registrationDate"));
        return ResponseEntity.ok(quotaService.getAllQuotas(search, brtaCode, registrationCode, status, pageable)
                .map(quotaMapper::toResponse));
    }

    @PutMapping("/quotas/{vehicleId}/adjust")
    @Operation(summary = "Adjust quota limit (marks vehicle as individually overridden)",
            description = "Modifies the fuel limit for a specific vehicle and marks it as individually overridden — excluded from bulk sync.")
    public ResponseEntity<QuotaResponse> adjustQuota(@PathVariable UUID vehicleId,
            @Valid @RequestBody QuotaAdjustmentRequest req, HttpServletRequest request) {
        QuotaResponse quota = quotaMapper.toResponse(
                quotaService.adjustQuotaLimit(vehicleId, req.getNewLimitLiters(), req.getReason()));
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_ADJUSTMENT,
                "Quota", quota.getId(),
                null, Map.of("newLimitLiters", req.getNewLimitLiters()), req.getReason());
        return ResponseEntity.ok(quota);
    }

    @PostMapping("/quotas/{vehicleId}/reset")
    @Operation(summary = "Reset vehicle quota")
    public ResponseEntity<Map<String, String>> resetQuota(
            @PathVariable UUID vehicleId, HttpServletRequest request) {
        quotaService.manualResetQuota(vehicleId);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_RESET,
                "Quota", vehicleId.toString(),
                null, Map.of("action", "manual_reset"), null);
        return ResponseEntity.ok(Map.of("message", "Quota reset successfully"));
    }

    @PostMapping("/quotas/bulk-reset")
    @Operation(summary = "Bulk reset all quotas")
    public ResponseEntity<Map<String, String>> bulkResetQuotas(HttpServletRequest request) {
        quotaService.resetWeeklyQuotas();
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_RESET,
                "Quota", "ALL",
                null, Map.of("action", "bulk_reset"), null);
        return ResponseEntity.ok(Map.of("message", "Bulk quota reset completed"));
    }

    @DeleteMapping("/quotas/{vehicleId}/override")
    @Operation(summary = "Remove manual quota override",
            description = "Clears the individually_overridden flag so the quota participates in future bulk sync operations.")
    public ResponseEntity<QuotaResponse> clearQuotaOverride(
            @PathVariable UUID vehicleId, HttpServletRequest request) {
        QuotaResponse quota = quotaMapper.toResponse(quotaService.clearQuotaOverride(vehicleId));
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_ADJUSTMENT,
                "Quota", quota.getId(),
                Map.of("individuallyOverridden", true),
                Map.of("individuallyOverridden", false),
                "Manual quota override removed");
        return ResponseEntity.ok(quota);
    }

    // ── System-wide Quota Config (geofence, cron, default limit) ─────────────

    @GetMapping("/quota-config")
    @Operation(summary = "Get global quota configuration",
            description = "Returns the global quota configuration (default limit, geofence radius, cron expression)")
    public ResponseEntity<QuotaConfigResponse> getQuotaConfig() {
        return ResponseEntity.ok(quotaConfigService.getConfigResponse());
    }

    @PutMapping("/quota-config")
    @Operation(summary = "Update global quota configuration")
    public ResponseEntity<QuotaConfigResponse> updateQuotaConfig(
            @Valid @RequestBody QuotaConfigRequest req, HttpServletRequest request) {
        QuotaConfigResponse updated = quotaConfigService.updateConfig(req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_ADJUSTMENT,
                "QuotaConfig", "DEFAULT",
                null, Map.of("limitLitres", req.getLimitLitres(), "period", req.getQuotaPeriod()),
                req.getDescription() != null ? req.getDescription() : "Quota config updated");
        return ResponseEntity.ok(updated);
    }

    // ── Quota Config Sets (registration-code-based) ───────────────────────────

    @GetMapping("/quota-config-sets")
    @Operation(summary = "Get all quota config sets",
            description = "Returns all quota configuration sets. Each set groups multiple registration codes with a shared limit and period.")
    public ResponseEntity<List<QuotaConfigSetResponse>> getAllQuotaConfigSets() {
        return ResponseEntity.ok(quotaConfigSetService.getAllSets());
    }

    @GetMapping("/quota-config-sets/{id}")
    @Operation(summary = "Get quota config set by ID")
    public ResponseEntity<QuotaConfigSetResponse> getQuotaConfigSet(@PathVariable UUID id) {
        return ResponseEntity.ok(quotaConfigSetService.getSetById(id));
    }

    @PostMapping("/quota-config-sets")
    @Operation(summary = "Create quota config set",
            description = "Creates a new quota config set covering one or more vehicle registration codes")
    @ApiResponse(responseCode = "201", description = "Config set created successfully")
    public ResponseEntity<QuotaConfigSetResponse> createQuotaConfigSet(
            @Valid @RequestBody QuotaConfigSetRequest req, HttpServletRequest request) {
        QuotaConfigSetResponse created = quotaConfigSetService.createSet(req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_ADJUSTMENT,
                "QuotaConfigSet", created.getId(),
                null, Map.of("name", created.getName(), "codes", created.getRegistrationCodes()),
                "Config set created");
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/quota-config-sets/{id}")
    @Operation(summary = "Update quota config set")
    public ResponseEntity<QuotaConfigSetResponse> updateQuotaConfigSet(
            @PathVariable UUID id, @Valid @RequestBody QuotaConfigSetRequest req, HttpServletRequest request) {
        QuotaConfigSetResponse updated = quotaConfigSetService.updateSet(id, req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_ADJUSTMENT,
                "QuotaConfigSet", id.toString(),
                null, Map.of("name", updated.getName(), "codes", updated.getRegistrationCodes()),
                "Config set updated");
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/quota-config-sets/{id}")
    @Operation(summary = "Delete quota config set")
    @ApiResponse(responseCode = "204", description = "Config set deleted")
    public ResponseEntity<Void> deleteQuotaConfigSet(@PathVariable UUID id) {
        quotaConfigSetService.deleteSet(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Syncs quota limits from config sets to all eligible vehicles.
     * Vehicles with individually overridden quotas are skipped.
     */
    @PostMapping("/quota-config/sync")
    @Operation(summary = "Sync quota config to vehicles",
            description = "Applies matching config set limits to all vehicles that have not been individually overridden. "
                    + "Returns the number of quotas updated.")
    public ResponseEntity<Map<String, Object>> syncQuotaConfig(HttpServletRequest request) {
        int updatedCount = quotaService.syncQuotaConfigs();
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_ADJUSTMENT,
                "Quota", "SYNC",
                null, Map.of("updatedCount", updatedCount),
                "Bulk quota config sync from registration-code config sets");
        return ResponseEntity.ok(Map.of("message", "Quota sync completed", "updatedCount", updatedCount));
    }
}

