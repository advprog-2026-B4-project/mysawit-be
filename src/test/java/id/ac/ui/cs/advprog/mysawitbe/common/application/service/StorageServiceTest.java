package id.ac.ui.cs.advprog.mysawitbe.common.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import id.ac.ui.cs.advprog.mysawitbe.common.application.port.out.StoragePort;
import id.ac.ui.cs.advprog.mysawitbe.common.dto.PresignedUrlResponse;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private StoragePort storagePort;

    @InjectMocks
    private StorageService storageService;

    private static final String PRE_SIGNED_URL = "https://r2.example.com/upload/panen/abc123.jpg?signature=xyz";
    private static final String PUBLIC_URL = "https://pub-abc.r2.dev/panen/abc123.jpg";

    // =========================================================================
    // generatePresignedUrl()
    // =========================================================================
    @Nested
    class GeneratePresignedUrl {

        @Test
        void generatePresignedUrl_nullContentType_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.generatePresignedUrl(null));

            assertEquals("Content type tidak boleh kosong.", ex.getMessage());
            verify(storagePort, never()).getPresignedUrl(anyString(), anyString());
        }

        @Test
        void generatePresignedUrl_blankContentType_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.generatePresignedUrl(""));

            assertEquals("Content type tidak boleh kosong.", ex.getMessage());
            verify(storagePort, never()).getPresignedUrl(anyString(), anyString());
        }

        @Test
        void generatePresignedUrl_whitespaceContentType_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.generatePresignedUrl("   "));

            assertEquals("Content type tidak boleh kosong.", ex.getMessage());
            verify(storagePort, never()).getPresignedUrl(anyString(), anyString());
        }

        @Test
        void generatePresignedUrl_unsupportedContentTypeGif_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.generatePresignedUrl("image/gif"));

            assertEquals("Hanya file JPG dan PNG yang diizinkan.", ex.getMessage());
            verify(storagePort, never()).getPresignedUrl(anyString(), anyString());
        }

        @Test
        void generatePresignedUrl_unsupportedContentTypeWebp_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.generatePresignedUrl("image/webp"));

            assertEquals("Hanya file JPG dan PNG yang diizinkan.", ex.getMessage());
            verify(storagePort, never()).getPresignedUrl(anyString(), anyString());
        }

        @Test
        void generatePresignedUrl_unsupportedContentTypePdf_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.generatePresignedUrl("application/pdf"));

            assertEquals("Hanya file JPG dan PNG yang diizinkan.", ex.getMessage());
            verify(storagePort, never()).getPresignedUrl(anyString(), anyString());
        }

        @Test
        void generatePresignedUrl_jpegType_generatesJpgExtension() {
            when(storagePort.getPresignedUrl(endsWith(".jpg"), eq("image/jpeg")))
                    .thenReturn(PRE_SIGNED_URL);
            when(storagePort.getPublicUrl(endsWith(".jpg")))
                    .thenReturn(PUBLIC_URL);

            PresignedUrlResponse result = storageService.generatePresignedUrl("image/jpeg");

            assertNotNull(result);
            assertEquals(PRE_SIGNED_URL, result.presignedUrl());
            assertEquals(PUBLIC_URL, result.publicUrl());
            assertTrue(result.fileKey().startsWith("panen/"));
            assertTrue(result.fileKey().endsWith(".jpg"));
        }

        @Test
        void generatePresignedUrl_pngType_generatesPngExtension() {
            when(storagePort.getPresignedUrl(endsWith(".png"), eq("image/png")))
                    .thenReturn(PRE_SIGNED_URL);
            when(storagePort.getPublicUrl(endsWith(".png")))
                    .thenReturn(PUBLIC_URL);

            PresignedUrlResponse result = storageService.generatePresignedUrl("image/png");

            assertNotNull(result);
            assertEquals(PRE_SIGNED_URL, result.presignedUrl());
            assertEquals(PUBLIC_URL, result.publicUrl());
            assertTrue(result.fileKey().startsWith("panen/"));
            assertTrue(result.fileKey().endsWith(".png"));
        }

        @Test
        void generatePresignedUrl_validJpeg_returnsPresignedUrlResponseWithAllFields() {
            when(storagePort.getPresignedUrl(anyString(), eq("image/jpeg")))
                    .thenReturn(PRE_SIGNED_URL);
            when(storagePort.getPublicUrl(anyString()))
                    .thenReturn(PUBLIC_URL);

            PresignedUrlResponse result = storageService.generatePresignedUrl("image/jpeg");

            assertNotNull(result.presignedUrl());
            assertNotNull(result.publicUrl());
            assertNotNull(result.fileKey());
            assertTrue(result.fileKey().matches("panen/[0-9a-f-]+\\.jpg"));
        }

        @Test
        void generatePresignedUrl_validPng_returnsPresignedUrlResponseWithAllFields() {
            when(storagePort.getPresignedUrl(anyString(), eq("image/png")))
                    .thenReturn(PRE_SIGNED_URL);
            when(storagePort.getPublicUrl(anyString()))
                    .thenReturn(PUBLIC_URL);

            PresignedUrlResponse result = storageService.generatePresignedUrl("image/png");

            assertNotNull(result.presignedUrl());
            assertNotNull(result.publicUrl());
            assertNotNull(result.fileKey());
            assertTrue(result.fileKey().matches("panen/[0-9a-f-]+\\.png"));
        }

        @Test
        void generatePresignedUrl_fileKeyContainsPanenPrefix() {
            when(storagePort.getPresignedUrl(anyString(), anyString()))
                    .thenReturn(PRE_SIGNED_URL);
            when(storagePort.getPublicUrl(anyString()))
                    .thenReturn(PUBLIC_URL);

            PresignedUrlResponse result = storageService.generatePresignedUrl("image/jpeg");

            assertTrue(result.fileKey().startsWith("panen/"),
                    "File key should start with 'panen/', got: " + result.fileKey());
        }

        @Test
        void generatePresignedUrl_fileKeyContainsUuid() {
            when(storagePort.getPresignedUrl(anyString(), anyString()))
                    .thenReturn(PRE_SIGNED_URL);
            when(storagePort.getPublicUrl(anyString()))
                    .thenReturn(PUBLIC_URL);

            PresignedUrlResponse result = storageService.generatePresignedUrl("image/jpeg");

            String uuidPart = result.fileKey().replace("panen/", "").replace(".jpg", "");
            assertTrue(uuidPart.matches("[0-9a-f-]{36}"),
                    "File key should contain UUID, got: " + uuidPart);
        }

        @Test
        void generatePresignedUrl_delegatesPresignedUrlToStoragePort() {
            when(storagePort.getPresignedUrl(anyString(), eq("image/jpeg")))
                    .thenReturn(PRE_SIGNED_URL);
            when(storagePort.getPublicUrl(anyString()))
                    .thenReturn(PUBLIC_URL);

            storageService.generatePresignedUrl("image/jpeg");

            verify(storagePort).getPresignedUrl(anyString(), eq("image/jpeg"));
        }

        @Test
        void generatePresignedUrl_delegatesPublicUrlToStoragePort() {
            when(storagePort.getPresignedUrl(anyString(), eq("image/jpeg")))
                    .thenReturn(PRE_SIGNED_URL);
            when(storagePort.getPublicUrl(anyString()))
                    .thenReturn(PUBLIC_URL);

            storageService.generatePresignedUrl("image/jpeg");

            verify(storagePort).getPublicUrl(anyString());
        }
    }

    // =========================================================================
    // deletePhoto()
    // =========================================================================
    @Nested
    class DeletePhoto {

        @Test
        void deletePhoto_nullFileKey_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.deletePhoto(null));

            assertEquals("File key tidak valid.", ex.getMessage());
            verify(storagePort, never()).deleteFile(anyString());
        }

        @Test
        void deletePhoto_blankFileKey_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.deletePhoto(""));

            assertEquals("File key tidak valid.", ex.getMessage());
            verify(storagePort, never()).deleteFile(anyString());
        }

        @Test
        void deletePhoto_whitespaceFileKey_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.deletePhoto("   "));

            assertEquals("File key tidak valid.", ex.getMessage());
            verify(storagePort, never()).deleteFile(anyString());
        }

        @Test
        void deletePhoto_validFileKey_delegatesToStoragePort() {
            String fileKey = "panen/abc-123.jpg";

            storageService.deletePhoto(fileKey);

            verify(storagePort).deleteFile(fileKey);
        }

        @Test
        void deletePhoto_validFileKeyWithUuid_delegatesToStoragePort() {
            String fileKey = "panen/550e8400-e29b-41d4-a716-446655440000.png";

            storageService.deletePhoto(fileKey);

            verify(storagePort).deleteFile(fileKey);
        }
    }
}
