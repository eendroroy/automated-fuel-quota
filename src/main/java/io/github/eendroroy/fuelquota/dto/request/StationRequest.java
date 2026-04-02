package io.github.eendroroy.fuelquota.dto.request;

import io.github.eendroroy.fuelquota.entity.FuelStation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating a fuel station.
 *
 * <p>Used by admin endpoints {@code POST /api/admin/stations} and
 * {@code PUT /api/admin/stations/{id}}.  Latitude and longitude are
 * supplied as strings to allow the front-end to pass arbitrary precision;
 * the service layer converts them to {@link java.math.BigDecimal}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for creating or updating a fuel station")
public class StationRequest {

    /** Human-readable name of the fuel station. */
    @NotBlank(message = "Station name is required")
    @Size(max = 100, message = "Station name cannot exceed 100 characters")
    @Schema(description = "Full name of the fuel station", example = "ABC Fuel Station Dhanmondi", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stationName;

    /** Short, unique identifier code for the station (used in transaction records). */
    @NotBlank(message = "Station code is required")
    @Size(max = 20, message = "Station code cannot exceed 20 characters")
    @Schema(description = "Unique station code", example = "ABC-DH-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stationCode;

    /** GPS latitude of the station, used for geofence calculations. */
    @NotBlank(message = "Latitude is required")
    @Pattern(regexp = "^-?\\d+\\.\\d+$", message = "Invalid latitude format")
    @Schema(description = "GPS latitude (decimal degrees)", example = "23.7465", requiredMode = Schema.RequiredMode.REQUIRED)
    private String latitude;

    /** GPS longitude of the station, used for geofence calculations. */
    @NotBlank(message = "Longitude is required")
    @Pattern(regexp = "^-?\\d+\\.\\d+$", message = "Invalid longitude format")
    @Schema(description = "GPS longitude (decimal degrees)", example = "90.3700", requiredMode = Schema.RequiredMode.REQUIRED)
    private String longitude;

    /**
     * Geofence radius in metres. Defaults to 100 m if omitted.
     * Pump-representative GPS must be within this radius for an authorization to succeed.
     */
    @Pattern(regexp = "^\\d+$", message = "Geofence radius must be a number")
    @Schema(description = "Geofence radius in metres (default 100)", example = "150")
    private String geofenceRadiusMeters;

    /** Contact telephone number for the station. */
    @NotBlank(message = "Phone number is required")
    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    @Schema(description = "Station contact phone number", example = "+8801711111111", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    /** Full name of the station manager. */
    @NotBlank(message = "Manager name is required")
    @Size(max = 100, message = "Manager name cannot exceed 100 characters")
    @Schema(description = "Name of the station manager", example = "Rahman Ahmed", requiredMode = Schema.RequiredMode.REQUIRED)
    private String managerName;

    /** E-mail address of the station manager. */
    @NotBlank(message = "Manager email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Manager email cannot exceed 100 characters")
    @Schema(description = "Manager's email address", example = "rahman@abcfuel.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String managerEmail;

    /** Administrative district where the station is located. */
    @NotBlank(message = "District is required")
    @Size(max = 50, message = "District cannot exceed 50 characters")
    @Schema(description = "District / administrative area", example = "Dhaka", requiredMode = Schema.RequiredMode.REQUIRED)
    private String district;

    /**
     * Operational status of the station.
     * Defaults to {@link FuelStation.StationStatus#ACTIVE} if not supplied.
     */
    @Builder.Default
    @Schema(description = "Operational status of the station", example = "ACTIVE")
    private FuelStation.StationStatus status = FuelStation.StationStatus.ACTIVE;
}

