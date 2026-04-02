package io.github.eendroroy.fuelquota.enums;

/**
 * Defines the configurable period for fuel quota allocation and reset.
 *
 * <p>The period determines how often the quota is reset and how far into the
 * future the {@code resetTimestamp} is projected.
 *
 * <p>The scheduled cron expression ({@code app.quota.reset-cron-expression}) should
 * match the chosen period. For example:
 * <ul>
 *   <li>{@code DAILY}    → {@code "0 0 0 * * *"} (every day at midnight)</li>
 *   <li>{@code WEEKLY}   → {@code "0 0 0 ? * SUN"} (every Sunday at midnight)</li>
 *   <li>{@code MONTHLY}  → {@code "0 0 0 1 * *"} (1st of every month)</li>
 *   <li>{@code QUARTERLY}→ {@code "0 0 0 1 1,4,7,10 *"} (1st of Jan, Apr, Jul, Oct)</li>
 *   <li>{@code YEARLY}   → {@code "0 0 0 1 1 *"} (1st January each year)</li>
 * </ul>
 */
public enum QuotaPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}

