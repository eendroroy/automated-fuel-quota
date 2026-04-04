package io.github.eendroroy.fuelquota.controller;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.request.QuotaConfigByRegistrationCodeRequest;
import io.github.eendroroy.fuelquota.dto.response.QuotaConfigByRegistrationCodeResponse;
import io.github.eendroroy.fuelquota.service.QuotaConfigByRegistrationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin endpoints for managing quota configurations by vehicle registration code.
 *
 * <p>Allows admins to set different quota limits and periods for different
 * vehicle categories (e.g., LA = 20L DAILY, GA = 30L WEEKLY).
 */
@RestController
@RequestMapping("/api/admin/quota-config-by-code")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Quota Config by Registration Code", description = "Admin endpoints for managing quota configurations by vehicle registration code")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AdminQuotaConfigByCodeController {

    private final QuotaConfigByRegistrationCodeService service;

    /**
     * Retrieves all quota configurations by registration code.
     *
     * @return list of all configurations
     */
    @GetMapping
    @Operation(
        summary = "Get all quota configurations by registration code",
        description = "Returns all quota configurations organized by vehicle registration code"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configurations retrieved successfully",
            content = @Content(schema = @Schema(implementation = QuotaConfigByRegistrationCodeResponse.class))
        )
    })
    public ResponseEntity<List<QuotaConfigByRegistrationCodeResponse>> getAllConfigurations() {
        return ResponseEntity.ok(service.getAllConfigurations());
    }

    /**
     * Retrieves a single configuration by ID.
     *
     * @param id configuration UUID
     * @return the configuration response
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get quota configuration by ID",
        description = "Returns a single quota configuration by its UUID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuration retrieved successfully",
            content = @Content(schema = @Schema(implementation = QuotaConfigByRegistrationCodeResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    public ResponseEntity<QuotaConfigByRegistrationCodeResponse> getConfigurationById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getConfigurationById(id));
    }

    /**
     * Retrieves a configuration by registration code.
     *
     * @param code vehicle registration code
     * @return the configuration response
     */
    @GetMapping("/code/{code}")
    @Operation(
        summary = "Get quota configuration by registration code",
        description = "Returns the quota configuration for a specific vehicle registration code"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuration retrieved successfully",
            content = @Content(schema = @Schema(implementation = QuotaConfigByRegistrationCodeResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Configuration not found for this code")
    })
    public ResponseEntity<QuotaConfigByRegistrationCodeResponse> getConfigurationByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getConfigurationByCode(code));
    }

    /**
     * Creates a new quota configuration for a registration code.
     *
     * @param request configuration details
     * @return the created configuration
     */
    @PostMapping
    @Operation(
        summary = "Create quota configuration",
        description = "Creates a new quota configuration for a specific vehicle registration code"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Configuration created successfully",
            content = @Content(schema = @Schema(implementation = QuotaConfigByRegistrationCodeResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request or duplicate registration code")
    })
    public ResponseEntity<QuotaConfigByRegistrationCodeResponse> createConfiguration(
            @Valid @RequestBody QuotaConfigByRegistrationCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createConfiguration(request));
    }

    /**
     * Updates an existing quota configuration.
     *
     * @param id      configuration UUID
     * @param request updated configuration details
     * @return the updated configuration
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update quota configuration",
        description = "Updates an existing quota configuration"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuration updated successfully",
            content = @Content(schema = @Schema(implementation = QuotaConfigByRegistrationCodeResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    public ResponseEntity<QuotaConfigByRegistrationCodeResponse> updateConfiguration(
            @PathVariable UUID id,
            @Valid @RequestBody QuotaConfigByRegistrationCodeRequest request) {
        return ResponseEntity.ok(service.updateConfiguration(id, request));
    }

    /**
     * Deletes a quota configuration.
     *
     * @param id configuration UUID
     * @return no content
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete quota configuration",
        description = "Deletes a quota configuration by registration code"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Configuration deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    public ResponseEntity<Void> deleteConfiguration(@PathVariable UUID id) {
        service.deleteConfiguration(id);
        return ResponseEntity.noContent().build();
    }
}

