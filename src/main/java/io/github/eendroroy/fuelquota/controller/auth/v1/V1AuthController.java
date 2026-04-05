package io.github.eendroroy.fuelquota.controller.auth.v1;

import io.github.eendroroy.fuelquota.dto.request.LoginRequest;
import io.github.eendroroy.fuelquota.dto.request.RegisterCustomerRequest;
import io.github.eendroroy.fuelquota.dto.request.SendOtpRequest;
import io.github.eendroroy.fuelquota.dto.response.AuthResponse;
import io.github.eendroroy.fuelquota.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoints — v1.
 */
@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
@Tag(name = "Auth v1", description = "User authentication and customer registration")
public class V1AuthController {

    private final AuthService authService;

    @PostMapping("/customer/login")
    @Operation(summary = "Customer login")
    public ResponseEntity<AuthResponse> customerLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.customerLogin(request));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Admin login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.adminLogin(request));
    }

    @PostMapping("/customer/send-otp")
    @Operation(summary = "Send OTP for mobile verification")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.getMobileNumber());
        return ResponseEntity.ok(Map.of("message", "OTP sent to your mobile number."));
    }

    @PostMapping("/customer/register")
    @Operation(summary = "Customer self-registration")
    public ResponseEntity<Map<String, String>> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        authService.registerCustomer(request);
        return ResponseEntity.ok(Map.of("message", "Registration successful. You can now log in."));
    }
}

