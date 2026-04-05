package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.QuotaConfigSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link QuotaConfigSet} entities.
 */
@Repository
public interface QuotaConfigSetRepository extends JpaRepository<QuotaConfigSet, UUID> {

    /**
     * Finds a config set that contains the given registration code.
     *
     * @param code vehicle registration code (e.g. GA, LA, KHA)
     * @return Optional containing the matching config set if found
     */
    @Query("SELECT cs FROM QuotaConfigSet cs JOIN cs.registrationCodes rc WHERE rc = :code")
    Optional<QuotaConfigSet> findByRegistrationCode(@Param("code") String code);

    /**
     * Checks if a registration code already belongs to any config set.
     *
     * @param code vehicle registration code
     * @return true if the code is already assigned
     */
    @Query("SELECT COUNT(cs) > 0 FROM QuotaConfigSet cs JOIN cs.registrationCodes rc WHERE rc = :code")
    boolean existsByRegistrationCode(@Param("code") String code);

    /**
     * Checks if a registration code belongs to another config set (when updating a set).
     *
     * @param code      vehicle registration code
     * @param excludeId the ID of the config set to exclude from the check
     * @return true if the code is used in another set
     */
    @Query("SELECT COUNT(cs) > 0 FROM QuotaConfigSet cs JOIN cs.registrationCodes rc WHERE rc = :code AND cs.id <> :excludeId")
    boolean existsByRegistrationCodeAndIdNot(@Param("code") String code, @Param("excludeId") UUID excludeId);
}

