package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.BrtaOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrtaOfficeRepository extends JpaRepository<BrtaOffice, UUID> {
    Optional<BrtaOffice> findByBrtaCode(String brtaCode);
    boolean existsByBrtaCode(String brtaCode);
}

