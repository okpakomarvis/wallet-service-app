package org.fintech.wallet.dto.response;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private boolean success;
    private String reference;
    private String authorizationUrl;
    private String accessCode;
}

