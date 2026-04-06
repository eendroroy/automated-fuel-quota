package io.github.eendroroy.fuelquota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for an authenticated customer to add a new vehicle to their account.
 *
 * <p>Owner name and email are derived from the authenticated user's account;
 * only vehicle-specific details and the NID/mobile from the vehicle document are required.
 *
 * <p>The registration number is composed from four structured parts:
 * BRTA office code, vehicle registration code, 2-digit serial and 4-digit serial.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Add a new vehicle to an existing customer account")
public class AddVehicleRequest {

    // ── Registration Number (4-part structured input) ──────────────────────────

    @NotBlank(message = "BRTA office code is required")
    @Size(max = 50)
    @Schema(description = "BRTA office code", example = "DHAKA METRO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String brtaOfficeCode;

    @NotBlank(message = "Vehicle registration code is required")
    @Size(max = 10)
    @Schema(description = "Vehicle category registration code", example = "GA", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleRegistrationCode;

    @NotBlank(message = "Serial part 1 (2-digit) is required")
    @Pattern(regexp = "^\\d{2}$", message = "Serial part 1 must be exactly 2 digits")
    @Schema(description = "First serial number component (2 digits)", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serialPart1;

    @NotBlank(message = "Serial part 2 (4-digit) is required")
    @Pattern(regexp = "^\\d{4}$", message = "Serial part 2 must be exactly 4 digits")
    @Schema(description = "Second serial number component (4 digits)", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serialPart2;

    // ── Owner details (from vehicle document) ─────────────────────────────────
    // Optional — if omitted the service derives them from the authenticated user's profile.

    @Size(max = 20, message = "NID cannot exceed 20 characters")
    @Schema(description = "Owner NID from vehicle registration document (optional — defaults to account NID)",
            example = "199012345678")
    private String ownerNid;

    @Pattern(regexp = "^(\\+?[1-9]\\d{1,14})?$", message = "Please provide a valid mobile number")
    @Schema(description = "Owner mobile number (optional — defaults to account mobile)",
            example = "+8801711123456")
    private String ownerMobile;

    // ── Vehicle details ────────────────────────────────────────────────────────

    @NotBlank(message = "Vehicle make is required")
    @Size(max = 50, message = "Vehicle make cannot exceed 50 characters")
    @Schema(description = "Vehicle manufacturer / make", example = "Honda", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleMake;

    @NotBlank(message = "Vehicle color is required")
    @Size(max = 30, message = "Vehicle color cannot exceed 30 characters")
    @Schema(description = "Vehicle body colour", example = "Black", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleColor;

    @NotBlank(message = "Fuel type is required")
    @Size(max = 30, message = "Fuel type cannot exceed 30 characters")
    @Schema(description = "Type of fuel the vehicle uses", example = "Petrol", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fuelType;

    /**
     * Optional secondary fuel types the vehicle can use in addition to the primary
     * (e.g. CNG as an alternative for a Petrol vehicle).
     */
    @Schema(description = "Optional secondary fuel types (e.g. CNG for a Petrol vehicle)", example = "[\"CNG\"]")
    private List<String> secondaryFuelTypes;

    /** Engine displacement in cubic centimetres (optional). */
    @Schema(description = "Engine displacement in CC (optional)", example = "1500")
    private Integer engineDisplacement;

    @NotBlank(message = "Registration date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Registration date must be in YYYY-MM-DD format")
    @Schema(description = "Vehicle registration date (YYYY-MM-DD)", example = "2022-08-20", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registrationDate;

    /**
     * Assembles the four-part input into the canonical registration number string.
     *
     * @return e.g. {@code DHAKA METRO GA 11-1234}
     */
    public String assembleRegistrationNumber() {
        return brtaOfficeCode.toUpperCase().trim() + " "
                + vehicleRegistrationCode.toUpperCase().trim() + " "
                + serialPart1.trim() + "-"
                + serialPart2.trim();
    }
}
