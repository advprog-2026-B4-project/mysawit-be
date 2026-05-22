package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.mysawitbe.common.application.port.out.StoragePort;
import id.ac.ui.cs.advprog.mysawitbe.common.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class R2StorageAdapter implements StoragePort {

    private final R2Properties properties;

    private S3Client buildClient() {
        validateCredentials();

        log.info("Building R2 S3Client: endpoint={}, bucket={}",
                properties.getEndpoint(), properties.getBucket());

        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                properties.getAccessKey(),
                                properties.getSecretKey()
                        )
                ))
                .region(Region.of("auto"))
                .build();
    }

    private void validateCredentials() {
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
            log.debug("Uploading file to R2: {} ({})", fileName, contentType);

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
            log.info("File uploaded successfully: {}", publicUrl);
            return publicUrl;

        } catch (S3Exception e) {
            log.error("Failed to upload file {} to R2", fileName, e);
            throw new StorageException("File upload failed: " + fileName, e);
        }
    }

    private S3Presigner buildPresigner() {
        validateCredentials();

        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                properties.getAccessKey(),
                                properties.getSecretKey()
                        )
                ))
                .region(Region.of("auto"))
                .build();
    }

    @Override
    public String getPresignedUrl(String fileKey, String contentType) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key cannot be empty");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type cannot be empty");
        }

        try (S3Presigner presigner = buildPresigner()) {
            log.debug("Generating presigned URL for R2: {} ({})", fileKey, contentType);

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(15))
                    .putObjectRequest(PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(fileKey)
                            .contentType(contentType)
                            .build())
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.debug("Presigned URL generated: {}", presignedUrl);
            return presignedUrl;

        } catch (S3Exception e) {
            log.error("Failed to generate presigned URL for {}", fileKey, e);
            throw new StorageException("Presigned URL generation failed: " + fileKey, e);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key cannot be empty");
        }

        try (S3Client s3 = buildClient()) {
            log.debug("Deleting file from R2: {}", fileKey);

            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(fileKey)
                    .build());

            log.info("File deleted successfully: {}", fileKey);

        } catch (S3Exception e) {
            log.error("Failed to delete file {} from R2", fileKey, e);
            throw new StorageException("File deletion failed: " + fileKey, e);
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
