package io.github.eendroroy.fuelquota.controller;

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
 *
 * <p>Used by the customer self-registration form to populate the
 * structured vehicle number picker dropdowns.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public Reference Data", description = "Unauthenticated lookup endpoints for BRTA offices and registration codes")
public class ReferenceDataController {

    private final RegistrationCodeService registrationCodeService;
    private final BrtaOfficeService brtaOfficeService;

    /**
     * Returns all vehicle category registration codes.
     *
     * <p>Used to populate the "CODE" dropdown in the vehicle number picker.
     */
    @GetMapping("/registration-codes")
    @Operation(
        summary = "Get all vehicle registration codes",
        description = "Returns all vehicle category prefix codes (e.g. GA → Private Cars 1301-2000 cc)"
    )
    public ResponseEntity<List<RegistrationCodeResponse>> getRegistrationCodes() {
        return ResponseEntity.ok(registrationCodeService.getAllCodes());
    }

    /**
     * Returns all BRTA regional office codes.
     *
     * <p>Used to populate the "BRTA" dropdown in the vehicle number picker.
     */
    @GetMapping("/brta-offices")
    @Operation(
        summary = "Get all BRTA regional offices",
        description = "Returns all BRTA office / region codes (e.g. DHAKA METRO → Dhaka Metropolitan Area)"
    )
    public ResponseEntity<List<BrtaOfficeResponse>> getBrtaOffices() {
        return ResponseEntity.ok(brtaOfficeService.getAllOffices());
    }
}

