package org.fintech.wallet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fintech.wallet.dto.request.KycSubmissionRequest;
import org.fintech.wallet.dto.request.KycTierDto;
import org.fintech.wallet.dto.response.ApiResponse;
import org.fintech.wallet.dto.response.KycResponse;
import org.fintech.wallet.dto.response.UserKycStatusResponse;
import org.fintech.wallet.security.CurrentUser;
import org.fintech.wallet.service.KycService;
import org.fintech.wallet.service.impl.KycTierService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC", description = "Know Your Customer (KYC) APIs")
@SecurityRequirement(name = "bearerAuth")
public class KycController {

    private final KycService kycService;
    private final KycTierService kycTierService;

    /**
     * Submit KYC with documents.
     * - Documents are uploaded to Cloudinary inside KycService (storage concern stays out of controller)
     * - Controller stays thin: HTTP mapping only
     */
    @Operation(
            summary = "Submit KYC",
            description = "Submit KYC information and required documents for verification"
    )
    @PostMapping(
            value = "",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<KycResponse>> submitKyc(
            @Parameter(hidden = true) @CurrentUser UUID userId,

            // Files
            @Parameter(
                    description = "Government issued ID document",
                    content = @Content(schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart("idDocument") MultipartFile idDocument,

            @Parameter(
                    description = "Proof of address document",
                    content = @Content(schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart("proofOfAddress") MultipartFile proofOfAddress,

            @Parameter(
                    description = "User selfie image",
                    content = @Content(schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart("selfie") MultipartFile selfie
    ) {
        KycResponse kyc = kycService.submitKyc(userId, idDocument, proofOfAddress, selfie);
        return ResponseEntity.ok(ApiResponse.success("KYC submitted for review", kyc));
    }

    @Operation(
            summary = "Get my KYC",
            description = "Retrieve the authenticated user's KYC details"
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<KycResponse>> getMyKyc(
            @Parameter(hidden = true) @CurrentUser UUID userId
    ) {
        KycResponse kyc = kycService.getUserKyc(userId);
        return ResponseEntity.ok(ApiResponse.success(kyc));
    }
    /** For landing pages / signup flow */
    @Operation(
            summary = "Get my KYC Documents requirements",
            description = "Retrieve the list of KYC tiers and their document requirements"
    )
    @GetMapping(value = "/tiers",produces = MediaType.APPLICATION_JSON_VALUE)
    public List<KycTierDto> getAllKycTiers() {
        return kycTierService.getAllTiers();
    }

    /** For logged-in user dashboard */
    @Operation(
            summary = "Get my KYC levels",
            description = "Retrieve the authenticated user's KYC details and levels"
    )
    @GetMapping(value = "/me",produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public UserKycStatusResponse getMyKycStatus(
            @Parameter(hidden = true) @CurrentUser UUID userId
    ) {
        return kycTierService.getUserKycStatus(userId);
    }
}