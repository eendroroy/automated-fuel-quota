package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.RegistrationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationCodeRepository extends JpaRepository<RegistrationCode, UUID> {
    Optional<RegistrationCode> findByCode(String code);
    boolean existsByCode(String code);
}

