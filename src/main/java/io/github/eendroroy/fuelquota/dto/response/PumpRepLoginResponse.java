package io.github.eendroroy.fuelquota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned after successful pump representative login.
 * Contains the representative's details and assigned station info.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Pump representative session info returned after login")
public class PumpRepLoginResponse {

    @Schema(description = "UUID of the pump representative", example = "b5e2d3a1-1234-...")
    private String id;

    @Schema(description = "Full name of the representative", example = "Mohammad Rahman")
    private String name;

    @Schema(description = "Employee ID used to log in", example = "EMP-001")
    private String employeeId;

    @Schema(description = "UUID of the assigned fuel station")
    private String stationId;

    @Schema(description = "Display name of the station", example = "Dhaka North Station")
    private String stationName;

    @Schema(description = "Short station code", example = "DHK-N-01")
    private String stationCode;
}

