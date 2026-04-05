package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.QuotaConfigSetRequest;
import io.github.eendroroy.fuelquota.dto.response.QuotaConfigSetResponse;
import io.github.eendroroy.fuelquota.entity.QuotaConfigSet;
import io.github.eendroroy.fuelquota.entity.RegistrationCode;
import io.github.eendroroy.fuelquota.exception.BadRequestException;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;
import io.github.eendroroy.fuelquota.repository.QuotaConfigSetRepository;
import io.github.eendroroy.fuelquota.repository.RegistrationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing quota configuration sets.
 *
 * <p>A config set groups multiple vehicle registration codes under a single
 * fuel limit and period. Vehicles whose registration code is not covered by
 * any set fall back to the global {@code QuotaConfig} default.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class QuotaConfigSetService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaConfigSetService.class);

    private final QuotaConfigSetRepository repository;
    private final RegistrationCodeRepository registrationCodeRepository;

    /**
     * Returns all quota config sets.
     */
    @Transactional(readOnly = true)
    public List<QuotaConfigSetResponse> getAllSets() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single config set by ID.
     */
    @Transactional(readOnly = true)
    public QuotaConfigSetResponse getSetById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Quota config set not found"));
    }

    /**
     * Creates a new config set.
     *
     * @throws BadRequestException if any registration code is invalid or already in another set
     */
    public QuotaConfigSetResponse createSet(QuotaConfigSetRequest request) {
        List<String> codes = normalise(request.getRegistrationCodes());
        validateCodes(codes, null);

        QuotaConfigSet set = QuotaConfigSet.builder()
                .name(request.getName().trim())
                .limitLitres(request.getLimitLitres())
                .quotaPeriod(request.getQuotaPeriod())
                .description(request.getDescription())
                .registrationCodes(codes)
                .build();

        set = repository.save(set);
        logger.info("Created quota config set '{}' with {} codes: {}L / {}",
                set.getName(), codes.size(), set.getLimitLitres(), set.getQuotaPeriod());
        return toResponse(set);
    }

    /**
     * Updates an existing config set.
     *
     * @throws ResourceNotFoundException if the set is not found
     * @throws BadRequestException       if any code is invalid or conflicts with another set
     */
    public QuotaConfigSetResponse updateSet(UUID id, QuotaConfigSetRequest request) {
        QuotaConfigSet set = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quota config set not found"));

        List<String> codes = normalise(request.getRegistrationCodes());
        validateCodes(codes, id);

        set.setName(request.getName().trim());
        set.setLimitLitres(request.getLimitLitres());
        set.setQuotaPeriod(request.getQuotaPeriod());
        set.setDescription(request.getDescription());
        set.setRegistrationCodes(codes);

        set = repository.save(set);
        logger.info("Updated quota config set '{}': {}L / {}", set.getName(), set.getLimitLitres(), set.getQuotaPeriod());
        return toResponse(set);
    }

    /**
     * Deletes a config set.
     *
     * @throws ResourceNotFoundException if the set is not found
     */
    public void deleteSet(UUID id) {
        QuotaConfigSet set = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quota config set not found"));
        repository.delete(set);
        logger.info("Deleted quota config set '{}'", set.getName());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> normalise(List<String> raw) {
        if (raw == null) return new ArrayList<>();
        return raw.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(String::toUpperCase)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateCodes(List<String> codes, UUID excludeSetId) {
        for (String code : codes) {
            if (!registrationCodeRepository.existsByCode(code)) {
                throw new BadRequestException("Invalid registration code: " + code);
            }
            boolean conflict = excludeSetId == null
                    ? repository.existsByRegistrationCode(code)
                    : repository.existsByRegistrationCodeAndIdNot(code, excludeSetId);
            if (conflict) {
                throw new BadRequestException("Registration code '" + code + "' is already assigned to another config set");
            }
        }
    }

    private QuotaConfigSetResponse toResponse(QuotaConfigSet set) {
        List<QuotaConfigSetResponse.RegistrationCodeInfo> details = set.getRegistrationCodes().stream()
                .map(code -> {
                    String desc = registrationCodeRepository.findByCode(code)
                            .map(RegistrationCode::getDescription)
                            .orElse(null);
                    return QuotaConfigSetResponse.RegistrationCodeInfo.builder()
                            .code(code)
                            .description(desc)
                            .build();
                })
                .collect(Collectors.toList());

        return QuotaConfigSetResponse.builder()
                .id(set.getId().toString())
                .name(set.getName())
                .limitLitres(set.getLimitLitres())
                .quotaPeriod(set.getQuotaPeriod())
                .description(set.getDescription())
                .registrationCodes(new ArrayList<>(set.getRegistrationCodes()))
                .registrationCodeDetails(details)
                .createdAt(set.getCreatedAt())
                .updatedAt(set.getUpdatedAt())
                .build();
    }
}

