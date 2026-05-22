package id.ac.ui.cs.advprog.mysawitbe.common.exception;

/**
 * Thrown when a storage operation (upload, download, delete, presigned URL) fails.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
