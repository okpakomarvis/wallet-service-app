package org.fintech.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetPinRequest {

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "\\d{4}", message = "PIN must be 4 digits")
    private String pin;

    @NotBlank(message = "Confirm PIN is required")
    private String confirmPin;
}