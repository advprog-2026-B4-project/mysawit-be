package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanApprovedByMandorEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanStatusTibaEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out.PengirimanRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.domain.PengirimanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PengirimanCommandUseCaseImplTest {

    @Mock
    private PengirimanRepositoryPort repository;

    @Mock
    private KebunQueryUseCase kebunQueryUseCase;

    @Mock
    private PanenQueryUseCase panenQueryUseCase;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PengirimanCommandUseCaseImpl service;

    private UUID mandorId;
    private UUID supirId;
    private UUID kebunId;

    @BeforeEach
    void setUp() {
        mandorId = UUID.randomUUID();
        supirId = UUID.randomUUID();
        kebunId = UUID.randomUUID();
    }

    @Test
    void assignSupirForDelivery_savesAssignmentWithoutPublishingApprovalEvent() {
        UUID panenA = UUID.randomUUID();
        UUID panenB = UUID.randomUUID();

        when(kebunQueryUseCase.getSupirListByMandorId(mandorId)).thenReturn(List.of(
                new UserDTO(supirId, "supir-a", "Supir A", "SUPIR", "supir@example.com")
        ));
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(
                new PanenDTO(panenA, UUID.randomUUID(), "Buruh A", kebunId, "Panen A", 180000, "APPROVED", null, List.of(), LocalDateTime.now()),
                new PanenDTO(panenB, UUID.randomUUID(), "Buruh B", kebunId, "Panen B", 120000, "APPROVED", null, List.of(), LocalDateTime.now())
        ));
        when(repository.findAssignedPanenIds(List.of(panenA, panenB))).thenReturn(List.of());
        when(repository.save(any(PengirimanDTO.class))).thenAnswer(invocation -> {
            PengirimanDTO request = invocation.getArgument(0);
            return new PengirimanDTO(
                    UUID.randomUUID(),
                    request.supirId(),
                    request.supirName(),
                    request.mandorId(),
                    request.mandorName(),
                    request.status(),
                    request.totalWeight(),
                    request.acceptedWeight(),
                    request.statusReason(),
                    request.panenIds(),
                    request.timestamp()
            );
        });

        PengirimanDTO result = service.assignSupirForDelivery(mandorId, supirId, List.of(panenA, panenB));

        assertThat(result.status()).isEqualTo(PengirimanStatus.ASSIGNED.name());
        assertThat(result.totalWeight()).isEqualTo(300000);
        assertThat(result.panenIds()).containsExactly(panenA, panenB);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void assignSupirForDelivery_overMaxWeight_throwsIllegalArgumentException() {
        UUID panenA = UUID.randomUUID();
        UUID panenB = UUID.randomUUID();

        when(kebunQueryUseCase.getSupirListByMandorId(mandorId)).thenReturn(List.of(
                new UserDTO(supirId, "supir-a", "Supir A", "SUPIR", "supir@example.com")
        ));
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(
                new PanenDTO(panenA, UUID.randomUUID(), "Buruh A", kebunId, "Panen A", 250000, "APPROVED", null, List.of(), LocalDateTime.now()),
                new PanenDTO(panenB, UUID.randomUUID(), "Buruh B", kebunId, "Panen B", 200000, "APPROVED", null, List.of(), LocalDateTime.now())
        ));
        when(repository.findAssignedPanenIds(List.of(panenA, panenB))).thenReturn(List.of());

        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, supirId, List.of(panenA, panenB)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("400 kg");
    }

    @Test
    void mandorApproveDelivery_fromTiba_updatesStatusAndPublishesEvent() {
        UUID pengirimanId = UUID.randomUUID();
        PengirimanDTO current = new PengirimanDTO(
                pengirimanId,
                supirId,
                null,
                mandorId,
                null,
                PengirimanStatus.TIBA.name(),
                250000,
                0,
                null,
                List.of(UUID.randomUUID()),
                LocalDateTime.now()
        );

        when(repository.findById(pengirimanId)).thenReturn(current);
        when(repository.save(any(PengirimanDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PengirimanDTO result = service.mandorApproveDelivery(pengirimanId, mandorId);

        assertThat(result.status()).isEqualTo(PengirimanStatus.APPROVED_MANDOR.name());
        verify(eventPublisher).publishEvent(any(PengirimanApprovedByMandorEvent.class));
    }

    @Test
    void mandorRejectDelivery_withoutReason_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();

        assertThatThrownBy(() -> service.mandorRejectDelivery(pengirimanId, mandorId, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reason is required");
    }

    @Test
    void updateDeliveryStatus_toTiba_publishesArrivalEvent() {
        UUID pengirimanId = UUID.randomUUID();
        PengirimanDTO current = new PengirimanDTO(
                pengirimanId,
                supirId,
                null,
                mandorId,
                null,
                PengirimanStatus.IN_TRANSIT.name(),
                250000,
                0,
                null,
                List.of(UUID.randomUUID()),
                LocalDateTime.now()
        );

        when(repository.findById(pengirimanId)).thenReturn(current);
        when(repository.save(any(PengirimanDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PengirimanDTO result = service.updateDeliveryStatus(pengirimanId, supirId, PengirimanStatus.TIBA);

        assertThat(result.status()).isEqualTo(PengirimanStatus.TIBA.name());
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(PengirimanStatusTibaEvent.class);
    }

    @Test
    void adminProcessDelivery_reject_updatesDeliveryAndPublishesNormalizedEvent() {
        UUID pengirimanId = UUID.randomUUID();
        PengirimanDTO current = new PengirimanDTO(
                pengirimanId,
                supirId,
                null,
                mandorId,
                null,
                PengirimanStatus.APPROVED_MANDOR.name(),
                250000,
                0,
                null,
                List.of(UUID.randomUUID()),
                LocalDateTime.now()
        );

        when(repository.findById(pengirimanId)).thenReturn(current);
        when(repository.save(any(PengirimanDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PengirimanDTO result = service.adminProcessDelivery(
                pengirimanId,
                UUID.randomUUID(),
                0,
                PengirimanStatus.REJECTED_ADMIN,
                "Ditolak admin"
        );

        assertThat(result.status()).isEqualTo(PengirimanStatus.REJECTED_ADMIN.name());
        assertThat(result.acceptedWeight()).isZero();
        assertThat(result.statusReason()).isEqualTo("Ditolak admin");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(PengirimanProcessedByAdminEvent.class, event ->
                        assertThat(event.status()).isEqualTo("REJECTED"));
    }
}
