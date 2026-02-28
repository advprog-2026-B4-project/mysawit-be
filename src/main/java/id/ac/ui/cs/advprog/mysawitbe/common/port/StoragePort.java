package id.ac.ui.cs.advprog.mysawitbe.common.port;

public interface StoragePort {
    String uploadFile(byte[] data, String fileName, String contentType);
    void deleteFile(String fileKey);
    String getPublicUrl(String fileKey);
}
