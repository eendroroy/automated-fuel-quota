package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.Quota;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotaRepository extends JpaRepository<Quota, UUID>,
        JpaSpecificationExecutor<Quota> {

    Optional<Quota> findByVehicle(Vehicle vehicle);

    Optional<Quota> findByVehicleId(UUID vehicleId);

    Optional<Quota> findByVehicleRegistrationNumber(String registrationNumber);

    @Query("SELECT q FROM Quota q WHERE q.status = :status")
    List<Quota> findByStatus(@Param("status") Quota.QuotaStatus status);

    @Query("SELECT q FROM Quota q WHERE q.resetTimestamp <= :currentTime")
    List<Quota> findQuotasToReset(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT AVG(q.usedLiters) FROM Quota q WHERE q.status = 'ACTIVE'")
    Double getAverageQuotaUsed();

    @Query("SELECT q FROM Quota q JOIN q.vehicle v WHERE " +
           "LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.ownerName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Quota> findQuotasWithSearch(@Param("search") String search, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Quota q SET q.usedLiters = 0, " +
           "q.remainingLiters = q.limitLiters, " +
           "q.resetTimestamp = :nextResetTime, " +
           "q.updatedAt = :now " +
           "WHERE q.resetTimestamp <= :currentTime")
    int resetExpiredQuotas(@Param("currentTime") LocalDateTime currentTime,
                          @Param("nextResetTime") LocalDateTime nextResetTime,
                          @Param("now") LocalDateTime now);

    @Query("SELECT v.vehicleClass, AVG(q.usedLiters) FROM Quota q " +
           "JOIN q.vehicle v WHERE q.status = 'ACTIVE' " +
           "GROUP BY v.vehicleClass")
    List<Object[]> getQuotaUsageByVehicleClass();
}
