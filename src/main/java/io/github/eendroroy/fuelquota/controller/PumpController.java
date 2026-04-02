package io.github.eendroroy.fuelquota.controller;

import io.github.eendroroy.fuelquota.dto.request.AuthorizationRequest;
import io.github.eendroroy.fuelquota.dto.request.DispenseConfirmationRequest;
import io.github.eendroroy.fuelquota.dto.response.AuthorizationResponse;
import io.github.eendroroy.fuelquota.dto.response.DispenseConfirmationResponse;
import io.github.eendroroy.fuelquota.service.PumpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Pump Representative App API Controller.
 *
 * <p>Implements BRD requirements for the pump representative mobile app:
 * <ul>
 *   <li>FR-04: Scan QR code</li>
 *   <li>FR-05: Request Authorization (secure web service call)</li>
 *   <li>FR-06: Display Authorization Result</li>
 *   <li>FR-07: Confirm Dispense</li>
 * </ul>
 *
 * <p><strong>Security:</strong> These endpoints are intentionally public
 * (no JWT required) as specified in the BRD. The pump representative app
 * authenticates via QR token validation and station/representative ID verification.
 */
@RestController
@RequestMapping("/api/pump")
@RequiredArgsConstructor
@Tag(name = "Pump", description = "Pump representative mobile app API (public endpoints as per BRD)")
public class PumpController {

    private static final Logger logger = LoggerFactory.getLogger(PumpController.class);

    private final PumpService pumpService;

    /**
     * Authorizes fuel dispensing based on a scanned QR token (BRD FR-04, FR-05, FR-06).
     *
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Pump representative scans customer's QR code via mobile app</li>
     *   <li>App calls this endpoint with the QR token and station/location details</li>
     *   <li>System validates token, checks vehicle status, verifies GPS geofence</li>
     *   <li>Returns authorization decision with vehicle info and authorized fuel amount</li>
     * </ol>
     *
     * <p><strong>Partial Dispense:</strong> If requested amount exceeds remaining quota,
     * the system authorizes only the available amount (BRD FR-12).
     *
     * @param request authorization request containing QR token and pump details
     * @return authorization decision with vehicle info and authorized fuel amount
     */
    @PostMapping("/authorize")
    @Operation(
        summary = "Authorize fuel dispensing",
        description = "Validates QR token, checks vehicle eligibility, verifies geofence, and returns authorization decision with partial dispense support"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authorization decision returned (may be APPROVED, PARTIAL, or DENIED)",
            content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload or malformed QR token"
        )
    })
    public ResponseEntity<AuthorizationResponse> authorizeDispensing(@Valid @RequestBody AuthorizationRequest request) {
        logger.info("Authorization request received for station: {}", request.getStationId());

        AuthorizationResponse response = pumpService.authorizeDispensing(request);

        logger.info("Authorization decision: {} for vehicle found: {}",
            response.getDecision(), response.getVehicleFound());

        return ResponseEntity.ok(response);
    }

    /**
     * Confirms actual fuel dispensing and records the transaction (BRD FR-07, FR-13).
     *
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>After authorization is obtained, pump representative dispenses actual fuel</li>
     *   <li>App calls this endpoint with exact amount dispensed and transaction details</li>
     *   <li>System re-validates QR token, consumes quota, records transaction</li>
     *   <li>Returns transaction receipt with reference number</li>
     * </ol>
     *
     * <p><strong>Idempotency:</strong> Each QR token can only be used once.
     * Duplicate confirmations will return a 400 error.
     *
     * @param request dispense confirmation with exact fuel amount and details
     * @return transaction receipt with reference number and remaining quota
     */
    @PostMapping("/confirm")
    @Operation(
        summary = "Confirm fuel dispensing",
        description = "Records transaction, consumes quota, and returns receipt. Idempotent - each QR token can only be used once."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transaction recorded successfully",
            content = @Content(schema = @Schema(implementation = DispenseConfirmationResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "QR token already used, invalid dispensed amount, or validation failure"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Vehicle, station, or pump representative not found"
        )
    })
    public ResponseEntity<DispenseConfirmationResponse> confirmDispensing(
            @Valid @RequestBody DispenseConfirmationRequest request) {

        logger.info("Dispense confirmation request for {} liters at station: {}",
            request.getDispensedLiters(), request.getStationId());

        DispenseConfirmationResponse response = pumpService.confirmDispensing(request);

        logger.info("Transaction confirmed: {} - {} liters dispensed",
            response.getTransactionReference(), response.getDispensedLiters());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for pump app connectivity testing.
     *
     * @return simple status message indicating the API is operational
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Simple connectivity test for pump representative mobile apps"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API is operational",
        content = @Content(schema = @Schema(type = "string", example = "Pump API is operational"))
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pump API is operational");
    }
}
