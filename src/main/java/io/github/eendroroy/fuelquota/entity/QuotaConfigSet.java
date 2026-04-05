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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A named quota configuration set that can cover multiple vehicle registration codes.
 *
 * <p>Replaces the old single-code {@code QuotaConfigByRegistrationCode} model.
 * Multiple registration codes (e.g. GA, KHA, BHA) can be grouped into one set
 * with a shared limit and period.
 *
 * <p>When creating a quota for a new vehicle, the system checks for a matching set
 * by registration code; if none is found it falls back to the global {@link QuotaConfig}.
 */
@Entity
@Table(
    name = "quota_config_sets",
    indexes = @Index(name = "idx_quota_config_sets_name", columnList = "name")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaConfigSet implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Human-readable name for this config set (e.g. "Private Cars", "Motorcycles"). */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Maximum fuel litres allocated per quota period for vehicles in this set. */
    @Column(name = "limit_litres", nullable = false, precision = 7, scale = 2)
    private BigDecimal limitLitres;

    /**
     * Quota reset period for this set — DAILY, WEEKLY, MONTHLY, QUARTERLY, or YEARLY.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "quota_period", nullable = false, length = 20)
    private QuotaPeriod quotaPeriod;

    /** Human-readable description or notes for admin reference. */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Vehicle registration codes covered by this config set (e.g. GA, KHA, BHA).
     * A code must appear in at most one config set.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "quota_config_set_codes",
        joinColumns = @JoinColumn(name = "config_set_id"),
        indexes = @Index(name = "idx_quota_config_set_codes_code", columnList = "registration_code")
    )
    @Column(name = "registration_code", nullable = false, length = 10)
    @Builder.Default
    private List<String> registrationCodes = new ArrayList<>();

    /** Automatically populated by Spring Data JPA auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated by Spring Data JPA auditing on every save. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

