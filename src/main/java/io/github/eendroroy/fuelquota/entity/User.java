package io.github.eendroroy.fuelquota.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.eendroroy.fuelquota.config.DataInitializer;
import io.github.eendroroy.fuelquota.service.AuthService;
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
 * Represents a system user (customer, admin, or pump representative).
 *
 * <p>Authentication is handled via Spring Security with BCrypt password hashing.
 * The {@code role} field drives route-level authorisation through
 * {@code @PreAuthorize} annotations on controllers.
 *
 * <p>{@code enabled = false} is used to soft-lock accounts when a vehicle is
 * suspended, rather than deleting the user record.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "password")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique e-mail address (optional for customers/drivers, required for admin).
     * Only used for admin login. For customers and drivers, login is via mobileNumber.
     */
    @Column(unique = true, nullable = true, length = 100)
    private String email;

    /** BCrypt-hashed password. Excluded from JSON serialisation. */
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    /** Display name of the user. */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Mobile phone number used for customer/driver login and contact.
     * Unique identifier for CUSTOMER and PUMP_REPRESENTATIVE roles.
     * Required for customers and drivers; optional for admin.
     */
    @Column(name = "mobile_number", unique = true, nullable = true, length = 20)
    private String mobileNumber;

    /**
     * Authorization role that determines which API endpoints the user may access.
     *
     * @see UserRole
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /**
    /**
     * National Identity Document number of the user.
     * Populated from vehicle registration NID at account creation for customers.
     */
    @Column(length = 20)
    private String nid;

    /**
     * Current lifecycle status of this account.
     *
     * <ul>
     *   <li>{@code ACTIVE} – account is fully operational.</li>
     *   <li>{@code SUSPENDED} – account is temporarily locked by an admin.</li>
     *   <li>{@code INACTIVE} – account is dormant but not deleted.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private UserStatus status = UserStatus.ACTIVE;

    /** Whether the account is currently enabled (synced with {@link #status}). */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** Timestamp of the user's most recent successful login. */
    @Column(name = "last_login_timestamp")
    private LocalDateTime lastLoginTimestamp;

    /** Automatically populated by Spring Data JPA auditing. */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated by Spring Data JPA auditing on every save. */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Defines the access roles available in the system.
     *
     * <ul>
     *   <li>{@code CUSTOMER} – registered vehicle owner.</li>
     *   <li>{@code ADMIN} – government administrator with full system access.</li>
     *   <li>{@code PUMP_REPRESENTATIVE} – fuel station employee using the pump app.</li>
     * </ul>
     */
    public enum UserRole {
        CUSTOMER, ADMIN, PUMP_REPRESENTATIVE
    }

    /**
     * Lifecycle status of a user account.
     */
    public enum UserStatus {
        ACTIVE, SUSPENDED, INACTIVE
    }

    /**
     * Convenience constructor used by {@link DataInitializer}
     * and {@link AuthService#registerCustomer}.
     *
     * @param email    optional e-mail address (required for admin, optional for customers)
     * @param password BCrypt-encoded password
     * @param name     display name
     * @param role     authorization role
     */
    public User(String email, String password, String name, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }
}
