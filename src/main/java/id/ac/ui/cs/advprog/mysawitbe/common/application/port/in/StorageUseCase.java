package id.ac.ui.cs.advprog.mysawitbe.common.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.PresignedUrlResponse;

public interface StorageUseCase {

    void deletePhoto(String fileKey);

    PresignedUrlResponse generatePresignedUrl(String contentType);
}