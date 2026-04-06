package io.github.eendroroy.fuelquota.controller.admin.v1;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.request.PumpRepresentativeRequest;
import io.github.eendroroy.fuelquota.dto.response.PumpRepresentativeResponse;
import io.github.eendroroy.fuelquota.entity.AuditLog;
import io.github.eendroroy.fuelquota.entity.PumpRepresentative;
import io.github.eendroroy.fuelquota.service.AuditLogService;
import io.github.eendroroy.fuelquota.service.PumpRepresentativeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/v1")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin v1 - Pump Representatives", description = "Pump representative management")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class V1AdminPumpRepController {

    private final PumpRepresentativeService repService;
    private final AuditLogService auditLogService;

    @GetMapping("/pump-representatives")
    @Operation(summary = "Get all pump representatives",
            description = "Returns a paginated list of pump representatives with optional search, status, and station filters")
    public ResponseEntity<Page<PumpRepresentativeResponse>> getAllPumpReps(
            @Parameter(description = "Free-text search on name, email, or employee ID")
            @RequestParam(required = false) String search,
            @Parameter(description = "Status filter: ACTIVE | INACTIVE | SUSPENDED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by station UUID")
            @RequestParam(required = false) UUID stationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(repService.getAllReps(search, status, stationId, pageable));
    }

    @GetMapping("/pump-representatives/{id}")
    @Operation(summary = "Get pump representative by ID")
    public ResponseEntity<PumpRepresentativeResponse> getPumpRep(@PathVariable UUID id) {
        return ResponseEntity.ok(repService.getRepById(id));
    }

    @PostMapping("/pump-representatives")
    @Operation(summary = "Create pump representative")
    public ResponseEntity<PumpRepresentativeResponse> createPumpRep(
            @Valid @RequestBody PumpRepresentativeRequest req, HttpServletRequest request) {
        PumpRepresentativeResponse rep = repService.createRep(req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.REP_CREATED,
                "PumpRepresentative", rep.getId().toString(),
                null, Map.of("name", rep.getName(), "username", rep.getUsername(), "stationId", rep.getStationId()), null);
        return ResponseEntity.ok(rep);
    }

    @PutMapping("/pump-representatives/{id}")
    @Operation(summary = "Update pump representative")
    public ResponseEntity<PumpRepresentativeResponse> updatePumpRep(
            @PathVariable UUID id, @Valid @RequestBody PumpRepresentativeRequest req, HttpServletRequest request) {
        PumpRepresentativeResponse rep = repService.updateRep(id, req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.REP_UPDATED,
                "PumpRepresentative", id.toString(),
                null, Map.of("name", rep.getName(), "username", rep.getUsername()), null);
        return ResponseEntity.ok(rep);
    }

    @PutMapping("/pump-representatives/{id}/status")
    @Operation(summary = "Update pump representative status")
    public ResponseEntity<PumpRepresentativeResponse> updatePumpRepStatus(
            @PathVariable UUID id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        PumpRepresentative.RepStatus status = PumpRepresentative.RepStatus.valueOf(body.get("status"));
        PumpRepresentativeResponse rep = repService.updateStatus(id, status);
        AuditLog.AuditAction action = status == PumpRepresentative.RepStatus.ACTIVE
                ? AuditLog.AuditAction.USER_ACTIVATED : AuditLog.AuditAction.USER_SUSPENDED;
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                action, "PumpRepresentative", id.toString(),
                null, Map.of("status", status.name()), null);
        return ResponseEntity.ok(rep);
    }

    @DeleteMapping("/pump-representatives/{id}")
    @Operation(summary = "Delete pump representative")
    public ResponseEntity<Void> deletePumpRep(@PathVariable UUID id) {
        repService.deleteRep(id);
        return ResponseEntity.noContent().build();
    }
}

