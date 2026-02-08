package org.fintech.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.Currency;
import org.fintech.wallet.domain.enums.TransactionStatus;
import org.fintech.wallet.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReportResponse {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalCount;
    private BigDecimal totalVolume;
    private Map<TransactionType, Long> countByType;
    private Map<TransactionStatus, Long> countByStatus;
    private Map<Currency, BigDecimal> volumeByCurrency;
    private LocalDateTime generatedAt;
}
