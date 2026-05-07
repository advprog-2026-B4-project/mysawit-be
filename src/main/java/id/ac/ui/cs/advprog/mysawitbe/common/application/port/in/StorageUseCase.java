package id.ac.ui.cs.advprog.mysawitbe.common.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.PresignedUrlResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StorageUseCase {

    String uploadPhoto(MultipartFile file);

    void deletePhoto(String fileKey);

    PresignedUrlResponse generatePresignedUrl(String contentType);
}