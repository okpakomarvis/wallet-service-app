package org.fintech.wallet.repository;


import org.fintech.wallet.domain.entity.LedgerEntry;
import org.fintech.wallet.domain.entity.Wallet;
import org.fintech.wallet.domain.enums.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<LedgerEntry> findByWalletOrderByCreatedAtDesc(Wallet wallet, Pageable pageable);

    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    @Query("SELECT l FROM LedgerEntry l WHERE l.wallet.id = :walletId " +
            "AND l.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY l.createdAt DESC")
    List<LedgerEntry> findByWalletAndDateRange(
            UUID walletId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT SUM(l.amount) FROM LedgerEntry l WHERE l.wallet.id = :walletId AND l.entryType = :type")
    java.math.BigDecimal sumAmountByWalletAndType(UUID walletId, EntryType type);
    List<LedgerEntry> findByTransactionReference(String transactionReference);
}
