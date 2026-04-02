package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Structured error response returned by the global exception handler.
 *
 * <p>All API error responses share this schema, making front-end error
 * handling consistent across the application.
 *
 * <p>For validation failures ({@code 400}), the optional {@link #validationErrors}
 * map contains per-field messages keyed by field name.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Structured error response for API failures")
public class ErrorResponse {

    /**
     * Machine-readable error code (e.g. {@code RESOURCE_NOT_FOUND},
     * {@code VALIDATION_FAILED}, {@code INTERNAL_SERVER_ERROR}).
     */
    @Schema(description = "Error code", example = "RESOURCE_NOT_FOUND")
    private String error;

    /** Human-readable error description. */
    @Schema(description = "Human-readable error message", example = "Vehicle not found")
    private String message;

    /** HTTP status code mirroring the response status. */
    @Schema(description = "HTTP status code", example = "404")
    private int status;

    /** UTC timestamp when the error occurred. */
    @Schema(description = "Error timestamp (UTC)")
    private Instant timestamp;

    /** Request path that triggered the error. */
    @Schema(description = "Request path", example = "/api/admin/vehicles/abc123")
    private String path;

    /**
     * Per-field validation errors.
     * Only populated when {@code error} is {@code VALIDATION_FAILED}.
     * Key = field name, value = constraint message.
     */
    @Schema(description = "Per-field validation error messages (present on VALIDATION_FAILED only)")
    private Map<String, String> validationErrors;
}

