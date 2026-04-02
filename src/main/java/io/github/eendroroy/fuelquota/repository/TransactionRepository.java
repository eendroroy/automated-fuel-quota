package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.Transaction;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    @Query("SELECT t FROM Transaction t WHERE t.vehicle.user.id = :userId ORDER BY t.transactionTimestamp DESC")
    Page<Transaction> findByVehicleUserId(@Param("userId") UUID userId, Pageable pageable);

    boolean existsByVehicleId(UUID vehicleId);

    Page<Transaction> findByVehicleOrderByTransactionTimestampDesc(Vehicle vehicle, Pageable pageable);

    Page<Transaction> findByStationOrderByTransactionTimestampDesc(FuelStation station, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.vehicle.id = :vehicleId " +
           "AND t.transactionTimestamp >= :startOfWeek " +
           "AND t.status = 'COMPLETED' " +
           "ORDER BY t.transactionTimestamp DESC")
    List<Transaction> findVehicleTransactionsThisWeek(@Param("vehicleId") UUID vehicleId,
                                                     @Param("startOfWeek") LocalDateTime startOfWeek);

    @Query("SELECT SUM(t.amountDispensedLiters) FROM Transaction t " +
           "WHERE t.vehicle.id = :vehicleId " +
           "AND t.transactionTimestamp >= :startOfWeek " +
           "AND t.status = 'COMPLETED'")
    BigDecimal getTotalLitersUsedThisWeek(@Param("vehicleId") UUID vehicleId,
                                        @Param("startOfWeek") LocalDateTime startOfWeek);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.transactionTimestamp >= :startOfDay " +
           "AND t.transactionTimestamp < :endOfDay " +
           "AND t.status = 'COMPLETED'")
    long countTransactionsToday(@Param("startOfDay") LocalDateTime startOfDay,
                                @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.transactionTimestamp >= :startOfWeek " +
           "AND t.status = 'COMPLETED'")
    long countTransactionsThisWeek(@Param("startOfWeek") LocalDateTime startOfWeek);

    @Query("SELECT cast(t.transactionTimestamp as LocalDate) as date, COUNT(t) as count " +
           "FROM Transaction t " +
           "WHERE t.transactionTimestamp >= :startDate " +
           "AND t.status = 'COMPLETED' " +
           "GROUP BY cast(t.transactionTimestamp as LocalDate) " +
           "ORDER BY cast(t.transactionTimestamp as LocalDate) DESC")
    List<Object[]> getDailyTransactionCounts(@Param("startDate") LocalDateTime startDate);


    @Query("SELECT t FROM Transaction t WHERE t.qrTokenUsed = :qrToken")
    List<Transaction> findByQrToken(@Param("qrToken") String qrToken);
}
