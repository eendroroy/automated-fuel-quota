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

import java.math.BigDecimal;
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

    /**
     * Bulk-updates quota limit and period for all non-overridden, non-suspended vehicles
     * whose registration code is covered by a quota config set entry.
     * Uses a native JOIN across quota_config_set_codes and vehicles tables.
     *
     * @param now timestamp to set as updated_at
     * @return number of rows updated
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE quotas q
        SET weekly_limit_liters = config_data.limit_litres,
            quota_period = config_data.quota_period,
            remaining_liters = LEAST(q.remaining_liters, config_data.limit_litres),
            updated_at = :now
        FROM (
            SELECT v.id AS vehicle_id, cs.limit_litres, cs.quota_period
            FROM quota_config_set_codes csrc
            JOIN quota_config_sets cs ON cs.id = csrc.config_set_id
            JOIN vehicles v ON v.vehicle_registration_code = csrc.registration_code
            WHERE v.status <> 'DEREGISTERED'
        ) AS config_data
        WHERE q.vehicle_id = config_data.vehicle_id
          AND q.individually_overridden = false
          AND q.status <> 'SUSPENDED'
        """, nativeQuery = true)
    int bulkSyncFromConfigSets(@Param("now") LocalDateTime now);

    /**
     * Bulk-updates quota limit and period for all non-overridden, non-suspended vehicles
     * that are NOT covered by any quota config set (global default fallback).
     *
     * @param defaultLimit  global default fuel limit (litres)
     * @param defaultPeriod global default reset period (e.g. 'WEEKLY')
     * @param now           timestamp to set as updated_at
     * @return number of rows updated
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE quotas q
        SET weekly_limit_liters = :defaultLimit,
            quota_period = :defaultPeriod,
            remaining_liters = LEAST(q.remaining_liters, :defaultLimit),
            updated_at = :now
        FROM vehicles v
        WHERE v.id = q.vehicle_id
          AND q.individually_overridden = false
          AND q.status <> 'SUSPENDED'
          AND v.status <> 'DEREGISTERED'
          AND NOT EXISTS (
              SELECT 1 FROM quota_config_set_codes csrc
              WHERE csrc.registration_code = v.vehicle_registration_code
          )
        """, nativeQuery = true)
    int bulkSyncDefault(@Param("defaultLimit") BigDecimal defaultLimit,
                        @Param("defaultPeriod") String defaultPeriod,
                        @Param("now") LocalDateTime now);
}
