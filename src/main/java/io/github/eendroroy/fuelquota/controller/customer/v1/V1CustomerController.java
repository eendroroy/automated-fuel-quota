package io.github.eendroroy.fuelquota.controller.customer.v1;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.request.AddVehicleRequest;
import io.github.eendroroy.fuelquota.dto.request.AssignDriverRequest;
import io.github.eendroroy.fuelquota.dto.request.ClaimVehicleRequest;
import io.github.eendroroy.fuelquota.dto.response.*;
import io.github.eendroroy.fuelquota.service.CustomerService;
import io.github.eendroroy.fuelquota.service.VehicleClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/v1")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Tag(name = "Customer v1", description = "Customer vehicle owner app API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class V1CustomerController {

    private final CustomerService customerService;
    private final VehicleClaimService vehicleClaimService;

    @GetMapping("/vehicle")
    @Operation(summary = "Get my primary vehicle")
    public ResponseEntity<VehicleResponse> getMyVehicle(HttpServletRequest request) {
        return ResponseEntity.ok(customerService.getVehicleByUserId((UUID) request.getAttribute("userId")));
    }

    @GetMapping("/quota")
    @Operation(summary = "Get my quota status")
    public ResponseEntity<QuotaResponse> getMyQuota(HttpServletRequest request) {
        return ResponseEntity.ok(customerService.getQuotaByUserId((UUID) request.getAttribute("userId")));
    }

    @GetMapping("/qr-code")
    @Operation(summary = "Generate QR code for first active vehicle")
    public ResponseEntity<QrTokenResponse> getQrCode(HttpServletRequest request) {
        return ResponseEntity.ok(customerService.generateQrToken((UUID) request.getAttribute("userId")));
    }

    @PostMapping("/qr-code/regenerate")
    @Operation(summary = "Regenerate QR code for first active vehicle")
    public ResponseEntity<Map<String, String>> regenerateQrCode(HttpServletRequest request) {
        return ResponseEntity.ok(Map.of("token", customerService.regenerateQrToken((UUID) request.getAttribute("userId"))));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get all my transactions")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(HttpServletRequest request, Pageable pageable) {
        return ResponseEntity.ok(customerService.getTransactionsByUserId((UUID) request.getAttribute("userId"), pageable));
    }

    @GetMapping("/vehicles")
    @Operation(summary = "List all my vehicles")
    public ResponseEntity<Page<VehicleResponse>> getMyVehicles(HttpServletRequest request, Pageable pageable) {
        return ResponseEntity.ok(customerService.getVehiclesByUserId((UUID) request.getAttribute("userId"), pageable));
    }

    @PostMapping("/vehicles")
    @Operation(summary = "Add a new vehicle")
    public ResponseEntity<VehicleResponse> addVehicle(@Valid @RequestBody AddVehicleRequest req, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.addVehicle((UUID) request.getAttribute("userId"), req));
    }

    @DeleteMapping("/vehicles/{vehicleId}")
    @Operation(summary = "Remove a vehicle")
    public ResponseEntity<Void> removeVehicle(@PathVariable UUID vehicleId, HttpServletRequest request) {
        customerService.removeVehicle((UUID) request.getAttribute("userId"), vehicleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vehicles/{vehicleId}/quota")
    @Operation(summary = "Get quota for a specific vehicle")
    public ResponseEntity<QuotaResponse> getVehicleQuota(@PathVariable UUID vehicleId, HttpServletRequest request) {
        return ResponseEntity.ok(customerService.getQuotaByVehicleId((UUID) request.getAttribute("userId"), vehicleId));
    }

    @GetMapping("/vehicles/{vehicleId}/qr-code")
    @Operation(summary = "Generate QR code for a specific vehicle")
    public ResponseEntity<QrTokenResponse> getVehicleQrCode(@PathVariable UUID vehicleId, HttpServletRequest request) {
        return ResponseEntity.ok(customerService.generateQrTokenForVehicle((UUID) request.getAttribute("userId"), vehicleId));
    }

    @PostMapping("/vehicles/{vehicleId}/qr-code/regenerate")
    @Operation(summary = "Regenerate QR code for a specific vehicle")
    public ResponseEntity<Map<String, String>> regenerateVehicleQrCode(@PathVariable UUID vehicleId, HttpServletRequest request) {
        return ResponseEntity.ok(Map.of("token", customerService.regenerateQrTokenForVehicle((UUID) request.getAttribute("userId"), vehicleId)));
    }

    @GetMapping("/vehicles/{vehicleId}/transactions")
    @Operation(summary = "Get transactions for a specific vehicle")
    public ResponseEntity<Page<TransactionResponse>> getVehicleTransactions(
            @PathVariable UUID vehicleId, HttpServletRequest request, Pageable pageable) {
        return ResponseEntity.ok(customerService.getTransactionsByVehicleId((UUID) request.getAttribute("userId"), vehicleId, pageable));
    }

    @PostMapping("/vehicles/claim")
    @Operation(summary = "Claim a registered vehicle")
    public ResponseEntity<VehicleClaimResponse> claimVehicle(@Valid @RequestBody ClaimVehicleRequest req, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleClaimService.submitClaim((UUID) request.getAttribute("userId"), req));
    }

    @GetMapping("/vehicles/claims")
    @Operation(summary = "Get my vehicle ownership claims")
    public ResponseEntity<Page<VehicleClaimResponse>> getMyClaims(HttpServletRequest request, Pageable pageable) {
        return ResponseEntity.ok(vehicleClaimService.getMyClaims((UUID) request.getAttribute("userId"), pageable));
    }

    @PostMapping("/vehicles/{vehicleId}/driver")
    @Operation(summary = "Assign a driver to a vehicle")
    public ResponseEntity<VehicleResponse> assignDriver(
            @PathVariable UUID vehicleId, @Valid @RequestBody AssignDriverRequest req, HttpServletRequest request) {
        return ResponseEntity.ok(customerService.assignDriver((UUID) request.getAttribute("userId"), vehicleId, req.getDriverMobile()));
    }

    @DeleteMapping("/vehicles/{vehicleId}/driver")
    @Operation(summary = "Remove driver from a vehicle")
    public ResponseEntity<VehicleResponse> removeDriver(@PathVariable UUID vehicleId, HttpServletRequest request) {
        return ResponseEntity.ok(customerService.removeDriver((UUID) request.getAttribute("userId"), vehicleId));
    }

    @GetMapping("/vehicles-as-driver")
    @Operation(summary = "Get vehicles where I am the driver")
    public ResponseEntity<List<VehicleResponse>> getVehiclesAsDriver(HttpServletRequest request) {
        return ResponseEntity.ok(customerService.getVehiclesWhereDriver((UUID) request.getAttribute("userId")));
    }
}

