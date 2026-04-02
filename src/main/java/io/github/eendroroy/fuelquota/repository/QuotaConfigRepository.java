package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.QuotaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link QuotaConfig} entities.
 */
@Repository
public interface QuotaConfigRepository extends JpaRepository<QuotaConfig, UUID> {

    /** Returns the configuration row with the given logical key. */
    Optional<QuotaConfig> findByConfigKey(String configKey);
}

