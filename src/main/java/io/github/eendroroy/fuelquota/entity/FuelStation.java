package io.github.eendroroy.fuelquota.entity;

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
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a registered fuel dispensing station.
 *
 * <p>Each station has a geographic location (lat/lon) and a configurable geofence
 * radius. During the authorization flow (BRD FR-10), the pump representative's
 * GPS coordinates are validated against this geofence via the Haversine formula.
 */
@Entity
@Table(name = "fuel_stations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "stationName", "stationCode", "status"})
public class FuelStation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Human-readable station name. */
    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;

    /** Short unique identifier code for the station. */
    @Column(name = "station_code", unique = true, nullable = false, length = 20)
    private String stationCode;

    /** GPS latitude used as the geofence centre point. */
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    /** GPS longitude used as the geofence centre point. */
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    /**
     * Geofence radius in metres. A pump representative must be within this
     * distance of the station centre for the authorization to succeed.
     * Defaults to 100 m (configurable via {@code app.quota.geofence-radius-meters}).
     */
    @Column(name = "geofence_radius_meters", nullable = false)
    private Integer geofenceRadiusMeters = 100;

    /** Contact telephone number for the station. */
    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    /** Full name of the station manager. */
    @Column(name = "manager_name", nullable = false, length = 100)
    private String managerName;

    /** E-mail address of the station manager. */
    @Column(name = "manager_email", nullable = false, length = 100)
    private String managerEmail;

    /** Administrative district where the station is located. */
    @Column(nullable = false, length = 50)
    private String district;

    /** Date/time the station was registered in the system. */
    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    /**
     * Operational status of the station.
     * Only {@link StationStatus#ACTIVE} stations can participate in transactions.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StationStatus status = StationStatus.ACTIVE;

    /** Automatically populated by Spring Data JPA auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated by Spring Data JPA auditing on every save. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Station operational status values.
     */
    public enum StationStatus {
        /** Station is operational and accepting transactions. */
        ACTIVE,
        /** Station is temporarily not in service. */
        INACTIVE,
        /** Station has been suspended by admin action. */
        SUSPENDED
    }

    /**
     * Default constructor required by JPA.
     * Initialises {@link #registrationDate} to the current timestamp.
     */
    public FuelStation() {
        this.registrationDate = LocalDateTime.now();
    }

    /**
     * Primary constructor for creating a new station record.
     *
     * @param stationName  human-readable station name
     * @param stationCode  unique short code
     * @param latitude     GPS latitude of the station
     * @param longitude    GPS longitude of the station
     * @param phoneNumber  contact phone number
     * @param managerName  station manager's name
     * @param managerEmail station manager's e-mail
     * @param district     administrative district
     */
    public FuelStation(String stationName, String stationCode, BigDecimal latitude,
                       BigDecimal longitude, String phoneNumber, String managerName,
                       String managerEmail, String district) {
        this.stationName = stationName;
        this.stationCode = stationCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.managerName = managerName;
        this.managerEmail = managerEmail;
        this.district = district;
        this.registrationDate = LocalDateTime.now();
    }

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Determines whether the given GPS coordinates are within this station's
     * geofence, using the Haversine great-circle distance formula.
     *
     * @param checkLatitude  latitude of the point to test
     * @param checkLongitude longitude of the point to test
     * @return {@code true} if the distance is ≤ {@link #geofenceRadiusMeters}
     */
    public boolean isWithinGeofence(BigDecimal checkLatitude, BigDecimal checkLongitude) {
        double distance = calculateDistance(
                latitude.doubleValue(), longitude.doubleValue(),
                checkLatitude.doubleValue(), checkLongitude.doubleValue()
        );
        return distance <= geofenceRadiusMeters;
    }

    /**
     * Returns {@code true} if the station is currently {@link StationStatus#ACTIVE}.
     *
     * @return {@code true} when the station can accept transactions
     */
    public boolean isActive() {
        return status == StationStatus.ACTIVE;
    }

    /**
     * Calculates the great-circle distance between two GPS coordinates using
     * the Haversine formula.
     *
     * @param lat1 latitude of point 1 (degrees)
     * @param lon1 longitude of point 1 (degrees)
     * @param lat2 latitude of point 2 (degrees)
     * @param lon2 longitude of point 2 (degrees)
     * @return distance in metres
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6_371_000; // Earth's radius in metres
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
