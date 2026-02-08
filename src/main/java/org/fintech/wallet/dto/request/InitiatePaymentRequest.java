package org.fintech.wallet.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequest {
    private UUID userId;
    private UUID walletId;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private String email;
    private String callbackUrl;
}
