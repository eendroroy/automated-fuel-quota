package io.github.eendroroy.fuelquota.dto.response;

import io.github.eendroroy.fuelquota.entity.PumpRepresentative;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pump representative account details returned to admin callers.
 *
 * <p>The {@code passwordHash} field is intentionally excluded.
 * Returned by all {@code /api/admin/pump-representatives/**} endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Pump representative account details")
public class PumpRepresentativeResponse {

    /** UUID of the pump representative record. */
    @Schema(description = "Pump representative UUID")
    private UUID id;

    /** UUID of the fuel station this representative is assigned to. */
    @Schema(description = "Assigned fuel station UUID")
    private UUID stationId;

    /** Human-readable name of the assigned fuel station. */
    @Schema(description = "Assigned fuel station name", example = "ABC Fuel Station Dhanmondi")
    private String stationName;

    /** Full name of the representative. */
    @Schema(description = "Representative's full name", example = "Ali Hassan")
    private String name;

    /** Contact mobile number. */
    @Schema(description = "Mobile phone number", example = "+8801755000001")
    private String mobileNumber;

    /** Work e-mail address. */
    @Schema(description = "Work email address", example = "ali.hassan@station.com")
    private String email;

    /** Internal employee identifier. */
    @Schema(description = "Employee ID", example = "EMP-2025-001")
    private String employeeId;

    /** Login username used by the pump representative mobile app. */
    @Schema(description = "App login username", example = "ali.hassan")
    private String username;

    /** Current account status: {@code ACTIVE}, {@code INACTIVE}, or {@code SUSPENDED}. */
    @Schema(description = "Account status", example = "ACTIVE")
    private PumpRepresentative.RepStatus status;

    /** Timestamp of the most recent successful login. */
    @Schema(description = "Last login timestamp")
    private LocalDateTime lastLoginTimestamp;

    /** Record creation timestamp. */
    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;
}

