package org.fintech.wallet.dto.response;

import lombok.Builder;
import lombok.Data;
import org.fintech.wallet.domain.enums.KycLevel;
import org.fintech.wallet.dto.request.KycTierDto;

import java.util.List;

@Data
@Builder
public class UserKycStatusResponse {
    private KycLevel currentLevel;
    private List<KycTierDto> tiers;
}
