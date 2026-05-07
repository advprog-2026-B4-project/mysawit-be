package id.ac.ui.cs.advprog.mysawitbe.common.application.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import id.ac.ui.cs.advprog.mysawitbe.common.application.port.in.StorageUseCase;
import id.ac.ui.cs.advprog.mysawitbe.common.application.port.out.StoragePort;
import id.ac.ui.cs.advprog.mysawitbe.common.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorageService implements StorageUseCase {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");

    private final StoragePort storagePort;

    @Override
    public String uploadPhoto(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File tidak boleh kosong.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Hanya file JPG dan PNG yang diizinkan.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Ukuran file maksimal 5MB.");
        }

        try {
            String ext = "image/png".equals(file.getContentType()) ? ".png" : ".jpg";
            String fileKey = "panen/" + UUID.randomUUID() + ext;
            return storagePort.uploadFile(file.getBytes(), fileKey, file.getContentType());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Gagal mengupload file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePhoto(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key tidak valid.");
        }
        storagePort.deleteFile(fileKey);
    }

    @Override
    public PresignedUrlResponse generatePresignedUrl(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type tidak boleh kosong.");
        }
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Hanya file JPG dan PNG yang diizinkan.");
        }

        String ext = "image/png".equals(contentType) ? ".png" : ".jpg";
        String fileKey = "panen/" + UUID.randomUUID() + ext;

        String presignedUrl = storagePort.getPresignedUrl(fileKey, contentType);
        String publicUrl = storagePort.getPublicUrl(fileKey);

        return new PresignedUrlResponse(presignedUrl, publicUrl, fileKey);
    }
}