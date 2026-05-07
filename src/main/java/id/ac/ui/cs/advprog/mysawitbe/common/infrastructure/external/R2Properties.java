package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

/**
 * R2 (Cloudflare S3) storage configuration properties.
 *
 * Per agent.md: External config in infrastructure layer.
 * Binds to app.storage.r2.* properties from application.yml
 */
@ConfigurationProperties(prefix = "app.storage.r2")
@Data
public class R2Properties {
    
    /** R2 endpoint URL (e.g., https://<account-id>.r2.cloudflarestorage.com) */
    private String endpoint;
    
    /** R2 bucket name */
    private String bucket;
    
    /** R2 API token access key */
    private String accessKey;
    
    /** R2 API token secret key */
    private String secretKey;
    
    /** Public CDN URL untuk access files (e.g., https://pub-<hash>.r2.dev) */
    private String publicUrl;
}