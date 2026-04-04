package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.QuotaConfigByRegistrationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link QuotaConfigByRegistrationCode} entities.
 *
 * <p>Provides methods to lookup quota configuration by vehicle registration code.
 */
@Repository
public interface QuotaConfigByRegistrationCodeRepository extends JpaRepository<QuotaConfigByRegistrationCode, UUID> {

    /**
     * Finds quota configuration by registration code.
     *
     * @param registrationCode vehicle registration code (e.g. GA, LA, KHA)
     * @return Optional containing the configuration if found
     */
    Optional<QuotaConfigByRegistrationCode> findByRegistrationCode(String registrationCode);

    /**
     * Checks if a quota configuration exists for the given registration code.
     *
     * @param registrationCode vehicle registration code
     * @return true if configuration exists
     */
    boolean existsByRegistrationCode(String registrationCode);
}

