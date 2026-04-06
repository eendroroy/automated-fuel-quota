package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.response.VehicleResponse;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.repository.VehicleRepository;
import io.github.eendroroy.fuelquota.mapper.VehicleMapper;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for vehicle management.
 *
 * <p>Vehicles are automatically {@code VERIFIED} on registration.
 * Admins may trigger a manual BRTA re-verification at any time.
 *
 * <p><strong>Future scope:</strong> The reverify operation will call the BRTA API
 * to confirm ownership. For now it always succeeds and leaves the vehicle as
 * {@code VERIFIED}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class VehicleService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    /**
     * Returns a paginated list of vehicles, optionally filtered by search term, status,
     * BRTA office code, registration code, and registration date range.
     *
     * <p>Default sort is registration date descending.
     * Uses {@link Specification} to build the WHERE clause dynamically.
     */
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getAllVehicles(String search, Vehicle.VehicleStatus status,
                                                String brtaCode, String registrationCode,
                                                LocalDate registrationDateFrom, LocalDate registrationDateTo,
                                                Pageable pageable) {
        Specification<Vehicle> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("registrationNumber")), pattern),
                        cb.like(cb.lower(root.get("ownerName")), pattern)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (brtaCode != null && !brtaCode.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("brtaOfficeCode")),
                        "%" + brtaCode.toLowerCase() + "%"));
            }
            if (registrationCode != null && !registrationCode.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("vehicleRegistrationCode")),
                        "%" + registrationCode.toLowerCase() + "%"));
            }
            if (registrationDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("registrationDate"), registrationDateFrom));
            }
            if (registrationDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("registrationDate"), registrationDateTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return vehicleRepository.findAll(spec, pageable).map(vehicleMapper::toResponse);
    }

    /**
     * Retrieves a single vehicle by its UUID.
     */
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        return vehicleMapper.toResponse(vehicle);
    }

    /**
     * Triggers a BRTA re-verification for the given vehicle.
     *
     * <p>Currently always succeeds and keeps the vehicle as {@code VERIFIED}.
     *
     * <p><strong>Future scope:</strong> Will call the BRTA API to confirm ownership
     * and may set status to {@code UNVERIFIED} if the check fails.
     *
     * @param vehicleId UUID of the vehicle to reverify
     * @return updated {@link VehicleResponse}
     * @throws ResourceNotFoundException if the vehicle is not found
     */
    // ...existing code...
    public VehicleResponse reverifyVehicle(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        // Future scope: call BRTA API here.
        // If BRTA confirms ownership → VERIFIED; if not → UNVERIFIED.
        // For now: always VERIFIED.
        vehicle.setStatus(Vehicle.VehicleStatus.VERIFIED);
        vehicle = vehicleRepository.save(vehicle);

        logger.info("Vehicle reverified (BRTA check passed): {} - {}", vehicle.getRegistrationNumber(), vehicle.getOwnerName());
        return vehicleMapper.toResponse(vehicle);
    }

    /**
     * Returns the count of vehicles in a specific status.
     */
    @Transactional(readOnly = true)
    public long getVehicleCountByStatus(Vehicle.VehicleStatus status) {
        return vehicleRepository.countByStatus(status);
    }

    /**
     * Checks if a registration number is already in use.
     */
    @Transactional(readOnly = true)
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return vehicleRepository.existsByRegistrationNumber(registrationNumber);
    }

    /**
     * Checks if an owner NID is already registered.
     */
    @Transactional(readOnly = true)
    public boolean existsByOwnerNid(String ownerNid) {
        return vehicleRepository.existsByOwnerNid(ownerNid);
    }
}
