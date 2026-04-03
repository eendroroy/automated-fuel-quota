package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.StationRequest;
import io.github.eendroroy.fuelquota.dto.response.StationResponse;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import io.github.eendroroy.fuelquota.repository.FuelStationRepository;
import io.github.eendroroy.fuelquota.mapper.StationMapper;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;
import io.github.eendroroy.fuelquota.exception.BadRequestException;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for fuel station management.
 *
 * <p>Manages CRUD operations for fuel stations including geofence configuration
 * and location-based search.
 * frequent lookups during fuel authorization.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FuelStationService {

    private static final Logger logger = LoggerFactory.getLogger(FuelStationService.class);

    private final FuelStationRepository stationRepository;
    private final StationMapper stationMapper;

    /**
     * Retrieves a paginated list of fuel stations, optionally filtered by status.
     *
     * <p>Uses {@link Specification} to build the WHERE clause dynamically — avoids
     * the PostgreSQL {@code ? IS NULL OR column = ?} type-inference failure.
     *
     * @param status   optional status filter
     * @param pageable pagination parameters
     * @return paginated {@link StationResponse} results
     */
    @Transactional(readOnly = true)
    public Page<StationResponse> getAllStations(FuelStation.StationStatus status, Pageable pageable) {
        Specification<FuelStation> spec = (root, query, cb) -> {
            if (status != null) {
                return cb.equal(root.get("status"), status);
            }
            return cb.conjunction();
        };
        return stationRepository.findAll(spec, pageable).map(stationMapper::toResponse);
    }

    /**
     * Retrieves a single fuel station by its UUID.
     *
     * @param id UUID of the fuel station
     * @return {@link StationResponse} containing station details
     * @throws ResourceNotFoundException if the station is not found
     */
    // ...existing code...
    @Transactional(readOnly = true)
    public StationResponse getStationById(UUID id) {
        FuelStation station = stationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fuel station not found"));
        return stationMapper.toResponse(station);
    }

    /**
     * Creates a new fuel station.
     *
     * @param request station creation details
     * @return {@link StationResponse} of the created station
     * @throws BadRequestException if the station code already exists
     */
    // ...existing code...
    public StationResponse createStation(StationRequest request) {
        // Validate unique station code
        if (stationRepository.existsByStationCode(request.getStationCode())) {
            throw new BadRequestException("Station code already exists");
        }

        FuelStation station = stationMapper.toEntity(request);
        station = stationRepository.save(station);

        logger.info("Created fuel station: {} - {}", station.getStationCode(), station.getStationName());
        return stationMapper.toResponse(station);
    }

    /**
     * Updates an existing fuel station.
     *
     * @param id      UUID of the station to update
     * @param request updated station details
     * @return {@link StationResponse} of the updated station
     * @throws ResourceNotFoundException if the station is not found
     * @throws BadRequestException       if the station code conflicts with another station
     */
    // ...existing code...
    public StationResponse updateStation(UUID id, StationRequest request) {
        FuelStation station = stationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fuel station not found"));

        // Check for duplicate station code (excluding current station)
        if (!station.getStationCode().equals(request.getStationCode()) &&
            stationRepository.existsByStationCode(request.getStationCode())) {
            throw new BadRequestException("Station code already exists");
        }

        stationMapper.updateEntityFromRequest(request, station);
        station = stationRepository.save(station);

        logger.info("Updated fuel station: {} - {}", station.getStationCode(), station.getStationName());
        return stationMapper.toResponse(station);
    }

    /**
     * Deletes a fuel station.
     *
     * @param id UUID of the station to delete
     * @throws ResourceNotFoundException if the station is not found
     */
    // ...existing code...
    public void deleteStation(UUID id) {
        FuelStation station = stationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fuel station not found"));

        stationRepository.delete(station);
        logger.info("Deleted fuel station: {} - {}", station.getStationCode(), station.getStationName());
    }

    /**
     * Returns the count of active fuel stations.
     *
     * @return number of stations with ACTIVE status
     */
    @Transactional(readOnly = true)
    public long getActiveStationCount() {
        return stationRepository.countByStatus(FuelStation.StationStatus.ACTIVE);
    }

    /**
     * Finds active fuel stations within a specified radius of given coordinates.
     *
     * <p>Used for location-based station discovery in mobile apps.
     *
     * @param latitude  latitude coordinate
     * @param longitude longitude coordinate
     * @param radiusKm  search radius in kilometers
     * @return list of nearby active fuel stations
     */
    @Transactional(readOnly = true)
    public List<StationResponse> findNearbyStations(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        List<FuelStation> stations = stationRepository.findNearbyActiveStations(latitude, longitude, radiusKm);
        return stations.stream()
            .map(stationMapper::toResponse)
            .toList();
    }

    /**
     * Checks if a station code is already in use.
     *
     * @param stationCode the station code to check
     * @return {@code true} if the station code exists
     */
    @Transactional(readOnly = true)
    public boolean existsByStationCode(String stationCode) {
        return stationRepository.existsByStationCode(stationCode);
    }
}
