package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vehicle details returned to the authenticated customer or admin.
 *
 * <p>Returned by:
 * <ul>
 *   <li>{@code GET /api/customer/vehicle} – customer's own vehicle info</li>
 *   <li>{@code GET /api/admin/vehicles} / {@code GET /api/admin/vehicles/{id}} – admin view</li>
 * </ul>
 * The {@code user}, {@code quota}, and other lazy associations are intentionally
 * excluded to prevent serialisation issues and avoid over-fetching.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vehicle details")
public class VehicleResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** String-serialised UUID of the vehicle record. */
    @Schema(description = "Vehicle UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    /** String-serialised UUID of the owning user account. */
    @Schema(description = "Owner User UUID")
    private String userId;

    /** Official registration plate number (assembled from 4 parts). */
    @Schema(description = "Vehicle registration number", example = "DHAKA METRO GA 11-1234")
    private String registrationNumber;

    /** BRTA office / region code component (e.g. {@code DHAKA METRO}). */
    @Schema(description = "BRTA office code", example = "DHAKA METRO")
    private String brtaOfficeCode;

    /** Vehicle category registration code component (e.g. {@code GA}). */
    @Schema(description = "Vehicle category registration code", example = "GA")
    private String vehicleRegistrationCode;

    /** Full legal name of the vehicle owner. */
    @Schema(description = "Owner's full name", example = "John Doe")
    private String ownerName;

    /** Owner's NID number. */
    @Schema(description = "Owner NID", example = "199012345678")
    private String ownerNid;

    /** Owner's mobile number. */
    @Schema(description = "Owner mobile number", example = "01711123456")
    private String ownerMobile;

    /** Owner's e-mail address. */
    @Schema(description = "Owner email address", example = "john@example.com")
    private String ownerEmail;

    /** String-serialised UUID of the assigned driver account (if any). */
    @Schema(description = "Driver User UUID (if assigned)")
    private String driverId;

    /** Driver's full name (if assigned). */
    @Schema(description = "Driver's full name", example = "Jane Smith")
    private String driverName;

    /** Driver's mobile number (if assigned). */
    @Schema(description = "Driver's mobile number", example = "01711123456")
    private String driverMobile;

    /** Manufacturer / brand (e.g. Toyota, Honda). */
    @Schema(description = "Vehicle manufacturer / make", example = "Toyota")
    private String vehicleMake;

    /** Body colour. */
    @Schema(description = "Vehicle body colour", example = "White")
    private String vehicleColor;

    /** Regulatory vehicle class (derived from registration code description). */
    @Schema(description = "Regulatory vehicle class", example = "Private Cars (1301 to 2000 cc)")
    private String vehicleClass;

    /** Fuel type the vehicle uses (e.g. Petrol, Diesel, CNG, LPG). */
    @Schema(description = "Fuel type", example = "Petrol")
    private String fuelType;

    /** Engine displacement in cubic centimetres (optional). */
    @Schema(description = "Engine displacement in CC", example = "1500")
    private Integer engineDisplacement;

    /** Official vehicle registration date. */
    @Schema(description = "Vehicle registration date")
    private LocalDate registrationDate;

    /**
     * BRTA verification status: {@code VERIFIED}, {@code UNVERIFIED}, or {@code DEREGISTERED}.
     */
    @Schema(description = "BRTA verification status", example = "VERIFIED")
    private String status;

    /** Timestamp when the vehicle record was first created. */
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;
}

