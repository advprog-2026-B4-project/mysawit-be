package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.common.exception.GlobalExceptionHandler;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.AssignPersonRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CreateKebunRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WithMockUser(roles = "ADMIN_UTAMA")
class KebunControllerTest {

    @Mock
    private KebunCommandUseCase commandUseCase;

    @Mock
    private KebunQueryUseCase queryUseCase;

    @InjectMocks
    private KebunController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_validRequest_returns200() throws Exception {
        UUID kebunId = UUID.randomUUID();
        CreateKebunRequestDTO body = new CreateKebunRequestDTO(
                "Kebun A",
                "KB-01",
                20,
                List.of(
                        new CoordinateDTO(0, 0),
                        new CoordinateDTO(0, 10),
                        new CoordinateDTO(10, 0),
                        new CoordinateDTO(10, 10)
                )
        );

        KebunDTO expectedResponse = new KebunDTO(
                kebunId,
                body.nama(),
                body.kode(),
                body.luas(),
                body.coordinates()
        );

        when(commandUseCase.createKebun(
                eq(body.nama()),
                eq(body.kode()),
                eq(body.luas()),
                eq(body.coordinates())
        )).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/kebun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kebunId").value(kebunId.toString()));
    }

    @Test
    void create_invalidCoordinateCount_returns400() throws Exception {
        CreateKebunRequestDTO body = new CreateKebunRequestDTO(
                "Kebun A",
                "KB-01",
                20,
                List.of(
                        new CoordinateDTO(0, 0),
                        new CoordinateDTO(0, 10),
                        new CoordinateDTO(10, 0)
                )
        );

        mockMvc.perform(post("/api/kebun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_withoutFilters_returnsAllKebun() throws Exception {
        UUID kebunId = UUID.randomUUID();
        KebunDTO kebun = new KebunDTO(
                kebunId,
                "Kebun A",
                "KB-01",
                20,
                List.of(
                        new CoordinateDTO(0, 0),
                        new CoordinateDTO(0, 10),
                        new CoordinateDTO(10, 0),
                        new CoordinateDTO(10, 10)
                )
        );

        when(queryUseCase.listKebun(null, null)).thenReturn(List.of(kebun));

        mockMvc.perform(get("/api/kebun")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].kebunId").value(kebunId.toString()));
    }

    @Test
    void list_withNamaFilter_returnsFilteredKebun() throws Exception {
        UUID kebunId = UUID.randomUUID();
        KebunDTO kebun = new KebunDTO(
                kebunId,
                "Kebun A",
                "KB-01",
                20,
                List.of(
                        new CoordinateDTO(0, 0),
                        new CoordinateDTO(0, 10),
                        new CoordinateDTO(10, 0),
                        new CoordinateDTO(10, 10)
                )
        );

        when(queryUseCase.listKebun("Kebun A", null)).thenReturn(List.of(kebun));

        mockMvc.perform(get("/api/kebun")
                        .param("nama", "Kebun A")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getKebun_validUuid_returns200() throws Exception {
        UUID kebunId = UUID.randomUUID();
        KebunDTO kebun = new KebunDTO(
                kebunId,
                "Kebun A",
                "KB-01",
                20,
                List.of(
                        new CoordinateDTO(0, 0),
                        new CoordinateDTO(0, 10),
                        new CoordinateDTO(10, 0),
                        new CoordinateDTO(10, 10)
                )
        );

        when(queryUseCase.getKebunById(kebunId)).thenReturn(kebun);

        mockMvc.perform(get("/api/kebun/{kebunId}", kebunId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kebunId").value(kebunId.toString()));
    }

    @Test
    void getKebun_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/kebun/{kebunId}", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignMandor_validRequest_returns200() throws Exception {
        UUID kebunId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        AssignPersonRequestDTO body = new AssignPersonRequestDTO(mandorId, kebunId);

        mockMvc.perform(post("/api/kebun/{kebunId}/assign/mandor", kebunId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void assignMandor_missingPersonId_returns400() throws Exception {
        UUID kebunId = UUID.randomUUID();
        AssignPersonRequestDTO body = new AssignPersonRequestDTO(null, kebunId);

        mockMvc.perform(post("/api/kebun/{kebunId}/assign/mandor", kebunId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_validRequest_returns200() throws Exception {
        UUID kebunId = UUID.randomUUID();

        mockMvc.perform(delete("/api/kebun/{kebunId}", kebunId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getSupirList_validKebunId_returns200() throws Exception {
        UUID kebunId = UUID.randomUUID();

        when(queryUseCase.getSupirList(kebunId)).thenReturn(List.of());

        mockMvc.perform(get("/api/kebun/{kebunId}/supir", kebunId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void moveMandor_validRequest_returns200() throws Exception {
        UUID kebunId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        AssignPersonRequestDTO body = new AssignPersonRequestDTO(mandorId, kebunId);

        mockMvc.perform(post("/api/kebun/{kebunId}/move/mandor", kebunId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}