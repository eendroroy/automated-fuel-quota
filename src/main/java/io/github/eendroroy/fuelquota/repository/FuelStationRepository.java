package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.FuelStation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuelStationRepository extends JpaRepository<FuelStation, UUID>,
        JpaSpecificationExecutor<FuelStation> {

    Optional<FuelStation> findByStationCode(String stationCode);

    boolean existsByStationCode(String stationCode);

    @Query("SELECT COUNT(fs) FROM FuelStation fs WHERE fs.status = :status")
    long countByStatus(@Param("status") FuelStation.StationStatus status);

    @Query("SELECT fs FROM FuelStation fs WHERE fs.status = :status")
    Page<FuelStation> findByStatus(@Param("status") FuelStation.StationStatus status, Pageable pageable);

    @Query("SELECT fs FROM FuelStation fs WHERE fs.district = :district AND fs.status = 'ACTIVE'")
    List<FuelStation> findActiveStationsByDistrict(@Param("district") String district);

    @Query("SELECT fs FROM FuelStation fs WHERE fs.status = 'ACTIVE' AND " +
           "(:latitude - fs.latitude) * (:latitude - fs.latitude) + " +
           "(:longitude - fs.longitude) * (:longitude - fs.longitude) <= " +
           "POWER(:radiusKm / 111.0, 2)")
    List<FuelStation> findNearbyActiveStations(@Param("latitude") BigDecimal latitude,
                                             @Param("longitude") BigDecimal longitude,
                                             @Param("radiusKm") double radiusKm);
}
