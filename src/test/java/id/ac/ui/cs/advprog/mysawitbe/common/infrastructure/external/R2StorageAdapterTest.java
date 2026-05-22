package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

import id.ac.ui.cs.advprog.mysawitbe.common.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class R2StorageAdapterTest {

    private R2Properties properties;
    private R2StorageAdapter adapter;
    private S3Client mockS3Client;
    private S3ClientBuilder mockS3Builder;
    private S3Presigner mockPresigner;
    private Builder mockPresignerBuilder;

    private static final String ENDPOINT = "https://abc123.r2.cloudflarestorage.com";
    private static final String BUCKET = "mysawit-bucket";
    private static final String ACCESS_KEY = "test-access-key";
    private static final String SECRET_KEY = "test-secret-key";
    private static final String PUBLIC_URL = "https://pub-abc123.r2.dev";

    @BeforeEach
    void setUp() {
        properties = new R2Properties();
        properties.setEndpoint(ENDPOINT);
        properties.setBucket(BUCKET);
        properties.setAccessKey(ACCESS_KEY);
        properties.setSecretKey(SECRET_KEY);
        properties.setPublicUrl(PUBLIC_URL);

        adapter = new R2StorageAdapter(properties);

        mockS3Client = mock(S3Client.class);
        mockS3Builder = mock(S3ClientBuilder.class);
        when(mockS3Builder.endpointOverride(any(URI.class))).thenReturn(mockS3Builder);
        when(mockS3Builder.credentialsProvider(any())).thenReturn(mockS3Builder);
        when(mockS3Builder.region(any(Region.class))).thenReturn(mockS3Builder);
        when(mockS3Builder.build()).thenReturn(mockS3Client);

        mockPresigner = mock(S3Presigner.class);
        mockPresignerBuilder = mock(Builder.class);
        when(mockPresignerBuilder.endpointOverride(any(URI.class))).thenReturn(mockPresignerBuilder);
        when(mockPresignerBuilder.credentialsProvider(any())).thenReturn(mockPresignerBuilder);
        when(mockPresignerBuilder.region(any(Region.class))).thenReturn(mockPresignerBuilder);
        when(mockPresignerBuilder.build()).thenReturn(mockPresigner);
    }

    @Nested
    class ValidateCredentials {

        @Test
        void validateCredentials_nullAccessKey_throwsRuntimeException() {
            properties.setAccessKey(null);
            adapter = new R2StorageAdapter(properties);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> adapter.uploadFile("test".getBytes(), "file.jpg", "image/jpeg"));

            assertTrue(ex.getMessage().contains("R2_ACCESS_KEY"));
        }

        @Test
        void validateCredentials_blankAccessKey_throwsRuntimeException() {
            properties.setAccessKey("   ");
            adapter = new R2StorageAdapter(properties);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> adapter.uploadFile("test".getBytes(), "file.jpg", "image/jpeg"));

            assertTrue(ex.getMessage().contains("R2_ACCESS_KEY"));
        }

        @Test
        void validateCredentials_nullSecretKey_throwsRuntimeException() {
            properties.setSecretKey(null);
            adapter = new R2StorageAdapter(properties);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> adapter.uploadFile("test".getBytes(), "file.jpg", "image/jpeg"));

            assertTrue(ex.getMessage().contains("R2_SECRET_KEY"));
        }

        @Test
        void validateCredentials_nullEndpoint_throwsRuntimeException() {
            properties.setEndpoint(null);
            adapter = new R2StorageAdapter(properties);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> adapter.uploadFile("test".getBytes(), "file.jpg", "image/jpeg"));

            assertTrue(ex.getMessage().contains("R2_ENDPOINT"));
        }

        @Test
        void validateCredentials_nullBucket_throwsRuntimeException() {
            properties.setBucket(null);
            adapter = new R2StorageAdapter(properties);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> adapter.uploadFile("test".getBytes(), "file.jpg", "image/jpeg"));

            assertTrue(ex.getMessage().contains("R2_BUCKET"));
        }

        @Test
        void validateCredentials_blankSecretKey_throwsRuntimeException() {
            properties.setSecretKey("");
            adapter = new R2StorageAdapter(properties);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> adapter.uploadFile("test".getBytes(), "file.jpg", "image/jpeg"));

            assertTrue(ex.getMessage().contains("R2_SECRET_KEY"));
        }
    }

    @Nested
    class UploadFile {

        @Test
        void uploadFile_nullData_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.uploadFile(null, "file.jpg", "image/jpeg"));
        }

        @Test
        void uploadFile_emptyData_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.uploadFile(new byte[0], "file.jpg", "image/jpeg"));
        }

        @Test
        void uploadFile_nullFileName_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.uploadFile("test".getBytes(), null, "image/jpeg"));
        }

        @Test
        void uploadFile_blankFileName_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.uploadFile("test".getBytes(), "", "image/jpeg"));
        }

        @Test
        void uploadFile_whitespaceFileName_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.uploadFile("test".getBytes(), "   ", "image/jpeg"));
        }

        @Test
        void uploadFile_validData_returnsPublicUrl() {
            byte[] data = "hello world".getBytes();

            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);

                String result = adapter.uploadFile(data, "panen/test.jpg", "image/jpeg");

                verify(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
                assertEquals(PUBLIC_URL + "/" + "panen/test.jpg", result);
            }
        }

        @Test
        void uploadFile_validData_closesS3Client() {
            byte[] data = "hello world".getBytes();

            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);

                adapter.uploadFile(data, "panen/test.jpg", "image/jpeg");

                verify(mockS3Client).close();
            }
        }

        @Test
        void uploadFile_s3ThrowsException_wrapsInRuntimeException() {
            byte[] data = "hello world".getBytes();

            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);
                doThrow(S3Exception.builder().message("S3 error").build())
                        .when(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

                StorageException ex = assertThrows(StorageException.class,
                        () -> adapter.uploadFile(data, "panen/test.jpg", "image/jpeg"));

                assertTrue(ex.getMessage().contains("File upload failed"));
            }
        }

        @Test
        void uploadFile_buildsS3ClientWithCorrectEndpoint() {
            byte[] data = "hello world".getBytes();

            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);

                adapter.uploadFile(data, "panen/test.jpg", "image/jpeg");

                verify(mockS3Builder).endpointOverride(URI.create(ENDPOINT));
            }
        }

        @Test
        void uploadFile_buildsS3ClientWithRegionAuto() {
            byte[] data = "hello world".getBytes();

            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);

                adapter.uploadFile(data, "panen/test.jpg", "image/jpeg");

                verify(mockS3Builder).region(Region.of("auto"));
            }
        }

        @Test
        void uploadFile_publicUrl_trailingSlashStrippedFromBase() {
            properties.setPublicUrl(PUBLIC_URL + "/");
            adapter = new R2StorageAdapter(properties);
            byte[] data = "hello world".getBytes();

            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);

                String result = adapter.uploadFile(data, "panen/test.jpg", "image/jpeg");

                assertEquals(PUBLIC_URL + "/" + "panen/test.jpg", result);
            }
        }
    }

    @Nested
    class GetPresignedUrl {

        @Test
        void getPresignedUrl_nullFileKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.getPresignedUrl(null, "image/jpeg"));
        }

        @Test
        void getPresignedUrl_blankFileKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.getPresignedUrl("", "image/jpeg"));
        }

        @Test
        void getPresignedUrl_nullContentType_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.getPresignedUrl("panen/file.jpg", null));
        }

        @Test
        void getPresignedUrl_blankContentType_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.getPresignedUrl("panen/file.jpg", ""));
        }

        @Test
        void getPresignedUrl_whitespaceFileKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.getPresignedUrl("   ", "image/jpeg"));
        }

        @Test
        void getPresignedUrl_whitespaceContentType_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.getPresignedUrl("panen/file.jpg", "   "));
        }

        @Test
        void getPresignedUrl_validRequest_returnsPresignedUrl() {
            String expectedUrl = "https://r2.example.com/upload/signed?token=abc";
            PresignedPutObjectRequest mockPresignedReq = mock(PresignedPutObjectRequest.class);
            when(mockPresignedReq.url()).thenReturn(url(expectedUrl));

            try (MockedStatic<S3Presigner> presignerStatic = mockStatic(S3Presigner.class);
                 MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                presignerStatic.when(S3Presigner::builder).thenReturn(mockPresignerBuilder);
                when(mockPresigner.presignPutObject(any(PutObjectPresignRequest.class)))
                        .thenReturn(mockPresignedReq);

                String result = adapter.getPresignedUrl("panen/file.jpg", "image/jpeg");

                assertEquals(expectedUrl, result);
                verify(mockPresigner).close();
            }
        }

        @Test
        void getPresignedUrl_buildsPresignerWithCorrectEndpoint() {
            PresignedPutObjectRequest mockPresignedReq = mock(PresignedPutObjectRequest.class);
            when(mockPresignedReq.url()).thenReturn(url("https://example.com"));

            try (MockedStatic<S3Presigner> presignerStatic = mockStatic(S3Presigner.class);
                 MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                presignerStatic.when(S3Presigner::builder).thenReturn(mockPresignerBuilder);
                when(mockPresigner.presignPutObject(any(PutObjectPresignRequest.class)))
                        .thenReturn(mockPresignedReq);

                adapter.getPresignedUrl("panen/file.jpg", "image/jpeg");

                verify(mockPresignerBuilder).endpointOverride(URI.create(ENDPOINT));
            }
        }

        @Test
        void getPresignedUrl_s3ThrowsException_wrapsInRuntimeException() {
            try (MockedStatic<S3Presigner> presignerStatic = mockStatic(S3Presigner.class);
                 MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                presignerStatic.when(S3Presigner::builder).thenReturn(mockPresignerBuilder);
                doThrow(S3Exception.builder().message("S3 presign error").build())
                        .when(mockPresigner).presignPutObject(any(PutObjectPresignRequest.class));

                StorageException ex = assertThrows(StorageException.class,
                        () -> adapter.getPresignedUrl("panen/file.jpg", "image/jpeg"));

                assertTrue(ex.getMessage().contains("Presigned URL generation failed"));
            }
        }

        @Test
        void getPresignedUrl_uses15MinuteSignatureDuration() {
            PresignedPutObjectRequest mockPresignedReq = mock(PresignedPutObjectRequest.class);
            when(mockPresignedReq.url()).thenReturn(url("https://example.com"));

            try (MockedStatic<S3Presigner> presignerStatic = mockStatic(S3Presigner.class);
                 MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                presignerStatic.when(S3Presigner::builder).thenReturn(mockPresignerBuilder);
                ArgumentCaptor<PutObjectPresignRequest> captor =
                        ArgumentCaptor.forClass(PutObjectPresignRequest.class);
                when(mockPresigner.presignPutObject(captor.capture()))
                        .thenReturn(mockPresignedReq);

                adapter.getPresignedUrl("panen/file.jpg", "image/jpeg");

                assertEquals(Duration.ofMinutes(15),
                        captor.getValue().signatureDuration());
            }
        }

        @Test
        void getPresignedUrl_closesPresignerAfterUse() {
            PresignedPutObjectRequest mockPresignedReq = mock(PresignedPutObjectRequest.class);
            when(mockPresignedReq.url()).thenReturn(url("https://example.com"));

            try (MockedStatic<S3Presigner> presignerStatic = mockStatic(S3Presigner.class);
                 MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                presignerStatic.when(S3Presigner::builder).thenReturn(mockPresignerBuilder);
                when(mockPresigner.presignPutObject(any(PutObjectPresignRequest.class)))
                        .thenReturn(mockPresignedReq);

                adapter.getPresignedUrl("panen/file.jpg", "image/jpeg");

                verify(mockPresigner).close();
            }
        }
    }

    @Nested
    class DeleteFile {

        @Test
        void deleteFile_nullFileKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.deleteFile(null));
        }

        @Test
        void deleteFile_blankFileKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.deleteFile(""));
        }

        @Test
        void deleteFile_whitespaceFileKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.deleteFile("   "));
        }

        @Test
        void deleteFile_validFileKey_deletesFromS3() {
            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);

                adapter.deleteFile("panen/old-file.jpg");

                verify(mockS3Client).deleteObject(any(DeleteObjectRequest.class));
            }
        }

        @Test
        void deleteFile_validFileKey_closesS3Client() {
            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);

                adapter.deleteFile("panen/old-file.jpg");

                verify(mockS3Client).close();
            }
        }

        @Test
        void deleteFile_s3ThrowsException_wrapsInRuntimeException() {
            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);
                doThrow(S3Exception.builder().message("S3 delete error").build())
                        .when(mockS3Client).deleteObject(any(DeleteObjectRequest.class));

                StorageException ex = assertThrows(StorageException.class,
                        () -> adapter.deleteFile("panen/old-file.jpg"));

                assertTrue(ex.getMessage().contains("File deletion failed"));
            }
        }

        @Test
        void deleteFile_usesCorrectBucket() {
            try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
                s3Static.when(S3Client::builder).thenReturn(mockS3Builder);
                ArgumentCaptor<DeleteObjectRequest> captor =
                        ArgumentCaptor.forClass(DeleteObjectRequest.class);

                adapter.deleteFile("panen/old-file.jpg");

                verify(mockS3Client).deleteObject(captor.capture());
                assertEquals(BUCKET, captor.getValue().bucket());
                assertEquals("panen/old-file.jpg", captor.getValue().key());
            }
        }
    }

    @Nested
    class GetPublicUrl {

        @Test
        void getPublicUrl_nullFileKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.getPublicUrl(null));
        }

        @Test
        void getPublicUrl_blankFileKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.getPublicUrl(""));
        }

        @Test
        void getPublicUrl_whitespaceFileKey_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.getPublicUrl("   "));
        }

        @Test
        void getPublicUrl_validFileKey_returnsPublicUrl() {
            String result = adapter.getPublicUrl("panen/file.jpg");

            assertEquals(PUBLIC_URL + "/" + "panen/file.jpg", result);
        }

        @Test
        void getPublicUrl_baseUrlHasTrailingSlash_returnsCorrectUrl() {
            properties.setPublicUrl(PUBLIC_URL + "/");
            adapter = new R2StorageAdapter(properties);

            String result = adapter.getPublicUrl("panen/file.jpg");

            assertEquals(PUBLIC_URL + "/" + "panen/file.jpg", result);
        }

        @Test
        void getPublicUrl_baseUrlMultipleTrailingSlashes_stripsOne() {
            properties.setPublicUrl(PUBLIC_URL + "//");
            adapter = new R2StorageAdapter(properties);

            String result = adapter.getPublicUrl("panen/file.jpg");

            assertEquals(PUBLIC_URL + "//" + "panen/file.jpg", result);
        }

        @Test
        void getPublicUrl_noTrailingSlashOnBaseUrl_returnsCorrectly() {
            properties.setPublicUrl("https://pub-abc.r2.dev");
            adapter = new R2StorageAdapter(properties);

            String result = adapter.getPublicUrl("panen/file.jpg");

            assertEquals("https://pub-abc.r2.dev/panen/file.jpg", result);
        }
    }

    private static URL url(String spec) {
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
