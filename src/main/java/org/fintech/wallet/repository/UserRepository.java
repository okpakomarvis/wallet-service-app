package org.fintech.wallet.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.enums.KycStatus;
import org.fintech.wallet.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    long countByStatus(UserStatus status);
    long countByKycStatus(KycStatus kycStatus);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt > :since")
    long countCreatedSince(@Param("since") LocalDateTime since);

    Page<User> findByStatus(UserStatus status, Pageable pageable);
    Page<User> findByKycStatus(KycStatus kycStatus, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.kycStatus = :kycStatus")
    Page<User> findByStatusAndKycStatus(
            @Param("status") UserStatus status,
            @Param("kycStatus") KycStatus kycStatus,
            Pageable pageable
    );

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchByEmailOrName(@Param("searchTerm") String searchTerm, Pageable pageable);
}
