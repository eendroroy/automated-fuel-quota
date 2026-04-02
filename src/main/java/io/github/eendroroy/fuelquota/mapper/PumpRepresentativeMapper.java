package io.github.eendroroy.fuelquota.mapper;

import io.github.eendroroy.fuelquota.dto.response.PumpRepresentativeResponse;
import io.github.eendroroy.fuelquota.entity.PumpRepresentative;
import org.springframework.stereotype.Component;

/**
 * Maps {@link PumpRepresentative} entities to {@link PumpRepresentativeResponse} DTOs.
 *
 * <p>The {@code passwordHash} field is intentionally excluded from the response.
 * Navigates the LAZY-loaded {@code station} association; ensure it is initialised
 * within a transaction context before calling this mapper.
 */
@Component
public class PumpRepresentativeMapper {

    /**
     * Converts a {@link PumpRepresentative} entity to a {@link PumpRepresentativeResponse}.
     *
     * @param rep the source entity (must not be {@code null})
     * @return a populated {@link PumpRepresentativeResponse}
     */
    public PumpRepresentativeResponse toResponse(PumpRepresentative rep) {
        return PumpRepresentativeResponse.builder()
                .id(rep.getId())
                .stationId(rep.getStation().getId())
                .stationName(rep.getStation().getStationName())
                .name(rep.getName())
                .mobileNumber(rep.getMobileNumber())
                .email(rep.getEmail())
                .employeeId(rep.getEmployeeId())
                .username(rep.getUsername())
                .status(rep.getStatus())
                .lastLoginTimestamp(rep.getLastLoginTimestamp())
                .createdAt(rep.getCreatedAt())
                .build();
    }
}

