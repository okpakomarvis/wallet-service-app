package org.fintech.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private RateLimit rateLimit = new RateLimit();
    private Transaction transaction = new Transaction();
    private Kyc kyc = new Kyc();

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 60;
    }

    @Data
    public static class Transaction {
        private BigDecimal maxAmount = new BigDecimal("1000000");
        private BigDecimal minAmount = new BigDecimal("100");
    }

    @Data
    public static class Kyc {
        private boolean requiredForWithdrawal = true;
        private BigDecimal maxUnverifiedBalance = new BigDecimal("50000");
    }
}
