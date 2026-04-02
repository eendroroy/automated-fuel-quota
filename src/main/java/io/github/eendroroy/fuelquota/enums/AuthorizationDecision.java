package io.github.eendroroy.fuelquota.enums;

import io.github.eendroroy.fuelquota.service.QuotaService;

/**
 * Represents the decision outcome of a fuel dispensing authorization request.
 *
 * <p>Used by {@link QuotaService} to communicate
 * quota availability results to the pump authorization workflow.
 *
 * <ul>
 *   <li>{@code APPROVED} – full requested amount can be dispensed.</li>
 *   <li>{@code PARTIAL} – only a portion of the requested amount is available
 *       (partial dispense as per BRD FR-12).</li>
 *   <li>{@code DENIED} – no fuel can be dispensed (inactive vehicle, zero quota,
 *       geofence failure, etc.).</li>
 * </ul>
 */
public enum AuthorizationDecision {

    /** The full requested fuel quantity is authorized. */
    APPROVED,

    /**
     * Only the remaining quota is authorized, which is less than the requested
     * quantity. Implements BRD partial-dispense requirement (FR-12).
     */
    PARTIAL,

    /** Authorization refused. See accompanying deny-reason message for details. */
    DENIED
}

