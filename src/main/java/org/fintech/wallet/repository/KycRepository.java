package org.fintech.wallet.repository;

import org.fintech.wallet.domain.entity.KycVerification;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.enums.KycLevel;
import org.fintech.wallet.domain.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycRepository extends JpaRepository<KycVerification, UUID> {

    Optional<KycVerification> findByUser(User user);

    Optional<KycVerification> findByUserId(UUID userId);

    Page<KycVerification> findByStatus(KycStatus status, Pageable pageable);

    boolean existsByUserId(UUID userId);
    Optional<KycVerification> findByUserIdAndLevel(UUID userId, KycLevel level);


    boolean existsByUserIdAndLevel(UUID userId, KycLevel level);

    @Query("""
        SELECT k.level FROM KycVerification k
        WHERE k.user.id = :userId
        AND k.status = 'VERIFIED'
        ORDER BY k.level DESC
        LIMIT 1
        """)
    Optional<KycLevel> findHighestVerifiedLevel(UUID userId);


}