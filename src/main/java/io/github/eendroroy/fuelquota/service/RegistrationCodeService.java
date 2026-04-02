package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.response.RegistrationCodeResponse;
import io.github.eendroroy.fuelquota.entity.RegistrationCode;
import io.github.eendroroy.fuelquota.repository.RegistrationCodeRepository;

import io.github.eendroroy.fuelquota.config.DataInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for vehicle registration code lookups.
 *
 * <p>Provides access to BRTA vehicle category prefix codes
 * (e.g. {@code GA} → "Private Cars (1301 to 2000 cc)").
 *
 * <p>Data is seeded at startup by {@link DataInitializer}.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegistrationCodeService {

    private final RegistrationCodeRepository registrationCodeRepository;

    /**
     * Returns all active vehicle registration codes.
     *
     * @return list of {@link RegistrationCodeResponse} DTOs
     */
    public List<RegistrationCodeResponse> getAllCodes() {
        return registrationCodeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private RegistrationCodeResponse toResponse(RegistrationCode rc) {
        return RegistrationCodeResponse.builder()
                .code(rc.getCode())
                .description(rc.getDescription())
                .build();
    }
}

