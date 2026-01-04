package org.fintech.wallet.repository;

import org.fintech.wallet.domain.entity.Transaction;
import org.fintech.wallet.domain.enums.TransactionStatus;
import org.fintech.wallet.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReference(String reference);

    Optional<Transaction> findByExternalReference(String externalReference);

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.sourceWallet.id = :walletId OR t.destinationWallet.id = :walletId) " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.sourceWallet.user.id = :userId OR t.destinationWallet.user.id = :userId) " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    List<Transaction> findByStatusAndCreatedAtBefore(
            TransactionStatus status,
            LocalDateTime dateTime
    );

    @Query("SELECT t FROM Transaction t WHERE t.status = :status " +
            "AND t.type = :type AND t.createdAt > :since")
    List<Transaction> findRecentByStatusAndType(
            TransactionStatus status,
            TransactionType type,
            LocalDateTime since
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE " +
            "t.sourceWallet.user.id = :userId AND t.createdAt > :since")
    long countUserTransactionsSince(UUID userId, LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt > :since")
    long countTransactionsSince(LocalDateTime since);

    long countByStatus(TransactionStatus status);
    long countByType(TransactionType type);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
            "t.status = 'SUCCESS' AND t.createdAt > :since")
    BigDecimal sumTransactionAmountSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE " +
            "(t.sourceWallet.user.id = :userId OR t.destinationWallet.user.id = :userId)")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
            "(t.sourceWallet.user.id = :userId OR t.destinationWallet.user.id = :userId) " +
            "AND t.status = 'SUCCESS'")
    BigDecimal sumTransactionVolumeByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE " +
            "(t.sourceWallet.user.id = :userId OR t.destinationWallet.user.id = :userId) " +
            "AND t.createdAt > :since")
    long countByUserIdSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.sourceWallet.user.id = :userId OR t.destinationWallet.user.id = :userId) " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findTop10ByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    long countByUserIdAndStatus(UUID userId, TransactionStatus status);

    Page<Transaction> findByReferenceContaining(String reference, Pageable pageable);
    Page<Transaction> findByStatusAndType(TransactionStatus status, TransactionType type, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.sourceWallet.user.id = :userId OR t.destinationWallet.user.id = :userId) " +
            "AND t.status = :status " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByUserIdAndStatusAndDateRange(
            @Param("userId") UUID userId,
            @Param("status") TransactionStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
