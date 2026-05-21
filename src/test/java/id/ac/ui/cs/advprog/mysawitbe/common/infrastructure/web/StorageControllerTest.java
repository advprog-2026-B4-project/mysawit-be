package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import id.ac.ui.cs.advprog.mysawitbe.common.application.port.in.StorageUseCase;
import id.ac.ui.cs.advprog.mysawitbe.common.dto.PresignedUrlResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;

@ExtendWith(MockitoExtension.class)
class StorageControllerTest {

    @Mock
    private StorageUseCase storageUseCase;

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private StorageController storageController;

    private MockMvc mockMvc;

    private UUID userId;
    private UUID mandorId;
    private static final String PRE_SIGNED_URL = "https://r2.example.com/upload/test?signature=abc";
    private static final String PUBLIC_URL = "https://pub-abc.r2.dev/panen/test.jpg";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(storageController)
                .build();

        userId = UUID.randomUUID();
        mandorId = UUID.randomUUID();
    }

    @Nested
    class GetPresignedUrl {

        @Test
        void getPresignedUrl_userNotExists_returns401Unauthorized() throws Exception {
            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(false);

            mockMvc.perform(get("/api/storage/presigned-url")
                            .param("contentType", "image/jpeg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void getPresignedUrl_buruhHasNoMandor_returns403Forbidden() throws Exception {
            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(true);
            when(userQueryUseCase.getMandorIdByBuruhId(userId)).thenReturn(null);

            mockMvc.perform(get("/api/storage/presigned-url")
                            .param("contentType", "image/jpeg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getPresignedUrl_validRequest_returns200WithPresignedUrlResponse() throws Exception {
            PresignedUrlResponse response = new PresignedUrlResponse(
                    PRE_SIGNED_URL, PUBLIC_URL, "panen/test-file.jpg");

            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(true);
            when(userQueryUseCase.getMandorIdByBuruhId(userId)).thenReturn(mandorId);
            when(storageUseCase.generatePresignedUrl("image/jpeg")).thenReturn(response);

            mockMvc.perform(get("/api/storage/presigned-url")
                            .param("contentType", "image/jpeg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.presignedUrl").value(PRE_SIGNED_URL))
                    .andExpect(jsonPath("$.data.publicUrl").value(PUBLIC_URL))
                    .andExpect(jsonPath("$.data.fileKey").value("panen/test-file.jpg"));
        }

        @Test
        void getPresignedUrl_validPngRequest_returns200() throws Exception {
            PresignedUrlResponse response = new PresignedUrlResponse(
                    PRE_SIGNED_URL, PUBLIC_URL, "panen/test-file.png");

            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(true);
            when(userQueryUseCase.getMandorIdByBuruhId(userId)).thenReturn(mandorId);
            when(storageUseCase.generatePresignedUrl("image/png")).thenReturn(response);

            mockMvc.perform(get("/api/storage/presigned-url")
                            .param("contentType", "image/png")
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fileKey").value("panen/test-file.png"));
        }
    }

    @Nested
    class GetUploadToken {

        @Test
        void getUploadToken_userNotExists_returns401Unauthorized() throws Exception {
            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(false);

            mockMvc.perform(get("/api/storage/upload-token")
                            .param("contentType", "image/jpeg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void getUploadToken_buruhHasNoMandor_returns403Forbidden() throws Exception {
            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(true);
            when(userQueryUseCase.getMandorIdByBuruhId(userId)).thenReturn(null);

            mockMvc.perform(get("/api/storage/upload-token")
                            .param("contentType", "image/jpeg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getUploadToken_validRequest_returns200WithTokenString() throws Exception {
            PresignedUrlResponse response = new PresignedUrlResponse(
                    PRE_SIGNED_URL, PUBLIC_URL, "panen/file.jpg");

            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(true);
            when(userQueryUseCase.getMandorIdByBuruhId(userId)).thenReturn(mandorId);
            when(storageUseCase.generatePresignedUrl("image/jpeg")).thenReturn(response);

            mockMvc.perform(get("/api/storage/upload-token")
                            .param("contentType", "image/jpeg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(PRE_SIGNED_URL + "|" + PUBLIC_URL));
        }

        @Test
        void getUploadToken_tokenFormat_containsPipeSeparator() throws Exception {
            PresignedUrlResponse response = new PresignedUrlResponse(
                    PRE_SIGNED_URL, PUBLIC_URL, "panen/file.jpg");

            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(true);
            when(userQueryUseCase.getMandorIdByBuruhId(userId)).thenReturn(mandorId);
            when(storageUseCase.generatePresignedUrl(any())).thenReturn(response);

            String result = mockMvc.perform(get("/api/storage/upload-token")
                            .param("contentType", "image/jpeg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertTrue(result.contains("|"));
        }
    }

    @Nested
    class DeleteFile {

        @Test
        void deleteFile_userNotExists_returns401Unauthorized() throws Exception {
            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(false);

            mockMvc.perform(delete("/api/storage/file")
                            .param("fileKey", "panen/test.jpg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void deleteFile_buruhHasNoMandor_returns403Forbidden() throws Exception {
            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(true);
            when(userQueryUseCase.getMandorIdByBuruhId(userId)).thenReturn(null);

            mockMvc.perform(delete("/api/storage/file")
                            .param("fileKey", "panen/test.jpg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isForbidden());
        }

        @Test
        void deleteFile_validRequest_returns200Success() throws Exception {
            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(true);
            when(userQueryUseCase.getMandorIdByBuruhId(userId)).thenReturn(mandorId);

            mockMvc.perform(delete("/api/storage/file")
                            .param("fileKey", "panen/test.jpg")
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void deleteFile_validRequest_delegatesToStorageUseCase() throws Exception {
            String fileKey = "panen/abc-123.jpg";
            when(userQueryUseCase.verifyUserExists(userId)).thenReturn(true);
            when(userQueryUseCase.getMandorIdByBuruhId(userId)).thenReturn(mandorId);

            mockMvc.perform(delete("/api/storage/file")
                            .param("fileKey", fileKey)
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk());

            verify(storageUseCase).deletePhoto(fileKey);
        }
    }
}
