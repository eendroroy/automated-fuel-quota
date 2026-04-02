package io.github.eendroroy.fuelquota.entity;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persisted quota configuration managed by admins via the admin panel.
 *
 * <p>The system maintains a single <em>default</em> configuration row identified by
 * the key {@code "DEFAULT"}.  All new quotas are initialised from this configuration.
 * Individual vehicle quotas can still be overridden by admins after creation.
 *
 * <p>Configuration values previously hard-coded in {@code application.yaml} under
 * {@code app.quota.*} are now stored here and can be changed at runtime without
 * restarting the application.
 */
@Entity
@Table(name = "quota_config")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Singleton key used to look up the default configuration. */
    public static final String DEFAULT_KEY = "DEFAULT";

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Logical key for this configuration row (e.g. {@code "DEFAULT"}). */
    @Column(name = "config_key", unique = true, nullable = false, length = 50)
    private String configKey;

    /**
     * Maximum fuel litres allocated per quota period.
     * Applies to all newly created quotas unless overridden per vehicle.
     */
    @Column(name = "limit_litres", nullable = false, precision = 7, scale = 2)
    private BigDecimal limitLitres;

    /** Geofence radius in metres applied when validating pump-station proximity. */
    @Column(name = "geofence_radius_meters", nullable = false)
    private Integer geofenceRadiusMeters;

    /**
     * Quota reset period — one of DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY.
     * Determines how often used-quota counters are zeroed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "quota_period", nullable = false, length = 20)
    private QuotaPeriod quotaPeriod;

    /**
     * Spring cron expression that drives the {@code QuotaService} reset scheduler.
     *
     * <p>Example: {@code "0 0 0 ? * SUN"} resets quotas every Sunday at midnight.
     * The expression must be consistent with {@link #quotaPeriod}.
     */
    @Column(name = "reset_cron_expression", nullable = false, length = 100)
    private String resetCronExpression;

    /** Human-readable description or change notes for admin reference. */
    @Column(name = "description", length = 500)
    private String description;

    /** Automatically populated by Spring Data JPA auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated by Spring Data JPA auditing on every save. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

