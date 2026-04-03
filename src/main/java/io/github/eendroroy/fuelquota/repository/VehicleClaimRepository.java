package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.VehicleClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link VehicleClaim} entities.
 */
@Repository
public interface VehicleClaimRepository extends JpaRepository<VehicleClaim, UUID>,
        JpaSpecificationExecutor<VehicleClaim> {

    /** Returns all claims submitted by a specific user. */
    List<VehicleClaim> findByClaimantId(UUID claimantId);

    /** Returns paginated claims submitted by a specific user. */
    Page<VehicleClaim> findPageByClaimantId(UUID claimantId, Pageable pageable);

    /** Returns all claims for a specific vehicle. */
    List<VehicleClaim> findByVehicleId(UUID vehicleId);

    /** Checks if the given user already has a PENDING claim for this vehicle. */
    boolean existsByVehicleIdAndClaimantIdAndStatus(
            UUID vehicleId, UUID claimantId, VehicleClaim.ClaimStatus status);
}

