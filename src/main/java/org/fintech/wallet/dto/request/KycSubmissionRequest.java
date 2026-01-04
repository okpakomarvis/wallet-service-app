package org.fintech.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.KycLevel;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycSubmissionRequest {

    @NotNull
    private KycLevel level;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Size(max = 50)
    private String idType;

    @NotBlank
    @Size(max = 50)
    private String idNumber;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    @Size(max = 100)
    private String nationality;

    @NotBlank
    @Size(max = 500)
    private String address;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String postalCode;

    @NotBlank
    @Size(max = 50)
    private String country;
}
