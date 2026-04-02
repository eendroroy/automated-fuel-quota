package io.github.eendroroy.fuelquota.entity;

import io.github.eendroroy.fuelquota.config.DataInitializer;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Lookup table for BRTA (Bangladesh Road Transport Authority) regional offices.
 *
 * <p>Examples: {@code DHAKA METRO} → Dhaka Metropolitan Area,
 * {@code CHATTOGRAM METRO} → Chattogram Metropolitan Area.
 *
 * <p>Seeded by {@link DataInitializer} at startup.
 */
@Entity
@Table(name = "brta_offices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrtaOffice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Auto-generated UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * BRTA office / region code (e.g. {@code DHAKA METRO}, {@code SYLHET}).
     * Unique across the system.
     */
    @Column(name = "brta_code", unique = true, nullable = false, length = 50)
    private String brtaCode;

    /**
     * Human-readable description of the BRTA region
     * (e.g. "Dhaka Metropolitan Area", "Sylhet District").
     */
    @Column(nullable = false, length = 255)
    private String description;
}

