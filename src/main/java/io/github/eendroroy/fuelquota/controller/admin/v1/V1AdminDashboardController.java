package io.github.eendroroy.fuelquota.controller.admin.v1;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.response.DashboardStatsResponse;
import io.github.eendroroy.fuelquota.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin v1 - Dashboard", description = "Admin dashboard statistics")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class V1AdminDashboardController {

    private final AdminStatsService statsService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics", description = "Returns overview statistics for the admin dashboard")
    @ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = DashboardStatsResponse.class)))
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(statsService.getDashboardStats());
    }
}

