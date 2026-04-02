package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.PumpRepresentative;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PumpRepresentativeRepository extends JpaRepository<PumpRepresentative, UUID> {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmployeeId(String employeeId);

    Optional<PumpRepresentative> findByUsername(String username);

    @Query("SELECT p FROM PumpRepresentative p WHERE p.station.id = :stationId")
    Page<PumpRepresentative> findByStationId(@Param("stationId") UUID stationId, Pageable pageable);

    @Query("SELECT p FROM PumpRepresentative p LEFT JOIN FETCH p.station")
    Page<PumpRepresentative> findAllWithStation(Pageable pageable);
}

