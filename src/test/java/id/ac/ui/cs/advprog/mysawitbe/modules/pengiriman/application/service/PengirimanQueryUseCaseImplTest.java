package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignedSupirDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignablePanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.exception.KebunQueryDependencyUnavailableException;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out.PengirimanRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PengirimanQueryUseCaseImplTest {

    @Mock
    private PengirimanRepositoryPort repository;

    @Mock
    private ObjectProvider<KebunQueryUseCase> kebunQueryUseCaseProvider;

    @Mock
    private KebunQueryUseCase kebunQueryUseCase;

    @Mock
    private PanenQueryUseCase panenQueryUseCase;

    @InjectMocks
    private PengirimanQueryUseCaseImpl service;

    @Test
    void listDeliveriesBySupir_withoutDateFilter_returnsRepositoryResult() {
        UUID supirId = UUID.randomUUID();
        PengirimanDTO dto = new PengirimanDTO(
                UUID.randomUUID(),
                supirId,
                UUID.randomUUID(),
                "ASSIGNED",
                120000,
                0,
                LocalDateTime.now()
        );
        when(repository.findBySupirId(supirId, null, null)).thenReturn(List.of(dto));

        List<PengirimanDTO> result = service.listDeliveriesBySupir(supirId, null, null);

        assertThat(result).containsExactly(dto);
        verify(repository).findBySupirId(supirId, null, null);
    }

    @Test
    void listDeliveriesBySupir_endDateBeforeStartDate_throwsBadRequestError() {
        UUID supirId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 1);

        assertThatThrownBy(() -> service.listDeliveriesBySupir(supirId, startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date cannot be before start date");

        verifyNoInteractions(repository);
    }

    @Test
    void getPengirimanById_notFound_throwsEntityNotFoundException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(null);

        assertThatThrownBy(() -> service.getPengirimanById(pengirimanId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Pengiriman not found");
    }

    @Test
    void listAssignedSupirForMandor_withoutKebunQueryDependency_throwsUnavailableException() {
        UUID mandorId = UUID.randomUUID();
        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.listAssignedSupirForMandor(mandorId, null))
                .isInstanceOf(KebunQueryDependencyUnavailableException.class)
                .hasMessageContaining("Kebun query dependency is unavailable");

        verifyNoInteractions(repository);
    }

    @Test
    void listAssignedSupirForMandor_filtersByRoleAndName() {
        UUID mandorId = UUID.randomUUID();
        UUID supirAId = UUID.randomUUID();
        UUID supirBId = UUID.randomUUID();

        UserDTO supirA = new UserDTO(supirAId, "ega", "Ega Jawa", "SUPIR", "ega@example.com");
        UserDTO supirB = new UserDTO(supirBId, "andi", "Andi Supir", "SUPIR", "andi@example.com");
        UserDTO buruh = new UserDTO(UUID.randomUUID(), "budi", "Budi Buruh", "BURUH", "budi@example.com");

        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.getSupirListByMandorId(mandorId))
                .thenReturn(List.of(supirA, supirB, buruh));

        List<AssignedSupirDTO> result = service.listAssignedSupirForMandor(mandorId, "ega");

        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.supirId()).isEqualTo(supirAId);
                    assertThat(dto.name()).isEqualTo("Ega Jawa");
                });

        verify(kebunQueryUseCase).getSupirListByMandorId(mandorId);
    }

    @Test
    void listAssignedSupirForMandor_nullSupirList_returnsEmptyList() {
        UUID mandorId = UUID.randomUUID();

        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.getSupirListByMandorId(mandorId)).thenReturn(null);

        List<AssignedSupirDTO> result = service.listAssignedSupirForMandor(mandorId, null);

        assertThat(result).isEmpty();
        verify(kebunQueryUseCase).getSupirListByMandorId(mandorId);
    }

    @Test
    void listAssignablePanenForMandor_returnsOnlyApprovedPanenThatAreNotAssigned() {
        UUID mandorId = UUID.randomUUID();
        UUID kebunId = UUID.randomUUID();
        UUID panenA = UUID.randomUUID();
        UUID panenB = UUID.randomUUID();

        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(
                new PanenDTO(panenA, UUID.randomUUID(), "Buruh A", kebunId, "Panen A", 180000, "APPROVED", null, List.of(), LocalDateTime.now()),
                new PanenDTO(panenB, UUID.randomUUID(), "Buruh B", kebunId, "Panen B", 120000, "APPROVED", null, List.of(), LocalDateTime.now())
        ));
        when(repository.findAssignedPanenIds(List.of(panenA, panenB))).thenReturn(List.of(panenB));

        List<AssignablePanenDTO> result = service.listAssignablePanenForMandor(mandorId);

        assertThat(result)
                .extracting(AssignablePanenDTO::panenId)
                .containsExactly(panenA);
    }
}
