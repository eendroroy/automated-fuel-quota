package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Admin dashboard statistics snapshot.
 *
 * <p>Returned by {@code GET /api/admin/stats}.
 * Aggregates vehicle counts, transaction totals, quota usage, and
 * trend data for the admin dashboard charts.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Aggregated admin dashboard statistics")
public class DashboardStatsResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ── Vehicle statistics ────────────────────────────────────────────────────

    /** Total number of vehicles registered in the system. */
    @Schema(description = "Total number of vehicles registered", example = "1024")
    private long totalVehicles;

    /** Vehicles with UNVERIFIED status (BRTA verification pending / failed). */
    @Schema(description = "Number of unverified vehicles", example = "5")
    private long unverifiedVehicles;

    /** Vehicles with VERIFIED status that can receive fuel. */
    @Schema(description = "Number of verified vehicles", example = "980")
    private long verifiedVehicles;

    // ── Transaction statistics ────────────────────────────────────────────────

    /** Number of completed transactions recorded today. */
    @Schema(description = "Number of transactions today", example = "47")
    private long transactionsToday;

    /** Total transactions recorded since the start of the current calendar week. */
    @Schema(description = "Total transactions this week", example = "215")
    private long totalTransactionsThisWeek;

    // ── Station statistics ────────────────────────────────────────────────────

    /** Number of fuel stations with ACTIVE status. */
    @Schema(description = "Number of active fuel stations", example = "18")
    private long activeStations;

    // ── Quota statistics ──────────────────────────────────────────────────────

    /**
     * System-wide average percentage of the weekly quota consumed.
     * Calculated as {@code (avg_used / weekly_limit) * 100}.
     */
    @Schema(description = "System-wide average quota usage as a percentage", example = "43.75")
    private double averageQuotaUsedPercent;

    // ── Trend data ────────────────────────────────────────────────────────────

    /** Daily transaction counts for the last 7 days (for bar/line charts). */
    @Schema(description = "Daily transaction trend for the past 7 days")
    private List<DailyTransactionEntry> dailyTransactions;

    /** Average fuel usage broken down by vehicle class (for pie/bar charts). */
    @Schema(description = "Average fuel usage grouped by vehicle class")
    private List<QuotaUsageByClassEntry> quotaUsageByVehicleClass;

    // ── Nested entry types ────────────────────────────────────────────────────

    /**
     * A single data point for the daily transaction trend chart.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Daily transaction count data point")
    public static class DailyTransactionEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** Abbreviated day label (e.g. {@code Mon}, {@code Tue}). */
        @Schema(description = "Day label", example = "Mon")
        private String date;

        /** Number of transactions on that day. */
        @Schema(description = "Transaction count", example = "34")
        private long count;
    }

    /**
     * A single data point for the quota-usage-by-vehicle-class chart.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Average quota usage by vehicle class")
    public static class QuotaUsageByClassEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** Vehicle class name (e.g. {@code Private Car}, {@code Motor Cycle}). */
        @Schema(description = "Vehicle class", example = "Private Car")
        private String vehicleClass;

        /** Average litres consumed per vehicle in this class for the current week. */
        @Schema(description = "Average litres used", example = "18.50")
        private Double avgUsed;
    }
}

