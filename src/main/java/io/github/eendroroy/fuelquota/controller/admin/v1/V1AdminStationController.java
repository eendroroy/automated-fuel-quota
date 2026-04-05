package io.github.eendroroy.fuelquota.controller.admin.v1;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.request.StationRequest;
import io.github.eendroroy.fuelquota.dto.response.StationResponse;
import io.github.eendroroy.fuelquota.entity.AuditLog;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import io.github.eendroroy.fuelquota.service.AuditLogService;
import io.github.eendroroy.fuelquota.service.FuelStationService;
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
@Tag(name = "Admin v1 - Stations", description = "Fuel station management")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class V1AdminStationController {

    private final FuelStationService stationService;
    private final AuditLogService auditLogService;

    @GetMapping("/stations")
    @Operation(summary = "Get all fuel stations")
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

    @GetMapping("/stations/{id}")
    @Operation(summary = "Get fuel station by ID")
    public ResponseEntity<StationResponse> getStation(@PathVariable UUID id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }

    @PostMapping("/stations")
    @Operation(summary = "Create fuel station")
    public ResponseEntity<StationResponse> createStation(
            @Valid @RequestBody StationRequest req, HttpServletRequest request) {
        StationResponse station = stationService.createStation(req);
        auditLogService.log(
                (UUID) request.getAttribute("userId"),
                (String) request.getAttribute("userName"),
                AuditLog.AuditAction.STATION_CREATED,
                "FuelStation", String.valueOf(station.getId()),
                null, Map.of("stationName", station.getStationName(), "stationCode", station.getStationCode()), null);
        return ResponseEntity.ok(station);
    }

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
                null, Map.of("stationName", station.getStationName(), "status", station.getStatus()), null);
        return ResponseEntity.ok(station);
    }

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
                Map.of("stationName", station.getStationName()), null, null);
        return ResponseEntity.ok().build();
    }
}

