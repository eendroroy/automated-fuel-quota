package io.github.eendroroy.fuelquota.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a registered vehicle in the fuel quota management system.
 *
 * <p>Vehicles are automatically {@code VERIFIED} upon registration.
 * An admin may trigger a BRTA re-verification at any time (currently always
 * succeeds; actual BRTA API integration is future scope).
 *
 * <p>A vehicle has a 1-to-1 relationship with a {@link User} (the owner) and
 * a 1-to-1 relationship with a {@link Quota} (weekly fuel allocation).
 */
@Entity
@Table(name = "vehicles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "quota"})
public class Vehicle implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Official registration plate number assembled as
     * {@code {BRTA_OFFICE_CODE} {VEHICLE_REG_CODE} {SERIAL_PART1}-{SERIAL_PART2}}
     * (e.g. {@code DHAKA METRO GA 11-1234}). Unique across the system.
     */
    @Column(name = "registration_number", unique = true, nullable = false, length = 50)
    private String registrationNumber;

    /**
     * BRTA office / region code used in the registration number
     * (e.g. {@code DHAKA METRO}). Stored for structured lookup.
     */
    @Column(name = "brta_office_code", nullable = false, length = 50)
    private String brtaOfficeCode;

    /**
     * Vehicle category registration code (e.g. {@code GA}, {@code KHA}).
     * Stored for structured lookup.
     */
    @Column(name = "vehicle_registration_code", nullable = false, length = 10)
    private String vehicleRegistrationCode;

    /** Full legal name of the vehicle owner. */
    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    /** National Identity Document number of the owner (from vehicle registration document). */
    @Column(name = "owner_nid", nullable = false, length = 20)
    private String ownerNid;

    /** Owner's contact mobile number. */
    @Column(name = "owner_mobile", nullable = false, length = 15)
    private String ownerMobile;

    /** Owner's e-mail address used for account lookup. */
    @Column(name = "owner_email", nullable = false, length = 100)
    private String ownerEmail;

    /** Vehicle manufacturer / brand (e.g. Toyota, Honda). */
    @Column(name = "vehicle_make", nullable = false, length = 50)
    private String vehicleMake;

    /** Body colour of the vehicle. */
    @Column(name = "vehicle_color", nullable = false, length = 30)
    private String vehicleColor;

    /**
     * Regulatory vehicle class derived from the {@link RegistrationCode} description
     * (e.g. "Private Cars (1301 to 2000 cc)").
     */
    @Column(name = "vehicle_class", nullable = false, length = 100)
    private String vehicleClass;

    /** Primary fuel type (e.g. Petrol, Diesel, CNG, LPG). */
    @Column(name = "fuel_type", nullable = false, length = 30)
    private String fuelType;

    /** Engine displacement in cubic centimetres (CC). Optional. */
    @Column(name = "engine_displacement")
    private Integer engineDisplacement;

    /** Date the vehicle was first registered with the transport authority. */
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    /**
     * BRTA verification status of the vehicle.
     * Defaults to {@link VehicleStatus#VERIFIED} on registration.
     *
     * <p><strong>Future scope:</strong> Actual BRTA API verification will transition
     * vehicles to {@code UNVERIFIED} when ownership cannot be confirmed.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status = VehicleStatus.VERIFIED;

    /**
     * The system {@link User} account that owns this vehicle.
     * Multiple vehicles can belong to the same user.
     * Excluded from JSON to prevent circular serialisation.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The fuel {@link Quota} allocated to this vehicle.
     * Excluded from JSON to prevent circular serialisation.
     */
    @JsonIgnore
    @OneToOne(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Quota quota;

    /** Automatically populated by Spring Data JPA auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated by Spring Data JPA auditing on every save. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Defines the BRTA verification lifecycle states.
     *
     * <ul>
     *   <li>{@code VERIFIED} – BRTA ownership confirmed; eligible to receive fuel.</li>
     *   <li>{@code UNVERIFIED} – BRTA ownership could not be confirmed (future scope).</li>
     *   <li>{@code DEREGISTERED} – vehicle soft-deleted; transaction history preserved.</li>
     * </ul>
     */
    public enum VehicleStatus {
        VERIFIED, UNVERIFIED, DEREGISTERED
    }

    /**
     * Full-argument constructor used during customer registration.
     *
     * @param registrationNumber    assembled registration plate (e.g. {@code DHAKA METRO GA 11-1234})
     * @param brtaOfficeCode        BRTA office code component (e.g. {@code DHAKA METRO})
     * @param vehicleRegistrationCode vehicle category code component (e.g. {@code GA})
     * @param ownerName             legal name of the owner
     * @param ownerNid              NID of the owner
     * @param ownerMobile           mobile number
     * @param ownerEmail            e-mail address
     * @param vehicleMake           manufacturer/brand
     * @param vehicleColor          body colour
     * @param vehicleClass          regulatory class description
     * @param fuelType              primary fuel type
     * @param registrationDate      official registration date
     */
    public Vehicle(String registrationNumber, String brtaOfficeCode, String vehicleRegistrationCode,
                   String ownerName, String ownerNid,
                   String ownerMobile, String ownerEmail, String vehicleMake,
                   String vehicleColor, String vehicleClass, String fuelType,
                   LocalDate registrationDate) {
        this.registrationNumber = registrationNumber;
        this.brtaOfficeCode = brtaOfficeCode;
        this.vehicleRegistrationCode = vehicleRegistrationCode;
        this.ownerName = ownerName;
        this.ownerNid = ownerNid;
        this.ownerMobile = ownerMobile;
        this.ownerEmail = ownerEmail;
        this.vehicleMake = vehicleMake;
        this.vehicleColor = vehicleColor;
        this.vehicleClass = vehicleClass;
        this.fuelType = fuelType;
        this.registrationDate = registrationDate;
    }
}
