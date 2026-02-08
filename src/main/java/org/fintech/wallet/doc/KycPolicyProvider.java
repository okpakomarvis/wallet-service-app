package org.fintech.wallet.doc;

import org.fintech.wallet.domain.enums.KycDocumentType;
import org.fintech.wallet.domain.enums.KycLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KycPolicyProvider {

    private static final Map<KycLevel, List<KycDocumentType>> POLICY = Map.of(
            KycLevel.TIER_1, List.of(
                    KycDocumentType.NATIONAL_ID,
                    KycDocumentType.SELFIE
            ),
            KycLevel.TIER_2, List.of(
                    KycDocumentType.PASSPORT,
                    KycDocumentType.PROOF_OF_ADDRESS,
                    KycDocumentType.SELFIE
            ),
            KycLevel.TIER_3, List.of(
                    KycDocumentType.PASSPORT,
                    KycDocumentType.PROOF_OF_ADDRESS,
                    KycDocumentType.SELFIE
            )
    );

    public List<KycDocumentType> getRequiredDocuments(KycLevel level) {
        return POLICY.getOrDefault(level, List.of());
    }
}