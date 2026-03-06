package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.VariabelPokokRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VariabelPokokServiceTest {

    @Mock  VariabelPokokRepositoryPort repository;
    @InjectMocks VariabelPokokService service;

    private VariabelPokokDTO buruhDto;

    @BeforeEach
    void setUp() {
        buruhDto = new VariabelPokokDTO(
                VariableKey.UPAH_BURUH,
                VariableKey.UPAH_BURUH.getLabel(),
                VariableKey.UPAH_BURUH.getDescription(),
                10_000
        );
    }

    // -- Query ---

    @Test
    void getAllVariabelPokok_delegatesToRepository() {
        when(repository.findAll()).thenReturn(List.of(buruhDto));

        List<VariabelPokokDTO> result = service.getAllVariabelPokok();

        assertThat(result).hasSize(1).contains(buruhDto);
        verify(repository).findAll();
    }

    @Test
    void getVariabelPokok_existingKey_returnDto() {
        when(repository.findByKey(VariableKey.UPAH_BURUH)).thenReturn(Optional.of(buruhDto));

        VariabelPokokDTO result = service.getVariabelPokok(VariableKey.UPAH_BURUH);

        assertThat(result).isEqualTo(buruhDto);
    }

    @Test
    void getVariabelPokok_missingKey_throws() {
        when(repository.findByKey(VariableKey.UPAH_SUPIR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVariabelPokok(VariableKey.UPAH_SUPIR))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -- Command ---

    @Test
    void updateVariabelPokok_validValue_savesAndReturns() {
        VariabelPokokDTO updated = new VariabelPokokDTO(
                VariableKey.UPAH_BURUH,
                VariableKey.UPAH_BURUH.getLabel(),
                VariableKey.UPAH_BURUH.getDescription(),
                20_000
        );
        when(repository.findByKey(VariableKey.UPAH_BURUH)).thenReturn(Optional.of(buruhDto));
        when(repository.save(VariableKey.UPAH_BURUH, 20_000)).thenReturn(updated);

        VariabelPokokDTO result = service.updateVariabelPokok(VariableKey.UPAH_BURUH, 20_000);

        assertThat(result.value()).isEqualTo(20_000);
        verify(repository).save(VariableKey.UPAH_BURUH, 20_000);
    }

    @Test
    void updateVariabelPokok_zeroValue_throwsBeforePersisting() {
        when(repository.findByKey(VariableKey.UPAH_BURUH)).thenReturn(Optional.of(buruhDto));

        assertThatThrownBy(() -> service.updateVariabelPokok(VariableKey.UPAH_BURUH, 0))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any(), anyInt());
    }

    @Test
    void updateVariabelPokok_negativeValue_throwsBeforePersisting() {
        when(repository.findByKey(VariableKey.UPAH_BURUH)).thenReturn(Optional.of(buruhDto));

        assertThatThrownBy(() -> service.updateVariabelPokok(VariableKey.UPAH_BURUH, -100))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any(), anyInt());
    }
}
