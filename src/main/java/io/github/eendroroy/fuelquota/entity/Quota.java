package io.github.eendroroy.fuelquota.entity;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

/**
 * Tracks the periodic fuel quota allocation for a single {@link Vehicle}.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Default limit is 24 litres per period (configurable via {@code app.quota.limit-litres}).</li>
 *   <li>The reset period is configurable via {@code app.quota.period} (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY).</li>
 *   <li>Reset is triggered by a scheduled job matching {@code app.quota.reset-cron-expression}.</li>
 *   <li>Partial dispense: when requested > remaining, only available amount is authorized (FR-12).</li>
 *   <li>Optimistic locking via {@code @Version} prevents concurrent over-dispense.</li>
 * </ul>
 */
@Entity
@Table(name = "quotas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(exclude = "vehicle")
public class Quota implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The vehicle this quota belongs to.
     * 1-to-1 relationship enforced by a unique constraint on {@code vehicle_id}.
     */
    @OneToOne
    @JoinColumn(name = "vehicle_id", nullable = false, unique = true)
    private Vehicle vehicle;

    /** Maximum fuel the vehicle may receive per quota period (litres). */
    @Column(name = "weekly_limit_liters", nullable = false, precision = 7, scale = 2)
    private BigDecimal limitLiters = BigDecimal.valueOf(24.00);

    /** Cumulative litres consumed since the last period reset. */
    @Column(name = "used_liters", nullable = false, precision = 7, scale = 2)
    private BigDecimal usedLiters = BigDecimal.ZERO;

    /**
     * Litres remaining in the current period. Recomputed on every {@link #consumeQuota} call
     * and whenever the limit is changed via {@link #setLimitLiters}.
     */
    @Column(name = "remaining_liters", nullable = false, precision = 7, scale = 2)
    private BigDecimal remainingLiters = BigDecimal.valueOf(24.00);

    /** The configured reset period for this quota. */
    @Enumerated(EnumType.STRING)
    @Column(name = "quota_period", nullable = false, length = 20)
    private QuotaPeriod period = QuotaPeriod.WEEKLY;

    /** Scheduled date/time of the next quota reset. */
    @Column(name = "reset_timestamp", nullable = false)
    private LocalDateTime resetTimestamp;

    /** Timestamp of the most recent fuel transaction against this quota. */
    @Column(name = "last_transaction_timestamp")
    private LocalDateTime lastTransactionTimestamp;

    /**
     * Operational status of the quota.
     * Only {@link QuotaStatus#ACTIVE} quotas can authorize fuel dispensing.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotaStatus status = QuotaStatus.ACTIVE;

    /**
     * JPA optimistic-locking version counter.
     * Prevents concurrent transactions from double-decrementing the quota.
     */
    @Version
    @Column(name = "version")
    private Long version = 0L;

    /** Automatically populated by Spring Data JPA auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated by Spring Data JPA auditing on every save. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Defines the lifecycle states of a quota.
     *
     * <ul>
     *   <li>{@code ACTIVE} – quota is operational; fuel dispensing is allowed.</li>
     *   <li>{@code SUSPENDED} – quota is frozen (e.g. vehicle suspended by admin).</li>
     *   <li>{@code EXPIRED} – quota period has elapsed without being reset.</li>
     * </ul>
     */
    public enum QuotaStatus {
        ACTIVE, SUSPENDED, EXPIRED
    }

    /** Default constructor — defaults to WEEKLY period. */
    public Quota() {
        this.resetTimestamp = calculateNextResetTime(QuotaPeriod.WEEKLY);
    }

    /** Constructor for quota creation when a vehicle is approved (WEEKLY default). */
    public Quota(Vehicle vehicle, BigDecimal limitLiters) {
        this(vehicle, limitLiters, QuotaPeriod.WEEKLY);
    }

    /** Full constructor with explicit period. */
    public Quota(Vehicle vehicle, BigDecimal limitLiters, QuotaPeriod period) {
        this.vehicle = vehicle;
        this.limitLiters = limitLiters;
        this.remainingLiters = limitLiters;
        this.period = period;
        this.resetTimestamp = calculateNextResetTime(period);
    }

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Records consumption of a given fuel amount.
     * Updates {@link #usedLiters}, {@link #remainingLiters}, and
     * {@link #lastTransactionTimestamp}.
     * Clamps {@link #remainingLiters} to zero if it would go negative.
     *
     * @param litersDispensed the amount dispensed (must be positive)
     */
    public void consumeQuota(BigDecimal litersDispensed) {
        this.usedLiters = this.usedLiters.add(litersDispensed);
        this.remainingLiters = this.limitLiters.subtract(this.usedLiters);
        this.lastTransactionTimestamp = LocalDateTime.now();
        if (this.remainingLiters.compareTo(BigDecimal.ZERO) < 0) {
            this.remainingLiters = BigDecimal.ZERO;
        }
    }

    /** Resets the quota for the next period and advances the resetTimestamp. */
    public void resetQuota() {
        this.usedLiters = BigDecimal.ZERO;
        this.remainingLiters = this.limitLiters;
        this.resetTimestamp = calculateNextResetTime(this.period);
    }

    /** @deprecated Use {@link #resetQuota()} */
    @Deprecated
    public void resetWeeklyQuota() {
        resetQuota();
    }

    /**
     * Returns {@code true} if the quota has available litres and is currently active.
     *
     * @return {@code true} when dispensing may proceed
     */
    public boolean hasAvailableQuota() {
        return this.remainingLiters.compareTo(BigDecimal.ZERO) > 0
                && this.status == QuotaStatus.ACTIVE;
    }

    /**
     * Returns {@code true} if the full requested amount can be dispensed.
     *
     * @param requestedLiters the requested fuel quantity
     * @return {@code true} when sufficient quota is available
     */
    public boolean canDispense(BigDecimal requestedLiters) {
        return hasAvailableQuota() && this.remainingLiters.compareTo(requestedLiters) >= 0;
    }

    /**
     * Calculates the maximum dispense amount, implementing the partial-dispense
     * BRD requirement (FR-12).
     *
     * @param requestedLiters the requested fuel quantity
     * @return the authorized amount — either {@code requestedLiters} if sufficient
     *         quota is available, or the remaining quota if it is less; returns
     *         {@link BigDecimal#ZERO} if no quota is available
     */
    public BigDecimal getMaxDispensableAmount(BigDecimal requestedLiters) {
        if (!hasAvailableQuota()) {
            return BigDecimal.ZERO;
        }
        return requestedLiters.compareTo(remainingLiters) <= 0 ? requestedLiters : remainingLiters;
    }

    /** Updates the limit and recomputes remaining when changed by admin. */
    public void setLimitLiters(BigDecimal limitLiters) {
        this.limitLiters = limitLiters;
        this.remainingLiters = limitLiters.subtract(this.usedLiters);
    }

    /**
     * @deprecated Use {@link #setLimitLiters(BigDecimal)}
     */
    @Deprecated
    public void setWeeklyLimitLiters(BigDecimal limitLiters) {
        setLimitLiters(limitLiters);
    }

    /**
     * @deprecated Use {@link #getLimitLiters()}
     */
    @Deprecated
    public BigDecimal getWeeklyLimitLiters() {
        return limitLiters;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public static LocalDateTime calculateNextResetTime(QuotaPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case DAILY -> now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case WEEKLY -> now.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            case MONTHLY -> now.plusMonths(1).withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            case QUARTERLY -> now.plusMonths(3).withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            case YEARLY -> now.plusYears(1).withDayOfYear(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
        };
    }
}
