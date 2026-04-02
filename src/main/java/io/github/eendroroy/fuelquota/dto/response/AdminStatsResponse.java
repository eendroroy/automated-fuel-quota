package io.github.eendroroy.fuelquota.dto.response;

import lombok.Data;
import lombok.Builder;

/**
 * Response DTO for admin dashboard statistics.
 * Contains aggregated metrics for the fuel quota system dashboard.
 *
 * @author eendroroy
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
public class AdminStatsResponse {

    private Long totalVehicles;
    private Long activeVehicles;
    private Long pendingVehicles;
    private Long suspendedVehicles;

    private Long totalQuotas;
    private Double totalQuotaAllocated;
    private Double totalQuotaUsed;
    private Double totalQuotaRemaining;

    private Long totalTransactions;
    private Double totalFuelDispensed;

    private Long totalStations;
    private Long totalCustomers;

    private Double averageQuotaUtilization;
}
