package io.github.eendroroy.fuelquota.mapper;

import io.github.eendroroy.fuelquota.dto.response.VehicleResponse;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Vehicle} entities to {@link VehicleResponse} DTOs.
 *
 * <p>Lazy-loaded associations ({@code user}, {@code quota}) are intentionally
 * excluded from the response to avoid N+1 queries and serialisation issues.
 */
@Component
public class VehicleMapper {

    /**
     * Converts a {@link Vehicle} entity to a {@link VehicleResponse}.
     *
     * @param vehicle the source entity (must not be {@code null})
     * @return a populated {@link VehicleResponse}
     */
    public VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId().toString())
                .userId(vehicle.getUser() != null ? vehicle.getUser().getId().toString() : null)
                .registrationNumber(vehicle.getRegistrationNumber())
                .brtaOfficeCode(vehicle.getBrtaOfficeCode())
                .vehicleRegistrationCode(vehicle.getVehicleRegistrationCode())
                .ownerName(vehicle.getOwnerName())
                .ownerNid(vehicle.getOwnerNid())
                .ownerMobile(vehicle.getOwnerMobile())
                .ownerEmail(vehicle.getOwnerEmail())
                .driverId(vehicle.getDriver() != null ? vehicle.getDriver().getId().toString() : null)
                .driverName(vehicle.getDriver() != null ? vehicle.getDriver().getName() : null)
                .driverEmail(vehicle.getDriver() != null ? vehicle.getDriver().getEmail() : null)
                .vehicleMake(vehicle.getVehicleMake())
                .vehicleColor(vehicle.getVehicleColor())
                .vehicleClass(vehicle.getVehicleClass())
                .fuelType(vehicle.getFuelType())
                .engineDisplacement(vehicle.getEngineDisplacement())
                .registrationDate(vehicle.getRegistrationDate())
                .status(vehicle.getStatus().name())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}
