package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KebunControllerTest {

    @Mock private KebunCommandUseCase commandUseCase;
    @Mock private KebunQueryUseCase queryUseCase;
    @InjectMocks private KebunController controller;

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
                List.of(new CoordinateDTO(0, 0), new CoordinateDTO(0, 10), new CoordinateDTO(10, 0), new CoordinateDTO(10, 10))
        );
        when(commandUseCase.createKebun(body.nama(), body.kode(), body.luas(), body.coordinates()))
                .thenReturn(new KebunDTO(kebunId, body.nama(), body.kode(), body.luas(), body.coordinates()));

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
                List.of(new CoordinateDTO(0, 0), new CoordinateDTO(0, 10), new CoordinateDTO(10, 0))
        );

        mockMvc.perform(post("/api/kebun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void assignMandor_missingPersonId_returns400() throws Exception {
        mockMvc.perform(post("/api/kebun/{kebunId}/assign/mandor", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignPersonRequestDTO(null, UUID.randomUUID()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getKebun_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/kebun/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}