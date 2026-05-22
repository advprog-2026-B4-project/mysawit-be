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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import id.ac.ui.cs.advprog.mysawitbe.common.port.DomainEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private DomainEventPublisher eventPublisher;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private PengirimanCommandUseCaseImpl service;

    private UUID mandorId;
    private UUID supirId;
    private UUID kebunId;

    @BeforeEach
    void setUp() {
        service = new PengirimanCommandUseCaseImpl(repository, kebunQueryUseCase, panenQueryUseCase, eventPublisher, meterRegistry);
        service.initMetrics();
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
        verify(eventPublisher, never()).publish(any());
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
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(PengirimanStatusTibaEvent.class);
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
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(PengirimanApprovedByMandorEvent.class);
    }

    @Test
    void mandorRejectDelivery_withoutReason_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        assertThatThrownBy(() -> service.mandorRejectDelivery(pengirimanId, mandorId, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reason is required");
    }

    @Test
    void adminProcessDelivery_partialAccept_updatesDeliveryAndPublishesNormalizedEvent() {
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
                175000,
                PengirimanStatus.PARTIAL,
                "Sebagian sawit rusak"
        );

        assertThat(result.status()).isEqualTo(PengirimanStatus.PARTIAL.name());
        assertThat(result.acceptedWeight()).isEqualTo(175000);
        assertThat(result.statusReason()).isEqualTo("Sebagian sawit rusak");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(PengirimanProcessedByAdminEvent.class, event ->
                        assertThat(event.status()).isEqualTo("PARTIAL"));
    }

    @Test
    void assignSupirForDelivery_withNullMandor_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.assignSupirForDelivery(null, supirId, List.of(UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mandor ID is required");
    }

    @Test
    void assignSupirForDelivery_withNullSupir_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, null, List.of(UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Supir ID is required");
    }

    @Test
    void assignSupirForDelivery_withMissingPanenList_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, supirId, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Panen IDs are required");
    }

    @Test
    void assignSupirForDelivery_withNullPanenList_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, supirId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Panen IDs are required");
    }

    @Test
    void assignSupirForDelivery_withNullPanenId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, supirId, java.util.Arrays.asList(UUID.randomUUID(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Panen ID cannot be null.");
    }

    @Test
    void assignSupirForDelivery_whenSupirNotInMandorKebun_throwsIllegalArgumentException() {
        when(kebunQueryUseCase.getSupirListByMandorId(mandorId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, supirId, List.of(UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Supir tidak terdaftar di kebun mandor ini.");
    }

    @Test
    void assignSupirForDelivery_whenMandorHasNoKebun_throwsIllegalStateException() {
        UUID panenId = UUID.randomUUID();
        stubSupirAssignedToMandor();
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(null);

        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, supirId, List.of(panenId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mandor belum memiliki kebun.");
    }

    @Test
    void assignSupirForDelivery_whenPanenNotApprovedForKebun_throwsIllegalArgumentException() {
        UUID panenId = UUID.randomUUID();
        stubSupirAssignedToMandor();
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, supirId, List.of(panenId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Semua panen harus sudah disetujui dan berasal dari kebun mandor.");
    }

    @Test
    void assignSupirForDelivery_whenPanenAlreadyAssigned_throwsIllegalStateException() {
        UUID panenId = UUID.randomUUID();
        stubSupirAssignedToMandor();
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(approvedPanen(panenId, 100000)));
        when(repository.findAssignedPanenIds(List.of(panenId))).thenReturn(List.of(panenId));

        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, supirId, List.of(panenId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Sebagian panen sudah pernah dimasukkan ke pengiriman lain.");
    }

    @Test
    void assignSupirForDelivery_withZeroWeight_throwsIllegalArgumentException() {
        UUID panenId = UUID.randomUUID();
        stubSupirAssignedToMandor();
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(approvedPanen(panenId, 0)));
        when(repository.findAssignedPanenIds(List.of(panenId))).thenReturn(List.of());

        assertThatThrownBy(() -> service.assignSupirForDelivery(mandorId, supirId, List.of(panenId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total berat pengiriman harus lebih dari 0.");
    }

    @Test
    void assignSupirForDelivery_deduplicatesPanenIds() {
        UUID panenId = UUID.randomUUID();
        stubSupirAssignedToMandor();
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(approvedPanen(panenId, 100000)));
        when(repository.findAssignedPanenIds(List.of(panenId))).thenReturn(List.of());
        stubSaveReturnsInputWithGeneratedId();

        PengirimanDTO result = service.assignSupirForDelivery(mandorId, supirId, List.of(panenId, panenId));

        assertThat(result.panenIds()).containsExactly(panenId);
    }

    @Test
    void updateDeliveryStatus_toInTransit_fromAssigned_doesNotPublishArrivalEvent() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.ASSIGNED, 0, null));
        when(repository.save(any(PengirimanDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PengirimanDTO result = service.updateDeliveryStatus(pengirimanId, supirId, PengirimanStatus.IN_TRANSIT);

        assertThat(result.status()).isEqualTo(PengirimanStatus.IN_TRANSIT.name());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateDeliveryStatus_withNullStatus_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.updateDeliveryStatus(UUID.randomUUID(), supirId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New status is required");
    }

    @Test
    void updateDeliveryStatus_notFound_throwsEntityNotFoundException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(null);

        assertThatThrownBy(() -> service.updateDeliveryStatus(pengirimanId, supirId, PengirimanStatus.TIBA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Pengiriman not found");
    }

    @Test
    void updateDeliveryStatus_wrongSupir_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.ASSIGNED, 0, null));

        assertThatThrownBy(() -> service.updateDeliveryStatus(pengirimanId, UUID.randomUUID(), PengirimanStatus.IN_TRANSIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Supir tidak berhak mengubah pengiriman ini.");
    }

    @Test
    void updateDeliveryStatus_nullSupir_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.ASSIGNED, 0, null));

        assertThatThrownBy(() -> service.updateDeliveryStatus(pengirimanId, null, PengirimanStatus.IN_TRANSIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Supir tidak berhak mengubah pengiriman ini.");
    }

    @Test
    void updateDeliveryStatus_inTransitFromWrongStatus_throwsIllegalStateException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.TIBA, 0, null));

        assertThatThrownBy(() -> service.updateDeliveryStatus(pengirimanId, supirId, PengirimanStatus.IN_TRANSIT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Pengiriman hanya bisa dimulai dari status ASSIGNED.");
    }

    @Test
    void updateDeliveryStatus_tibaFromWrongStatus_throwsIllegalStateException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.ASSIGNED, 0, null));

        assertThatThrownBy(() -> service.updateDeliveryStatus(pengirimanId, supirId, PengirimanStatus.TIBA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Pengiriman hanya bisa ditandai tiba dari status IN_TRANSIT.");
    }

    @Test
    void updateDeliveryStatus_unsupportedStatus_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.TIBA, 0, null));

        assertThatThrownBy(() -> service.updateDeliveryStatus(pengirimanId, supirId, PengirimanStatus.APPROVED_ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Supir hanya boleh mengubah status ke IN_TRANSIT atau TIBA.");
    }

    @Test
    void updateDeliveryStatus_unknownCurrentStatus_throwsIllegalStateException() {
        UUID pengirimanId = UUID.randomUUID();
        PengirimanDTO current = new PengirimanDTO(
                pengirimanId, supirId, null, mandorId, null, "UNKNOWN", 100000, 0, null, List.of(), LocalDateTime.now());
        when(repository.findById(pengirimanId)).thenReturn(current);

        assertThatThrownBy(() -> service.updateDeliveryStatus(pengirimanId, supirId, PengirimanStatus.IN_TRANSIT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown pengiriman status: UNKNOWN");
    }

    @Test
    void mandorApproveDelivery_wrongMandor_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.TIBA, 0, null));

        assertThatThrownBy(() -> service.mandorApproveDelivery(pengirimanId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mandor tidak berhak memproses pengiriman ini.");
    }

    @Test
    void mandorApproveDelivery_nullMandor_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.TIBA, 0, null));

        assertThatThrownBy(() -> service.mandorApproveDelivery(pengirimanId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mandor tidak berhak memproses pengiriman ini.");
    }

    @Test
    void mandorApproveDelivery_wrongStatus_throwsIllegalStateException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.IN_TRANSIT, 0, null));

        assertThatThrownBy(() -> service.mandorApproveDelivery(pengirimanId, mandorId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mandor hanya bisa menyetujui pengiriman yang sudah TIBA.");
    }

    @Test
    void mandorRejectDelivery_fromTiba_savesReason() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.TIBA, 0, null));
        when(repository.save(any(PengirimanDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PengirimanDTO result = service.mandorRejectDelivery(pengirimanId, mandorId, "  Tidak lengkap  ");

        assertThat(result.status()).isEqualTo(PengirimanStatus.REJECTED_MANDOR.name());
        assertThat(result.statusReason()).isEqualTo("Tidak lengkap");
    }

    @Test
    void mandorRejectDelivery_wrongStatus_throwsIllegalStateException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.ASSIGNED, 0, null));

        assertThatThrownBy(() -> service.mandorRejectDelivery(pengirimanId, mandorId, "Ditolak"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mandor hanya bisa menolak pengiriman yang sudah TIBA.");
    }

    @Test
    void adminProcessDelivery_withNullAdmin_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.adminProcessDelivery(UUID.randomUUID(), null, 0, PengirimanStatus.REJECTED_ADMIN, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Admin ID is required");
    }

    @Test
    void adminProcessDelivery_withNullStatus_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.adminProcessDelivery(UUID.randomUUID(), UUID.randomUUID(), 0, null, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status is required");
    }

    @Test
    void adminProcessDelivery_withNegativeWeight_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.adminProcessDelivery(UUID.randomUUID(), UUID.randomUUID(), -1, PengirimanStatus.REJECTED_ADMIN, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Accepted weight cannot be negative.");
    }

    @Test
    void adminProcessDelivery_wrongCurrentStatus_throwsIllegalStateException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.TIBA, 0, null));

        assertThatThrownBy(() -> service.adminProcessDelivery(pengirimanId, UUID.randomUUID(), 0, PengirimanStatus.REJECTED_ADMIN, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Admin hanya bisa memproses pengiriman yang sudah disetujui mandor.");
    }

    @Test
    void adminProcessDelivery_fullApprove_savesAndPublishesApprovedEvent() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));
        when(repository.save(any(PengirimanDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PengirimanDTO result = service.adminProcessDelivery(
                pengirimanId, UUID.randomUUID(), 250000, PengirimanStatus.APPROVED_ADMIN, null);

        assertThat(result.status()).isEqualTo(PengirimanStatus.APPROVED_ADMIN.name());
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(PengirimanProcessedByAdminEvent.class, event ->
                        assertThat(event.status()).isEqualTo("APPROVED"));
    }

    @Test
    void adminProcessDelivery_fullApproveWrongWeight_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));

        assertThatThrownBy(() -> service.adminProcessDelivery(pengirimanId, UUID.randomUUID(), 249999, PengirimanStatus.APPROVED_ADMIN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full approve harus menerima seluruh berat pengiriman.");
    }

    @Test
    void adminProcessDelivery_fullApproveWithReason_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));

        assertThatThrownBy(() -> service.adminProcessDelivery(pengirimanId, UUID.randomUUID(), 250000, PengirimanStatus.APPROVED_ADMIN, "Tidak boleh"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full approve tidak memerlukan alasan.");
    }

    @Test
    void adminProcessDelivery_partialInvalidWeight_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));

        assertThatThrownBy(() -> service.adminProcessDelivery(pengirimanId, UUID.randomUUID(), 250000, PengirimanStatus.PARTIAL, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Partial accept harus menerima sebagian berat di antara 0 dan total pengiriman.");
    }

    @Test
    void adminProcessDelivery_partialZeroWeight_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));

        assertThatThrownBy(() -> service.adminProcessDelivery(pengirimanId, UUID.randomUUID(), 0, PengirimanStatus.PARTIAL, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Partial accept harus menerima sebagian berat di antara 0 dan total pengiriman.");
    }

    @Test
    void adminProcessDelivery_partialWithoutReason_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));

        assertThatThrownBy(() -> service.adminProcessDelivery(pengirimanId, UUID.randomUUID(), 100000, PengirimanStatus.PARTIAL, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alasan wajib diisi untuk partial accept.");
    }

    @Test
    void adminProcessDelivery_reject_savesAndPublishesRejectedEvent() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));
        when(repository.save(any(PengirimanDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PengirimanDTO result = service.adminProcessDelivery(
                pengirimanId, UUID.randomUUID(), 0, PengirimanStatus.REJECTED_ADMIN, "  Rusak  ");

        assertThat(result.status()).isEqualTo(PengirimanStatus.REJECTED_ADMIN.name());
        assertThat(result.statusReason()).isEqualTo("Rusak");
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(PengirimanProcessedByAdminEvent.class, event ->
                        assertThat(event.status()).isEqualTo("REJECTED"));
    }

    @Test
    void adminProcessDelivery_rejectWithAcceptedWeight_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));

        assertThatThrownBy(() -> service.adminProcessDelivery(pengirimanId, UUID.randomUUID(), 1, PengirimanStatus.REJECTED_ADMIN, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rejected delivery harus memiliki accepted weight 0.");
    }

    @Test
    void adminProcessDelivery_rejectWithoutReason_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));

        assertThatThrownBy(() -> service.adminProcessDelivery(pengirimanId, UUID.randomUUID(), 0, PengirimanStatus.REJECTED_ADMIN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alasan penolakan admin wajib diisi.");
    }

    @Test
    void adminProcessDelivery_withNonAdminStatus_throwsIllegalArgumentException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(delivery(pengirimanId, PengirimanStatus.APPROVED_MANDOR, 0, null));

        assertThatThrownBy(() -> service.adminProcessDelivery(pengirimanId, UUID.randomUUID(), 0, PengirimanStatus.TIBA, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status admin tidak valid.");
    }

    @Test
    void toEventStatus_defaultBranchIsCovered() throws Exception {
        var method = PengirimanCommandUseCaseImpl.class.getDeclaredMethod("toEventStatus", PengirimanStatus.class);
        method.setAccessible(true);

        assertThat(method.invoke(service, PengirimanStatus.TIBA)).isEqualTo("TIBA");
    }

    private void stubSupirAssignedToMandor() {
        when(kebunQueryUseCase.getSupirListByMandorId(mandorId)).thenReturn(List.of(
                new UserDTO(supirId, "supir-a", "Supir A", "SUPIR", "supir@example.com")
        ));
    }

    private PanenDTO approvedPanen(UUID panenId, int weight) {
        return new PanenDTO(
                panenId,
                UUID.randomUUID(),
                "Buruh A",
                kebunId,
                "Panen",
                weight,
                "APPROVED",
                null,
                List.of(),
                LocalDateTime.now()
        );
    }

    private PengirimanDTO delivery(UUID pengirimanId, PengirimanStatus status, int acceptedWeight, String reason) {
        return new PengirimanDTO(
                pengirimanId,
                supirId,
                null,
                mandorId,
                null,
                status.name(),
                250000,
                acceptedWeight,
                reason,
                List.of(UUID.randomUUID()),
                LocalDateTime.now()
        );
    }

    private void stubSaveReturnsInputWithGeneratedId() {
        when(repository.save(any(PengirimanDTO.class))).thenAnswer(invocation -> {
            PengirimanDTO request = invocation.getArgument(0);
            return new PengirimanDTO(
                    request.pengirimanId() == null ? UUID.randomUUID() : request.pengirimanId(),
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
    }
}
