package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import org.fintech.wallet.doc.KycPolicyProvider;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.enums.KycLevel;
import org.fintech.wallet.dto.request.KycTierDto;
import org.fintech.wallet.dto.response.UserKycStatusResponse;
import org.fintech.wallet.repository.KycRepository;
import org.fintech.wallet.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycTierService {

    private final KycRepository kycRepository;
    private final UserRepository userRepository;
    private final KycPolicyProvider kycPolicyProvider;

    @Transactional(readOnly = true)
    public UserKycStatusResponse getUserKycStatus(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        KycLevel currentLevel = user.getKycLevel();

        List<KycTierDto> tiers = Arrays.stream(KycLevel.values())
                .map(level -> {

                    boolean completed = level.ordinal() <= currentLevel.ordinal();
                    boolean canUpgrade = level.ordinal() == currentLevel.ordinal() + 1;

                    return KycTierDto.builder()
                            .level(level)
                            .name(level.getDisplayName())
                            .maxBalance(level.getPerTransactionLimit())
                            .maxTransaction(level.getDailyTransactionLimit())
                            .completed(completed)
                            .canUpgrade(canUpgrade)
                            .build();
                })
                .toList();

        return UserKycStatusResponse.builder()
                .currentLevel(currentLevel)
                .tiers(tiers)
                .build();
    }

    /** Public (unauthenticated) endpoint */
    public List<KycTierDto> getAllTiers() {
        return Arrays.stream(KycLevel.values())
                .map(level -> KycTierDto.builder()
                        .level(level)
                        .name(level.getDisplayName())
                        .maxBalance(level.getPerTransactionLimit())
                        .maxTransaction(level.getDailyTransactionLimit())
                        .requiredDocuments(kycPolicyProvider.getRequiredDocuments(level))
                        .completed(false)
                        .canUpgrade(false)
                        .build())
                .toList();
    }
}