package id.ac.ui.cs.advprog.mysawitbe.common.dto;

public record PresignedUrlResponse(
    String presignedUrl,
    String publicUrl,
    String fileKey
) {}