package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.web;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import id.ac.ui.cs.advprog.mysawitbe.common.application.port.in.StorageUseCase;
import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageUseCase storageUseCase;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        String publicUrl = storageUseCase.uploadPhoto(file);
        return ResponseEntity.ok(ApiResponse.success(publicUrl));
    }
}