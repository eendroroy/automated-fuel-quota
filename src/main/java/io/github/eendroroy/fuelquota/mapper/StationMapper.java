package io.github.eendroroy.fuelquota.mapper;

import io.github.eendroroy.fuelquota.dto.request.StationRequest;
import io.github.eendroroy.fuelquota.dto.response.StationResponse;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MapStruct mapper for FuelStation entity to StationResponse DTO conversion.
 * Handles the transformation between database entities and API response objects.
 *
 * @author eendroroy
 * @version 1.0
 * @since 1.0
 */
@Mapper(componentModel = "spring")
public interface StationMapper {

    StationMapper INSTANCE = Mappers.getMapper(StationMapper.class);

    /**
     * Maps FuelStation entity to StationResponse DTO.
     *
     * @param fuelStation the fuel station entity
     * @return the station response DTO
     */
    @Mapping(target = "registrationDate", source = "registrationDate", qualifiedByName = "formatDateTime")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    StationResponse toResponse(FuelStation fuelStation);

    /**
     * Maps a list of FuelStation entities to StationResponse DTOs.
     *
     * @param fuelStations list of fuel station entities
     * @return list of station response DTOs
     */
    List<StationResponse> toResponseList(List<FuelStation> fuelStations);

    /**
     * Maps a StationRequest to a new FuelStation entity.
     *
     * @param request the station creation/update request
     * @return a new FuelStation entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "latitude", qualifiedByName = "stringToBigDecimal")
    @Mapping(target = "longitude", qualifiedByName = "stringToBigDecimal")
    @Mapping(target = "geofenceRadiusMeters", qualifiedByName = "stringToIntegerOrDefault")
    FuelStation toEntity(StationRequest request);

    /**
     * Updates an existing FuelStation entity from a StationRequest.
     *
     * @param request the update request
     * @param station the entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "latitude", qualifiedByName = "stringToBigDecimal")
    @Mapping(target = "longitude", qualifiedByName = "stringToBigDecimal")
    @Mapping(target = "geofenceRadiusMeters", qualifiedByName = "stringToIntegerOrDefault")
    void updateEntityFromRequest(StationRequest request, @MappingTarget FuelStation station);

    /**
     * Formats LocalDateTime to String for API response.
     *
     * @param dateTime the datetime to format
     * @return formatted datetime string
     */
    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Converts FuelStation.StationStatus enum to String.
     *
     * @param status the station status enum
     * @return status as string
     */
    @Named("statusToString")
    default String statusToString(FuelStation.StationStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Named("stringToBigDecimal")
    default BigDecimal stringToBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        return new BigDecimal(value);
    }

    @Named("stringToIntegerOrDefault")
    default Integer stringToIntegerOrDefault(String value) {
        if (value == null || value.isBlank()) return 100;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 100;
        }
    }
}
