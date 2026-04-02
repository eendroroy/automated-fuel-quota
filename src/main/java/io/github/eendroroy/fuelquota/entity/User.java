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

    /** Unique e-mail address, also used as the Spring Security username. */
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /** BCrypt-hashed password. Excluded from JSON serialisation. */
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    /** Display name of the user. */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Mobile phone number used for contact and future OTP-based authentication.
     *
     * <p><strong>Future scope:</strong> OTP verification will be required to authenticate
     * this mobile number during customer registration to prevent impersonation.
     */
    @Column(name = "mobile_number", length = 20)
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
     * Whether the account is currently enabled.
     * Set to {@code false} when a customer's vehicle is suspended.
     */
    @Column(nullable = false)
    private Boolean enabled = true;

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
     * Convenience constructor used by {@link DataInitializer}
     * and {@link AuthService#registerCustomer}.
     *
     * @param email    unique e-mail address
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
