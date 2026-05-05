package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.web;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external.JwtService;
import id.ac.ui.cs.advprog.mysawitbe.common.application.port.in.StorageUseCase;
import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageUseCase storageUseCase;
    private final JwtService jwtService;
    private final id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase userQueryUseCase;

    @GetMapping("/upload-token")
    public ResponseEntity<ApiResponse<String>> getUploadToken(@RequestAttribute(value = "userId", required = false) UUID userId) {
        if (userId == null || !userQueryUseCase.verifyUserExists(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be logged in and valid to request an upload token");
        }

        UUID mandorId = userQueryUseCase.getMandorIdByBuruhId(userId);
        if (mandorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buruh belum memiliki Mandor yang ditugaskan");
        }

        String token = jwtService.generateUploadToken(userId.toString());
        return ResponseEntity.ok(ApiResponse.success("Token generated", token));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestHeader(value = "X-Upload-Token", required = false) String uploadToken,
            @RequestParam("file") MultipartFile file) {

        if (uploadToken == null || !jwtService.isValidUploadToken(uploadToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing upload token");
        }

        String publicUrl = storageUseCase.uploadPhoto(file);
        return ResponseEntity.ok(ApiResponse.success(publicUrl));
    }
}