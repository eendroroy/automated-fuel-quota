package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fuel station details returned to admin callers.
 *
 * <p>Returned by:
 * <ul>
 *   <li>{@code GET /api/admin/stations} – paginated list</li>
 *   <li>{@code GET /api/admin/stations/{id}} – single station</li>
 *   <li>{@code POST /api/admin/stations} – newly created station</li>
 *   <li>{@code PUT /api/admin/stations/{id}} – updated station</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Fuel station details")
public class FuelStationResponse {

    /** UUID of the fuel station record. */
    @Schema(description = "Fuel station UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    /** Human-readable station name. */
    @Schema(description = "Station name", example = "ABC Fuel Station Dhanmondi")
    private String stationName;

    /** Short unique identifier code for the station. */
    @Schema(description = "Unique station code", example = "ABC-DH-001")
    private String stationCode;

    /** GPS latitude used for geofence calculations. */
    @Schema(description = "GPS latitude", example = "23.7465")
    private BigDecimal latitude;

    /** GPS longitude used for geofence calculations. */
    @Schema(description = "GPS longitude", example = "90.3700")
    private BigDecimal longitude;

    /** Geofence radius in metres (default 100 m). */
    @Schema(description = "Geofence radius in metres", example = "100")
    private Integer geofenceRadiusMeters;

    /** Station contact telephone number. */
    @Schema(description = "Station contact phone number", example = "01711111111")
    private String phoneNumber;

    /** Full name of the station manager. */
    @Schema(description = "Station manager's name", example = "Rahman Ahmed")
    private String managerName;

    /** E-mail address of the station manager. */
    @Schema(description = "Station manager's email", example = "rahman@abcfuel.com")
    private String managerEmail;

    /** Administrative district. */
    @Schema(description = "District / administrative area", example = "Dhaka")
    private String district;

    /** Operational status: {@code ACTIVE}, {@code INACTIVE}, or {@code SUSPENDED}. */
    @Schema(description = "Station operational status", example = "ACTIVE")
    private String status;

    /** Date/time the station was registered in the system. */
    @Schema(description = "Station registration timestamp")
    private LocalDateTime registrationDate;

    /** Record creation timestamp. */
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;
}

