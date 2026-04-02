package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.response.BrtaOfficeResponse;
import io.github.eendroroy.fuelquota.entity.BrtaOffice;
import io.github.eendroroy.fuelquota.repository.BrtaOfficeRepository;

import io.github.eendroroy.fuelquota.config.DataInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for BRTA regional office code lookups.
 *
 * <p>Provides access to BRTA office / region codes used in the
 * structured vehicle registration number input
 * (e.g. {@code DHAKA METRO} → "Dhaka Metropolitan Area").
 *
 * <p>Data is seeded at startup by {@link DataInitializer}.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BrtaOfficeService {

    private final BrtaOfficeRepository brtaOfficeRepository;

    /**
     * Returns all BRTA office entries.
     *
     * @return list of {@link BrtaOfficeResponse} DTOs
     */
    public List<BrtaOfficeResponse> getAllOffices() {
        return brtaOfficeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private BrtaOfficeResponse toResponse(BrtaOffice office) {
        return BrtaOfficeResponse.builder()
                .brtaCode(office.getBrtaCode())
                .description(office.getDescription())
                .build();
    }
}

