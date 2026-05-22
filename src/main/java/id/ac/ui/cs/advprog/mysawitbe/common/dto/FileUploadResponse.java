package id.ac.ui.cs.advprog.mysawitbe.common.dto;

public record FileUploadResponse(
    String fileKey,
    String publicUrl,
    String fileName,
    long fileSize
) {}
