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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@WithMockUser(roles = "ADMIN")
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
        AssignPersonRequestDTO body = new AssignPersonRequestDTO(mandorId);

        mockMvc.perform(post("/api/kebun/{kebunId}/assign/mandor", kebunId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void assignMandor_missingPersonId_returns400() throws Exception {
        UUID kebunId = UUID.randomUUID();
        AssignPersonRequestDTO body = new AssignPersonRequestDTO(null);

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
        AssignPersonRequestDTO body = new AssignPersonRequestDTO(mandorId);

        mockMvc.perform(post("/api/kebun/{kebunId}/move/mandor", kebunId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void edit_invalidRequest_returns400() throws Exception {
        // Mencakup: EditKebunRequestDTO validation
        UUID kebunId = UUID.randomUUID();
        // Request dengan nama kosong dan luas negatif
        id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.EditKebunRequestDTO invalidBody =
                new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.EditKebunRequestDTO(
                        "", -5, List.of()
                );

        mockMvc.perform(put("/api/kebun/" + kebunId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMandor_validId_returns200() throws Exception {
        UUID kebunId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        when(queryUseCase.getMandorIdByKebun(kebunId)).thenReturn(mandorId);

        mockMvc.perform(get("/api/kebun/{kebunId}/mandor", kebunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mandorId").value(mandorId.toString()));
    }

    @Test
    void getBuruhList_withFilter_returnsFilteredList() throws Exception {
        UUID kebunId = UUID.randomUUID();
        id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO buruh =
                new id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO(
                        UUID.randomUUID(), "b1", "Budi Santoso", "BURUH", "b@e.com");

        when(queryUseCase.getBuruhList(kebunId)).thenReturn(List.of(buruh));

        mockMvc.perform(get("/api/kebun/{kebunId}/buruh", kebunId)
                        .param("nama", "Budi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Budi Santoso"));
    }

    @Test
    void getSupirList_withFilter_returnsFilteredList() throws Exception {
        UUID kebunId = UUID.randomUUID();
        id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO supir =
                new id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO(
                        UUID.randomUUID(), "s1", "Supir Agus", "SUPIR", "s@e.com");

        when(queryUseCase.getSupirList(kebunId)).thenReturn(List.of(supir));

        mockMvc.perform(get("/api/kebun/{kebunId}/supir", kebunId)
                        .param("nama", "Agus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void assignSupir_validRequest_returns200() throws Exception {
        UUID kebunId = UUID.randomUUID();
        AssignPersonRequestDTO body = new AssignPersonRequestDTO(UUID.randomUUID());

        mockMvc.perform(post("/api/kebun/{kebunId}/assign/supir", kebunId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void moveSupir_validRequest_returns200() throws Exception {
        UUID kebunId = UUID.randomUUID();
        AssignPersonRequestDTO body = new AssignPersonRequestDTO(UUID.randomUUID());

        mockMvc.perform(post("/api/kebun/{kebunId}/move/supir", kebunId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void handleTypeMismatch_invalidUuid_returns400() throws Exception {
        // Memicu ExceptionHandler MethodArgumentTypeMismatchException
        mockMvc.perform(get("/api/kebun/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format"));
    }

    @Test
    void edit_success_returns200() throws Exception {
        // Mencakup alur log.info dan return ResponseEntity.ok pada method edit
        UUID kebunId = UUID.randomUUID();
        id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.EditKebunRequestDTO body =
                new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.EditKebunRequestDTO(
                        "New Name", 20, List.of(
                        new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO(0,0),
                        new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO(0,10),
                        new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO(10,0),
                        new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO(10,10)
                )
                );

        KebunDTO updatedDto = new KebunDTO(kebunId, "New Name", "K-01", 20, List.of());
        when(commandUseCase.editKebun(any(), anyString(), anyInt(), any())).thenReturn(updatedDto);

        mockMvc.perform(put("/api/kebun/{kebunId}", kebunId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nama").value("New Name"));
    }

    @Test
    void listKebun_withFilters_callsQueryUseCase() throws Exception {
        // Mencakup parameter filter nama dan kode di listKebun
        mockMvc.perform(get("/api/kebun")
                        .param("nama", "Kebun A")
                        .param("kode", "K-01"))
                .andExpect(status().isOk());

        verify(queryUseCase).listKebun("Kebun A", "K-01");
    }

    @Test
    void create_coordinatesNull_returns400() throws Exception {
        // Memicu MethodArgumentNotValidException via @NotEmpty
        id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CreateKebunRequestDTO body =
                new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CreateKebunRequestDTO(
                        "Kebun A", "KB-01", 20, null
                );

        mockMvc.perform(post("/api/kebun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed")); // Pesan dari GlobalExceptionHandler
    }

    @Test
    void getSupirList_withEmptyFilter_returnsAll() throws Exception {
        // Mencakup branch: if (searchNama != null && !searchNama.isBlank()) -> Kasus Blank
        UUID kebunId = UUID.randomUUID();
        when(queryUseCase.getSupirList(kebunId)).thenReturn(List.of());

        mockMvc.perform(get("/api/kebun/{kebunId}/supir", kebunId)
                        .param("nama", "   "))
                .andExpect(status().isOk());
    }

    @Test
    void getBuruhList_withEmptyFilter_returnsAll() throws Exception {
        // Mencakup branch: if (searchNama != null && !searchNama.isBlank()) -> Kasus Empty
        UUID kebunId = UUID.randomUUID();
        when(queryUseCase.getBuruhList(kebunId)).thenReturn(List.of());

        mockMvc.perform(get("/api/kebun/{kebunId}/buruh", kebunId)
                        .param("nama", ""))
                .andExpect(status().isOk());
    }

    @Test
    void getSupirList_withNullFilter_returnsAll() throws Exception {
        // Mencakup: if (searchNama != null) -> Kasus NULL
        UUID kebunId = UUID.randomUUID();
        when(queryUseCase.getSupirList(kebunId)).thenReturn(List.of());

        mockMvc.perform(get("/api/kebun/{kebunId}/supir", kebunId))
                // Tanpa param "nama"
                .andExpect(status().isOk());

        verify(queryUseCase).getSupirList(kebunId);
    }

    @Test
    void getBuruhList_withNullFilter_returnsAll() throws Exception {
        // Mencakup: if (searchNama != null) -> Kasus NULL
        UUID kebunId = UUID.randomUUID();
        when(queryUseCase.getBuruhList(kebunId)).thenReturn(List.of());

        mockMvc.perform(get("/api/kebun/{kebunId}/buruh", kebunId))
                // Tanpa param "nama"
                .andExpect(status().isOk());

        verify(queryUseCase).getBuruhList(kebunId);
    }

    @Test
    void getSupirList_withBlankFilter_returnsAll() throws Exception {
        // Mencakup branch: if (searchNama != null && !searchNama.isBlank()) -> Kasus Blank
        UUID kebunId = UUID.randomUUID();
        when(queryUseCase.getSupirList(kebunId)).thenReturn(List.of());

        mockMvc.perform(get("/api/kebun/{kebunId}/supir", kebunId)
                        .param("nama", "   ")) // Hanya spasi
                .andExpect(status().isOk());
    }

    @Test
    void listKebun_withKodeFilterOnly_returnsFilteredKebun() throws Exception {
        UUID kebunId = UUID.randomUUID();
        KebunDTO kebun = new KebunDTO(
                kebunId,
                "Kebun B",
                "KB-02",
                25,
                List.of(
                        new CoordinateDTO(0, 0),
                        new CoordinateDTO(0, 10),
                        new CoordinateDTO(10, 0),
                        new CoordinateDTO(10, 10)
                )
        );

        when(queryUseCase.listKebun(null, "KB-02")).thenReturn(List.of(kebun));

        mockMvc.perform(get("/api/kebun")
                        .param("kode", "KB-02")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].kode").value("KB-02"));
    }

    @Test
    void getSupirList_withFilterNoMatch_returnsEmptyList() throws Exception {
        UUID kebunId = UUID.randomUUID();
        id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO supir =
                new id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO(
                        UUID.randomUUID(), "s1", "Supir Agus", "SUPIR", "s@e.com");

        when(queryUseCase.getSupirList(kebunId)).thenReturn(List.of(supir));

        mockMvc.perform(get("/api/kebun/{kebunId}/supir", kebunId)
                        .param("nama", "Tidak Ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getBuruhList_withFilterNoMatch_returnsEmptyList() throws Exception {
        UUID kebunId = UUID.randomUUID();
        id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO buruh =
                new id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO(
                        UUID.randomUUID(), "b1", "Budi Santoso", "BURUH", "b@e.com");

        when(queryUseCase.getBuruhList(kebunId)).thenReturn(List.of(buruh));

        mockMvc.perform(get("/api/kebun/{kebunId}/buruh", kebunId)
                        .param("nama", "Tidak Ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void edit_invalidKebunId_returns400() throws Exception {
        id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.EditKebunRequestDTO body =
                new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.EditKebunRequestDTO(
                        "New Name", 20, List.of(
                        new CoordinateDTO(0,0),
                        new CoordinateDTO(0,10),
                        new CoordinateDTO(10,0),
                        new CoordinateDTO(10,10)
                ));

        mockMvc.perform(put("/api/kebun/{kebunId}", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format"));
    }

    @Test
    void delete_invalidKebunId_returns400() throws Exception {
        mockMvc.perform(delete("/api/kebun/{kebunId}", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format"));
    }

    @Test
    void create_coordinatesSizeNotFour_triggersIllegalArgumentException() throws Exception {
        // Memicu branch: body.coordinates().size() != 4
        // Menggunakan list dengan 2 koordinat (bukan 4) untuk memastikan IllegalArgumentException dilempar
        id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CreateKebunRequestDTO body =
                new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CreateKebunRequestDTO(
                        "Kebun Test", "KT-01", 10, List.of(
                        new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO(0, 0),
                        new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO(1, 1)
                ));

        mockMvc.perform(post("/api/kebun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Coordinates must contain exactly 4 points"));
    }

    @Test
    void create_coordinatesEmptyList_returns400() throws Exception {
        // Memicu @NotEmpty pada List coordinates
        id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CreateKebunRequestDTO body =
                new id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CreateKebunRequestDTO(
                        "Kebun Test", "KT-01", 10, List.of() // Empty list
                );

        mockMvc.perform(post("/api/kebun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void create_nullCoordinates_directCall_throwsIllegalArgumentException() {
        // Direct call to bypass MockMvc/@Valid to ensure branch condition coverage 
        // for `body.coordinates() == null` in controller method
        CreateKebunRequestDTO body = new CreateKebunRequestDTO("Kebun", "K-01", 10, null);
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, 
            () -> controller.create(body)
        );
    }
}
