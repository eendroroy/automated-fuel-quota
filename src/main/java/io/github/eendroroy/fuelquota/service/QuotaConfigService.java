package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.config.AppProperties;
import io.github.eendroroy.fuelquota.dto.request.QuotaConfigRequest;
import io.github.eendroroy.fuelquota.dto.response.QuotaConfigResponse;
import io.github.eendroroy.fuelquota.entity.QuotaConfig;
import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import io.github.eendroroy.fuelquota.repository.QuotaConfigRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for managing the persisted quota configuration.
 *
 * <p>The application supports a single <em>default</em> configuration row stored
 * under key {@code "DEFAULT"}.  If no row exists in the database the service falls
 * back to the values defined in {@code application.yaml} ({@link AppProperties}).
 *
 * <p>Changes applied via {@link #updateConfig(QuotaConfigRequest)} take effect
 * immediately for all newly created quotas without requiring an application restart.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class QuotaConfigService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaConfigService.class);

    private final QuotaConfigRepository quotaConfigRepository;
    private final AppProperties appProperties;

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Returns the current default quota configuration, creating it from
     * {@code application.yaml} values if it does not yet exist in the database.
     *
     * @return the active {@link QuotaConfig}
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "quotaConfig", key = "'default'")
    public QuotaConfig getDefaultConfig() {
        return quotaConfigRepository.findByConfigKey(QuotaConfig.DEFAULT_KEY)
                .orElseGet(this::createDefaultFromProperties);
    }

    /** Convenience method — returns the configured fuel limit in litres. */
    @Transactional(readOnly = true)
    public BigDecimal getDefaultLimitLitres() {
        return getDefaultConfig().getLimitLitres();
    }

    /** Convenience method — returns the configured quota period. */
    @Transactional(readOnly = true)
    public QuotaPeriod getDefaultPeriod() {
        return getDefaultConfig().getQuotaPeriod();
    }

    /**
     * Returns the current configuration as a response DTO.
     *
     * @return {@link QuotaConfigResponse}
     */
    @Transactional(readOnly = true)
    public QuotaConfigResponse getConfigResponse() {
        return toResponse(getDefaultConfig());
    }

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Creates or updates the default quota configuration.
     *
     * <p>Evicts the {@code quotaConfig} cache so the next read picks up the
     * new values.
     *
     * @param request new configuration values
     * @return the saved configuration as a response DTO
     */
    @CacheEvict(value = "quotaConfig", allEntries = true)
    public QuotaConfigResponse updateConfig(QuotaConfigRequest request) {
        QuotaConfig config = quotaConfigRepository
                .findByConfigKey(QuotaConfig.DEFAULT_KEY)
                .orElseGet(() -> QuotaConfig.builder()
                        .configKey(QuotaConfig.DEFAULT_KEY)
                        .build());

        config.setLimitLitres(request.getLimitLitres());
        config.setGeofenceRadiusMeters(request.getGeofenceRadiusMeters());
        config.setQuotaPeriod(request.getQuotaPeriod());
        config.setResetCronExpression(request.getResetCronExpression());
        config.setDescription(request.getDescription());

        config = quotaConfigRepository.save(config);
        logger.info("Quota configuration updated: limit={}L, period={}, geofence={}m",
                config.getLimitLitres(), config.getQuotaPeriod(), config.getGeofenceRadiusMeters());
        return toResponse(config);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private QuotaConfig createDefaultFromProperties() {
        AppProperties.Quota props = appProperties.getQuota();
        QuotaConfig config = QuotaConfig.builder()
                .configKey(QuotaConfig.DEFAULT_KEY)
                .limitLitres(BigDecimal.valueOf(props.getLimitLitres()))
                .geofenceRadiusMeters(props.getGeofenceRadiusMeters())
                .quotaPeriod(props.getPeriod())
                .resetCronExpression(props.getResetCronExpression())
                .description("Default configuration (seeded from application.yaml)")
                .build();
        config = quotaConfigRepository.save(config);
        logger.info("Default quota configuration seeded from application.yaml");
        return config;
    }

    private QuotaConfigResponse toResponse(QuotaConfig config) {
        return QuotaConfigResponse.builder()
                .id(config.getId() != null ? config.getId().toString() : null)
                .limitLitres(config.getLimitLitres())
                .geofenceRadiusMeters(config.getGeofenceRadiusMeters())
                .quotaPeriod(config.getQuotaPeriod())
                .resetCronExpression(config.getResetCronExpression())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}

