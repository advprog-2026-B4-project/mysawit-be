package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.mysawitbe.common.application.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * R2 (Cloudflare S3) storage adapter.
 * 
 * Per agent.md: Infrastructure adapter layer.
 * Implements StoragePort (application port).
 * Only handles technical concerns (S3 API calls).
 */
@Component
@RequiredArgsConstructor
public class R2StorageAdapter implements StoragePort {

    private final R2Properties properties;

    /**
     * Build S3Client configured untuk R2.
     * 
     * @return S3Client
     * @throws IllegalStateException jika credentials tidak valid
     */
    private S3Client buildClient() {
        // ─── Validate credentials ───────────────────────────────────
        if (properties.getAccessKey() == null || properties.getAccessKey().isBlank()) {
            throw new IllegalStateException(
                    "R2_ACCESS_KEY not configured. Set env var atau app.storage.r2.accessKey");
        }
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            throw new IllegalStateException(
                    "R2_SECRET_KEY not configured. Set env var atau app.storage.r2.secretKey");
        }
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            throw new IllegalStateException(
                    "R2_ENDPOINT not configured. Set env var atau app.storage.r2.endpoint");
        }
        if (properties.getBucket() == null || properties.getBucket().isBlank()) {
            throw new IllegalStateException(
                    "R2_BUCKET not configured. Set env var atau app.storage.r2.bucket");
        }

        System.out.println("Building R2 S3Client: endpoint=" + properties.getEndpoint() 
                + ", bucket=" + properties.getBucket());

        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                properties.getAccessKey(), 
                                properties.getSecretKey()
                        )
                ))
                // R2 pakai region "auto" — tidak ada region nyata
                .region(Region.of("auto"))
                .build();
    }

    @Override
    public String uploadFile(byte[] data, String fileName, String contentType) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("File data cannot be empty");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        try (S3Client s3 = buildClient()) {
            System.out.println("Uploading file to R2: " + fileName + " (" + contentType + ")");

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(fileName)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build();

            s3.putObject(request, RequestBody.fromInputStream(
                    new ByteArrayInputStream(data), 
                    data.length
            ));

            String publicUrl = getPublicUrl(fileName);
            System.out.println("File uploaded successfully: " + publicUrl);
            return publicUrl;

        } catch (Exception e) {
            System.err.println("Failed to upload file " + fileName + " to R2: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key cannot be empty");
        }

        try (S3Client s3 = buildClient()) {
            System.out.println("Deleting file from R2: " + fileKey);

            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(fileKey)
                    .build());

            System.out.println("File deleted successfully: " + fileKey);

        } catch (Exception e) {
            System.err.println("Failed to delete file " + fileKey + " from R2: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("File deletion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPublicUrl(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key cannot be empty");
        }
        return properties.getPublicUrl().replaceAll("/$", "") + "/" + fileKey;
    }
}