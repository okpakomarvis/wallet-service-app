package org.fintech.wallet.repository;

import jakarta.persistence.LockModeType;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.entity.Wallet;
import org.fintech.wallet.domain.enums.WalletStatus;
import org.fintech.wallet.dto.response.WalletResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.fintech.wallet.domain.enums.Currency;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByUser(User user);

    List<Wallet> findByUserId(UUID userId);

    Optional<Wallet> findByWalletNumber(String walletNumber);
    Optional<Wallet> findByWalletNumberAndUserId(String walletNumber, UUID userId);

    Optional<Wallet> findByUserIdAndCurrency(UUID userId, Currency currency);

    boolean existsByWalletNumber(String walletNumber);

    // Pessimistic locking for balance operations to prevent race conditions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :walletId")
    Optional<Wallet> findByIdWithLock(UUID walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletNumber = :walletNumber")
    Optional<Wallet> findByWalletNumberWithLock(String walletNumber);

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND w.currency = :currency AND w.status = :status")
    Optional<Wallet> findActiveWallet(UUID userId, Currency currency, WalletStatus status);

    long countByStatus(WalletStatus status);

    @Query("""
    SELECT w.id
    FROM Wallet w
    WHERE w.user.id = :userId
""")
    List<UUID> findWalletIdsByUserId(@Param("userId") UUID userId);
}
