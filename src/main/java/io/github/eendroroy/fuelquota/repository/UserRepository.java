package io.github.eendroroy.fuelquota.repository;

import io.github.eendroroy.fuelquota.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") User.UserRole role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = 'ACTIVE'")
    List<User> findActiveUsersByRole(@Param("role") User.UserRole role);

    /**
     * Paginated search across CUSTOMER and ADMIN users with optional role, status
     * and free-text filters. PUMP_REPRESENTATIVE accounts are always excluded.
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.role IN ('CUSTOMER', 'ADMIN')
          AND (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
          AND (:search IS NULL OR :search = ''
               OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(u.mobileNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY u.createdAt DESC
        """)
    Page<User> searchUsers(@Param("role") User.UserRole role,
                           @Param("status") User.UserStatus status,
                           @Param("search") String search,
                           Pageable pageable);

    /**
     * Count of vehicles owned by a user — used for the admin user-management view.
     */
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.user.id = :userId")
    long countVehiclesByUserId(@Param("userId") UUID userId);
}
