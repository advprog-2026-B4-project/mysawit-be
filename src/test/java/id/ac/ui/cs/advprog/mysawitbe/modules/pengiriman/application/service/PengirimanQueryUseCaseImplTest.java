package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out.PengirimanRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
