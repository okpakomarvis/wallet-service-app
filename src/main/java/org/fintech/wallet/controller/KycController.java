package org.fintech.wallet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fintech.wallet.dto.request.KycSubmissionRequest;
import org.fintech.wallet.dto.response.ApiResponse;
import org.fintech.wallet.dto.response.KycResponse;
import org.fintech.wallet.security.CurrentUser;
import org.fintech.wallet.service.KycService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC", description = "Know Your Customer (KYC) APIs")
@SecurityRequirement(name = "bearerAuth")
public class KycController {

    private final KycService kycService;

    @Operation(
            summary = "Submit KYC",
            description = "Submit KYC information and required documents for verification"
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<KycResponse>> submitKyc(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @ModelAttribute KycSubmissionRequest request,
            @Parameter(
                    description = "Government issued ID document",
                    content = @Content(schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("idDocument") MultipartFile idDocument,
            @Parameter(
                    description = "Proof of address document",
                    content = @Content(schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("proofOfAddress") MultipartFile proofOfAddress,
            @Parameter(
                    description = "User selfie image",
                    content = @Content(schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("selfie") MultipartFile selfie) {

        KycResponse kyc = kycService.submitKyc(
                userId, request, idDocument, proofOfAddress, selfie
        );

        return ResponseEntity.ok(
                ApiResponse.success("KYC submitted for review", kyc)
        );
    }

    @Operation(
            summary = "Get my KYC",
            description = "Retrieve the authenticated user's KYC details"
    )
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<KycResponse>> getMyKyc(
            @Parameter(hidden = true) @CurrentUser UUID userId) {

        KycResponse kyc = kycService.getUserKyc(userId);
        return ResponseEntity.ok(ApiResponse.success(kyc));
    }
}