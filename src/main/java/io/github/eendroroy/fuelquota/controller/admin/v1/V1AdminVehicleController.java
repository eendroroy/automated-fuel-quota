package io.github.eendroroy.fuelquota.controller.admin.v1;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.response.BrtaOfficeResponse;
import io.github.eendroroy.fuelquota.dto.response.RegistrationCodeResponse;
import io.github.eendroroy.fuelquota.dto.response.VehicleClaimResponse;
import io.github.eendroroy.fuelquota.dto.response.VehicleResponse;
import io.github.eendroroy.fuelquota.entity.AuditLog;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.entity.VehicleClaim;
import io.github.eendroroy.fuelquota.service.AuditLogService;
import io.github.eendroroy.fuelquota.service.BrtaOfficeService;
import io.github.eendroroy.fuelquota.service.RegistrationCodeService;
import io.github.eendroroy.fuelquota.service.VehicleClaimService;
import io.github.eendroroy.fuelquota.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "Admin v1 - Vehicles", description = "Vehicle and ownership claim management")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class V1AdminVehicleController {

    private final VehicleService vehicleService;
    private final VehicleClaimService vehicleClaimService;
    private final AuditLogService auditLogService;
    private final BrtaOfficeService brtaOfficeService;
    private final RegistrationCodeService registrationCodeService;

    // ── Vehicles ──────────────────────────────────────────────────────────────

    @GetMapping("/vehicles")
    @Operation(summary = "Get all vehicles")
    public ResponseEntity<Page<VehicleResponse>> getAllVehicles(
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Vehicle.VehicleStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = Vehicle.VehicleStatus.valueOf(status);
        }
        return ResponseEntity.ok(vehicleService.getAllVehicles(search, statusEnum, pageable));
    }

    @GetMapping("/vehicles/{id}")
    @Operation(summary = "Get vehicle by ID")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @PutMapping("/vehicles/{id}/reverify")
    @Operation(summary = "Re-verify vehicle via BRTA")
    public ResponseEntity<VehicleResponse> reverifyVehicle(@PathVariable UUID id, HttpServletRequest request) {
        VehicleResponse vehicle = vehicleService.reverifyVehicle(id);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.VEHICLE_REVERIFIED,
                "Vehicle", id.toString(),
                null, Map.of("status", "VERIFIED", "registrationNumber", vehicle.getRegistrationNumber()),
                "Manual BRTA reverification");
        return ResponseEntity.ok(vehicle);
    }

    // ── Reference Data ────────────────────────────────────────────────────────

    @GetMapping("/brta-offices")
    @Operation(summary = "Get all BRTA offices")
    public ResponseEntity<List<BrtaOfficeResponse>> getAllBrtaOffices() {
        return ResponseEntity.ok(brtaOfficeService.getAllOffices());
    }

    @GetMapping("/registration-codes")
    @Operation(summary = "Get all registration codes")
    public ResponseEntity<List<RegistrationCodeResponse>> getAllRegistrationCodes() {
        return ResponseEntity.ok(registrationCodeService.getAllCodes());
    }

    // ── Vehicle Claims ────────────────────────────────────────────────────────

    @GetMapping("/vehicle-claims")
    @Operation(summary = "Get vehicle ownership claims")
    public ResponseEntity<Page<VehicleClaimResponse>> getVehicleClaims(
            @Parameter(description = "Filter by status: PENDING, APPROVED, REJECTED")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        VehicleClaim.ClaimStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = VehicleClaim.ClaimStatus.valueOf(status);
        }
        return ResponseEntity.ok(vehicleClaimService.getAllClaims(statusEnum, pageable));
    }

    @PutMapping("/vehicle-claims/{claimId}/approve")
    @Operation(summary = "Approve vehicle ownership claim")
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
                null, Map.of("status", "APPROVED", "vehicle", claim.getRegistrationNumber()), adminNotes);
        return ResponseEntity.ok(claim);
    }

    @PutMapping("/vehicle-claims/{claimId}/reject")
    @Operation(summary = "Reject vehicle ownership claim")
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
                null, Map.of("status", "REJECTED", "vehicle", claim.getRegistrationNumber()), adminNotes);
        return ResponseEntity.ok(claim);
    }
}

