package io.github.eendroroy.fuelquota.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a pump representative — a fuel station employee who operates the
 * pump representative mobile app to authorize and confirm fuel dispensing.
 *
 * <p>Each representative is assigned to a single {@link FuelStation} and has
 * a dedicated login (username / password) for the pump app. This is separate
 * from the main {@link User} entity used for customer/admin logins.
 */
@Entity
@Table(name = "pump_representatives")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name", "username", "status"})
public class PumpRepresentative implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The fuel station this representative is assigned to.
     * LAZY-loaded; always initialized within a transaction context before use.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private FuelStation station;

    /** Full name of the representative. */
    @Column(nullable = false, length = 100)
    private String name;

    /** Contact mobile number. */
    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    /** Work e-mail address. Unique across all representatives. */
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /** Internal employee identifier assigned by the station management. Unique. */
    @Column(name = "employee_id", unique = true, nullable = false, length = 50)
    private String employeeId;

    /** Login username for the pump representative mobile app. Unique. */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /** BCrypt-hashed password for pump app login. Never exposed in responses. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Current account status.
     * Only {@link RepStatus#ACTIVE} representatives may use the pump app.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RepStatus status = RepStatus.ACTIVE;

    /** Timestamp of the most recent successful login by this representative. */
    @Column(name = "last_login_timestamp")
    private LocalDateTime lastLoginTimestamp;

    /** Automatically populated by Spring Data JPA auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated by Spring Data JPA auditing on every save. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Account status values for pump representatives.
     */
    public enum RepStatus {
        /** Representative is active and may use the pump app. */
        ACTIVE,
        /** Representative account has been deactivated. */
        INACTIVE,
        /** Representative account has been suspended by admin. */
        SUSPENDED
    }
}
