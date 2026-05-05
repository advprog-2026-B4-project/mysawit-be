package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import id.ac.ui.cs.advprog.mysawitbe.common.exception.GlobalExceptionHandler;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.CreatePanenRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.ReviewPanenRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class PanenControllerTest {

    @Mock private PanenCommandUseCase commandUseCase;
    @Mock private PanenQueryUseCase queryUseCase;

    @InjectMocks
    private PanenController panenController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID panenId;
    private UUID userId;
    private PanenDTO samplePanen;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(panenController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        panenId = UUID.randomUUID();
        userId  = UUID.randomUUID();

        samplePanen = new PanenDTO(
                panenId, userId, "Budi", UUID.randomUUID(),
                "Deskripsi Test", 100, "PENDING", null, List.of(), LocalDateTime.now()
        );
    }

    // =========================================================================
    // GET /api/panen/{panenId}
    // =========================================================================
    @Nested
    class GetPanenById {

        @Test
        void shouldReturn200WithDataWhenFound() throws Exception {
            when(queryUseCase.getPanenById(panenId)).thenReturn(samplePanen);

            mockMvc.perform(get("/api/panen/{panenId}", panenId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.panenId").value(panenId.toString()));
        }

        @Test
        void shouldReturn404WhenNotFound() throws Exception {
            when(queryUseCase.getPanenById(panenId))
                    .thenThrow(new EntityNotFoundException("Not found"));

            mockMvc.perform(get("/api/panen/{panenId}", panenId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn500OnUnexpectedError() throws Exception {
            when(queryUseCase.getPanenById(panenId))
                    .thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(get("/api/panen/{panenId}", panenId))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =========================================================================
    // POST /api/panen
    // =========================================================================
    @Nested
    class CreatePanen {

        @Test
        void shouldReturn201WhenCreatedSuccessfully() throws Exception {
            CreatePanenRequestDTO request = new CreatePanenRequestDTO(
                    100, List.of("http://example.com/photo.jpg"), "Deskripsi Test"
            );
            
            when(commandUseCase.createPanen(any(UUID.class), anyString(), anyInt(), anyList()))
                    .thenReturn(samplePanen);

            mockMvc.perform(post("/api/panen")
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldReturn404WhenEntityNotFound() throws Exception {
            CreatePanenRequestDTO request = new CreatePanenRequestDTO(
                    100, List.of("http://test.com/1.jpg"), "Deskripsi Test"
            );
            
            when(commandUseCase.createPanen(any(UUID.class), anyString(), anyInt(), anyList()))
                    .thenThrow(new EntityNotFoundException("Buruh tidak ditemukan"));

            mockMvc.perform(post("/api/panen")
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn409WhenDailyLimitReached() throws Exception {
            CreatePanenRequestDTO request = new CreatePanenRequestDTO(
                    100, List.of("http://test.com/1.jpg"), "Deskripsi Test"
            );
            
            when(commandUseCase.createPanen(any(UUID.class), anyString(), anyInt(), anyList()))
                    .thenThrow(new IllegalStateException("Sudah panen hari ini"));

            mockMvc.perform(post("/api/panen")
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldReturn400WhenInvalidArgument() throws Exception {
            CreatePanenRequestDTO request = new CreatePanenRequestDTO(
                    100, List.of("http://test.com/1.jpg"), "Deskripsi Test"
            );
            
            when(commandUseCase.createPanen(any(UUID.class), anyString(), anyInt(), anyList()))
                    .thenThrow(new IllegalArgumentException("Invalid input"));

            mockMvc.perform(post("/api/panen")
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn500OnUnexpectedError() throws Exception {
            CreatePanenRequestDTO request = new CreatePanenRequestDTO(
                    100, List.of("http://test.com/1.jpg"), "Deskripsi Test"
            );
            
            when(commandUseCase.createPanen(any(UUID.class), anyString(), anyInt(), anyList()))
                    .thenThrow(new RuntimeException("Database down"));

            mockMvc.perform(post("/api/panen")
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =========================================================================
    // GET /api/panen/buruh
    // =========================================================================
    @Nested
    class GetRiwayatPanenBuruh {

        @Test
        void shouldReturn200WithListOfPanen() throws Exception {
            when(queryUseCase.listPanenByBuruh(any(UUID.class), any(), any(), any()))
                    .thenReturn(List.of(samplePanen));

            mockMvc.perform(get("/api/panen/buruh")
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldReturn404WhenEntityNotFound() throws Exception {
            when(queryUseCase.listPanenByBuruh(any(UUID.class), any(), any(), any()))
                    .thenThrow(new EntityNotFoundException("Buruh tidak ditemukan"));

            mockMvc.perform(get("/api/panen/buruh")
                            .requestAttr("userId", userId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn500OnUnexpectedError() throws Exception {
            when(queryUseCase.listPanenByBuruh(any(UUID.class), any(), any(), any()))
                    .thenThrow(new RuntimeException("System error"));

            mockMvc.perform(get("/api/panen/buruh")
                            .requestAttr("userId", userId))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =========================================================================
    // GET /api/panen/mandor
    // =========================================================================
    @Nested
    class GetRiwayatPanenMandor {

        @Test
        void shouldReturn200WithListOfPanen() throws Exception {
            when(queryUseCase.listPanenByMandor(any(UUID.class), any(), any()))
                    .thenReturn(List.of(samplePanen));

            mockMvc.perform(get("/api/panen/mandor")
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldReturn404WhenEntityNotFound() throws Exception {
            when(queryUseCase.listPanenByMandor(any(UUID.class), any(), any()))
                    .thenThrow(new EntityNotFoundException("Mandor tidak ditemukan"));

            mockMvc.perform(get("/api/panen/mandor")
                            .requestAttr("userId", userId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn409WhenIllegalState() throws Exception {
            when(queryUseCase.listPanenByMandor(any(UUID.class), any(), any()))
                    .thenThrow(new IllegalStateException("Mandor error"));

            mockMvc.perform(get("/api/panen/mandor")
                            .requestAttr("userId", userId))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldReturn500OnUnexpectedError() throws Exception {
            when(queryUseCase.listPanenByMandor(any(UUID.class), any(), any()))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/panen/mandor")
                            .requestAttr("userId", userId))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =========================================================================
    // GET /api/panen/checksubmission
    // =========================================================================
    @Nested
    class CheckPanenToday {

        @Test
        void shouldReturn200WithTrueWhenAlreadySubmitted() throws Exception {
            when(queryUseCase.hasPanenToday(any(UUID.class), any(LocalDate.class))).thenReturn(true);

            mockMvc.perform(get("/api/panen/checksubmission")
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        void shouldReturn500OnUnexpectedError() throws Exception {
            when(queryUseCase.hasPanenToday(any(UUID.class), any(LocalDate.class)))
                    .thenThrow(new RuntimeException("System failed"));

            mockMvc.perform(get("/api/panen/checksubmission")
                            .requestAttr("userId", userId))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =========================================================================
    // GET /api/panen/buruh/{buruhId}
    // =========================================================================
    @Nested
    class GetPanenByBuruhId {

        @Test
        void shouldReturn200WithDataWhenAuthorized() throws Exception {
            UUID buruhId = UUID.randomUUID();
            when(queryUseCase.listPanenByBuruhWithAuth(any(UUID.class), any(UUID.class), any(), any(), any()))
                    .thenReturn(List.of(samplePanen));

            mockMvc.perform(get("/api/panen/buruh/{buruhId}", buruhId)
                            .requestAttr("userId", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].panenId").value(panenId.toString()));
        }

        @Test
        void shouldReturn404WhenEntityNotFound() throws Exception {
            UUID buruhId = UUID.randomUUID();
            when(queryUseCase.listPanenByBuruhWithAuth(any(UUID.class), any(UUID.class), any(), any(), any()))
                    .thenThrow(new EntityNotFoundException("Data buruh tidak ada"));

            mockMvc.perform(get("/api/panen/buruh/{buruhId}", buruhId)
                            .requestAttr("userId", userId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn403WhenAccessDenied() throws Exception {
            when(queryUseCase.listPanenByBuruhWithAuth(any(UUID.class), any(UUID.class), any(), any(), any()))
                    .thenThrow(new IllegalAccessException("No access"));

            mockMvc.perform(get("/api/panen/buruh/{buruhId}", UUID.randomUUID())
                            .requestAttr("userId", userId))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturn500OnUnexpectedError() throws Exception {
            when(queryUseCase.listPanenByBuruhWithAuth(any(UUID.class), any(UUID.class), any(), any(), any()))
                    .thenThrow(new RuntimeException("Unknown error"));

            mockMvc.perform(get("/api/panen/buruh/{buruhId}", UUID.randomUUID())
                            .requestAttr("userId", userId))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =========================================================================
    // PATCH /api/panen/{panenId}/review
    // =========================================================================
    @Nested
    class ReviewPanen {

        @Test
        void shouldReturn200WhenApproveSuccessful() throws Exception {
            ReviewPanenRequestDTO request = new ReviewPanenRequestDTO("APPROVE", null);
            when(commandUseCase.approvePanen(any(UUID.class), any(UUID.class))).thenReturn(samplePanen);

            mockMvc.perform(patch("/api/panen/{panenId}/review", panenId)
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldReturn200WhenRejectSuccessful() throws Exception {
            ReviewPanenRequestDTO request = new ReviewPanenRequestDTO("REJECT", "Kualitas jelek");
            when(commandUseCase.rejectPanen(any(UUID.class), any(UUID.class), anyString()))
                    .thenReturn(samplePanen);

            mockMvc.perform(patch("/api/panen/{panenId}/review", panenId)
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldReturn400WhenRejectReasonIsBlank() throws Exception {
            ReviewPanenRequestDTO request = new ReviewPanenRequestDTO("REJECT", "   ");

            Exception exception = mockMvc.perform(patch("/api/panen/{panenId}/review", panenId)
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResolvedException();

            assertNotNull(exception);
            assertEquals(IllegalArgumentException.class, exception.getClass());
        }

        @Test
        void shouldReturn400WhenActionIsInvalid() throws Exception {
            ReviewPanenRequestDTO request = new ReviewPanenRequestDTO("INVALID", null);

            Exception exception = mockMvc.perform(patch("/api/panen/{panenId}/review", panenId)
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResolvedException();

            assertNotNull(exception);
            assertEquals(IllegalArgumentException.class, exception.getClass());
        }

        @Test
        void shouldReturn404WhenPanenNotFound() throws Exception {
            ReviewPanenRequestDTO request = new ReviewPanenRequestDTO("APPROVE", null);
            when(commandUseCase.approvePanen(any(UUID.class), any(UUID.class)))
                    .thenThrow(new EntityNotFoundException("Tidak ditemukan"));

            mockMvc.perform(patch("/api/panen/{panenId}/review", panenId)
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn409WhenPanenStatusInvalid() throws Exception {
            ReviewPanenRequestDTO request = new ReviewPanenRequestDTO("APPROVE", null);
            when(commandUseCase.approvePanen(any(UUID.class), any(UUID.class)))
                    .thenThrow(new IllegalStateException("Status tidak valid"));

            mockMvc.perform(patch("/api/panen/{panenId}/review", panenId)
                            .requestAttr("userId", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }
}