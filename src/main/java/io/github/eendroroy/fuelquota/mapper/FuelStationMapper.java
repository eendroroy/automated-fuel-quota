package io.github.eendroroy.fuelquota.mapper;

import io.github.eendroroy.fuelquota.dto.response.FuelStationResponse;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import org.springframework.stereotype.Component;

/**
 * Maps {@link FuelStation} entities to {@link FuelStationResponse} DTOs.
 *
 * <p>The {@code transactions} collection (if any) is intentionally excluded from
 * the response to prevent accidental N+1 queries.
 */
@Component
public class FuelStationMapper {

    /**
     * Converts a {@link FuelStation} entity to a {@link FuelStationResponse}.
     *
     * @param station the source entity (must not be {@code null})
     * @return a populated {@link FuelStationResponse}
     */
    public FuelStationResponse toResponse(FuelStation station) {
        return FuelStationResponse.builder()
                .id(station.getId())
                .stationName(station.getStationName())
                .stationCode(station.getStationCode())
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .geofenceRadiusMeters(station.getGeofenceRadiusMeters())
                .phoneNumber(station.getPhoneNumber())
                .managerName(station.getManagerName())
                .managerEmail(station.getManagerEmail())
                .district(station.getDistrict())
                .status(station.getStatus().name())
                .registrationDate(station.getRegistrationDate())
                .createdAt(station.getCreatedAt())
                .build();
    }
}

