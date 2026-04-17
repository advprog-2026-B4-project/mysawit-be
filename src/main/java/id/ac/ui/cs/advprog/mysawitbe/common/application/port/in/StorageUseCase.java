package id.ac.ui.cs.advprog.mysawitbe.common.application.port.in;

import org.springframework.web.multipart.MultipartFile;

public interface StorageUseCase {

    String uploadPhoto(MultipartFile file);
}