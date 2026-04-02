package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for new customer self-registration.
 *
 * <p>Captures both the vehicle owner's personal details and the vehicle
 * specification. On submission the system creates a {@code User}, a
 * {@code Vehicle} in {@code VERIFIED} state, and an {@code ACTIVE} {@code Quota}.
 *
 * <p><strong>Future scope:</strong> OTP verification via mobile number will be
 * required to confirm the customer's phone during registration.
 *
 * <p><strong>Future scope:</strong> BRTA API will be called to verify vehicle
 * ownership before finalising registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer self-registration payload including personal and vehicle information")
public class RegisterCustomerRequest {

    // ── Personal Information ──────────────────────────────────────────────────

    /** Full legal name of the vehicle owner. */
    @NotBlank(message = "Owner name is required")
    @Size(max = 100, message = "Owner name cannot exceed 100 characters")
    @Schema(description = "Full name of the vehicle owner", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerName;

    /** National Identity Document number. Must be unique across the system. */
    @NotBlank(message = "Owner NID is required")
    @Size(max = 20, message = "NID cannot exceed 20 characters")
    @Schema(description = "National Identity Document number (unique)", example = "199012345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerNid;

    /** Mobile phone number in international format. */
    @NotBlank(message = "Owner mobile is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid mobile number")
    @Schema(description = "Owner's mobile number in international format", example = "+8801711123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerMobile;

    /** E-mail address used for login and notifications. Must be unique. */
    @NotBlank(message = "Owner email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Schema(description = "Owner's email address (used for login)", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerEmail;

    /** Initial account password. Minimum 8 characters. */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Account password (min 8 characters)", example = "securePass1!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    // ── Vehicle Registration Number (structured 4-part input) ─────────────────

    /**
     * BRTA office / region code selected from dropdown
     * (e.g. {@code DHAKA METRO}, {@code SYLHET}).
     */
    @NotBlank(message = "BRTA office code is required")
    @Size(max = 50, message = "BRTA office code cannot exceed 50 characters")
    @Schema(description = "BRTA office code (select from available offices)", example = "DHAKA METRO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String brtaOfficeCode;

    /**
     * Vehicle category registration code selected from dropdown
     * (e.g. {@code GA}, {@code KHA}).
     */
    @NotBlank(message = "Vehicle registration code is required")
    @Size(max = 10, message = "Vehicle registration code cannot exceed 10 characters")
    @Schema(description = "Vehicle category registration code (select from available codes)", example = "GA", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleRegistrationCode;

    /**
     * Two-digit serial number part (e.g. {@code 11}).
     * Exactly two numeric digits are required.
     */
    @NotBlank(message = "Serial part 1 (2-digit) is required")
    @Pattern(regexp = "^\\d{2}$", message = "Serial part 1 must be exactly 2 digits")
    @Schema(description = "First serial number component (2 digits)", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serialPart1;

    /**
     * Four-digit serial number part (e.g. {@code 1234}).
     * Exactly four numeric digits are required.
     */
    @NotBlank(message = "Serial part 2 (4-digit) is required")
    @Pattern(regexp = "^\\d{4}$", message = "Serial part 2 must be exactly 4 digits")
    @Schema(description = "Second serial number component (4 digits)", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serialPart2;

    // ── Vehicle Information ───────────────────────────────────────────────────

    /** Manufacturer / brand of the vehicle (e.g. Toyota, Honda). */
    @NotBlank(message = "Vehicle make is required")
    @Size(max = 50, message = "Vehicle make cannot exceed 50 characters")
    @Schema(description = "Vehicle manufacturer / make", example = "Toyota", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleMake;

    /** Body colour of the vehicle. */
    @NotBlank(message = "Vehicle color is required")
    @Size(max = 30, message = "Vehicle color cannot exceed 30 characters")
    @Schema(description = "Vehicle body colour", example = "White", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleColor;

    /** Fuel type the vehicle uses (e.g. Petrol, Diesel, CNG, LPG). */
    @NotBlank(message = "Fuel type is required")
    @Size(max = 30, message = "Fuel type cannot exceed 30 characters")
    @Schema(description = "Type of fuel the vehicle uses", example = "Petrol", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fuelType;

    /** Engine displacement in cubic centimetres (optional). */
    @Schema(description = "Engine displacement in CC (optional)", example = "1500")
    private Integer engineDisplacement;

    /**
     * Date the vehicle was first registered with the transport authority.
     * Format: {@code YYYY-MM-DD}.
     */
    @NotBlank(message = "Registration date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Registration date must be in YYYY-MM-DD format")
    @Schema(description = "Vehicle registration date (YYYY-MM-DD)", example = "2020-05-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registrationDate;

    /**
     * Assembles the four-part input into the canonical registration number string:
     * {@code {brtaOfficeCode} {vehicleRegistrationCode} {serialPart1}-{serialPart2}}.
     *
     * @return assembled registration number, e.g. {@code DHAKA METRO GA 11-1234}
     */
    public String assembleRegistrationNumber() {
        return brtaOfficeCode.toUpperCase().trim() + " "
                + vehicleRegistrationCode.toUpperCase().trim() + " "
                + serialPart1.trim() + "-"
                + serialPart2.trim();
    }
}
