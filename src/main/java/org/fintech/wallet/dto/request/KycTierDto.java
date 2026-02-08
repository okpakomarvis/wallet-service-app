package org.fintech.wallet.dto.request;

import lombok.Builder;
import lombok.Data;
import org.fintech.wallet.domain.enums.KycDocumentType;
import org.fintech.wallet.domain.enums.KycLevel;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class KycTierDto {
    private KycLevel level;
    private String name;
    private BigDecimal maxBalance;
    private BigDecimal maxTransaction;
    private List<KycDocumentType> requiredDocuments;
    private boolean completed;
    private boolean canUpgrade;
}
