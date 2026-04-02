package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.ClaimVehicleRequest;
import io.github.eendroroy.fuelquota.dto.response.VehicleClaimResponse;
import io.github.eendroroy.fuelquota.entity.User;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.entity.VehicleClaim;
import io.github.eendroroy.fuelquota.exception.BadRequestException;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;
import io.github.eendroroy.fuelquota.repository.UserRepository;
import io.github.eendroroy.fuelquota.repository.VehicleClaimRepository;
import io.github.eendroroy.fuelquota.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service managing vehicle ownership claims.
 *
 * <p>Allows a customer to claim an already-registered vehicle (e.g. purchased
 * second-hand).  An admin reviews each claim and can approve or reject it.
 *
 * <p><strong>Future scope:</strong> Automatic approval via BRTA API verification
 * of ownership documents.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class VehicleClaimService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleClaimService.class);

    private final VehicleClaimRepository claimRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    // ── Customer operations ───────────────────────────────────────────────────

    /**
     * Submits a new ownership claim for an already-registered vehicle.
     *
     * <p>The claiming user must not be the current owner, and must not already
     * have an open PENDING claim for the same vehicle.
     *
     * <p><strong>Future scope:</strong> OTP verification of the claimant's mobile
     * number and BRTA API cross-check of the supplied NID will be required before
     * the claim is accepted.
     *
     * @param claimantUserId UUID of the authenticated customer submitting the claim
     * @param request        claim details (registration number, NID, reason)
     * @return the newly created {@link VehicleClaimResponse}
     */
    public VehicleClaimResponse submitClaim(UUID claimantUserId, ClaimVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findByRegistrationNumber(request.getRegistrationNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found: " + request.getRegistrationNumber()));

        User claimant = userRepository.findById(claimantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Prevent self-claiming
        if (vehicle.getUser() != null && vehicle.getUser().getId().equals(claimantUserId)) {
            throw new BadRequestException("You are already the registered owner of this vehicle");
        }

        // Prevent duplicate pending claims
        if (claimRepository.existsByVehicleIdAndClaimantIdAndStatus(
                vehicle.getId(), claimantUserId, VehicleClaim.ClaimStatus.PENDING)) {
            throw new BadRequestException(
                    "You already have a pending claim for this vehicle. Please wait for admin review.");
        }

        VehicleClaim claim = VehicleClaim.builder()
                .vehicle(vehicle)
                .claimant(claimant)
                .claimantNid(request.getClaimantNid())
                .reason(request.getReason())
                .status(VehicleClaim.ClaimStatus.PENDING)
                .build();

        claim = claimRepository.save(claim);
        logger.info("Vehicle claim submitted: vehicle={}, claimant={}",
                vehicle.getRegistrationNumber(), claimant.getEmail());
        return toResponse(claim);
    }

    /**
     * Returns all claims submitted by the authenticated customer.
     *
     * @param userId UUID of the authenticated customer
     * @return list of the customer's claims
     */
    @Transactional(readOnly = true)
    public List<VehicleClaimResponse> getMyClaims(UUID userId) {
        return claimRepository.findByClaimantId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    /**
     * Returns a paginated list of claims for admin review.
     *
     * <p>Uses {@link Specification} to build the WHERE clause dynamically — avoids
     * the PostgreSQL {@code ? IS NULL OR column = ?} type-inference failure.
     *
     * @param status   optional status filter (PENDING, APPROVED, REJECTED)
     * @param pageable pagination parameters
     * @return paginated {@link VehicleClaimResponse} results
     */
    @Transactional(readOnly = true)
    public Page<VehicleClaimResponse> getAllClaims(
            VehicleClaim.ClaimStatus status, Pageable pageable) {
        Specification<VehicleClaim> spec = (root, query, cb) -> {
            if (status != null) {
                return cb.equal(root.get("status"), status);
            }
            return cb.conjunction();
        };
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return claimRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /**
     * Approves a pending claim and transfers vehicle ownership to the claimant.
     *
     * <p>Upon approval:
     * <ul>
     *   <li>The vehicle's owning user is changed to the claimant.</li>
     *   <li>The vehicle status is reset to {@code PENDING} for re-verification.</li>
     *   <li>Any existing quota is suspended.</li>
     *   <li>Vehicle and quota caches are evicted.</li>
     * </ul>
     *
     * @param claimId    UUID of the claim to approve
     * @param adminNotes optional notes from the approving admin
     * @return the updated {@link VehicleClaimResponse}
     */
    @CacheEvict(value = {"vehicle", "quota"}, allEntries = true)
    public VehicleClaimResponse approveClaim(UUID claimId, String adminNotes) {
        VehicleClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));

        if (claim.getStatus() != VehicleClaim.ClaimStatus.PENDING) {
            throw new BadRequestException("Only PENDING claims can be approved");
        }

        // Transfer ownership
        Vehicle vehicle = claim.getVehicle();
        vehicle.setUser(claim.getClaimant());
        vehicle.setOwnerName(claim.getClaimant().getName());
        vehicle.setOwnerEmail(claim.getClaimant().getEmail());
        vehicle.setOwnerNid(claim.getClaimantNid());
        // Keep VERIFIED after ownership transfer.
        // Future scope: trigger BRTA re-verification before activating the new owner.
        vehicle.setStatus(Vehicle.VehicleStatus.VERIFIED);
        vehicleRepository.save(vehicle);

        // Finalize claim
        claim.setStatus(VehicleClaim.ClaimStatus.APPROVED);
        claim.setAdminNotes(adminNotes);
        claim = claimRepository.save(claim);

        logger.info("Vehicle claim approved: vehicle={}, new owner={}",
                vehicle.getRegistrationNumber(), claim.getClaimant().getEmail());
        return toResponse(claim);
    }

    /**
     * Rejects a pending claim.
     *
     * @param claimId    UUID of the claim to reject
     * @param adminNotes rejection reason/notes from the admin
     * @return the updated {@link VehicleClaimResponse}
     */
    public VehicleClaimResponse rejectClaim(UUID claimId, String adminNotes) {
        VehicleClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));

        if (claim.getStatus() != VehicleClaim.ClaimStatus.PENDING) {
            throw new BadRequestException("Only PENDING claims can be rejected");
        }

        claim.setStatus(VehicleClaim.ClaimStatus.REJECTED);
        claim.setAdminNotes(adminNotes);
        claim = claimRepository.save(claim);

        logger.info("Vehicle claim rejected: vehicle={}, claimant={}",
                claim.getVehicle().getRegistrationNumber(),
                claim.getClaimant().getEmail());
        return toResponse(claim);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private VehicleClaimResponse toResponse(VehicleClaim claim) {
        return VehicleClaimResponse.builder()
                .id(claim.getId().toString())
                .vehicleId(claim.getVehicle().getId().toString())
                .registrationNumber(claim.getVehicle().getRegistrationNumber())
                .claimantUserId(claim.getClaimant().getId().toString())
                .claimantName(claim.getClaimant().getName())
                .claimantNid(claim.getClaimantNid())
                .reason(claim.getReason())
                .status(claim.getStatus().name())
                .adminNotes(claim.getAdminNotes())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}

