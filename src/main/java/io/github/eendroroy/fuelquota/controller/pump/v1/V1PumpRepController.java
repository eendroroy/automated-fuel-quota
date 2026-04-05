package io.github.eendroroy.fuelquota.controller.pump.v1;

import io.github.eendroroy.fuelquota.dto.request.AuthorizationRequest;
import io.github.eendroroy.fuelquota.dto.request.DispenseConfirmationRequest;
import io.github.eendroroy.fuelquota.dto.request.ManualAuthorizationRequest;
import io.github.eendroroy.fuelquota.dto.request.PumpRepLoginRequest;
import io.github.eendroroy.fuelquota.dto.response.AuthorizationResponse;
import io.github.eendroroy.fuelquota.dto.response.DispenseConfirmationResponse;
import io.github.eendroroy.fuelquota.dto.response.PumpRepLoginResponse;
import io.github.eendroroy.fuelquota.service.PumpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Pump Representative App API — v1.
 *
 * <p>All endpoints are public (no JWT required) as specified in the BRD.
 */
@RestController
@RequestMapping("/api/pump-rep/v1")
@RequiredArgsConstructor
@Tag(name = "Pump Rep v1", description = "Pump representative mobile app API (public endpoints as per BRD)")
public class V1PumpRepController {

    private static final Logger logger = LoggerFactory.getLogger(V1PumpRepController.class);

    private final PumpService pumpService;

    @PostMapping("/login")
    @Operation(summary = "Pump representative login")
    public ResponseEntity<PumpRepLoginResponse> pumpRepLogin(@Valid @RequestBody PumpRepLoginRequest request) {
        logger.info("Pump rep login attempt for mobile: {}", request.getMobileNumber());
        return ResponseEntity.ok(pumpService.pumpRepLogin(request));
    }

    @PostMapping("/authorize")
    @Operation(summary = "Authorize fuel dispensing")
    public ResponseEntity<AuthorizationResponse> authorizeDispensing(@Valid @RequestBody AuthorizationRequest request) {
        logger.info("Authorization request received for station: {}", request.getStationId());
        AuthorizationResponse response = pumpService.authorizeDispensing(request);
        logger.info("Authorization decision: {} for vehicle found: {}", response.getDecision(), response.getVehicleFound());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm fuel dispensing")
    public ResponseEntity<DispenseConfirmationResponse> confirmDispensing(@Valid @RequestBody DispenseConfirmationRequest request) {
        logger.info("Dispense confirmation for {} liters at station: {}", request.getDispensedLiters(), request.getStationId());
        DispenseConfirmationResponse response = pumpService.confirmDispensing(request);
        logger.info("Transaction confirmed: {} - {} liters", response.getTransactionReference(), response.getDispensedLiters());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/authorize-manual")
    @Operation(summary = "Authorize by registration number")
    public ResponseEntity<AuthorizationResponse> authorizeByRegistration(@Valid @RequestBody ManualAuthorizationRequest request) {
        logger.info("Manual authorization request for: {}", request.getRegistrationNumber());
        return ResponseEntity.ok(pumpService.authorizeByRegistration(request));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pump Rep API is operational");
    }
}

