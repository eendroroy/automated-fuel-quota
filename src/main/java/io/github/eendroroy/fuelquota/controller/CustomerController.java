package io.github.eendroroy.fuelquota.controller;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.request.AddVehicleRequest;
import io.github.eendroroy.fuelquota.dto.request.ClaimVehicleRequest;
import io.github.eendroroy.fuelquota.dto.response.QuotaResponse;
import io.github.eendroroy.fuelquota.dto.response.QrTokenResponse;
import io.github.eendroroy.fuelquota.dto.response.VehicleResponse;
import io.github.eendroroy.fuelquota.dto.response.VehicleClaimResponse;
import io.github.eendroroy.fuelquota.dto.response.TransactionResponse;
import io.github.eendroroy.fuelquota.service.CustomerService;
import io.github.eendroroy.fuelquota.service.VehicleClaimService;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Customer API Controller for Vehicle Owner App.
 * <p>
 * <p>Implements BRD requirements:
 * <ul>
 *   <li>FR-01: Generate QR code for fuel authorization</li>
 *   <li>FR-02: View quota status</li>
 *   <li>FR-03: View fuel transaction history</li>
 *   <li>FR-14: Provide data retrieval APIs for customer app</li>
 * </ul>
 *
 * <p><strong>Security:</strong> All endpoints require CUSTOMER role authentication.
 * The authenticated user's ID is automatically extracted from the JWT token.
 */
@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer vehicle owner app API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CustomerController {

    private final CustomerService customerService;
    private final VehicleClaimService vehicleClaimService;

    // ── Single vehicle (backward-compatible) ──────────────────────────────────

    /**
     * Retrieves the authenticated customer's vehicle information.
     *
     * @param request HTTP request containing the authenticated user's details
     * @return vehicle details for the authenticated customer
     */
    @GetMapping("/vehicle")
    @Operation(
        summary = "Get my primary vehicle",
        description = "Returns the first vehicle for the authenticated customer (backward-compatible)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vehicle details retrieved successfully",
            content = @Content(schema = @Schema(implementation = VehicleResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No vehicle found for the authenticated customer")
    })
    public ResponseEntity<VehicleResponse> getMyVehicle(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        VehicleResponse vehicle = customerService.getVehicleByUserId(userId);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Retrieves the authenticated customer's current quota status (BRD FR-02).
     *
     * @param request HTTP request containing the authenticated user's details
     * @return current weekly quota status including used/remaining liters
     */
    @GetMapping("/quota")
    @Operation(
        summary = "Get my quota status",
        description = "Returns current weekly fuel quota status with remaining liters (BRD FR-02)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Quota status retrieved successfully",
            content = @Content(schema = @Schema(implementation = QuotaResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No quota found for the authenticated customer")
    })
    public ResponseEntity<QuotaResponse> getMyQuota(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        QuotaResponse quota = customerService.getQuotaByUserId(userId);
        return ResponseEntity.ok(quota);
    }

    /**
     * Generates/retrieves a QR code for fuel authorization (BRD FR-01).
     *
     * <p>The QR token is a signed JWT with 1-hour expiration containing the vehicle ID
     * and registration number. Present this QR code to pump representatives for fuel dispensing.
     *
     * @param request HTTP request containing the authenticated user's details
     * @return QR token and metadata for display as a QR code image
     */
    @GetMapping("/qr-code")
    @Operation(
        summary = "Generate QR code for first active vehicle",
        description = "Generates a QR token for the first active vehicle (backward-compatible)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "QR token generated successfully",
            content = @Content(schema = @Schema(implementation = QrTokenResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Vehicle is not active - cannot generate QR code"),
        @ApiResponse(responseCode = "404", description = "No vehicle found for the authenticated customer")
    })
    public ResponseEntity<QrTokenResponse> getQrCode(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        QrTokenResponse qrToken = customerService.generateQrToken(userId);
        return ResponseEntity.ok(qrToken);
    }

    /**
     * Regenerates a new QR code, invalidating any previous one.
     *
     * @param request HTTP request containing the authenticated user's details
     * @return new QR token string
     */
    @PostMapping("/qr-code/regenerate")
    @Operation(
        summary = "Regenerate QR code for first active vehicle",
        description = "Generates a new QR token, invalidating the previous one"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "New QR token generated",
            content = @Content(schema = @Schema(
                type = "object",
                example = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"
            ))
        ),
        @ApiResponse(responseCode = "400", description = "Vehicle is not active - cannot generate QR code")
    })
    public ResponseEntity<Map<String, String>> regenerateQrCode(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        String newToken = customerService.regenerateQrToken(userId);
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    /**
     * Retrieves the authenticated customer's fuel transaction history (BRD FR-03).
     *
     * @param request  HTTP request containing the authenticated user's details
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of fuel transactions for the customer's vehicle
     */
    @GetMapping("/transactions")
    @Operation(
        summary = "Get all my transactions",
        description = "Returns paginated transactions across all the customer's vehicles"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transaction history retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(responseCode = "404", description = "No vehicle found for the authenticated customer")
    })
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            HttpServletRequest request, Pageable pageable) {
        UUID userId = (UUID) request.getAttribute("userId");
        Page<TransactionResponse> transactions = customerService.getTransactionsByUserId(userId, pageable);
        return ResponseEntity.ok(transactions);
    }

    // ── Multi-vehicle management ───────────────────────────────────────────────

    /**
     * Retrieves a list of all vehicles registered to the authenticated customer.
     *
     * @param request HTTP request containing the authenticated user's details
     * @return list of vehicles for the authenticated customer
     */
    @GetMapping("/vehicles")
    @Operation(
        summary = "List all my vehicles",
        description = "Returns all vehicles registered to the authenticated customer"
    )
    @ApiResponse(responseCode = "200", description = "Vehicles retrieved successfully",
            content = @Content(schema = @Schema(implementation = VehicleResponse.class)))
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(customerService.getVehiclesByUserId(userId));
    }

    /**
     * Registers a new vehicle to the customer's account.
     *
     * <p>The vehicle starts with a PENDING status and requires admin approval.
     *
     * @param req     vehicle details to register
     * @param request HTTP request containing the authenticated user's details
     * @return details of the newly added vehicle
     */
    @PostMapping("/vehicles")
    @Operation(
        summary = "Add a new vehicle",
        description = "Registers a new vehicle to the customer's account (starts as PENDING, requires admin approval)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehicle added successfully"),
            @ApiResponse(responseCode = "400", description = "Registration number already exists or validation failed")
    })
    public ResponseEntity<VehicleResponse> addVehicle(
            @Valid @RequestBody AddVehicleRequest req, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.addVehicle(userId, req));
    }

    /**
     * Removes a vehicle from the customer's account.
     *
     * <p>PENDING/REJECTED vehicles with no transactions are deleted outright.
     * All others are marked DEREGISTERED to preserve transaction history.
     *
     * @param vehicleId UUID of the vehicle to remove
     * @param request   HTTP request containing the authenticated user's details
     * @return 204 No Content if successful, 404 if vehicle not found
     */
    @DeleteMapping("/vehicles/{vehicleId}")
    @Operation(
        summary = "Remove a vehicle",
        description = "Removes a vehicle from the customer's account. " +
                "PENDING/REJECTED vehicles with no transactions are deleted outright. " +
                "All others are marked DEREGISTERED to preserve transaction history."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vehicle removed successfully"),
            @ApiResponse(responseCode = "400", description = "Vehicle is suspended or does not belong to this user"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<Void> removeVehicle(
            @PathVariable UUID vehicleId, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        customerService.removeVehicle(userId, vehicleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the quota status for a specific vehicle.
     *
     * @param vehicleId UUID of the vehicle
     * @param request   HTTP request containing the authenticated user's details
     * @return quota status for the specified vehicle
     */
    @GetMapping("/vehicles/{vehicleId}/quota")
    @Operation(
        summary = "Get quota for a specific vehicle",
        description = "Returns the current quota status for the specified vehicle"
    )
    public ResponseEntity<QuotaResponse> getVehicleQuota(
            @PathVariable UUID vehicleId, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(customerService.getQuotaByVehicleId(userId, vehicleId));
    }

    /**
     * Generates a QR code for a specific vehicle.
     *
     * @param vehicleId UUID of the vehicle
     * @param request   HTTP request containing the authenticated user's details
     * @return QR token and metadata for the specified vehicle
     */
    @GetMapping("/vehicles/{vehicleId}/qr-code")
    @Operation(
        summary = "Generate QR code for a specific vehicle",
        description = "Generates a QR token for the specified vehicle"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "QR token generated",
                    content = @Content(schema = @Schema(implementation = QrTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Vehicle not active")
    })
    public ResponseEntity<QrTokenResponse> getVehicleQrCode(
            @PathVariable UUID vehicleId, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(customerService.generateQrTokenForVehicle(userId, vehicleId));
    }

    /**
     * Regenerates the QR code for a specific vehicle.
     *
     * @param vehicleId UUID of the vehicle
     * @param request   HTTP request containing the authenticated user's details
     * @return new QR token string for the specified vehicle
     */
    @PostMapping("/vehicles/{vehicleId}/qr-code/regenerate")
    @Operation(
        summary = "Regenerate QR code for a specific vehicle",
        description = "Generates a new QR token for the specified vehicle, invalidating the previous one"
    )
    public ResponseEntity<Map<String, String>> regenerateVehicleQrCode(
            @PathVariable UUID vehicleId, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(Map.of("token", customerService.regenerateQrTokenForVehicle(userId, vehicleId)));
    }

    /**
     * Retrieves the fuel transaction history for a specific vehicle.
     *
     * @param vehicleId UUID of the vehicle
     * @param request   HTTP request containing the authenticated user's details
     * @param pageable  pagination parameters (page, size, sort)
     * @return paginated list of fuel transactions for the specified vehicle
     */
    @GetMapping("/vehicles/{vehicleId}/transactions")
    @Operation(
        summary = "Get transactions for a specific vehicle",
        description = "Returns paginated fuel transaction history for the specified vehicle"
    )
    public ResponseEntity<Page<TransactionResponse>> getVehicleTransactions(
            @PathVariable UUID vehicleId, HttpServletRequest request, Pageable pageable) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(customerService.getTransactionsByVehicleId(userId, vehicleId, pageable));
    }

    // ── Vehicle ownership claims ───────────────────────────────────────────────

    /**
     * Submits an ownership claim for an already-registered vehicle.
     *
     * <p>Typical use case: a customer purchases a second-hand vehicle and wants
     * to transfer its registration to their account.  The claim is reviewed by an
     * admin.
     *
     * <p><strong>Future scope:</strong> OTP verification of the claimant's mobile
     * number and automatic BRTA ownership check will be required before the claim
     * is accepted.
     *
     * @param req     claim details (registration number, NID, reason)
     * @param request HTTP request containing the authenticated user's details
     * @return the created claim record
     */
    @PostMapping("/vehicles/claim")
    @Operation(
        summary = "Claim a registered vehicle",
        description = "Submit an ownership claim for an already-registered vehicle "
                + "(e.g. second-hand purchase). Requires admin approval. "
                + "FUTURE SCOPE: BRTA API and OTP verification will be required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Claim submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate pending claim"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<VehicleClaimResponse> claimVehicle(
            @Valid @RequestBody ClaimVehicleRequest req, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleClaimService.submitClaim(userId, req));
    }

    /**
     * Returns paginated ownership claims submitted by the authenticated customer.
     *
     * @param request  HTTP request containing the authenticated user's details
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of the customer's claim records
     */
    @GetMapping("/vehicles/claims")
    @Operation(
        summary = "Get my vehicle ownership claims",
        description = "Returns paginated ownership claims submitted by the authenticated customer"
    )
    @ApiResponse(responseCode = "200", description = "Claims retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class)))
    public ResponseEntity<Page<VehicleClaimResponse>> getMyClaims(
            HttpServletRequest request, Pageable pageable) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(vehicleClaimService.getMyClaims(userId, pageable));
    }
}
