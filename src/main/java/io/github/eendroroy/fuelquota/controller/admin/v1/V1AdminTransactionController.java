package io.github.eendroroy.fuelquota.controller.admin.v1;

import io.github.eendroroy.fuelquota.config.OpenApiConfig;
import io.github.eendroroy.fuelquota.dto.response.TransactionResponse;
import io.github.eendroroy.fuelquota.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/v1")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin v1 - Transactions", description = "Transaction history management")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class V1AdminTransactionController {

    private final TransactionService transactionService;

    @GetMapping("/transactions")
    @Operation(summary = "Get transaction history",
            description = "Returns paginated fuel transaction history with optional filtering")
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @Parameter(description = "Filter by vehicle ID")
            @RequestParam(required = false) String vehicleId,
            @Parameter(description = "Filter by station ID")
            @RequestParam(required = false) String stationId,
            @Parameter(description = "Start date filter")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date filter")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID vehicleUuid = vehicleId != null && !vehicleId.isBlank() ? UUID.fromString(vehicleId) : null;
        UUID stationUuid = stationId != null && !stationId.isBlank() ? UUID.fromString(stationId) : null;

        return ResponseEntity.ok(transactionService.getTransactionsWithFilters(
                vehicleUuid, stationUuid, startDate, endDate, pageable));
    }
}

