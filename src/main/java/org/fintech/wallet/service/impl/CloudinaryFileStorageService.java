package org.fintech.wallet.service.impl;


import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryFileStorageService implements FileStorageService {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024; // 10MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/jpg",
            "image/webp"
    );

    private final Cloudinary cloudinary;

    @Override
    public UploadResult uploadKycFile(UUID userId, String docType, MultipartFile file) {
        validate(file, docType);

        String folder = String.format("finpay/kyc/%s/%s", userId, docType);
        String publicId = String.format("%s/%s_%d_%s",
                folder, docType, System.currentTimeMillis(), UUID.randomUUID().toString().substring(0, 8));

        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", folder);
            options.put("public_id", publicId);
            options.put("resource_type", "auto"); // auto => image or raw (pdf)
            options.put("use_filename", true);
            options.put("unique_filename", true);
            options.put("overwrite", false);
            options.put("tags", List.of("kyc", "finpay", docType, userId.toString()));

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);

            String secureUrl = (String) result.get("secure_url");
            String returnedPublicId = (String) result.get("public_id");
            String resourceType = (String) result.get("resource_type"); // "image" or "raw"

            if (secureUrl == null || returnedPublicId == null || resourceType == null) {
                throw new RuntimeException("Cloudinary upload failed: missing secure_url/public_id/resource_type");
            }

            log.info("Uploaded KYC file: userId={}, docType={}, publicId={}, resourceType={}",
                    userId, docType, returnedPublicId, resourceType);

            return new UploadResult(secureUrl, returnedPublicId, resourceType);

        } catch (Exception e) {
            log.error("Cloudinary upload failed: userId={}, docType={}", userId, docType, e);
            throw new RuntimeException("Failed to upload " + docType + " document", e);
        }
    }
    @Override
    public UploadResult uploadProfileImage(UUID userId, MultipartFile file) {
        validate(file, "profile_image");

        String folder = String.format("finpay/profile/%s", userId);
        String publicId = String.format(
                "%s/avatar_%d_%s",
                folder,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8)
        );

        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", folder);
            options.put("public_id", publicId);
            options.put("resource_type", "image");
            options.put("overwrite", true);
            options.put("tags", List.of("profile", "finpay", userId.toString()));

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);

            return new UploadResult(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"),
                    (String) result.get("resource_type")
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile image", e);
        }
    }

    @Override
    public void deleteByPublicId(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) return;

        String rt = (resourceType == null || resourceType.isBlank()) ? "image" : resourceType;

        try {
            Map<?, ?> res = cloudinary.uploader().destroy(publicId, Map.of("resource_type", rt));
            log.info("Deleted Cloudinary asset: publicId={}, resourceType={}, result={}", publicId, rt, res.get("result"));
        } catch (Exception e) {
            // best-effort cleanup
            log.warn("Failed to delete Cloudinary asset: publicId={}, resourceType={}", publicId, rt, e);
        }
    }

    private void validate(MultipartFile file, String docType) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException(docType + " file is required");
        if (file.getSize() > MAX_FILE_BYTES) throw new IllegalArgumentException(docType + " exceeds max size (10MB)");

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(docType + " has unsupported content type: " + contentType);
        }
    }
}
