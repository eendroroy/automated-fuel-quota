package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.PumpRepresentative;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link PumpRepresentative} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic, type-safe
 * Specification-based queries (search, status filter, station filter).
 */
@Repository
public interface PumpRepresentativeRepository extends JpaRepository<PumpRepresentative, UUID>,
        JpaSpecificationExecutor<PumpRepresentative> {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByMobileNumber(String mobileNumber);

    Optional<PumpRepresentative> findByUsername(String username);

    Optional<PumpRepresentative> findByEmployeeId(String employeeId);

    Optional<PumpRepresentative> findByMobileNumber(String mobileNumber);

    @Query("SELECT p FROM PumpRepresentative p WHERE p.station.id = :stationId")
    Page<PumpRepresentative> findByStationId(@Param("stationId") UUID stationId, Pageable pageable);
}

