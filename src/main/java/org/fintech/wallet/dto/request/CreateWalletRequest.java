package org.fintech.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.Currency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequest {

    @NotNull(message = "Currency is required")
    private Currency currency;
}
