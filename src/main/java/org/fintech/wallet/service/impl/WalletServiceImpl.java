package org.fintech.wallet.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.entity.Wallet;
import org.fintech.wallet.domain.enums.WalletStatus;
import org.fintech.wallet.dto.event.WalletEvent;
import org.fintech.wallet.dto.request.CreateWalletRequest;
import org.fintech.wallet.dto.response.WalletResponse;
import org.fintech.wallet.exception.InsufficientBalanceException;
import org.fintech.wallet.exception.WalletAuthorizeException;
import org.fintech.wallet.exception.WalletNotFoundException;
import org.fintech.wallet.kafka.KafkaProducerService;
import org.fintech.wallet.repository.TransactionRepository;
import org.fintech.wallet.repository.UserRepository;
import org.fintech.wallet.repository.WalletRepository;
import org.fintech.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService{

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaProducerService kafkaProducerService;

    private final Random random = new Random();
    @Override
    @Transactional
    public WalletResponse createWallet(UUID userId, CreateWalletRequest request) {
        log.info("Creating wallet for user: {}, currency: {}", userId, request.getCurrency());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if wallet already exists for this currency
        walletRepository.findByUserIdAndCurrency(userId, request.getCurrency())
                .ifPresent(w -> {
                    throw new RuntimeException("Wallet already exists for currency: " + request.getCurrency());
                });

        Wallet wallet = Wallet.builder()
                .user(user)
                .walletNumber(generateWalletNumber())
                .currency(request.getCurrency())
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Wallet created successfully: {}", wallet.getWalletNumber());
        publishWalletEvent(
                wallet,
                user.getId(),
                "CREATED",
                null,
                wallet.getBalance()
        );

        return mapToResponse(wallet);
    }
    @Override
    @Transactional(readOnly = true)
    public List<WalletResponse> getUserWallets(UUID userId) {
        return walletRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByNumber(String walletNumber, UUID userId) {
        Wallet wallet = walletRepository
                .findByWalletNumberAndUserId(walletNumber, userId)
                .orElseThrow(() -> new WalletAuthorizeException("Unauthorized wallet access"));
        return mapToResponse(wallet);
    }
    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByNumberOnly(String walletNumber) {
        Wallet wallet = walletRepository
                .findByWalletNumber(walletNumber)
                .orElseThrow(() -> new WalletAuthorizeException("Destination Wallet Not found"));
        return mapToResponse(wallet);
    }
    @Override
    @Transactional(readOnly = true)
    public Wallet getWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
    }
    @Override
    @Transactional
    public Wallet getWalletByIdWithLock(UUID walletId) {
        return walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
    }
    @Override
    @Transactional
    public void updateBalance(UUID walletId, BigDecimal amount, boolean isCredit) {
        Wallet wallet = getWalletByIdWithLock(walletId);
        BigDecimal oldBalance = wallet.getAvailableBalance();

        if (isCredit) {
            wallet.setBalance(wallet.getBalance().add(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        } else {
            if (wallet.getAvailableBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance");
            }
            wallet.setBalance(wallet.getBalance().subtract(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        }
        publishWalletEvent(
                wallet,
                wallet.getUser().getId(),
                "BALANCE_UPDATED",
                oldBalance,
                wallet.getAvailableBalance()
        );

        // Optional LOW_BALANCE alert
        if (wallet.getAvailableBalance().compareTo(new BigDecimal("1000")) < 0) {
            publishWalletEvent(
                    wallet,
                    wallet.getUser().getId(),
                    "LOW_BALANCE",
                    wallet.getAvailableBalance(),
                    wallet.getAvailableBalance()
            );
        }

        walletRepository.save(wallet);
    }
    @Override
    public BigDecimal getUserDailyTotal(UUID userId) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        BigDecimal total = transactionRepository.sumUserTransactionsByDateRange(
                userId,
                startOfDay,
                endOfDay
        );

        return total != null ? total : BigDecimal.ZERO;
    }
    @Override
    @Transactional
    public void freezeWallet(UUID walletId) {
        Wallet wallet = getWalletById(walletId);
        wallet.setStatus(WalletStatus.FROZEN);
        walletRepository.save(wallet);
        log.info("Wallet frozen: {}", wallet.getWalletNumber());
        publishWalletEvent(
                wallet,
                wallet.getUser().getId(),
                "FROZEN",
                wallet.getAvailableBalance(),
                wallet.getAvailableBalance()
        );
    }
    @Override
    @Transactional
    public void unfreezeWallet(UUID walletId) {
        Wallet wallet = getWalletById(walletId);
        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);
        log.info("Wallet unfrozen: {}", wallet.getWalletNumber());
        publishWalletEvent(
                wallet,
                wallet.getUser().getId(),
                "UNFROZEN",
                wallet.getAvailableBalance(),
                wallet.getAvailableBalance()
        );
    }

    private String generateWalletNumber() {
        String walletNumber;
        do {
            walletNumber = String.format("WLT%010d", random.nextInt(1000000000));
        } while (walletRepository.existsByWalletNumber(walletNumber));
        return walletNumber;
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .walletNumber(wallet.getWalletNumber())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .availableBalance(wallet.getAvailableBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
    private void publishWalletEvent(
            Wallet wallet,
            UUID userId,
            String action,
            BigDecimal oldBalance,
            BigDecimal newBalance
    ) {
        try {
            WalletEvent event = WalletEvent.builder()
                    .walletId(wallet.getId())
                    .userId(userId)
                    .currency(wallet.getCurrency().name())
                    .action(action)
                    .oldBalance(oldBalance)
                    .newBalance(newBalance)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducerService.publishWalletEvent(event);

        } catch (Exception e) {
            log.error("Failed to publish wallet event: walletId={}, action={}",
                    wallet.getId(), action, e);
        }
    }

}