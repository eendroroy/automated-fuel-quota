package io.github.eendroroy.fuelquota.controller;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import io.github.eendroroy.fuelquota.entity.PumpRepresentative;
import io.github.eendroroy.fuelquota.entity.AuditLog;
import io.github.eendroroy.fuelquota.entity.VehicleClaim;
import io.github.eendroroy.fuelquota.dto.request.StationRequest;
import io.github.eendroroy.fuelquota.dto.request.QuotaAdjustmentRequest;
import io.github.eendroroy.fuelquota.dto.request.QuotaConfigRequest;
import io.github.eendroroy.fuelquota.dto.request.PumpRepresentativeRequest;
import io.github.eendroroy.fuelquota.mapper.QuotaMapper;

import io.github.eendroroy.fuelquota.dto.response.*;
import io.github.eendroroy.fuelquota.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin Dashboard API Controller.
 *
 * <p>Provides comprehensive management capabilities for the fuel quota system:
 * <ul>
 *   <li>Vehicle registration approval workflow</li>
 *   <li>Fuel station CRUD operations</li>
 *   <li>Quota management and adjustments</li>
 *   <li>Pump representative management</li>
 *   <li>System statistics and audit logs</li>
 * </ul>
 *
 * <p><strong>Security:</strong> All endpoints require ADMIN role authentication.
 * User actions are automatically logged for audit trails.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrator dashboard and system management")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AdminController {

    private final VehicleService vehicleService;
    private final FuelStationService stationService;
    private final QuotaService quotaService;
    private final QuotaMapper quotaMapper;
    private final QuotaConfigService quotaConfigService;
    private final VehicleClaimService vehicleClaimService;
    private final AdminStatsService statsService;
    private final PumpRepresentativeService repService;
    private final AuditLogService auditLogService;
    private final TransactionService transactionService;
    private final RegistrationCodeService registrationCodeService;
    private final BrtaOfficeService brtaOfficeService;

    // ===== DASHBOARD & STATS =====

    /**
     * Retrieves dashboard statistics for admin overview.
     *
     * @return dashboard statistics including vehicle counts, station counts, and recent activity
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Get dashboard statistics",
        description = "Returns overview statistics for the admin dashboard"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Dashboard statistics retrieved successfully",
        content = @Content(schema = @Schema(implementation = DashboardStatsResponse.class))
    )
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(statsService.getDashboardStats());
    }

    // ===== VEHICLE MANAGEMENT =====

    /**
     * Retrieves paginated list of vehicles with optional filtering.
     *
     * @param search   optional search term (registration number, owner name, etc.)
     * @param status   optional status filter (VERIFIED, UNVERIFIED, DEREGISTERED)
     * @param pageable pagination parameters
     * @return paginated vehicle results
     */
    @GetMapping("/vehicles")
    @Operation(
        summary = "Get all vehicles",
        description = "Returns paginated list of vehicles with optional search and status filtering"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Vehicles retrieved successfully",
        content = @Content(schema = @Schema(implementation = Page.class))
    )
    public ResponseEntity<Page<VehicleResponse>> getAllVehicles(
            @Parameter(description = "Search term for registration number, owner name, etc.")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by vehicle status (VERIFIED, UNVERIFIED, DEREGISTERED)")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {

        Vehicle.VehicleStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = Vehicle.VehicleStatus.valueOf(status);
        }
        return ResponseEntity.ok(vehicleService.getAllVehicles(search, statusEnum, pageable));
    }

    /**
     * Retrieves a specific vehicle by ID.
     */
    @GetMapping("/vehicles/{id}")
    @Operation(
        summary = "Get vehicle by ID",
        description = "Returns detailed vehicle information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vehicle found"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    /**
     * Triggers a BRTA re-verification for the given vehicle.
     *
     * <p>Currently always succeeds and keeps the vehicle as {@code VERIFIED}.
     *
     * <p><strong>Future scope:</strong> Will call the BRTA API to confirm ownership.
     */
    @PutMapping("/vehicles/{id}/reverify")
    @Operation(
        summary = "Re-verify vehicle via BRTA",
        description = "Triggers a BRTA ownership verification for the vehicle. "
                + "Currently always succeeds (BRTA API integration is future scope). "
                + "Future: may set status to UNVERIFIED if BRTA check fails."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reverification completed"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<VehicleResponse> reverifyVehicle(@PathVariable UUID id, HttpServletRequest request) {
        VehicleResponse vehicle = vehicleService.reverifyVehicle(id);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.VEHICLE_REVERIFIED,
                "Vehicle", id.toString(),
                null, Map.of("status", "VERIFIED", "registrationNumber", vehicle.getRegistrationNumber()),
                "Manual BRTA reverification (currently always passes)");
        return ResponseEntity.ok(vehicle);
    }

    // ===== BRTA LOOKUP DATA =====

    /**
     * Returns the list of all BRTA office / region codes.
     */
    @GetMapping("/brta-offices")
    @Operation(summary = "Get all BRTA offices", description = "Returns all available BRTA office / region codes")
    public ResponseEntity<List<BrtaOfficeResponse>> getAllBrtaOffices() {
        return ResponseEntity.ok(brtaOfficeService.getAllOffices());
    }

    /**
     * Returns the list of all vehicle registration codes.
     */
    @GetMapping("/registration-codes")
    @Operation(summary = "Get all registration codes", description = "Returns all available vehicle category registration codes")
    public ResponseEntity<List<RegistrationCodeResponse>> getAllRegistrationCodes() {
        return ResponseEntity.ok(registrationCodeService.getAllCodes());
    }

    // ===== FUEL STATION MANAGEMENT =====

    /**
     * Retrieves paginated list of fuel stations.
     */
    @GetMapping("/stations")
    @Operation(
        summary = "Get all fuel stations",
        description = "Returns paginated list of fuel stations with optional status filtering"
    )
    public ResponseEntity<Page<StationResponse>> getAllStations(
            @Parameter(description = "Filter by station status")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {

        FuelStation.StationStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = FuelStation.StationStatus.valueOf(status);
        }
        return ResponseEntity.ok(stationService.getAllStations(statusEnum, pageable));
    }

    /**
     * Retrieves a specific fuel station by ID.
     */
    @GetMapping("/stations/{id}")
    @Operation(summary = "Get fuel station by ID")
    public ResponseEntity<StationResponse> getStation(@PathVariable UUID id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }

    /**
     * Creates a new fuel station.
     */
    @PostMapping("/stations")
    @Operation(
        summary = "Create fuel station",
        description = "Creates a new fuel station with geofence configuration"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Station created successfully"),
        @ApiResponse(responseCode = "400", description = "Station code already exists or validation failed")
    })
    public ResponseEntity<StationResponse> createStation(
            @Valid @RequestBody StationRequest req, HttpServletRequest request) {
        StationResponse station = stationService.createStation(req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.STATION_CREATED,
                "FuelStation", String.valueOf(station.getId()),
                null, Map.of("stationName", station.getStationName(), "stationCode", station.getStationCode()),
                null);
        return ResponseEntity.ok(station);
    }

    /**
     * Updates an existing fuel station.
     */
    @PutMapping("/stations/{id}")
    @Operation(summary = "Update fuel station")
    public ResponseEntity<StationResponse> updateStation(@PathVariable UUID id,
            @Valid @RequestBody StationRequest req, HttpServletRequest request) {
        StationResponse station = stationService.updateStation(id, req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.STATION_UPDATED,
                "FuelStation", id.toString(),
                null, Map.of("stationName", station.getStationName(), "status", station.getStatus()),
                null);
        return ResponseEntity.ok(station);
    }

    /**
     * Deletes a fuel station.
     */
    @DeleteMapping("/stations/{id}")
    @Operation(summary = "Delete fuel station")
    public ResponseEntity<Void> deleteStation(@PathVariable UUID id, HttpServletRequest request) {
        StationResponse station = stationService.getStationById(id);
        stationService.deleteStation(id);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.STATION_DEACTIVATED,
                "FuelStation", id.toString(),
                Map.of("stationName", station.getStationName()), null,
                null);
        return ResponseEntity.ok().build();
    }

    // ===== QUOTA MANAGEMENT =====

    /**
     * Retrieves paginated list of quotas.
     */
    @GetMapping("/quotas")
    @Operation(
        summary = "Get all quotas",
        description = "Returns paginated list of vehicle quotas with optional search"
    )
    public ResponseEntity<Page<QuotaResponse>> getAllQuotas(
            @Parameter(description = "Search term for vehicle registration number")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(quotaService.getAllQuotas(search, pageable).map(quotaMapper::toResponse));
    }

    /**
     * Adjusts a vehicle's quota limit.
     */
    @PutMapping("/quotas/{vehicleId}/adjust")
    @Operation(
        summary = "Adjust quota limit",
        description = "Modifies the weekly fuel limit for a specific vehicle"
    )
    public ResponseEntity<QuotaResponse> adjustQuota(@PathVariable UUID vehicleId,
            @Valid @RequestBody QuotaAdjustmentRequest req, HttpServletRequest request) {
        QuotaResponse quota = quotaMapper.toResponse(quotaService.adjustQuotaLimit(vehicleId, req.getNewLimitLiters(), req.getReason()));
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_ADJUSTMENT,
                "Quota", quota.getId(),
                null, Map.of("newLimitLiters", req.getNewLimitLiters()),
                req.getReason());
        return ResponseEntity.ok(quota);
    }

    /**
     * Manually resets a specific vehicle's quota.
     */
    @PostMapping("/quotas/{vehicleId}/reset")
    @Operation(
        summary = "Reset vehicle quota",
        description = "Manually resets used fuel to zero for a specific vehicle"
    )
    public ResponseEntity<Map<String, String>> resetQuota(
            @PathVariable UUID vehicleId, HttpServletRequest request) {
        quotaService.manualResetQuota(vehicleId);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_RESET,
                "Quota", vehicleId.toString(),
                null, Map.of("action", "manual_reset"),
                null);
        return ResponseEntity.ok(Map.of("message", "Quota reset successfully"));
    }

    /**
     * Resets all vehicle quotas (weekly reset operation).
     */
    @PostMapping("/quotas/bulk-reset")
    @Operation(
        summary = "Bulk reset all quotas",
        description = "Resets used fuel to zero for all vehicles (equivalent to weekly reset job)"
    )
    public ResponseEntity<Map<String, String>> bulkResetQuotas(HttpServletRequest request) {
        quotaService.resetWeeklyQuotas();
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.QUOTA_RESET,
                "Quota", "ALL",
                null, Map.of("action", "bulk_reset"),
                null);
        return ResponseEntity.ok(Map.of("message", "Bulk quota reset completed"));
    }

    // ===== PUMP REPRESENTATIVE MANAGEMENT =====

    /**
     * Retrieves paginated list of pump representatives.
     */
    @GetMapping("/pump-representatives")
    @Operation(
        summary = "Get all pump representatives",
        description = "Returns paginated list of pump representative accounts"
    )
    public ResponseEntity<Page<PumpRepresentativeResponse>> getAllPumpReps(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(repService.getAllReps(pageable));
    }

    /**
     * Retrieves a specific pump representative by ID.
     */
    @GetMapping("/pump-representatives/{id}")
    @Operation(summary = "Get pump representative by ID")
    public ResponseEntity<PumpRepresentativeResponse> getPumpRep(@PathVariable UUID id) {
        return ResponseEntity.ok(repService.getRepById(id));
    }

    /**
     * Creates a new pump representative account.
     */
    @PostMapping("/pump-representatives")
    @Operation(
        summary = "Create pump representative",
        description = "Creates a new pump representative account with station assignment"
    )
    public ResponseEntity<PumpRepresentativeResponse> createPumpRep(
            @Valid @RequestBody PumpRepresentativeRequest req, HttpServletRequest request) {
        PumpRepresentativeResponse rep = repService.createRep(req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                 AuditLog.AuditAction.REP_CREATED,
                "PumpRepresentative", rep.getId().toString(),
                null, Map.of("name", rep.getName(), "username", rep.getUsername(), "stationId", rep.getStationId()),
                null);
        return ResponseEntity.ok(rep);
    }

    /**
     * Updates an existing pump representative.
     */
    @PutMapping("/pump-representatives/{id}")
    @Operation(summary = "Update pump representative")
    public ResponseEntity<PumpRepresentativeResponse> updatePumpRep(
            @PathVariable UUID id,
            @Valid @RequestBody PumpRepresentativeRequest req, HttpServletRequest request) {
        PumpRepresentativeResponse rep = repService.updateRep(id, req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.REP_UPDATED,
                "PumpRepresentative", id.toString(),
                null, Map.of("name", rep.getName(), "username", rep.getUsername()),
                null);
        return ResponseEntity.ok(rep);
    }

    /**
     * Updates a pump representative's status (ACTIVE/SUSPENDED).
     */
    @PutMapping("/pump-representatives/{id}/status")
    @Operation(
        summary = "Update pump representative status",
        description = "Activates or suspends a pump representative account"
    )
    public ResponseEntity<PumpRepresentativeResponse> updatePumpRepStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body, HttpServletRequest request) {
        PumpRepresentative.RepStatus status = PumpRepresentative.RepStatus.valueOf(body.get("status"));
        PumpRepresentativeResponse rep = repService.updateStatus(id, status);
        AuditLog.AuditAction action = status == PumpRepresentative.RepStatus.ACTIVE
                ? AuditLog.AuditAction.USER_ACTIVATED : AuditLog.AuditAction.USER_SUSPENDED;
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                action,
                "PumpRepresentative", id.toString(),
                null, Map.of("status", status.name()),
                null);
        return ResponseEntity.ok(rep);
    }

    /**
     * Deletes a pump representative account.
     */
    @DeleteMapping("/pump-representatives/{id}")
    @Operation(summary = "Delete pump representative")
    public ResponseEntity<Void> deletePumpRep(@PathVariable UUID id) {
        repService.deleteRep(id);
        return ResponseEntity.noContent().build();
    }

    // ===== TRANSACTIONS =====

    /**
     * Retrieves paginated transaction history with filtering.
     */
    @GetMapping("/transactions")
    @Operation(
        summary = "Get transaction history",
        description = "Returns paginated fuel transaction history with optional filtering by vehicle, station, and date range"
    )
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @Parameter(description = "Filter by vehicle ID")
            @RequestParam(required = false) String vehicleId,
            @Parameter(description = "Filter by station ID")
            @RequestParam(required = false) String stationId,
            @Parameter(description = "Start date filter")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date filter")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID vehicleUuid = vehicleId != null && !vehicleId.isBlank() ? UUID.fromString(vehicleId) : null;
        UUID stationUuid = stationId != null && !stationId.isBlank() ? UUID.fromString(stationId) : null;

        return ResponseEntity.ok(transactionService.getTransactionsWithFilters(
            vehicleUuid, stationUuid, startDate, endDate, pageable));
    }

    // ===== AUDIT LOGS =====

    /**
     * Retrieves paginated audit log entries with filtering.
     */
    @GetMapping("/audit-logs")
    @Operation(
        summary = "Get audit logs",
        description = "Returns paginated audit log entries with optional filtering by action type and date range"
    )
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @Parameter(description = "Filter by audit action type")
            @RequestParam(required = false) String actionType,
            @Parameter(description = "Start date filter")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date filter")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(auditLogService.getAuditLogs(actionType, startDate, endDate, pageable));
    }

    // ===== QUOTA CONFIGURATION =====

    /**
     * Returns the current global quota configuration stored in the database.
     */
    @GetMapping("/quota-config")
    @Operation(
        summary = "Get quota configuration",
        description = "Returns the current global quota configuration (limit, period, geofence, cron)"
    )
    @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully",
            content = @Content(schema = @Schema(implementation = QuotaConfigResponse.class)))
    public ResponseEntity<QuotaConfigResponse> getQuotaConfig() {
        return ResponseEntity.ok(quotaConfigService.getConfigResponse());
    }

    /**
     * Updates the global quota configuration.
     *
     * <p>Changes take effect immediately for all newly created quotas without
     * requiring an application restart.
     */
    @PutMapping("/quota-config")
    @Operation(
        summary = "Update quota configuration",
        description = "Updates global quota settings (limit litres, period, geofence radius, cron expression). "
                + "Changes apply to all newly created quotas immediately."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
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

    // ===== VEHICLE OWNERSHIP CLAIMS =====

    /**
     * Returns paginated vehicle ownership claims for admin review.
     */
    @GetMapping("/vehicle-claims")
    @Operation(
        summary = "Get vehicle ownership claims",
        description = "Returns paginated list of vehicle ownership claims. Filter by status to see pending claims."
    )
    public ResponseEntity<Page<VehicleClaimResponse>> getVehicleClaims(
            @Parameter(description = "Filter by claim status: PENDING, APPROVED, REJECTED")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        VehicleClaim.ClaimStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = VehicleClaim.ClaimStatus.valueOf(status);
        }
        return ResponseEntity.ok(vehicleClaimService.getAllClaims(statusEnum, pageable));
    }

    /**
     * Approves a pending vehicle ownership claim and transfers vehicle to the claimant.
     *
     * <p><strong>Future scope:</strong> This will trigger BRTA verification before approval.
     */
    @PutMapping("/vehicle-claims/{claimId}/approve")
    @Operation(
        summary = "Approve vehicle ownership claim",
        description = "Approves the claim and transfers vehicle ownership to the claimant. "
                + "The vehicle is reset to PENDING status and must be re-approved. "
                + "FUTURE SCOPE: BRTA API verification will be required before this action."
    )
    public ResponseEntity<VehicleClaimResponse> approveVehicleClaim(
            @PathVariable UUID claimId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        String adminNotes = body != null ? body.get("adminNotes") : null;
        VehicleClaimResponse claim = vehicleClaimService.approveClaim(claimId, adminNotes);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.VEHICLE_APPROVED,
                "VehicleClaim", claimId.toString(),
                null, Map.of("status", "APPROVED", "vehicle", claim.getRegistrationNumber()),
                adminNotes);
        return ResponseEntity.ok(claim);
    }

    /**
     * Rejects a pending vehicle ownership claim.
     */
    @PutMapping("/vehicle-claims/{claimId}/reject")
    @Operation(
        summary = "Reject vehicle ownership claim",
        description = "Rejects the claim. The vehicle remains with the original owner."
    )
    public ResponseEntity<VehicleClaimResponse> rejectVehicleClaim(
            @PathVariable UUID claimId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String adminNotes = body != null ? body.get("adminNotes") : null;
        VehicleClaimResponse claim = vehicleClaimService.rejectClaim(claimId, adminNotes);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.VEHICLE_REJECTED,
                "VehicleClaim", claimId.toString(),
                null, Map.of("status", "REJECTED", "vehicle", claim.getRegistrationNumber()),
                adminNotes);
        return ResponseEntity.ok(claim);
    }
}
