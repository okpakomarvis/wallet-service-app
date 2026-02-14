package org.fintech.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminKycApprovalRequest {


    @Size(max = 50)
    private String idType;


    @Size(max = 50)
    private String idNumber;


    private LocalDate dateOfBirth;

    @Size(max = 100)
    private String nationality;


    @Size(max = 500)
    private String address;


    @Size(max = 50)
    private String country;
}
