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
     * Authenticates a customer user by mobile number and returns a JWT access token.
     *
     * @param request login credentials (mobileNumber and password)
     * @return {@link AuthResponse} containing the JWT token and user details
     * @throws BadRequestException if the user is not a customer or credentials are invalid
     */
    public AuthResponse customerLogin(LoginRequest request) {
        // Authenticate using Spring Security (mobileNumber as username)
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getMobileNumber(), request.getPassword())
        );

        User user = userRepository.findByMobileNumber(request.getMobileNumber())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != User.UserRole.CUSTOMER) {
            throw new BadRequestException("Invalid credentials");
        }

        String token = tokenProvider.generateAccessToken(user);
        return authMapper.toResponse(token, user);
    }

    /**
     * Authenticates an admin user by email and returns a JWT access token.
     *
     * @param request login credentials (email and password)
     * @return {@link AuthResponse} containing the JWT token and user details
     * @throws BadRequestException if the user is not an admin or credentials are invalid
     */
    public AuthResponse adminLogin(LoginRequest request) {
        // Admin login uses email
        String adminEmail = request.getMobileNumber(); // Contains email for admin login

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(adminEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != User.UserRole.ADMIN) {
            throw new BadRequestException("Invalid credentials");
        }

        String token = tokenProvider.generateAccessToken(user);
        return authMapper.toResponse(token, user);
    }

    /**
     * Registers a new customer, creating a {@link User} account.
     * Optionally creates an associated {@link Vehicle} record if vehicle details are provided.
     *
     * <p>The registration number is assembled from the four structured input parts:
     * {@code {brtaOfficeCode} {vehicleRegistrationCode} {serialPart1}-{serialPart2}}.
     *
     * <p>The vehicle class description is derived from the {@link RegistrationCode}
     * lookup table seeded at startup.
     *
     * <p>If vehicle information is not provided, only the user account is created,
     * allowing drivers without vehicles to register.
     *
     * <p><strong>Future scope:</strong> OTP verification will be required to confirm the
     * mobile number before account activation.
     *
     * <p><strong>Future scope:</strong> BRTA API will be called to verify vehicle
     * ownership before finalising registration.
     *
     * @param request customer registration details
     * @throws BadRequestException if mobile number already exists, or if vehicle info is provided but incomplete/duplicate
     */
    public void registerCustomer(RegisterCustomerRequest request) {
        // Validation - mobile number must always be unique
        if (userRepository.existsByMobileNumber(request.getOwnerMobile())) {
            throw new BadRequestException("Mobile number already exists");
        }

        // Validation - email must be unique if provided
        if (request.getOwnerEmail() != null && !request.getOwnerEmail().isBlank() &&
            userRepository.existsByEmail(request.getOwnerEmail())) {
            throw new BadRequestException("Email already exists");
        }

        // Create user account with mobile number as unique identifier
        User user = new User(
            request.getOwnerEmail(), // Can be null for customer-only accounts
            passwordEncoder.encode(request.getPassword()),
            request.getOwnerName(),
            User.UserRole.CUSTOMER
        );
        user.setMobileNumber(request.getOwnerMobile());
        user = userRepository.save(user);

        // Only create vehicle if vehicle information is provided
        if (request.hasVehicleInfo()) {
            String registrationNumber = request.assembleRegistrationNumber();

            // Validate all vehicle fields are provided if any is provided
            if (registrationNumber == null) {
                throw new BadRequestException("If providing vehicle information, all registration number fields are required");
            }
            if (request.getVehicleMake() == null || request.getVehicleMake().isBlank()) {
                throw new BadRequestException("Vehicle make is required when registering a vehicle");
            }
            if (request.getVehicleColor() == null || request.getVehicleColor().isBlank()) {
                throw new BadRequestException("Vehicle color is required when registering a vehicle");
            }
            if (request.getFuelType() == null || request.getFuelType().isBlank()) {
                throw new BadRequestException("Fuel type is required when registering a vehicle");
            }
            if (request.getRegistrationDate() == null || request.getRegistrationDate().isBlank()) {
                throw new BadRequestException("Registration date is required when registering a vehicle");
            }

            // Check for duplicate registration number
            if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) {
                throw new BadRequestException("Vehicle registration number already exists");
            }

            // Check for duplicate NID
            if (vehicleRepository.existsByOwnerNid(request.getOwnerNid())) {
                throw new BadRequestException("NID already registered");
            }

            // Resolve vehicle class description from registration code
            String vehicleClass = registrationCodeRepository
                    .findByCode(request.getVehicleRegistrationCode().toUpperCase().trim())
                    .map(rc -> rc.getDescription())
                    .orElse(request.getVehicleRegistrationCode());

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
