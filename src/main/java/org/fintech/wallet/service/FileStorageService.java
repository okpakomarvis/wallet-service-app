package org.fintech.wallet.service;


import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileStorageService {

    UploadResult uploadKycFile(UUID userId, String docType, MultipartFile file);

    void deleteByPublicId(String publicId, String resourceType);

    UploadResult uploadProfileImage(UUID userId, MultipartFile file);

    record UploadResult(String secureUrl, String publicId, String resourceType) {}
}
