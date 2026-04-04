package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.QuotaConfigByRegistrationCodeRequest;
import io.github.eendroroy.fuelquota.dto.response.QuotaConfigByRegistrationCodeResponse;
import io.github.eendroroy.fuelquota.entity.QuotaConfigByRegistrationCode;
import io.github.eendroroy.fuelquota.entity.RegistrationCode;
import io.github.eendroroy.fuelquota.exception.BadRequestException;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;
import io.github.eendroroy.fuelquota.repository.QuotaConfigByRegistrationCodeRepository;
import io.github.eendroroy.fuelquota.repository.RegistrationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing quota configurations by vehicle registration code.
 *
 * <p>Allows admins to set different quota limits and periods for different
 * vehicle categories (e.g., LA = 20L DAILY, GA = 30L WEEKLY).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class QuotaConfigByRegistrationCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaConfigByRegistrationCodeService.class);

    private final QuotaConfigByRegistrationCodeRepository repository;
    private final RegistrationCodeRepository registrationCodeRepository;

    /**
     * Retrieves all quota configurations by registration code.
     *
     * @return list of all configurations
     */
    @Transactional(readOnly = true)
    public List<QuotaConfigByRegistrationCodeResponse> getAllConfigurations() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single configuration by ID.
     *
     * @param id configuration UUID
     * @return the configuration response
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public QuotaConfigByRegistrationCodeResponse getConfigurationById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found"));
    }

    /**
     * Retrieves a single configuration by registration code.
     *
     * @param registrationCode vehicle registration code
     * @return the configuration response
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public QuotaConfigByRegistrationCodeResponse getConfigurationByCode(String registrationCode) {
        return repository.findByRegistrationCode(registrationCode.toUpperCase())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for code: " + registrationCode));
    }

    /**
     * Creates a new quota configuration for a registration code.
     *
     * @param request configuration details
     * @return the created configuration
     * @throws BadRequestException if the registration code is invalid or already configured
     */
    public QuotaConfigByRegistrationCodeResponse createConfiguration(QuotaConfigByRegistrationCodeRequest request) {
        String code = request.getRegistrationCode().toUpperCase().trim();

        // Validate registration code exists
        if (!registrationCodeRepository.existsByCode(code)) {
            throw new BadRequestException("Invalid registration code: " + code);
        }

        // Check for duplicate
        if (repository.existsByRegistrationCode(code)) {
            throw new BadRequestException("Configuration already exists for registration code: " + code);
        }

        QuotaConfigByRegistrationCode config = QuotaConfigByRegistrationCode.builder()
                .registrationCode(code)
                .limitLitres(request.getLimitLitres())
                .quotaPeriod(request.getQuotaPeriod())
                .description(request.getDescription())
                .build();

        config = repository.save(config);
        logger.info("Created quota configuration for registration code {}: {} L / {}",
                code, request.getLimitLitres(), request.getQuotaPeriod());

        return toResponse(config);
    }

    /**
     * Updates an existing quota configuration.
     *
     * @param id      configuration UUID
     * @param request updated configuration details
     * @return the updated configuration
     * @throws ResourceNotFoundException if not found
     */
    public QuotaConfigByRegistrationCodeResponse updateConfiguration(UUID id, QuotaConfigByRegistrationCodeRequest request) {
        QuotaConfigByRegistrationCode config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found"));

        config.setLimitLitres(request.getLimitLitres());
        config.setQuotaPeriod(request.getQuotaPeriod());
        config.setDescription(request.getDescription());

        config = repository.save(config);
        logger.info("Updated quota configuration for registration code {}: {} L / {}",
                config.getRegistrationCode(), request.getLimitLitres(), request.getQuotaPeriod());

        return toResponse(config);
    }

    /**
     * Deletes a quota configuration.
     *
     * @param id configuration UUID
     * @throws ResourceNotFoundException if not found
     */
    public void deleteConfiguration(UUID id) {
        QuotaConfigByRegistrationCode config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found"));

        repository.delete(config);
        logger.info("Deleted quota configuration for registration code {}", config.getRegistrationCode());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private QuotaConfigByRegistrationCodeResponse toResponse(QuotaConfigByRegistrationCode config) {
        // Fetch registration code description if available
        String description = registrationCodeRepository.findByCode(config.getRegistrationCode())
                .map(RegistrationCode::getDescription)
                .orElse(null);

        return QuotaConfigByRegistrationCodeResponse.builder()
                .id(config.getId().toString())
                .registrationCode(config.getRegistrationCode())
                .registrationCodeDescription(description)
                .limitLitres(config.getLimitLitres())
                .quotaPeriod(config.getQuotaPeriod())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}

