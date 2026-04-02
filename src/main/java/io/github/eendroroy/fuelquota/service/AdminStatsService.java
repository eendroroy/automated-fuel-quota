package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.response.AdminStatsResponse;
import io.github.eendroroy.fuelquota.dto.response.DashboardStatsResponse;
import io.github.eendroroy.fuelquota.repository.VehicleRepository;
import io.github.eendroroy.fuelquota.repository.TransactionRepository;
import io.github.eendroroy.fuelquota.repository.FuelStationRepository;
import io.github.eendroroy.fuelquota.repository.QuotaRepository;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import io.github.eendroroy.fuelquota.config.AppProperties;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating administrative statistics and dashboard data.
 *
 * <p>Provides cached aggregated data for the admin dashboard including:
 * <ul>
 *   <li>Vehicle registration statistics</li>
 *   <li>Transaction volume and trends</li>
 *   <li>Fuel station status counts</li>
 *   <li>Quota utilization analytics</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminStatsService {

    private final VehicleRepository vehicleRepository;
    private final TransactionRepository transactionRepository;
    private final FuelStationRepository stationRepository;
    private final QuotaRepository quotaRepository;
    private final AppProperties appProperties;

    /**
     * Retrieves comprehensive dashboard statistics with caching.
     *
     * <p>Results are cached for performance as statistical queries can be expensive.
     * Cache is automatically invalidated when underlying data changes significantly.
     *
     * @return {@link AdminStatsResponse} containing all dashboard metrics
     */
    @Cacheable(value = "adminStats", key = "'dashboard'")
    public DashboardStatsResponse getDashboardStats() {
        // Vehicle statistics
        long totalVehicles = vehicleRepository.count();
        long unverifiedVehicles = vehicleRepository.countByStatus(Vehicle.VehicleStatus.UNVERIFIED);
        long verifiedVehicles = vehicleRepository.countByStatus(Vehicle.VehicleStatus.VERIFIED);

        // Transaction statistics
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        long transactionsToday = transactionRepository.countTransactionsToday(startOfToday, startOfTomorrow);

        LocalDateTime startOfWeek = LocalDateTime.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .withHour(0).withMinute(0).withSecond(0).withNano(0);
        long transactionsThisWeek = transactionRepository.countTransactionsThisWeek(startOfWeek);

        // Station statistics
        long activeStations = stationRepository.countByStatus(FuelStation.StationStatus.ACTIVE);

        // Quota statistics
        Double averageQuotaUsed = quotaRepository.getAverageQuotaUsed();
        double weeklyLimit = appProperties.getQuota().getLimitLitres();
        double averageQuotaUsedPercent = averageQuotaUsed != null ?
            (averageQuotaUsed / weeklyLimit) * 100 : 0.0;

        // Daily transaction trends (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> dailyTransactionsRaw = transactionRepository.getDailyTransactionCounts(sevenDaysAgo);
        List<DashboardStatsResponse.DailyTransactionEntry> dailyTransactions = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE");
        for (Object[] row : dailyTransactionsRaw) {
            String dateLabel = row[0] != null ? ((LocalDate) row[0]).format(fmt) : String.valueOf(row[0]);
            dailyTransactions.add(DashboardStatsResponse.DailyTransactionEntry.builder()
                    .date(dateLabel)
                    .count(((Number) row[1]).longValue())
                    .build());
        }

        // Quota usage by vehicle class
        List<Object[]> quotaUsageByClassRaw = quotaRepository.getQuotaUsageByVehicleClass();
        List<DashboardStatsResponse.QuotaUsageByClassEntry> quotaUsageByVehicleClass = new ArrayList<>();
        for (Object[] row : quotaUsageByClassRaw) {
            quotaUsageByVehicleClass.add(DashboardStatsResponse.QuotaUsageByClassEntry.builder()
                    .vehicleClass(String.valueOf(row[0]))
                    .avgUsed(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                    .build());
        }

        return DashboardStatsResponse.builder()
                .totalVehicles(totalVehicles)
                .unverifiedVehicles(unverifiedVehicles)
                .verifiedVehicles(verifiedVehicles)
                .transactionsToday(transactionsToday)
                .totalTransactionsThisWeek(transactionsThisWeek)
                .activeStations(activeStations)
                .averageQuotaUsedPercent(Math.round(averageQuotaUsedPercent * 100.0) / 100.0)
                .dailyTransactions(dailyTransactions)
                .quotaUsageByVehicleClass(quotaUsageByVehicleClass)
                .build();
    }

    /**
     * Returns vehicle counts by status for pie charts and summaries.
     *
     * @return map of status names to vehicle counts
     */
    public Map<String, Long> getVehicleStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (Vehicle.VehicleStatus status : Vehicle.VehicleStatus.values()) {
            counts.put(status.name(), vehicleRepository.countByStatus(status));
        }
        return counts;
    }
}
