package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.common.application.port.in.StorageUseCase;
import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.common.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageUseCase storageUseCase;
    private final UserQueryUseCase userQueryUseCase;

    @GetMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam("contentType") String contentType,
            @RequestAttribute("userId") UUID userId) {

        if (userId == null || !userQueryUseCase.verifyUserExists(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be logged in");
        }

        UUID mandorId = userQueryUseCase.getMandorIdByBuruhId(userId);
        if (mandorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buruh belum memiliki Mandor yang ditugaskan");
        }

        PresignedUrlResponse response = storageUseCase.generatePresignedUrl(contentType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get an upload token for direct R2 upload.
     * Frontend calls GET /api/storage/upload-token, then uploads directly to R2 using presigned URL.
     */
    @GetMapping("/upload-token")
    public ResponseEntity<ApiResponse<String>> getUploadToken(
            @RequestParam("contentType") String contentType,
            @RequestAttribute("userId") UUID userId) {

        if (userId == null || !userQueryUseCase.verifyUserExists(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be logged in");
        }

        UUID mandorId = userQueryUseCase.getMandorIdByBuruhId(userId);
        if (mandorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buruh belum memiliki Mandor yang ditugaskan");
        }

        PresignedUrlResponse presigned = storageUseCase.generatePresignedUrl(contentType);
        return ResponseEntity.ok(ApiResponse.success(presigned.presignedUrl() + "|" + presigned.publicUrl()));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestAttribute("userId") UUID userId) {

        if (userId == null || !userQueryUseCase.verifyUserExists(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be logged in");
        }

        UUID mandorId = userQueryUseCase.getMandorIdByBuruhId(userId);
        if (mandorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buruh belum memiliki Mandor yang ditugaskan");
        }

        String result = storageUseCase.uploadPhoto(file);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/file")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestParam("fileKey") String fileKey,
            @RequestAttribute("userId") UUID userId) {

        if (userId == null || !userQueryUseCase.verifyUserExists(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be logged in");
        }

        UUID mandorId = userQueryUseCase.getMandorIdByBuruhId(userId);
        if (mandorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buruh belum memiliki Mandor yang ditugaskan");
        }

        storageUseCase.deletePhoto(fileKey);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}