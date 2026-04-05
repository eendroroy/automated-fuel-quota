package io.github.eendroroy.fuelquota.controller.admin.v1;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.request.UserStatusUpdateRequest;
import io.github.eendroroy.fuelquota.dto.response.AppUserResponse;
import io.github.eendroroy.fuelquota.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin user management endpoints.
 *
 * <p>Provides paginated listing, individual look-up, and status management
 * of CUSTOMER and ADMIN accounts. PUMP_REPRESENTATIVE accounts are excluded.
 */
@RestController
@RequestMapping("/api/admin/v1")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin v1 - Users", description = "User management operations")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class V1AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Returns a paginated, filterable list of customer and admin users.
     *
     * @param page   zero-based page index (default 0)
     * @param size   number of records per page (default 20)
     * @param role   optional role filter: CUSTOMER | ADMIN
     * @param status optional status filter: ACTIVE | SUSPENDED | INACTIVE
     * @param search optional free-text search on name, email, or mobile
     */
    @GetMapping("/users")
    @Operation(summary = "List all users (paginated)",
            description = "Returns all CUSTOMER and ADMIN users with optional filtering and search")
    public ResponseEntity<Page<AppUserResponse>> getUsers(
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Role filter (CUSTOMER or ADMIN)") @RequestParam(required = false) String role,
            @Parameter(description = "Status filter (ACTIVE, SUSPENDED, INACTIVE)") @RequestParam(required = false) String status,
            @Parameter(description = "Search term for name, email, or mobile") @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminUserService.getUsers(page, size, role, status, search));
    }

    /**
     * Returns a single user by UUID.
     *
     * @param id user UUID
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<AppUserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    /**
     * Updates the account status (ACTIVE / SUSPENDED) of a user.
     *
     * @param id      user UUID
     * @param request new status and optional reason
     */
    @PutMapping("/users/{id}/status")
    @Operation(summary = "Update user account status",
            description = "Suspend or activate a CUSTOMER or ADMIN account. Audit log entry is created.")
    public ResponseEntity<AppUserResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UserStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        UUID adminId = (UUID) httpRequest.getAttribute("userId");
        String adminName = (String) httpRequest.getAttribute("userName");
        return ResponseEntity.ok(adminUserService.updateUserStatus(id, request, adminId, adminName));
    }
}

