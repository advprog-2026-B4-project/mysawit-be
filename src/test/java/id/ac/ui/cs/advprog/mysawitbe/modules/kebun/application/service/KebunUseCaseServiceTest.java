package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.out.KebunRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KebunUseCaseServiceTest {

    @Mock private KebunRepositoryPort kebunRepository;
    @Mock private UserQueryUseCase userQueryUseCase;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private KebunUseCaseService service;

    private List<CoordinateDTO> coordinates;
    private UUID kebunId;

    @BeforeEach
    void setUp() {
        coordinates = List.of(
                new CoordinateDTO(0, 0),
                new CoordinateDTO(0, 10),
                new CoordinateDTO(10, 0),
                new CoordinateDTO(10, 10)
        );
        kebunId = UUID.randomUUID();
    }

    @Test
    void createKebun_trimsTextAndSavesNormalizedDto() {
        KebunDTO saved = new KebunDTO(kebunId, "Kebun A", "KB-01", 20, coordinates);
        when(kebunRepository.existsByKode("KB-01")).thenReturn(false);
        when(kebunRepository.findAll()).thenReturn(List.of());
        when(kebunRepository.save(any(KebunDTO.class))).thenReturn(saved);

        KebunDTO result = service.createKebun("  Kebun A  ", "  KB-01  ", 20, coordinates);

        assertThat(result).isEqualTo(saved);
        verify(kebunRepository).save(new KebunDTO(null, "Kebun A", "KB-01", 20, coordinates));
    }

    @Test
    void createKebun_duplicateKode_throwsConflict() {
        when(kebunRepository.existsByKode("KB-01")).thenReturn(true);

        assertThatThrownBy(() -> service.createKebun("Kebun A", "KB-01", 20, coordinates))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kode kebun sudah digunakan");

        verify(kebunRepository, never()).save(any());
    }

    @Test
    void assignMandorToKebun_wrongRole_throwsIllegalArgumentException() {
        UUID mandorId = UUID.randomUUID();
        UserDTO wrongRoleUser = new UserDTO(mandorId, "username", "name", "SUPIR", "email@test.com");
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "Kebun A", "KB-01", 20, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(wrongRoleUser);

        assertThatThrownBy(() -> service.assignMandorToKebun(mandorId, kebunId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User is not a MANDOR");

        verify(kebunRepository, never()).assignMandor(any(), any());
    }

    @Test
    void assignMandorToKebun_validMandor_assignsToKebun() {
        UUID mandorId = UUID.randomUUID();
        UserDTO mandorUser = new UserDTO(mandorId, "mandor1", "Mandor Name", "MANDOR", "mandor@test.com");
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "Kebun A", "KB-01", 20, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(mandorUser);
        when(kebunRepository.findKebunIdByMandorId(mandorId)).thenReturn(null);

        service.assignMandorToKebun(mandorId, kebunId);

        verify(kebunRepository).assignMandor(mandorId, kebunId);
    }

    @Test
    void assignMandorToKebun_targetKebunAlreadyHasMandor_throwsConflict() {
        UUID mandorId = UUID.randomUUID();
        UUID otherMandorId = UUID.randomUUID();
        UserDTO mandorUser = new UserDTO(mandorId, "mandor1", "Mandor Name", "MANDOR", "mandor@test.com");

        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "Kebun A", "KB-01", 20, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(mandorUser);
        when(kebunRepository.findKebunIdByMandorId(mandorId)).thenReturn(null);
        when(kebunRepository.findMandorIdByKebunId(kebunId)).thenReturn(otherMandorId);

        assertThatThrownBy(() -> service.assignMandorToKebun(mandorId, kebunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kebun sudah memiliki mandor");

        verify(kebunRepository, never()).assignMandor(any(), any());
    }

    @Test
    void moveMandorToKebun_targetAlreadyHasOtherMandor_throwsConflict() {
        UUID mandorId = UUID.randomUUID();
        UUID currentKebunId = UUID.randomUUID();
        UUID otherMandorId = UUID.randomUUID();
        UserDTO mandorUser = new UserDTO(mandorId, "mandor1", "Mandor Name", "MANDOR", "mandor@test.com");

        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "Kebun A", "KB-01", 20, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(mandorUser);
        when(kebunRepository.findKebunIdByMandorId(mandorId)).thenReturn(currentKebunId);
        when(kebunRepository.findMandorIdByKebunId(kebunId)).thenReturn(otherMandorId);

        assertThatThrownBy(() -> service.moveMandorToKebun(mandorId, kebunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kebun tujuan sudah memiliki mandor");

        verify(kebunRepository, never()).moveMandor(any(), any());
    }

    @Test
    void getKebunById_missingKebun_throwsNotFound() {
        when(kebunRepository.findById(kebunId)).thenReturn(null);

        assertThatThrownBy(() -> service.getKebunById(kebunId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Kebun not found");
    }

    @Test
    void listKebun_withBothFilters_usesRepositorySearch() {
        List<KebunDTO> expected = List.of(new KebunDTO(kebunId, "Alpha", "KB-01", 20, coordinates));
        when(kebunRepository.findByNamaContainingOrKodeContaining("Alpha", "KB-01")).thenReturn(expected);

        List<KebunDTO> result = service.listKebun(" Alpha ", " KB-01 ");

        assertThat(result).isEqualTo(expected);
        verify(kebunRepository).findByNamaContainingOrKodeContaining("Alpha", "KB-01");
    }

    @Test
    void createKebun_rectangleCoordinates_throwsIllegalArgumentException() {
        List<CoordinateDTO> rectangleCoordinates = List.of(
                new CoordinateDTO(0, 0),
                new CoordinateDTO(0, 20),
                new CoordinateDTO(10, 0),
                new CoordinateDTO(10, 20)
        );

        assertThatThrownBy(() -> service.createKebun("Kebun A", "KB-01", 20, rectangleCoordinates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kebun sawit hanya boleh berbentuk persegi");

        verify(kebunRepository, never()).save(any());
    }
}