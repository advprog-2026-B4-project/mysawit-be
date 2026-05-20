package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application;

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

    @Test
    void getBuruhList_withoutAssignedMandor_returnsEmptyList() {
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "Kebun A", "KB-01", 20, coordinates));
        when(kebunRepository.findMandorIdByKebunId(kebunId)).thenReturn(null);

        List<UserDTO> result = service.getBuruhList(kebunId);

        assertThat(result).isEmpty();
        verify(userQueryUseCase, never()).getBuruhByMandorId(any());
    }

    @Test
    void getBuruhList_withAssignedMandor_returnsSortedBuruh() {
        UUID mandorId = UUID.randomUUID();
        UserDTO buruhB = new UserDTO(UUID.randomUUID(), "buruh2", "Budi", "BURUH", "budi@test.com");
        UserDTO buruhA = new UserDTO(UUID.randomUUID(), "buruh1", "Andi", "BURUH", "andi@test.com");

        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "Kebun A", "KB-01", 20, coordinates));
        when(kebunRepository.findMandorIdByKebunId(kebunId)).thenReturn(mandorId);
        when(userQueryUseCase.getBuruhByMandorId(mandorId)).thenReturn(List.of(buruhB, buruhA));

        List<UserDTO> result = service.getBuruhList(kebunId);

        assertThat(result).containsExactly(buruhA, buruhB);
    }

    @Test
    void editKebun_validRequest_updatesSuccessfully() {
        KebunDTO existing = new KebunDTO(kebunId, "Old Name", "KB-01", 10, coordinates);
        when(kebunRepository.findById(kebunId)).thenReturn(existing);
        when(kebunRepository.findAll()).thenReturn(List.of(existing));
        when(kebunRepository.save(any(KebunDTO.class))).thenReturn(new KebunDTO(kebunId, "New Name", "KB-01", 15, coordinates));

        KebunDTO result = service.editKebun(kebunId, "New Name", 15, coordinates);

        assertThat(result.nama()).isEqualTo("New Name");
        verify(kebunRepository).save(any());
    }

    @Test
    void deleteKebun_hasMandor_throwsIllegalStateException() {
        // Mencakup: if (kebunRepository.hasMandorAssigned(kebunId))
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(kebunRepository.hasMandorAssigned(kebunId)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteKebun(kebunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("masih ada mandor yang terikat");
    }

    @Test
    void assignMandorToKebun_mandorAlreadyAssignedElsewhere_throwsIllegalStateException() {
        UUID mandorId = UUID.randomUUID();
        UUID otherKebunId = UUID.randomUUID();
        UserDTO mandorUser = new UserDTO(mandorId, "m", "n", "MANDOR", "e");

        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(mandorUser);
        when(kebunRepository.findKebunIdByMandorId(mandorId)).thenReturn(otherKebunId);

        assertThatThrownBy(() -> service.assignMandorToKebun(mandorId, kebunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mandor sudah terikat pada kebun lain");
    }

    @Test
    void moveMandorToKebun_mandorNotAssigned_throwsIllegalStateException() {
        UUID mandorId = UUID.randomUUID();
        UserDTO mandorUser = new UserDTO(mandorId, "m", "n", "MANDOR", "e");

        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(mandorUser);
        when(kebunRepository.findKebunIdByMandorId(mandorId)).thenReturn(null);

        assertThatThrownBy(() -> service.moveMandorToKebun(mandorId, kebunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mandor belum terikat ke kebun manapun");
    }

    @Test
    void createKebun_overlappingCoordinates_throwsIllegalStateException() {
        List<CoordinateDTO> newCoords = coordinates; // Koordinat yang sama (pasti overlap)
        KebunDTO existing = new KebunDTO(UUID.randomUUID(), "Existing", "EX-01", 10, coordinates);

        // Hapus stubbing existsByKode karena validasi overlap terjadi lebih dulu
        when(kebunRepository.findAll()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createKebun("New", "NEW-01", 10, newCoords))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void validateCoordinates_pointInside_returnsFalse() {
        when(kebunRepository.findAllCoordinates()).thenReturn(List.of(coordinates));
        // coordinates are (0,0) to (10,10), point (5,5) is inside
        boolean result = service.validateCoordinates(5, 5);
        assertThat(result).isFalse();
    }

    @Test
    void assignSupirToKebun_validSupir_callsRepository() {
        UUID supirId = UUID.randomUUID();
        UserDTO supir = new UserDTO(supirId, "s", "n", "SUPIR", "e");
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(supirId)).thenReturn(supir);

        service.assignSupirToKebun(supirId, kebunId);
        verify(kebunRepository).assignSupir(supirId, kebunId);
    }

    @Test
    void createKebun_invalidInput_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.createKebun("", "KB-01", 10, coordinates))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createKebun("Nama", "", 10, coordinates))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createKebun("Nama", "KB-01", 0, coordinates))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createKebun("Nama", "KB-01", 10, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignSupirToKebun_supirNotFound_throwsEntityNotFoundException() {
        UUID supirId = UUID.randomUUID();
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(supirId)).thenReturn(null);

        assertThatThrownBy(() -> service.assignSupirToKebun(supirId, kebunId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void moveSupirToKebun_validRequest_callsRepository() {
        UUID supirId = UUID.randomUUID();
        UserDTO supir = new UserDTO(supirId, "s", "n", "SUPIR", "e");
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(supirId)).thenReturn(supir);

        service.moveSupirToKebun(supirId, kebunId);
        verify(kebunRepository).moveSupir(supirId, kebunId);
    }

    @Test
    void getSupirList_returnsSortedSupir() {
        UUID s1Id = UUID.randomUUID();
        UUID s2Id = UUID.randomUUID();
        UserDTO s1 = new UserDTO(s1Id, "b", "Budi", "SUPIR", "b@e.com");
        UserDTO s2 = new UserDTO(s2Id, "a", "Andi", "SUPIR", "a@e.com");

        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(kebunRepository.findSupirIdsByKebunId(kebunId)).thenReturn(List.of(s1Id, s2Id));
        when(userQueryUseCase.getUserById(s1Id)).thenReturn(s1);
        when(userQueryUseCase.getUserById(s2Id)).thenReturn(s2);

        List<UserDTO> result = service.getSupirList(kebunId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Andi");
        assertThat(result.get(1).name()).isEqualTo("Budi");
    }

    @Test
    void listKebun_filterOnlyNama_usesRepositorySearch() {
        service.listKebun("Kebun A", null);
        verify(kebunRepository).findByNamaContainingOrKodeContaining("Kebun A", "");
    }

    @Test
    void listKebun_filterOnlyKode_usesRepositorySearch() {
        service.listKebun(null, "KB-01");
        verify(kebunRepository).findByNamaContainingOrKodeContaining("", "KB-01");
    }

    @Test
    void listKebun_noFilter_usesFindAll() {
        service.listKebun(null, "");
        verify(kebunRepository).findAll();
    }

    @Test
    void createKebun_invalidPointsCount_throwsIllegalArgumentException() {
        List<CoordinateDTO> invalidCoords = List.of(new CoordinateDTO(0,0));
        assertThatThrownBy(() -> service.createKebun("N", "K", 10, invalidCoords))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tepat 4 titik");
    }

    @Test
    void createKebun_nonSquare_throwsIllegalArgumentException() {
        List<CoordinateDTO> rectangle = List.of(
                new CoordinateDTO(0, 0), new CoordinateDTO(0, 10),
                new CoordinateDTO(5, 0), new CoordinateDTO(5, 10)
        );
        assertThatThrownBy(() -> service.createKebun("N", "K", 10, rectangle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("persegi");
    }

    @Test
    void editKebun_invalidInput_throwsIllegalArgumentException() {
        // Kasus kebunId null (langsung melempar exception sebelum cari ke repo)
        assertThatThrownBy(() -> service.editKebun(null, "Nama", 10, coordinates))
                .isInstanceOf(IllegalArgumentException.class);

        // Berikan stubbing agar findById tidak mengembalikan null
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));

        // Sekarang validasi nama dan luas bisa tercapai
        assertThatThrownBy(() -> service.editKebun(kebunId, "", 10, coordinates))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.editKebun(kebunId, "Nama", 0, coordinates))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignMandorToKebun_mandorNotFound_throwsEntityNotFoundException() {
        UUID mandorId = UUID.randomUUID();
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(null);

        assertThatThrownBy(() -> service.assignMandorToKebun(mandorId, kebunId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void moveMandorToKebun_mandorNotFound_throwsEntityNotFoundException() {
        UUID mandorId = UUID.randomUUID();
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(null);

        assertThatThrownBy(() -> service.moveMandorToKebun(mandorId, kebunId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void moveMandorToKebun_wrongRole_throwsIllegalArgumentException() {
        UUID mandorId = UUID.randomUUID();
        UserDTO notMandor = new UserDTO(mandorId, "user1", "Budi", "SUPIR", "budi@mail.com");

        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(notMandor);

        assertThatThrownBy(() -> service.moveMandorToKebun(mandorId, kebunId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User is not a MANDOR");
    }

    @Test
    void assignSupirToKebun_wrongRole_throwsIllegalArgumentException() {
        UUID supirId = UUID.randomUUID();
        UserDTO notSupir = new UserDTO(supirId, "s", "n", "MANDOR", "e");
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(supirId)).thenReturn(notSupir);

        assertThatThrownBy(() -> service.assignSupirToKebun(supirId, kebunId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moveSupirToKebun_wrongRole_throwsIllegalArgumentException() {
        UUID supirId = UUID.randomUUID();
        UserDTO notSupir = new UserDTO(supirId, "s", "n", "MANDOR", "e");
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(supirId)).thenReturn(notSupir);

        assertThatThrownBy(() -> service.moveSupirToKebun(supirId, kebunId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getBuruhList_nullKebunId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.getBuruhList(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getBuruhList_repositoryReturnsNull_returnsEmptyList() {
        UUID mandorId = UUID.randomUUID();
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(kebunRepository.findMandorIdByKebunId(kebunId)).thenReturn(mandorId);
        when(userQueryUseCase.getBuruhByMandorId(mandorId)).thenReturn(null);

        List<UserDTO> result = service.getBuruhList(kebunId);
        assertThat(result).isEmpty();
    }

    @Test
    void getMandorIdByKebun_validId_returnsMandorId() {
        UUID mandorId = UUID.randomUUID();
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(kebunRepository.findMandorIdByKebunId(kebunId)).thenReturn(mandorId);

        UUID result = service.getMandorIdByKebun(kebunId);
        assertThat(result).isEqualTo(mandorId);
    }

    @Test
    void getKebunById_validId_returnsDto() {
        KebunDTO dto = new KebunDTO(kebunId, "A", "K", 10, coordinates);
        when(kebunRepository.findById(kebunId)).thenReturn(dto);

        assertThat(service.getKebunById(kebunId)).isEqualTo(dto);
    }

    @Test
    void validateCoordinates_pointOutside_returnsTrue() {
        when(kebunRepository.findAllCoordinates()).thenReturn(List.of(coordinates));
        // coordinates (0,0) to (10,10), point (15,15) is outside
        boolean result = service.validateCoordinates(15, 15);
        assertThat(result).isTrue();
    }

    @Test
    void moveMandorToKebun_sameKebun_throwsIllegalStateException() {
        UUID mandorId = UUID.randomUUID();
        UserDTO mandorUser = new UserDTO(mandorId, "m", "n", "MANDOR", "e");
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(mandorUser);
        when(kebunRepository.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);

        assertThatThrownBy(() -> service.moveMandorToKebun(mandorId, kebunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sudah terikat pada kebun tujuan");
    }

    @Test
    void editKebun_notFound_throwsEntityNotFoundException() {
        when(kebunRepository.findById(kebunId)).thenReturn(null);
        assertThatThrownBy(() -> service.editKebun(kebunId, "Nama", 10, coordinates))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteKebun_invalidId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.deleteKebun(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteKebun_notFound_throwsEntityNotFoundException() {
        // Mencakup: if (kebunRepository.findById(kebunId) == null)
        when(kebunRepository.findById(kebunId)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteKebun(kebunId))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Kebun not found");
    }

    @Test
    void assignMandorToKebun_nullIds_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.assignMandorToKebun(null, kebunId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("personId wajib diisi");
        assertThatThrownBy(() -> service.assignMandorToKebun(UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kebunId wajib diisi");
    }

    @Test
    void moveMandorToKebun_nullIds_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.moveMandorToKebun(null, kebunId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignSupirToKebun_nullIds_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.assignSupirToKebun(null, kebunId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moveSupirToKebun_nullIds_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.moveSupirToKebun(null, kebunId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moveSupirToKebun_supirNotFound_throwsEntityNotFoundException() {
        UUID supirId = UUID.randomUUID();
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(supirId)).thenReturn(null);

        assertThatThrownBy(() -> service.moveSupirToKebun(supirId, kebunId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getMandorIdByKebun_nullId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.getMandorIdByKebun(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getSupirList_nullId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.getSupirList(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editKebun_nullCoordinates_throwsIllegalArgumentException() {
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        assertThatThrownBy(() -> service.editKebun(kebunId, "Nama", 10, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateNoOverlap_sameKebun_skipsOverlapCheck() {
        // Skenario untuk menutupi cabang 'if (selfKebunId != null && selfKebunId.equals(existing.kebunId())) continue;'
        KebunDTO existing = new KebunDTO(kebunId, "Existing", "EX", 10, coordinates);
        when(kebunRepository.findById(kebunId)).thenReturn(existing);
        when(kebunRepository.findAll()).thenReturn(List.of(existing));
        when(kebunRepository.save(any())).thenReturn(existing);

        // Edit kebun yang sama dengan koordinat yang sama tidak boleh melempar IllegalStateException (overlap)
        KebunDTO result = service.editKebun(kebunId, "New Name", 10, coordinates);
        assertThat(result).isNotNull();
    }

    @Test
    void assignMandorToKebun_publishesEvent() {
        UUID mandorId = UUID.randomUUID();
        UserDTO mandorUser = new UserDTO(mandorId, "m", "n", "MANDOR", "e");
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(mandorUser);

        service.assignMandorToKebun(mandorId, kebunId);

        verify(eventPublisher).publishEvent(any(id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.event.MandorAssignedToKebunEvent.class));
    }

    @Test
    void moveMandorToKebun_publishesEvent() {
        UUID mandorId = UUID.randomUUID();
        UserDTO mandorUser = new UserDTO(mandorId, "m", "n", "MANDOR", "e");
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(userQueryUseCase.getUserById(mandorId)).thenReturn(mandorUser);
        when(kebunRepository.findKebunIdByMandorId(mandorId)).thenReturn(UUID.randomUUID());

        service.moveMandorToKebun(mandorId, kebunId);

        verify(eventPublisher).publishEvent(any(id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.event.MandorAssignedToKebunEvent.class));
    }

    @Test
    void getBuruhList_emptyFromUseCase_returnsEmptyList() {
        UUID mandorId = UUID.randomUUID();
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(kebunRepository.findMandorIdByKebunId(kebunId)).thenReturn(mandorId);
        when(userQueryUseCase.getBuruhByMandorId(mandorId)).thenReturn(List.of());

        List<UserDTO> result = service.getBuruhList(kebunId);
        assertThat(result).isEmpty();
    }

    @Test
    void validateNoOverlap_differentKebun_checksOverlap() {
        // Kasus selfKebunId null (Create) vs Existing Kebun
        List<CoordinateDTO> newCoords = List.of(
                new CoordinateDTO(100, 100), new CoordinateDTO(100, 110),
                new CoordinateDTO(110, 100), new CoordinateDTO(110, 110)
        );
        KebunDTO existing = new KebunDTO(UUID.randomUUID(), "E", "K", 10, coordinates);
        when(kebunRepository.findAll()).thenReturn(List.of(existing));
        when(kebunRepository.existsByKode("NEW")).thenReturn(false);
        when(kebunRepository.save(any())).thenReturn(new KebunDTO(UUID.randomUUID(), "N", "NEW", 10, newCoords));

        service.createKebun("New", "NEW", 10, newCoords);
        verify(kebunRepository).findAll();
    }

    @Test
    void validateSquareCorners_duplicatePoints_throwsIllegalArgumentException() {
        List<CoordinateDTO> dupPoints = List.of(
                new CoordinateDTO(0, 0), new CoordinateDTO(0, 0),
                new CoordinateDTO(10, 10), new CoordinateDTO(10, 10)
        );
        assertThatThrownBy(() -> service.createKebun("N", "K", 10, dupPoints))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kebun sawit hanya boleh berbentuk persegi");
    }

    @Test
    void listKebun_trimsSearchInput() {
        service.listKebun("  Kebun  ", "  KB  ");
        verify(kebunRepository).findByNamaContainingOrKodeContaining("Kebun", "KB");
    }

    @Test
    void listKebun_nullSearchInput_usesFindAll() {
        // Mencakup baris 228-229 saat input null
        service.listKebun(null, null);
        verify(kebunRepository).findAll();
    }

    @Test
    void getBuruhList_noMandor_returnsEmptyList() {
        // Mencakup baris 180-182
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        when(kebunRepository.findMandorIdByKebunId(kebunId)).thenReturn(null);

        List<UserDTO> result = service.getBuruhList(kebunId);
        assertThat(result).isEmpty();
    }

    @Test
    void createKebun_zeroArea_throwsIllegalArgumentException() {
        // Mencakup baris 47-49 di KebunGeometry (lewat KebunUseCaseService)
        List<CoordinateDTO> flatCoords = List.of(
                new CoordinateDTO(0, 0), new CoordinateDTO(0, 0),
                new CoordinateDTO(0, 10), new CoordinateDTO(0, 10)
        );
        assertThatThrownBy(() -> service.createKebun("N", "K", 10, flatCoords))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("luas sisi harus > 0");
    }

    @Test
    void createKebun_nullNamaAndKode_throwsIllegalArgumentException() {
        // Mencakup baris 44-45 (penanganan null) dan 251 (validasi basic)
        assertThatThrownBy(() -> service.createKebun(null, "K-01", 10, coordinates))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createKebun("Nama", null, 10, coordinates))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editKebun_blankNama_throwsIllegalArgumentException() {
        // Mencakup baris 67 (nama.isBlank())
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));
        assertThatThrownBy(() -> service.editKebun(kebunId, "   ", 10, coordinates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nama kebun wajib diisi");
    }

    @Test
    void validateNoOverlap_realOverlap_throwsIllegalStateException() {
        // Mencakup baris 264-266
        List<CoordinateDTO> newCoords = List.of(
                new CoordinateDTO(5, 5), new CoordinateDTO(5, 15),
                new CoordinateDTO(15, 5), new CoordinateDTO(15, 15)
        );
        // Existing di (0,0) - (10,10). New di (5,5) - (15,15). Overlap di (5,5) - (10,10).
        KebunDTO existing = new KebunDTO(UUID.randomUUID(), "Existing", "EX", 10, coordinates);
        when(kebunRepository.findAll()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createKebun("New", "NEW", 10, newCoords))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlap dengan kebun lain");
    }

    @Test
    void validateCoordinates_noPolygons_returnsTrue() {
        // Mencakup baris 240-241 (polygons kosong)
        when(kebunRepository.findAllCoordinates()).thenReturn(List.of());
        boolean result = service.validateCoordinates(5, 5);
        assertThat(result).isTrue();
    }

    @Test
    void ensureKebunExists_notFound_throwsEntityNotFoundException() {
        // Mencakup baris 270-274 secara langsung via pemanggilnya
        when(kebunRepository.findById(kebunId)).thenReturn(null);
        assertThatThrownBy(() -> service.getMandorIdByKebun(kebunId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Kebun not found");
    }

    @Test
    void listKebun_oneFilterNullOneFilterEmpty_usesFindAll() {
        // Menguji kombinasi ternary null dan isBlank()
        service.listKebun(null, "   ");
        verify(kebunRepository).findAll();
    }

    @Test
    void validateNoOverlap_createMode_checksAgainstAll() {
        // Memastikan saat selfKebunId null (Create), perbandingan id (equals) tidak dilewati secara salah
        List<CoordinateDTO> newCoords = List.of(
                new CoordinateDTO(100, 100), new CoordinateDTO(100, 110),
                new CoordinateDTO(110, 100), new CoordinateDTO(110, 110)
        );
        KebunDTO existing = new KebunDTO(UUID.randomUUID(), "E", "K", 10, coordinates);
        when(kebunRepository.findAll()).thenReturn(List.of(existing));
        when(kebunRepository.existsByKode("NEW")).thenReturn(false);

        service.createKebun("New", "NEW", 10, newCoords);
        verify(kebunRepository).findAll();
    }

    @Test
    void editKebun_nullNama_throwsIllegalArgumentException() {
        // Mencakup: if (nama == null || nama.isBlank())
        when(kebunRepository.findById(kebunId)).thenReturn(new KebunDTO(kebunId, "A", "K", 10, coordinates));

        assertThatThrownBy(() -> service.editKebun(kebunId, null, 10, coordinates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nama kebun wajib diisi");
    }

    @Test
    void deleteKebun_nullId_throwsIllegalArgumentException() {
        // Mencakup: if (kebunId == null) throw new IllegalArgumentException("kebunId wajib diisi");
        assertThatThrownBy(() -> service.deleteKebun(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kebunId wajib diisi");
    }

    @Test
    void getKebunById_nullId_throwsIllegalArgumentException() {
        // Mencakup: if (kebunId == null) pada getKebunById
        assertThatThrownBy(() -> service.getKebunById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kebunId wajib diisi");
    }

    @Test
    void validateNoOverlap_sameKebunId_continuesLoop() {
        // Mencakup: if (selfKebunId != null && selfKebunId.equals(existing.kebunId())) continue;
        KebunDTO existing = new KebunDTO(kebunId, "Existing", "K-01", 10, coordinates);

        when(kebunRepository.findById(kebunId)).thenReturn(existing);
        when(kebunRepository.findAll()).thenReturn(List.of(existing));
        when(kebunRepository.save(any())).thenReturn(existing);

        // Harusnya tidak melempar IllegalStateException meskipun koordinat sama karena ID-nya sama (mode edit)
        KebunDTO result = service.editKebun(kebunId, "Updated Name", 10, coordinates);
        assertThat(result).isNotNull();
        verify(kebunRepository).save(any());
    }

    @Test
    void deleteKebun_fullCoverage_scenarios() {
        // 1. Kasus null ID
        assertThatThrownBy(() -> service.deleteKebun(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kebunId wajib diisi");

        // 2. Kasus Kebun tidak ditemukan
        when(kebunRepository.findById(kebunId)).thenReturn(null);
        assertThatThrownBy(() -> service.deleteKebun(kebunId))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Kebun not found");

        // 3. Kasus Kebun masih punya mandor
        KebunDTO existing = new KebunDTO(kebunId, "Kebun A", "K-01", 10, coordinates);
        when(kebunRepository.findById(kebunId)).thenReturn(existing);
        when(kebunRepository.hasMandorAssigned(kebunId)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteKebun(kebunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("masih ada mandor yang terikat");

        // 4. Kasus Berhasil Hapus (untuk menutup branch terakhir)
        when(kebunRepository.hasMandorAssigned(kebunId)).thenReturn(false);
        service.deleteKebun(kebunId);
        verify(kebunRepository).deleteById(kebunId);
    }

    @Test
    void validateNoOverlap_multipleKebuns_coversAllBranches() {
        // 1. Kebun yang sedang di-edit (harus di-skip lewat 'continue')
        KebunDTO self = new KebunDTO(kebunId, "Self", "K-01", 10, coordinates);

        // 2. Kebun lain yang posisinya berjauhan (tidak overlap, untuk cover baris overlaps)
        List<CoordinateDTO> otherCoords = List.of(
                new CoordinateDTO(100, 100), new CoordinateDTO(100, 110),
                new CoordinateDTO(110, 100), new CoordinateDTO(110, 110)
        );
        KebunDTO other = new KebunDTO(UUID.randomUUID(), "Other", "K-02", 10, otherCoords);

        when(kebunRepository.findById(kebunId)).thenReturn(self);
        when(kebunRepository.findAll()).thenReturn(List.of(self, other));
        when(kebunRepository.save(any())).thenReturn(self);

        // Eksekusi edit. Loop akan berjalan 2x:
        // Iterasi 1 (self): memicu 'continue'
        // Iterasi 2 (other): memicu 'overlaps' (bernilai false)
        KebunDTO result = service.editKebun(kebunId, "New Name", 10, coordinates);

        assertThat(result).isNotNull();
        verify(kebunRepository).save(any());
    }

    @Test
    void findKebunIdByMandorId_nullId_throwsIllegalArgumentException() {
        // Mencakup: if (mandorId == null) throw new IllegalArgumentException("mandorId wajib diisi");
        assertThatThrownBy(() -> service.findKebunIdByMandorId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mandorId wajib diisi");
    }

    @Test
    void findKebunIdByMandorId_validId_callsRepository() {
        UUID mandorId = UUID.randomUUID();
        service.findKebunIdByMandorId(mandorId);
        verify(kebunRepository).findKebunIdByMandorId(mandorId);
    }

    @Test
    void getSupirListByMandorId_nullMandorId_throwsIllegalArgumentException() {
        // Mencakup: if (mandorId == null) throw new IllegalArgumentException("mandorId wajib diisi");
        assertThatThrownBy(() -> service.getSupirListByMandorId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mandorId wajib diisi");
    }

    @Test
    void getSupirListByMandorId_kebunNotFound_returnsEmptyList() {
        // Mencakup: if (kebunId == null) { return List.of(); }
        UUID mandorId = UUID.randomUUID();
        when(kebunRepository.findKebunIdByMandorId(mandorId)).thenReturn(null);

        List<UserDTO> result = service.getSupirListByMandorId(mandorId);

        assertThat(result).isEmpty();
        verify(kebunRepository).findKebunIdByMandorId(mandorId);
        verify(kebunRepository, never()).findSupirIdsByKebunId(any());
    }

    @Test
    void getSupirListByMandorId_validMandor_returnsSortedSupir() {
        // Mencakup alur sukses untuk memastikan branch 'kebunId != null' terlewati
        UUID mandorId = UUID.randomUUID();
        UUID s1Id = UUID.randomUUID();
        UserDTO s1 = new UserDTO(s1Id, "b", "Budi", "SUPIR", "b@e.com");

        when(kebunRepository.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(kebunRepository.findSupirIdsByKebunId(kebunId)).thenReturn(List.of(s1Id));
        when(userQueryUseCase.getUserById(s1Id)).thenReturn(s1);

        List<UserDTO> result = service.getSupirListByMandorId(mandorId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Budi");
    }
}
