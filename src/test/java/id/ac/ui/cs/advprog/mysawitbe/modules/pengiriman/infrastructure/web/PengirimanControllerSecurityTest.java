package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.exception.GlobalExceptionHandler;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignedSupirDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PengirimanControllerSecurityTest.TestConfig.class)
class PengirimanControllerSecurityTest {

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {

        @Bean
        PengirimanQueryUseCase pengirimanQueryUseCase() {
            return mock(PengirimanQueryUseCase.class);
        }

        @Bean
        PengirimanCommandUseCase pengirimanCommandUseCase() {
            return mock(PengirimanCommandUseCase.class);
        }

        @Bean
        PengirimanController pengirimanController(
                PengirimanQueryUseCase pengirimanQueryUseCase,
                PengirimanCommandUseCase pengirimanCommandUseCase
        ) {
            return new PengirimanController(pengirimanQueryUseCase, pengirimanCommandUseCase);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private PengirimanController pengirimanController;

    @org.springframework.beans.factory.annotation.Autowired
    private PengirimanQueryUseCase pengirimanQueryUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pengirimanController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @WithMockUser(
            username = "00000000-0000-0000-0000-000000000010",
            roles = "MANDOR"
    )
    void listAssignedSupirForMandor_withMandorAuthenticationAndMatchingRequestAttr_returns200() throws Exception {
        UUID mandorId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        AssignedSupirDTO supir = new AssignedSupirDTO(
                UUID.randomUUID(),
                "ega",
                "Ega Jawa",
                "ega@example.com"
        );

        when(pengirimanQueryUseCase.listAssignedSupirForMandor(mandorId, "ega"))
                .thenReturn(List.of(supir));

        mockMvc.perform(get("/api/pengiriman/mandor/supir")
                        .requestAttr("userId", mandorId)
                        .param("searchNama", "ega"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].supirId").value(supir.supirId().toString()))
                .andExpect(jsonPath("$.data[0].name").value("Ega Jawa"));

        verify(pengirimanQueryUseCase).listAssignedSupirForMandor(mandorId, "ega");
    }

    @Test
    @WithMockUser(
            username = "00000000-0000-0000-0000-000000000010",
            roles = "MANDOR"
    )
    void listActiveDeliveriesByMandor_withMandorAuthentication_returns200() throws Exception {
        UUID mandorId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        when(pengirimanQueryUseCase.listActiveDeliveriesByMandor(mandorId))
                .thenReturn(List.of(new id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        mandorId,
                        "ASSIGNED",
                        100000,
                        0,
                        java.time.LocalDateTime.now()
                )));

        mockMvc.perform(get("/api/pengiriman/mandor/active")
                        .requestAttr("userId", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ASSIGNED"));
    }

    @Test
    @WithMockUser(
            username = "00000000-0000-0000-0000-000000000010",
            roles = "MANDOR"
    )
    void listDeliveriesOfSupirByMandor_withMandorAuthentication_returns200() throws Exception {
        UUID mandorId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID supirId = UUID.randomUUID();

        when(pengirimanQueryUseCase.listDeliveriesOfSupirByMandor(mandorId, supirId))
                .thenReturn(List.of(new id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO(
                        UUID.randomUUID(),
                        supirId,
                        mandorId,
                        "TIBA",
                        100000,
                        0,
                        java.time.LocalDateTime.now()
                )));

        mockMvc.perform(get("/api/pengiriman/supir/{supirId}/mandor", supirId)
                        .requestAttr("userId", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].supirId").value(supirId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("TIBA"));
    }
}
