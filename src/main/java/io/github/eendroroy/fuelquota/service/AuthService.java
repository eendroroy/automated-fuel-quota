package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.LoginRequest;
import io.github.eendroroy.fuelquota.dto.request.RegisterCustomerRequest;
import io.github.eendroroy.fuelquota.dto.response.AuthResponse;
import io.github.eendroroy.fuelquota.entity.User;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.entity.Quota;
import io.github.eendroroy.fuelquota.config.AppProperties;
import io.github.eendroroy.fuelquota.repository.UserRepository;
import io.github.eendroroy.fuelquota.repository.VehicleRepository;
import io.github.eendroroy.fuelquota.repository.QuotaRepository;
import io.github.eendroroy.fuelquota.repository.RegistrationCodeRepository;
import io.github.eendroroy.fuelquota.security.JwtTokenProvider;
import io.github.eendroroy.fuelquota.mapper.AuthMapper;
import io.github.eendroroy.fuelquota.exception.BadRequestException;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;

import io.github.eendroroy.fuelquota.entity.RegistrationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Authentication and user registration service.
 *
 * <p>Handles login for customers and admins, as well as new customer self-registration.
 * Authentication is performed via Spring Security with BCrypt password hashing and
 * JWT token generation.
 *
 * <p>Handles user registration, login, and JWT token generation.
 *
 * <p><strong>Future scope:</strong> OTP verification via mobile number will be added
 * to confirm the customer's phone during registration before account activation.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final QuotaRepository quotaRepository;
    private final AppProperties appProperties;
    private final QuotaConfigService quotaConfigService;
    private final RegistrationCodeRepository registrationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuthMapper authMapper;

    /**
     * Authenticates a customer user and returns a JWT access token.
     *
     * @param request login credentials
     * @return {@link AuthResponse} containing the JWT token and user details
     * @throws BadRequestException if the user is not a customer or credentials are invalid
     */
    public AuthResponse customerLogin(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != User.UserRole.CUSTOMER) {
            throw new BadRequestException("Invalid credentials");
        }

        String token = tokenProvider.generateAccessToken(user);
        return authMapper.toResponse(token, user);
    }

    /**
     * Authenticates an admin user and returns a JWT access token.
     *
     * @param request login credentials
     * @return {@link AuthResponse} containing the JWT token and user details
     * @throws BadRequestException if the user is not an admin or credentials are invalid
     */
    public AuthResponse adminLogin(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != User.UserRole.ADMIN) {
            throw new BadRequestException("Invalid credentials");
        }

        String token = tokenProvider.generateAccessToken(user);
        return authMapper.toResponse(token, user);
    }

    /**
     * Registers a new customer, creating both a {@link User} account and an associated
     * {@link Vehicle} record in {@code VERIFIED} status with an {@code ACTIVE} {@link Quota}.
     *
     * <p>The registration number is assembled from the four structured input parts:
     * {@code {brtaOfficeCode} {vehicleRegistrationCode} {serialPart1}-{serialPart2}}.
     *
     * <p>The vehicle class description is derived from the {@link RegistrationCode}
     * lookup table seeded at startup.
     *
     * <p><strong>Future scope:</strong> OTP verification will be required to confirm the
     * mobile number before account activation.
     *
     * <p><strong>Future scope:</strong> BRTA API will be called to verify vehicle
     * ownership before finalising registration.
     *
     * @param request customer registration details
     * @throws BadRequestException if email, registration number, or NID already exists
     */
    public void registerCustomer(RegisterCustomerRequest request) {
        String registrationNumber = request.assembleRegistrationNumber();

        // Validation
        if (userRepository.existsByEmail(request.getOwnerEmail())) {
            throw new BadRequestException("Email already exists");
        }

        if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new BadRequestException("Vehicle registration number already exists");
        }

        if (vehicleRepository.existsByOwnerNid(request.getOwnerNid())) {
            throw new BadRequestException("NID already registered");
        }

        // Resolve vehicle class description from registration code
        String vehicleClass = registrationCodeRepository
                .findByCode(request.getVehicleRegistrationCode().toUpperCase().trim())
                .map(rc -> rc.getDescription())
                .orElse(request.getVehicleRegistrationCode());

        // Create user account — mobile number stored for future OTP verification
        User user = new User(
            request.getOwnerEmail(),
            passwordEncoder.encode(request.getPassword()),
            request.getOwnerName(),
            User.UserRole.CUSTOMER
        );
        user.setMobileNumber(request.getOwnerMobile());
        user = userRepository.save(user);

        // Create vehicle record — automatically VERIFIED
        Vehicle vehicle = new Vehicle(
            registrationNumber,
            request.getBrtaOfficeCode().toUpperCase().trim(),
            request.getVehicleRegistrationCode().toUpperCase().trim(),
            request.getOwnerName(),
            request.getOwnerNid(),
            request.getOwnerMobile(),
            request.getOwnerEmail(),
            request.getVehicleMake(),
            request.getVehicleColor(),
            vehicleClass,
            request.getFuelType(),
            LocalDate.parse(request.getRegistrationDate())
        );
        vehicle.setStatus(Vehicle.VehicleStatus.VERIFIED);
        vehicle.setUser(user);
        if (request.getEngineDisplacement() != null) {
            vehicle.setEngineDisplacement(request.getEngineDisplacement());
        }
        vehicle = vehicleRepository.save(vehicle);

        // Create quota immediately as ACTIVE (no admin approval required)
        BigDecimal limit = quotaConfigService.getDefaultLimitLitres();
        Quota quota = new Quota(vehicle, limit, quotaConfigService.getDefaultPeriod());
        quota.setStatus(Quota.QuotaStatus.ACTIVE);
        quotaRepository.save(quota);
    }

    /**
     * Retrieves a user by UUID.
     *
     * @param id UUID of the user
     * @return the {@link User} entity
     * @throws ResourceNotFoundException if the user is not found
     */
    // ...existing code...
    public User findUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Retrieves a user by email address.
     *
     * @param email email address of the user
     * @return the {@link User} entity
     * @throws ResourceNotFoundException if the user is not found
     */
    // ...existing code...
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
