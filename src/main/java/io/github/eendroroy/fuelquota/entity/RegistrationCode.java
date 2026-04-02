package io.github.eendroroy.fuelquota.entity;

import io.github.eendroroy.fuelquota.config.DataInitializer;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Lookup table for Bangladesh BRTA vehicle registration code prefixes.
 *
 * <p>Examples: {@code A} → Motorcycles (Up to 100 cc), {@code GA} → Private Cars (1301 to 2000 cc).
 *
 * <p>Seeded by {@link DataInitializer} at startup.
 */
@Entity
@Table(name = "registration_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * BRTA alphanumeric prefix code (e.g. {@code A}, {@code GA}, {@code KHA}).
     * Unique across the system.
     */
    @Column(unique = true, nullable = false, length = 10)
    private String code;

    /**
     * Human-readable description of the vehicle category associated with this code.
     * (e.g. "Motorcycles (Up to 100 cc)", "Private Cars (1301 to 2000 cc)").
     */
    @Column(nullable = false, length = 255)
    private String description;
}

