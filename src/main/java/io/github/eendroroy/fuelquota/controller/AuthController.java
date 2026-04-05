package io.github.eendroroy.fuelquota.controller;

import io.github.eendroroy.fuelquota.dto.request.LoginRequest;
import io.github.eendroroy.fuelquota.dto.request.RegisterCustomerRequest;
import io.github.eendroroy.fuelquota.dto.response.AuthResponse;
import io.github.eendroroy.fuelquota.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication and registration endpoints.
 *
 * <p>Handles user authentication and new customer self-registration.
 * All endpoints are public (no JWT required).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User login and customer self-registration")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a customer user by mobile number and returns a JWT access token.
     *
     * @param request login credentials (mobileNumber and password)
     * @return JWT token and user details on successful authentication
     */
    @PostMapping("/customer/login")
    @Operation(
        summary = "Customer login",
        description = "Authenticates a customer by mobile number and returns a JWT access token valid for 24 hours"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid credentials or user is not a customer"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed"
        )
    })
    public ResponseEntity<AuthResponse> customerLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.customerLogin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticates an admin user by email and returns a JWT access token.
     *
     * @param request login credentials (email and password)
     * @return JWT token and user details on successful authentication
     */
    @PostMapping("/admin/login")
    @Operation(
        summary = "Admin login",
        description = "Authenticates an admin user by email and returns a JWT access token valid for 24 hours"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid credentials or user is not an admin"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed"
        )
    })
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.adminLogin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new customer with optional vehicle details.
     *
     * <p>Creates a {@code User} account and {@code Vehicle} record in {@code VERIFIED} status
     * with an active quota. Login is immediately available using the mobile number.
     *
     * <p><strong>Email is optional:</strong> Customers can register without providing an email address.
     * If provided, the email must be unique.
     *
     * <p><strong>Future scope:</strong> OTP verification and BRTA ownership check will
     * be required before account activation.
     *
     * @param request customer and vehicle registration details
     * @return success message
     */
    @PostMapping("/customer/register")
    @Operation(
        summary = "Customer self-registration",
        description = "Registers a new customer with vehicle details (optional). Vehicle is automatically verified. Login is available immediately via mobile number."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Registration successful",
            content = @Content(schema = @Schema(
                type = "object",
                example = "{\"message\": \"Registration successful. You can now log in.\"}"
            ))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Mobile number already exists, or registration/NID duplicate"
        )
    })
    public ResponseEntity<Map<String, String>> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        authService.registerCustomer(request);
        return ResponseEntity.ok(Map.of(
            "message", "Registration successful. You can now log in."
        ));
    }
}
