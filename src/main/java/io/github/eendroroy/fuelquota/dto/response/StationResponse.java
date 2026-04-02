package io.github.eendroroy.fuelquota.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Response DTO for fuel station information.
 * Used in admin panel for station management operations.
 *
 * @author eendroroy
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
public class StationResponse {

    private UUID id;
    private String stationName;
    private String stationCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer geofenceRadiusMeters;
    private String phoneNumber;
    private String managerName;
    private String managerEmail;
    private String district;
    private String registrationDate;
    private String status;
    private String createdAt;
    private String updatedAt;
}
