package io.github.eendroroy.fuelquota.controller.admin.v1;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.response.BrtaOfficeResponse;
import io.github.eendroroy.fuelquota.dto.response.RegistrationCodeResponse;
import io.github.eendroroy.fuelquota.dto.response.VehicleResponse;
import io.github.eendroroy.fuelquota.entity.AuditLog;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.service.AuditLogService;
import io.github.eendroroy.fuelquota.service.BrtaOfficeService;
import io.github.eendroroy.fuelquota.service.RegistrationCodeService;
import io.github.eendroroy.fuelquota.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/v1")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin v1 - Vehicles", description = "Vehicle management and BRTA verification")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class V1AdminVehicleController {

    private final VehicleService vehicleService;
    private final AuditLogService auditLogService;
    private final BrtaOfficeService brtaOfficeService;
    private final RegistrationCodeService registrationCodeService;

    // ── Vehicles ──────────────────────────────────────────────────────────────

    @GetMapping("/vehicles")
    @Operation(summary = "Get all vehicles")
    public ResponseEntity<Page<VehicleResponse>> getAllVehicles(
            @Parameter(description = "Search term (registration number / owner name)") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by BRTA office code") @RequestParam(required = false) String brtaCode,
            @Parameter(description = "Filter by vehicle registration code") @RequestParam(required = false) String registrationCode,
            @Parameter(description = "Registration date from") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateFrom,
            @Parameter(description = "Registration date to") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateTo,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Vehicle.VehicleStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = Vehicle.VehicleStatus.valueOf(status);
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registrationDate"));
        return ResponseEntity.ok(vehicleService.getAllVehicles(search, statusEnum, brtaCode, registrationCode,
                registrationDateFrom, registrationDateTo, pageable));
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
}
