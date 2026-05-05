package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenRejectedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenMapperPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.Panen;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenStatus;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class PanenCommandImplTest {

    @Mock private PanenRepositoryPort repositoryPort;
    @Mock private PanenMapperPort mapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserQueryUseCase userQueryUseCase;
    @Mock private KebunQueryUseCase kebunQueryUseCase;

    @InjectMocks
    private PanenCommandImpl panenCommandImpl;

    // Shared fixtures
    private UUID buruhId;
    private UUID mandorId;
    private UUID kebunId;
    private UUID panenId;
    private UserDTO buruhDTO;

    @BeforeEach
    void setUp() {
        buruhId  = UUID.randomUUID();
        mandorId = UUID.randomUUID();
        kebunId  = UUID.randomUUID();
        panenId  = UUID.randomUUID();
        buruhDTO = new UserDTO(buruhId, "Budi", "BUDIGAMING", "BURUH", "buruh@example.com");
    }

    // =========================================================================
    // createPanen()
    // =========================================================================
    @Nested
    class CreatePanen {

        @Test
        void shouldThrowWhenDailyLimitReached() {
            when(repositoryPort.existsByBuruhIdAndDate(eq(buruhId), any(LocalDate.class)))
                    .thenReturn(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> panenCommandImpl.createPanen(buruhId, "desc", 100, List.of()));

            assertEquals("Pencatatan gagal: Batas harian tercapai (maksimal 1 kali sehari).",
                    ex.getMessage());
            verify(repositoryPort, never()).save(any());
        }

        @Test
        void shouldThrowWhenBuruhHasNoMandor() {
            when(repositoryPort.existsByBuruhIdAndDate(eq(buruhId), any(LocalDate.class)))
                    .thenReturn(false);
            when(userQueryUseCase.getMandorIdByBuruhId(buruhId)).thenReturn(null);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> panenCommandImpl.createPanen(buruhId, "desc", 100, List.of()));

            assertEquals("Buruh belum memiliki Mandor!", ex.getMessage());
            verify(repositoryPort, never()).save(any());
        }

        @Test
        void shouldThrowWhenMandorHasNoKebun() {
            when(repositoryPort.existsByBuruhIdAndDate(eq(buruhId), any(LocalDate.class)))
                    .thenReturn(false);
            when(userQueryUseCase.getMandorIdByBuruhId(buruhId)).thenReturn(mandorId);
            when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(null);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> panenCommandImpl.createPanen(buruhId, "desc", 100, List.of()));

            assertEquals("Mandor belum di-assign ke kebun manapun!", ex.getMessage());
            verify(repositoryPort, never()).save(any());
        }

        @Test
        void shouldCreateAndSavePanenWhenAllValid() {
            List<String> photoUrls = List.of("https://cdn.example.com/foto.jpg");
            PanenDTO savedDTO = buildPanenDTO(panenId, buruhId, kebunId, "PENDING", null);

            when(repositoryPort.existsByBuruhIdAndDate(eq(buruhId), any(LocalDate.class)))
                    .thenReturn(false);
            when(userQueryUseCase.getMandorIdByBuruhId(buruhId)).thenReturn(mandorId);
            when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
            when(userQueryUseCase.getUserById(buruhId)).thenReturn(buruhDTO);
            when(mapper.toDTO(any(Panen.class))).thenReturn(savedDTO);
            when(repositoryPort.save(savedDTO)).thenReturn(savedDTO);

            PanenDTO result = panenCommandImpl.createPanen(buruhId, "desc", 100, photoUrls);

            assertNotNull(result);
            verify(repositoryPort).save(any());
        }

        @Test
        void shouldReturnSavedDTO() {
            PanenDTO savedDTO = buildPanenDTO(panenId, buruhId, kebunId, "PENDING", null);

            when(repositoryPort.existsByBuruhIdAndDate(eq(buruhId), any(LocalDate.class)))
                    .thenReturn(false);
            when(userQueryUseCase.getMandorIdByBuruhId(buruhId)).thenReturn(mandorId);
            when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
            when(userQueryUseCase.getUserById(buruhId)).thenReturn(buruhDTO);
            when(mapper.toDTO(any(Panen.class))).thenReturn(savedDTO);
            when(repositoryPort.save(savedDTO)).thenReturn(savedDTO);

            PanenDTO result = panenCommandImpl.createPanen(buruhId, "desc", 100, List.of());

            assertEquals(savedDTO, result);
        }

        @Test
        void shouldPassCorrectKebunIdToDomain() {
            when(repositoryPort.existsByBuruhIdAndDate(eq(buruhId), any(LocalDate.class)))
                    .thenReturn(false);
            when(userQueryUseCase.getMandorIdByBuruhId(buruhId)).thenReturn(mandorId);
            when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
            when(userQueryUseCase.getUserById(buruhId)).thenReturn(buruhDTO);

            ArgumentCaptor<Panen> panenCaptor = ArgumentCaptor.forClass(Panen.class);
            PanenDTO dtoFromMapper = buildPanenDTO(panenId, buruhId, kebunId, "PENDING", null);
            when(mapper.toDTO(panenCaptor.capture())).thenReturn(dtoFromMapper);
            when(repositoryPort.save(any())).thenReturn(dtoFromMapper);

            panenCommandImpl.createPanen(buruhId, "desc", 100, List.of());

            assertEquals(kebunId, panenCaptor.getValue().getKebunId());
        }

        @Test
        void shouldUseBuruhNameFromUserQuery() {
            when(repositoryPort.existsByBuruhIdAndDate(eq(buruhId), any(LocalDate.class)))
                    .thenReturn(false);
            when(userQueryUseCase.getMandorIdByBuruhId(buruhId)).thenReturn(mandorId);
            when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
            when(userQueryUseCase.getUserById(buruhId)).thenReturn(buruhDTO);

            ArgumentCaptor<Panen> panenCaptor = ArgumentCaptor.forClass(Panen.class);
            PanenDTO dtoFromMapper = buildPanenDTO(panenId, buruhId, kebunId, "PENDING", null);
            when(mapper.toDTO(panenCaptor.capture())).thenReturn(dtoFromMapper);
            when(repositoryPort.save(any())).thenReturn(dtoFromMapper);

            panenCommandImpl.createPanen(buruhId, "desc", 100, List.of());

            assertEquals("BUDIGAMING", panenCaptor.getValue().getBuruhName());
        }
    }

    // =========================================================================
    // approvePanen()
    // =========================================================================
    @Nested
    class ApprovePanen {

        @Test
        void shouldThrowWhenPanenNotFound() {
            when(repositoryPort.findById(panenId)).thenReturn(null);

            assertThrows(EntityNotFoundException.class,
                    () -> panenCommandImpl.approvePanen(panenId, mandorId));

            verify(repositoryPort, never()).save(any());
        }

        @Test
        void shouldNotPublishEventWhenPanenNotFound() {
            when(repositoryPort.findById(panenId)).thenReturn(null);

            assertThrows(EntityNotFoundException.class,
                    () -> panenCommandImpl.approvePanen(panenId, mandorId));

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void shouldApproveAndSavePanen() {
            PanenDTO pendingDTO = buildPanenDTO(panenId, buruhId, kebunId, "PENDING", null);
            Panen domainPanen   = buildDomainPanen(panenId, buruhId, kebunId, PanenStatus.PENDING, null);
            PanenDTO approvedDTO = buildPanenDTO(panenId, buruhId, kebunId, "APPROVED", null);

            when(repositoryPort.findById(panenId)).thenReturn(pendingDTO);
            when(mapper.dtoToDomain(pendingDTO)).thenReturn(domainPanen);
            when(mapper.toDTO(any(Panen.class))).thenReturn(approvedDTO);
            when(repositoryPort.save(approvedDTO)).thenReturn(approvedDTO);

            PanenDTO result = panenCommandImpl.approvePanen(panenId, mandorId);

            assertEquals("APPROVED", result.status());
            verify(repositoryPort).save(approvedDTO);
        }

        @Test
        void shouldPublishPanenApprovedEvent() {
            PanenDTO pendingDTO  = buildPanenDTO(panenId, buruhId, kebunId, "PENDING", null);
            Panen domainPanen    = buildDomainPanen(panenId, buruhId, kebunId, PanenStatus.PENDING, null);
            PanenDTO approvedDTO = buildPanenDTO(panenId, buruhId, kebunId, "APPROVED", null);

            when(repositoryPort.findById(panenId)).thenReturn(pendingDTO);
            when(mapper.dtoToDomain(pendingDTO)).thenReturn(domainPanen);
            when(mapper.toDTO(any(Panen.class))).thenReturn(approvedDTO);
            when(repositoryPort.save(any())).thenReturn(approvedDTO);

            panenCommandImpl.approvePanen(panenId, mandorId);

            ArgumentCaptor<PanenApprovedEvent> eventCaptor =
                    ArgumentCaptor.forClass(PanenApprovedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            PanenApprovedEvent event = eventCaptor.getValue();
            assertEquals(panenId, event.panenId());
            assertEquals(buruhId, event.buruhId());
            assertEquals(kebunId, event.kebunId());
        }
    }

    // =========================================================================
    // rejectPanen()
    // =========================================================================
    @Nested
    class RejectPanen {

        @Test
        void shouldThrowWhenPanenNotFound() {
            when(repositoryPort.findById(panenId)).thenReturn(null);

            assertThrows(EntityNotFoundException.class,
                    () -> panenCommandImpl.rejectPanen(panenId, mandorId, "Berat tidak sesuai"));

            verify(repositoryPort, never()).save(any());
        }

        @Test
        void shouldNotPublishEventWhenPanenNotFound() {
            when(repositoryPort.findById(panenId)).thenReturn(null);

            assertThrows(EntityNotFoundException.class,
                    () -> panenCommandImpl.rejectPanen(panenId, mandorId, "alasan"));

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void shouldRejectAndSavePanen() {
            String reason = "Berat tidak sesuai";
            PanenDTO pendingDTO  = buildPanenDTO(panenId, buruhId, kebunId, "PENDING", null);
            Panen domainPanen    = buildDomainPanen(panenId, buruhId, kebunId, PanenStatus.PENDING, null);
            PanenDTO rejectedDTO = buildPanenDTO(panenId, buruhId, kebunId, "REJECTED", reason);

            when(repositoryPort.findById(panenId)).thenReturn(pendingDTO);
            when(mapper.dtoToDomain(pendingDTO)).thenReturn(domainPanen);
            when(mapper.toDTO(any(Panen.class))).thenReturn(rejectedDTO);
            when(repositoryPort.save(rejectedDTO)).thenReturn(rejectedDTO);

            PanenDTO result = panenCommandImpl.rejectPanen(panenId, mandorId, reason);

            assertEquals("REJECTED", result.status());
            assertEquals(reason, result.rejectionReason());
            verify(repositoryPort).save(rejectedDTO);
        }

        @Test
        void shouldPublishPanenRejectedEvent() {
            String reason       = "Berat tidak sesuai";
            PanenDTO pendingDTO  = buildPanenDTO(panenId, buruhId, kebunId, "PENDING", null);
            Panen domainPanen    = buildDomainPanen(panenId, buruhId, kebunId, PanenStatus.PENDING, null);
            PanenDTO rejectedDTO = buildPanenDTO(panenId, buruhId, kebunId, "REJECTED", reason);

            when(repositoryPort.findById(panenId)).thenReturn(pendingDTO);
            when(mapper.dtoToDomain(pendingDTO)).thenReturn(domainPanen);
            when(mapper.toDTO(any(Panen.class))).thenReturn(rejectedDTO);
            when(repositoryPort.save(any())).thenReturn(rejectedDTO);

            panenCommandImpl.rejectPanen(panenId, mandorId, reason);

            ArgumentCaptor<PanenRejectedEvent> eventCaptor =
                    ArgumentCaptor.forClass(PanenRejectedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            PanenRejectedEvent event = eventCaptor.getValue();
            assertEquals(panenId, event.panenId());
            assertEquals(buruhId, event.buruhId());
            assertEquals(reason,  event.reason());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private PanenDTO buildPanenDTO(UUID panenId, UUID buruhId, UUID kebunId,
                                   String status, String rejectionReason) {
        return new PanenDTO(panenId, buruhId, "Budi", kebunId,
                "desc", 100, status, rejectionReason, List.of(),
                LocalDateTime.now());
    }

    private Panen buildDomainPanen(UUID panenId, UUID buruhId, UUID kebunId,
                                   PanenStatus status, String rejectionReason) {
        return new Panen(panenId, buruhId, "Budi", kebunId,
                "desc", 100, status, rejectionReason, LocalDateTime.now(), List.of());
    }
}