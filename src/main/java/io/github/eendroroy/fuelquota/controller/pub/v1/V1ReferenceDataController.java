package io.github.eendroroy.fuelquota.controller.pub.v1;

import io.github.eendroroy.fuelquota.dto.response.BrtaOfficeResponse;
import io.github.eendroroy.fuelquota.dto.response.RegistrationCodeResponse;
import io.github.eendroroy.fuelquota.service.BrtaOfficeService;
import io.github.eendroroy.fuelquota.service.RegistrationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public reference-data endpoints — no authentication required.
 */
@RestController
@RequestMapping("/api/public/v1")
@RequiredArgsConstructor
@Tag(name = "Public v1 - Reference Data", description = "Unauthenticated lookup endpoints for BRTA offices and registration codes")
public class V1ReferenceDataController {

    private final RegistrationCodeService registrationCodeService;
    private final BrtaOfficeService brtaOfficeService;

    @GetMapping("/registration-codes")
    @Operation(summary = "Get all vehicle registration codes")
    public ResponseEntity<List<RegistrationCodeResponse>> getRegistrationCodes() {
        return ResponseEntity.ok(registrationCodeService.getAllCodes());
    }

    @GetMapping("/brta-offices")
    @Operation(summary = "Get all BRTA regional offices")
    public ResponseEntity<List<BrtaOfficeResponse>> getBrtaOffices() {
        return ResponseEntity.ok(brtaOfficeService.getAllOffices());
    }
}

