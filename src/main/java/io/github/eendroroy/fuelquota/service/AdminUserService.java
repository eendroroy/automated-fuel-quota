package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.CreateAdminUserRequest;
import io.github.eendroroy.fuelquota.dto.request.UserStatusUpdateRequest;
import io.github.eendroroy.fuelquota.dto.response.AppUserResponse;
import io.github.eendroroy.fuelquota.entity.AuditLog;
import io.github.eendroroy.fuelquota.entity.User;
import io.github.eendroroy.fuelquota.exception.BadRequestException;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;
import io.github.eendroroy.fuelquota.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-facing user management service.
 *
 * <p>Supports listing, filtering, viewing, and status-updating of CUSTOMER
 * and ADMIN accounts. PUMP_REPRESENTATIVE accounts are excluded from all
 * results as they are managed separately.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Returns a paginated, filterable list of CUSTOMER and ADMIN users.
     *
     * <p>Builds the WHERE clause via {@link Specification} so that null filter
     * parameters are simply omitted from the query — avoids the PostgreSQL
     * {@code ? IS NULL OR column = ?} type-inference failure.
     *
     * @param page           zero-based page index
     * @param size           number of records per page
     * @param roleStr        optional role filter ("CUSTOMER" | "ADMIN")
     * @param statusStr      optional status filter ("ACTIVE" | "SUSPENDED" | "INACTIVE")
     * @param search         optional free-text search on name, email, or mobile
     * @param createdAtFrom  optional lower bound for registration date
     * @param createdAtTo    optional upper bound for registration date
     * @return page of {@link AppUserResponse}
     */
    public Page<AppUserResponse> getUsers(int page, int size, String roleStr, String statusStr,
                                          String search, LocalDateTime createdAtFrom, LocalDateTime createdAtTo) {
        User.UserRole role     = (roleStr   != null && !roleStr.isBlank())   ? parseRole(roleStr)     : null;
        User.UserStatus status = (statusStr != null && !statusStr.isBlank()) ? parseStatus(statusStr) : null;
        String searchTerm      = (search    != null && !search.isBlank())    ? search.trim()          : null;

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always restrict to CUSTOMER and ADMIN — exclude PUMP_REPRESENTATIVE
            predicates.add(root.get("role").in(User.UserRole.CUSTOMER, User.UserRole.ADMIN));

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (searchTerm != null) {
                String pattern = "%" + searchTerm.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("mobileNumber"), "")), pattern)
                ));
            }
            if (createdAtFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtFrom));
            }
            if (createdAtTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdAtTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /**
     * Returns a single user by ID.
     *
     * @param id user UUID
     * @return {@link AppUserResponse}
     * @throws ResourceNotFoundException if the user is not found
     */
    public AppUserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    /**
     * Updates the account status of a user.
     *
     * <p>An admin cannot suspend their own account.
     * PUMP_REPRESENTATIVE accounts cannot be managed through this endpoint.
     * When an account is suspended the {@code enabled} flag is also set to
     * {@code false} so that existing JWT tokens are rejected.
     *
     * @param id         user UUID
     * @param request    new status and optional reason
     * @param adminId    UUID of the admin performing the action (for audit)
     * @param adminName  display name of the admin (for audit)
     * @return updated {@link AppUserResponse}
     */
    @Transactional
    public AppUserResponse updateUserStatus(UUID id, UserStatusUpdateRequest request,
                                            UUID adminId, String adminName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == User.UserRole.PUMP_REPRESENTATIVE) {
            throw new BadRequestException("Pump representative accounts are managed separately");
        }

        User.UserStatus newStatus = parseStatus(request.getStatus());

        // Prevent admin from suspending themselves
        if (newStatus == User.UserStatus.SUSPENDED && id.equals(adminId)) {
            throw new BadRequestException("Admins cannot suspend their own account");
        }

        User.UserStatus oldStatus = user.getStatus() != null ? user.getStatus() : User.UserStatus.ACTIVE;

        user.setStatus(newStatus);
        // Keep Spring Security 'enabled' flag in sync
        user.setEnabled(newStatus == User.UserStatus.ACTIVE);
        userRepository.save(user);

        // Audit log
        AuditLog.AuditAction action = newStatus == User.UserStatus.ACTIVE
                ? AuditLog.AuditAction.USER_ACTIVATED
                : AuditLog.AuditAction.USER_SUSPENDED;
        auditLogService.log(
                adminId, adminName, action,
                "User", id.toString(),
                Map.of("status", oldStatus.name()),
                Map.of("status", newStatus.name()),
                request.getReason()
        );

        return toResponse(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates a new ADMIN user account.
     *
     * @param request    name, email, and password of the new admin
     * @param adminId    UUID of the acting admin (for audit)
     * @param adminName  display name of the acting admin (for audit)
     * @return the created {@link AppUserResponse}
     */
    @Transactional
    public AppUserResponse createAdminUser(CreateAdminUserRequest request, UUID adminId, String adminName) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                User.UserRole.ADMIN
        );
        user.setStatus(User.UserStatus.ACTIVE);
        user = userRepository.save(user);
        auditLogService.log(
                adminId, adminName, AuditLog.AuditAction.USER_ACTIVATED,
                "User", user.getId().toString(),
                null,
                Map.of("email", request.getEmail(), "name", request.getName(), "role", "ADMIN"),
                "New admin user created"
        );
        return toResponse(user);
    }

    private AppUserResponse toResponse(User user) {
        long vehicleCount = userRepository.countVehiclesByUserId(user.getId());
        return AppUserResponse.builder()
                .id(user.getId().toString())
                .name(user.getName())
                .mobileNumber(user.getMobileNumber())
                .email(user.getEmail())
                .nid(user.getNid())
                .role(user.getRole().name())
                .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
                .createdAt(user.getCreatedAt())
                .lastLoginTimestamp(user.getLastLoginTimestamp())
                .vehicleCount(vehicleCount)
                .build();
    }

    private User.UserRole parseRole(String roleStr) {
        try {
            return User.UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + roleStr);
        }
    }

    private User.UserStatus parseStatus(String statusStr) {
        try {
            return User.UserStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + statusStr);
        }
    }
}

