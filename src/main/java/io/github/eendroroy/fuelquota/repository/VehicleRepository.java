package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID>,
        JpaSpecificationExecutor<Vehicle> {

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    Optional<Vehicle> findByOwnerNid(String ownerNid);

    Optional<Vehicle> findByOwnerEmail(String ownerEmail);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByOwnerNid(String ownerNid);

    boolean existsByOwnerEmail(String ownerEmail);

    @Query("SELECT v FROM Vehicle v WHERE v.user.id = :userId ORDER BY v.createdAt DESC")
    List<Vehicle> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT v FROM Vehicle v WHERE v.user.id = :userId")
    Page<Vehicle> findPageByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.status = :status")
    long countByStatus(@Param("status") Vehicle.VehicleStatus status);

    @Query("SELECT v FROM Vehicle v WHERE v.status = :status")
    Page<Vehicle> findByStatus(@Param("status") Vehicle.VehicleStatus status, Pageable pageable);


    @Query("SELECT v FROM Vehicle v WHERE v.vehicleClass = :vehicleClass")
    java.util.List<Vehicle> findByVehicleClass(@Param("vehicleClass") String vehicleClass);

    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId ORDER BY v.createdAt DESC")
    List<Vehicle> findByDriverId(@Param("driverId") UUID driverId);
}
