package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.exception.GlobalExceptionHandler;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignedSupirDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignablePanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.exception.KebunQueryDependencyUnavailableException;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PengirimanControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PengirimanQueryUseCase queryUseCase;

    @Mock
    private PengirimanCommandUseCase commandUseCase;

    @InjectMocks
    private PengirimanController controller;

    private UUID supirId;
    private PengirimanDTO sample;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        supirId = UUID.randomUUID();
        sample = new PengirimanDTO(
                UUID.randomUUID(),
                supirId,
                UUID.randomUUID(),
                "ASSIGNED",
                140000,
                0,
                LocalDateTime.of(2026, 2, 25, 8, 30, 0)
        );
    }

    @Test
    void listDeliveriesBySupir_returns200WithData() throws Exception {
        when(queryUseCase.listDeliveriesBySupir(eq(supirId), isNull(), isNull()))
                .thenReturn(List.of(sample));

        mockMvc.perform(get("/api/pengiriman/supir")
                        .requestAttr("userId", supirId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].supirId").value(supirId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("ASSIGNED"));

        verify(queryUseCase).listDeliveriesBySupir(supirId, null, null);
    }

    @Test
    void listDeliveriesBySupir_invalidDateRange_returns400() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 1);
        when(queryUseCase.listDeliveriesBySupir(supirId, startDate, endDate))
                .thenThrow(new IllegalArgumentException("End date cannot be before start date"));

        mockMvc.perform(get("/api/pengiriman/supir")
                        .requestAttr("userId", supirId)
                        .param("startDate", "2026-02-10")
                        .param("endDate", "2026-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("End date cannot be before start date"));
    }

    @Test
    void listDeliveriesBySupir_withDateFilter_returnsRejectedReason() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 4, 10);
        LocalDate endDate = LocalDate.of(2026, 4, 12);
        when(queryUseCase.listDeliveriesBySupir(supirId, startDate, endDate))
                .thenReturn(List.of(new PengirimanDTO(
                        UUID.randomUUID(),
                        supirId,
                        null,
                        UUID.randomUUID(),
                        null,
                        "REJECTED_MANDOR",
                        140000,
                        0,
                        "Muatan rusak",
                        List.of(UUID.randomUUID()),
                        LocalDateTime.of(2026, 4, 12, 9, 30)
                )));

        mockMvc.perform(get("/api/pengiriman/supir")
                        .requestAttr("userId", supirId)
                        .param("startDate", "2026-04-10")
                        .param("endDate", "2026-04-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("REJECTED_MANDOR"))
                .andExpect(jsonPath("$.data[0].statusReason").value("Muatan rusak"));
    }

    @Test
    void listAssignedSupirForMandor_returns200WithFilteredData() throws Exception {
        UUID mandorId = UUID.randomUUID();
        AssignedSupirDTO supir = new AssignedSupirDTO(
                UUID.randomUUID(),
                "ega",
                "Ega Jawa",
                "ega@example.com"
        );

        when(queryUseCase.listAssignedSupirForMandor(mandorId, "ega"))
                .thenReturn(List.of(supir));

        mockMvc.perform(get("/api/pengiriman/mandor/supir")
                        .requestAttr("userId", mandorId)
                        .param("searchNama", "ega"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].supirId").value(supir.supirId().toString()))
                .andExpect(jsonPath("$.data[0].name").value("Ega Jawa"));

        verify(queryUseCase).listAssignedSupirForMandor(mandorId, "ega");
    }

    @Test
    void listAssignedSupirForMandor_dependencyUnavailable_returns503() throws Exception {
        UUID mandorId = UUID.randomUUID();

        when(queryUseCase.listAssignedSupirForMandor(mandorId, null))
                .thenThrow(new KebunQueryDependencyUnavailableException(
                        "Kebun query dependency is unavailable. Integrasi modul kebun belum siap."
                ));

        mockMvc.perform(get("/api/pengiriman/mandor/supir")
                        .requestAttr("userId", mandorId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Kebun query dependency is unavailable. Integrasi modul kebun belum siap."
                ));
    }

    @Test
    void assignSupirForDelivery_returns201WithCreatedDelivery() throws Exception {
        UUID mandorId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();
        UUID panenA = UUID.randomUUID();

        when(commandUseCase.assignSupirForDelivery(mandorId, supirId, List.of(panenA)))
                .thenReturn(new PengirimanDTO(
                        UUID.randomUUID(),
                        supirId,
                        null,
                        mandorId,
                        null,
                        "ASSIGNED",
                        180000,
                        0,
                        null,
                        List.of(panenA),
                        LocalDateTime.of(2026, 4, 12, 10, 0)
                ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/pengiriman")
                        .requestAttr("userId", mandorId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "supirId": "%s",
                                  "panenIds": ["%s"]
                                }
                                """.formatted(supirId, panenA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.totalWeight").value(180000));
    }

    @Test
    void listAssignablePanenForMandor_returnsApprovedPanenOptions() throws Exception {
        UUID mandorId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();

        when(queryUseCase.listAssignablePanenForMandor(mandorId))
                .thenReturn(List.of(new AssignablePanenDTO(
                        panenId,
                        UUID.randomUUID(),
                        "Buruh A",
                        "Panen pagi",
                        175000,
                        LocalDateTime.of(2026, 4, 12, 8, 30)
                )));

        mockMvc.perform(get("/api/pengiriman/mandor/panen")
                        .requestAttr("userId", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].panenId").value(panenId.toString()))
                .andExpect(jsonPath("$.data[0].weight").value(175000));
    }

    @Test
    void listActiveDeliveriesByMandor_returnsActiveDeliveries() throws Exception {
        UUID mandorId = UUID.randomUUID();

        when(queryUseCase.listActiveDeliveriesByMandor(mandorId))
                .thenReturn(List.of(new PengirimanDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        mandorId,
                        null,
                        "IN_TRANSIT",
                        150000,
                        0,
                        null,
                        List.of(UUID.randomUUID()),
                        LocalDateTime.of(2026, 4, 12, 11, 30)
                )));

        mockMvc.perform(get("/api/pengiriman/mandor/active")
                        .requestAttr("userId", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.data[0].mandorId").value(mandorId.toString()));
    }

    @Test
    void listDeliveriesOfSupirByMandor_returnsSupirHistory() throws Exception {
        UUID mandorId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();

        when(queryUseCase.listDeliveriesOfSupirByMandor(mandorId, supirId))
                .thenReturn(List.of(new PengirimanDTO(
                        UUID.randomUUID(),
                        supirId,
                        null,
                        mandorId,
                        null,
                        "TIBA",
                        160000,
                        0,
                        null,
                        List.of(UUID.randomUUID()),
                        LocalDateTime.of(2026, 4, 12, 12, 45)
                )));

        mockMvc.perform(get("/api/pengiriman/supir/{supirId}/mandor", supirId)
                        .requestAttr("userId", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].supirId").value(supirId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("TIBA"));
    }

    @Test
    void mandorApproveDelivery_returnsApprovedStatus() throws Exception {
        UUID mandorId = UUID.randomUUID();
        UUID pengirimanId = UUID.randomUUID();

        when(commandUseCase.mandorApproveDelivery(pengirimanId, mandorId))
                .thenReturn(new PengirimanDTO(
                        pengirimanId,
                        UUID.randomUUID(),
                        null,
                        mandorId,
                        null,
                        "APPROVED_MANDOR",
                        200000,
                        0,
                        null,
                        List.of(UUID.randomUUID()),
                        LocalDateTime.of(2026, 4, 12, 13, 0)
                ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/pengiriman/{pengirimanId}/approve", pengirimanId)
                        .requestAttr("userId", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED_MANDOR"));
    }

    @Test
    void mandorRejectDelivery_returnsRejectedStatusWithReason() throws Exception {
        UUID mandorId = UUID.randomUUID();
        UUID pengirimanId = UUID.randomUUID();

        when(commandUseCase.mandorRejectDelivery(pengirimanId, mandorId, "Muatan rusak"))
                .thenReturn(new PengirimanDTO(
                        pengirimanId,
                        UUID.randomUUID(),
                        null,
                        mandorId,
                        null,
                        "REJECTED_MANDOR",
                        200000,
                        0,
                        "Muatan rusak",
                        List.of(UUID.randomUUID()),
                        LocalDateTime.of(2026, 4, 12, 13, 15)
                ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/pengiriman/{pengirimanId}/reject", pengirimanId)
                        .requestAttr("userId", mandorId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "reason": "Muatan rusak"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED_MANDOR"))
                .andExpect(jsonPath("$.data.statusReason").value("Muatan rusak"));
    }

    @Test
    void updateDeliveryStatus_returns200WithUpdatedStatus() throws Exception {
        UUID pengirimanId = UUID.randomUUID();

        when(commandUseCase.updateDeliveryStatus(pengirimanId, supirId, id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.domain.PengirimanStatus.TIBA))
                .thenReturn(new PengirimanDTO(
                        pengirimanId,
                        supirId,
                        UUID.randomUUID(),
                        "TIBA",
                        140000,
                        0,
                        LocalDateTime.of(2026, 4, 13, 11, 0)
                ));

        mockMvc.perform(put("/api/pengiriman/{pengirimanId}/status", pengirimanId)
                        .requestAttr("userId", supirId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "newStatus": "TIBA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TIBA"));
    }

    @Test
    void listApprovedDeliveriesForAdmin_returnsFilteredResult() throws Exception {
        LocalDate date = LocalDate.of(2026, 4, 14);
        UUID mandorId = UUID.randomUUID();

        when(queryUseCase.listApprovedDeliveriesForAdmin("Awan", date))
                .thenReturn(List.of(new PengirimanDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Supir A",
                        mandorId,
                        "Awan Mandor",
                        "APPROVED_MANDOR",
                        210000,
                        0,
                        null,
                        List.of(UUID.randomUUID()),
                        LocalDateTime.of(2026, 4, 14, 14, 0)
                )));

        mockMvc.perform(get("/api/pengiriman/admin/approved")
                        .param("mandorName", "Awan")
                        .param("date", "2026-04-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mandorName").value("Awan Mandor"))
                .andExpect(jsonPath("$.data[0].status").value("APPROVED_MANDOR"));
    }
}
