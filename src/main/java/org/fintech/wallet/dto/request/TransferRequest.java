package org.fintech.wallet.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotNull(message = "Source wallet is required")
    private UUID sourceWalletId;

    @NotBlank(message = "Destination wallet number is required")
    @Size(min = 10, max = 20)
    private String destinationWalletNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000.00", message = "Amount exceeds maximum limit")
    private BigDecimal amount;

    @Size(max = 500, message = "Description too long")
    private String description;

    @NotBlank(message = "Transaction PIN is required")
    @Size(min = 4, max = 4, message = "PIN must be 4 digits")
    private String pin;

    private String ipAddress;
}