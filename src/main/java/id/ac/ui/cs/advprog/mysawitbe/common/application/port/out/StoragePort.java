package id.ac.ui.cs.advprog.mysawitbe.common.application.port.out;

public interface StoragePort {
    String uploadFile(byte[] data, String fileName, String contentType);
    void deleteFile(String fileKey);
    String getPublicUrl(String fileKey);
    String getPresignedUrl(String fileKey, String contentType);
}
