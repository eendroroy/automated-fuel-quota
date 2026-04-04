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
 * Quota configuration specific to a vehicle registration code.
 *
 * <p>Allows admins to set different quota limits and periods based on vehicle
 * registration codes (e.g. LA = 20L DAILY, GA = 30L WEEKLY).
 *
 * <p>When creating a quota for a new vehicle, the system first checks if a
 * configuration exists for that vehicle's registration code. If found, it uses
 * those settings; otherwise, it falls back to the default quota configuration.
 */
@Entity
@Table(
    name = "quota_config_by_registration_code",
    uniqueConstraints = @UniqueConstraint(columnNames = "registration_code")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaConfigByRegistrationCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Vehicle registration code (e.g. GA, LA, KHA).
     * Must match an existing code in {@link RegistrationCode}.
     * Unique across this table.
     */
    @Column(name = "registration_code", unique = true, nullable = false, length = 10)
    private String registrationCode;

    /** Maximum fuel litres allocated per quota period for this vehicle category. */
    @Column(name = "limit_litres", nullable = false, precision = 7, scale = 2)
    private BigDecimal limitLitres;

    /**
     * Quota reset period — one of DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY.
     * Determines how often used-quota counters are zeroed for vehicles in this category.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "quota_period", nullable = false, length = 20)
    private QuotaPeriod quotaPeriod;

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

