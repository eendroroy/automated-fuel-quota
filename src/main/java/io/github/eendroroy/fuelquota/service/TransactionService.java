package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.response.TransactionResponse;
import io.github.eendroroy.fuelquota.entity.Transaction;
import io.github.eendroroy.fuelquota.repository.TransactionRepository;
import io.github.eendroroy.fuelquota.mapper.TransactionMapper;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for transaction management and reporting.
 *
 * <p>Provides transaction history retrieval and filtering capabilities
 * for administrative reporting and audit purposes.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    /**
     * Retrieves paginated transaction history with optional filtering.
     *
     * <p>Uses {@link Specification} to build the WHERE clause dynamically so that
     * null parameters are simply omitted — avoids the PostgreSQL prepared-statement
     * type-inference error caused by the {@code ? IS NULL OR column = ?} pattern.
     *
     * @param vehicleId  optional vehicle ID filter
     * @param stationId  optional station ID filter
     * @param startDate  optional start date filter
     * @param endDate    optional end date filter
     * @param pageable   pagination parameters
     * @return paginated {@link TransactionResponse} results
     */
    public Page<TransactionResponse> getTransactionsWithFilters(UUID vehicleId, UUID stationId,
                                                              LocalDateTime startDate, LocalDateTime endDate,
                                                              Pageable pageable) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (vehicleId != null) {
                predicates.add(cb.equal(root.get("vehicle").get("id"), vehicleId));
            }
            if (stationId != null) {
                predicates.add(cb.equal(root.get("station").get("id"), stationId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionTimestamp"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionTimestamp"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "transactionTimestamp"));
        }

        return transactionRepository.findAll(spec, pageable).map(transactionMapper::toResponse);
    }
}

