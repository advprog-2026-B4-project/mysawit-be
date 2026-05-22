package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.mysawitbe.common.exception.GlobalExceptionHandler;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.UpdateVariabelPokokRequest;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.VariabelPokokCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.VariabelPokokQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import jakarta.persistence.EntityNotFoundException;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VariabelPokokControllerTest {

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    @Mock  VariabelPokokQueryUseCase   queryUseCase;
    @Mock  VariabelPokokCommandUseCase commandUseCase;
    @InjectMocks VariabelPokokController controller;

    private static final VariabelPokokDTO BURUH_DTO = new VariabelPokokDTO(
            VariableKey.UPAH_BURUH,
            VariableKey.UPAH_BURUH.getLabel(),
            VariableKey.UPAH_BURUH.getDescription(),
            10_000
    );

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAll_returns200WithList() throws Exception {
        when(queryUseCase.getAllVariabelPokok()).thenReturn(List.of(BURUH_DTO));

        mockMvc.perform(get("/api/pembayaran/variabel-pokok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].key").value("UPAH_BURUH"))
                .andExpect(jsonPath("$.data[0].value").value(10_000));
    }

    @Test
    void getOne_existingKey_returns200() throws Exception {
        when(queryUseCase.getVariabelPokok(VariableKey.UPAH_BURUH)).thenReturn(BURUH_DTO);

        mockMvc.perform(get("/api/pembayaran/variabel-pokok/UPAH_BURUH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("UPAH_BURUH"));
    }

    @Test
    void getOne_missingKey_returns404() throws Exception {
        when(queryUseCase.getVariabelPokok(VariableKey.UPAH_SUPIR))
                .thenThrow(new EntityNotFoundException("not found"));

        mockMvc.perform(get("/api/pembayaran/variabel-pokok/UPAH_SUPIR"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        VariabelPokokDTO updated = new VariabelPokokDTO(
                VariableKey.UPAH_BURUH, BURUH_DTO.label(), BURUH_DTO.description(), 20_000);
        when(commandUseCase.updateVariabelPokok(VariableKey.UPAH_BURUH, 20_000)).thenReturn(updated);

        UpdateVariabelPokokRequest body = new UpdateVariabelPokokRequest(VariableKey.UPAH_BURUH, 20_000);

        mockMvc.perform(put("/api/pembayaran/variabel-pokok/UPAH_BURUH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value(20_000));
    }

    @Test
    void update_keyMismatch_returns400() throws Exception {
        UpdateVariabelPokokRequest body = new UpdateVariabelPokokRequest(VariableKey.UPAH_SUPIR, 20_000);

        mockMvc.perform(put("/api/pembayaran/variabel-pokok/UPAH_BURUH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_negativeValue_delegatesToServiceDomainValidation() throws Exception {
        // @Positive constraint on UpdateVariabelPokokRequest.newValue rejects -100
        // before the service is called, so no stub is needed here.
        UpdateVariabelPokokRequest body = new UpdateVariabelPokokRequest(VariableKey.UPAH_BURUH, -100);

        mockMvc.perform(put("/api/pembayaran/variabel-pokok/UPAH_BURUH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_nullKeyInBody_stillSucceeds() {
        // When request.key() is null, the condition short-circuits and no mismatch error occurs
        // This exercises the request.key() != null branch directly (bypassing @Valid to reach the code)
        VariabelPokokDTO updated = new VariabelPokokDTO(
                VariableKey.UPAH_BURUH, BURUH_DTO.label(), BURUH_DTO.description(), 25_000);
        when(commandUseCase.updateVariabelPokok(VariableKey.UPAH_BURUH, 25_000)).thenReturn(updated);

        VariabelPokokController controller = new VariabelPokokController(queryUseCase, commandUseCase);
        UpdateVariabelPokokRequest body = new UpdateVariabelPokokRequest(null, 25_000);

        var response = controller.update(VariableKey.UPAH_BURUH, body);
        
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals(25_000, response.getBody().data().value());
    }
}
