package io.github.eendroroy.fuelquota.mapper;

import io.github.eendroroy.fuelquota.dto.response.TransactionResponse;
import io.github.eendroroy.fuelquota.entity.Transaction;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Transaction} entities to {@link TransactionResponse} DTOs.
 *
 * <p>Accesses the LAZY-loaded {@code vehicle} and {@code station} associations;
 * ensure these are initialised within a transaction context before calling this mapper.
 * The raw QR token ({@code qrTokenUsed}) is intentionally excluded from the response.
 */
@Component
public class TransactionMapper {

    /**
     * Converts a {@link Transaction} entity to a {@link TransactionResponse}.
     *
     * @param transaction the source entity (must not be {@code null})
     * @return a populated {@link TransactionResponse}
     */
    public TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId().toString())
                .vehicleId(transaction.getVehicle().getId().toString())
                .registrationNumber(transaction.getVehicle().getRegistrationNumber())
                .stationId(transaction.getStation().getId().toString())
                .stationName(transaction.getStation().getStationName())
                .pumpRepresentativeId(transaction.getPumpRepresentative() != null
                        ? transaction.getPumpRepresentative().getId().toString() : null)
                .amountDispensedLiters(transaction.getAmountDispensedLiters())
                .fuelTypeDispensed(transaction.getFuelTypeDispensed())
                .transactionTimestamp(transaction.getTransactionTimestamp())
                .remainingQuotaAfter(transaction.getRemainingQuotaAfter())
                .geofenceVerified(transaction.getGeofenceVerified())
                .status(transaction.getStatus().name())
                .build();
    }
}

