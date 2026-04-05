package io.github.eendroroy.fuelquota.controller;

import io.github.eendroroy.fuelquota.dto.request.LoginRequest;
import io.github.eendroroy.fuelquota.dto.request.RegisterCustomerRequest;
import io.github.eendroroy.fuelquota.dto.request.SendOtpRequest;
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
     * Sends an OTP to the given mobile number for registration verification.
     *
     * <p>Currently uses a dummy OTP ({@code 000000}); no SMS is sent.
     * Call this endpoint before {@code POST /api/auth/customer/register}.
     *
     * @param request contains the mobile number to send the OTP to
     * @return success message
     */
    @PostMapping("/customer/send-otp")
    @Operation(
        summary = "Send OTP for mobile verification",
        description = "Sends a 6-digit OTP to the provided mobile number. Currently a dummy OTP (000000) is used — no SMS is sent."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OTP sent successfully",
            content = @Content(schema = @Schema(
                type = "object",
                example = "{\"message\": \"OTP sent to your mobile number.\"}"
            ))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid mobile number format"
        )
    })
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.getMobileNumber());
        return ResponseEntity.ok(Map.of("message", "OTP sent to your mobile number."));
    }

    /**
     * Registers a new customer with optional vehicle details.
     *
     * <p>Creates a {@code User} account and {@code Vehicle} record in {@code VERIFIED} status
     * with an active quota. Login is immediately available using the mobile number.
     *
     * <p><strong>OTP required:</strong> Call {@code POST /api/auth/customer/send-otp} first,
     * then include the received OTP in the {@code otp} field of this request.
     *
     * @param request customer and vehicle registration details (including OTP)
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
