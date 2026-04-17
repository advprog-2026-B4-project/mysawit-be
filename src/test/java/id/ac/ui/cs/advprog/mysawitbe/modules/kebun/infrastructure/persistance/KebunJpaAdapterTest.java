package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KebunJpaAdapterTest {

    @Mock private KebunJpaRepository kebunRepo;
    @Mock private KebunSupirJpaRepository supirRepo;
    @Mock private KebunJpaMapper mapper;
    @InjectMocks private KebunJpaAdapter adapter;

    @Test
    void save_newKebun_callsRepository() {
        KebunDTO dto = new KebunDTO(null, "A", "K", 10, List.of());
        when(kebunRepo.findById(any())).thenReturn(Optional.empty());
        when(mapper.toEntity(any())).thenReturn(new KebunJpaEntity());
        when(kebunRepo.save(any())).thenReturn(new KebunJpaEntity());

        adapter.save(dto);
        verify(kebunRepo).save(any());
    }

    @Test
    void save_existingKebun_updatesFields() {
        UUID id = UUID.randomUUID();
        KebunDTO dto = new KebunDTO(id, "New", "K", 20, List.of());
        KebunJpaEntity existing = new KebunJpaEntity();

        when(kebunRepo.findById(id)).thenReturn(Optional.of(existing));
        when(mapper.toEntity(any())).thenReturn(new KebunJpaEntity());
        when(kebunRepo.save(any())).thenReturn(existing);

        adapter.save(dto);
        assertThat(existing.getNama()).isEqualTo("New");
    }

    @Test
    void findMandorIdByKebunId_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(kebunRepo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adapter.findMandorIdByKebunId(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void assignSupir_alreadyAssigned_throwsIllegalStateException() {
        UUID supirId = UUID.randomUUID();
        when(supirRepo.findBySupirId(supirId)).thenReturn(Optional.of(new KebunSupirJpaEntity()));
        assertThatThrownBy(() -> adapter.assignSupir(supirId, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void moveSupir_notFound_throwsException() {
        UUID supirId = UUID.randomUUID();
        when(supirRepo.findBySupirId(supirId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adapter.moveSupir(supirId, UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void moveMandor_withOldKebun_detachesFirst() {
        UUID mandorId = UUID.randomUUID();
        UUID newKebunId = UUID.randomUUID();
        KebunJpaEntity oldKebun = new KebunJpaEntity();
        KebunJpaEntity newKebun = new KebunJpaEntity();

        when(kebunRepo.findByMandorId(mandorId)).thenReturn(Optional.of(oldKebun));
        when(kebunRepo.findById(newKebunId)).thenReturn(Optional.of(newKebun));

        adapter.moveMandor(mandorId, newKebunId);

        assertThat(oldKebun.getMandorId()).isNull();
        assertThat(newKebun.getMandorId()).isEqualTo(mandorId);
        verify(kebunRepo, times(2)).save(any());
    }

    @Test
    void findById_scenarios() {
        UUID id = UUID.randomUUID();
        // 1. Found
        KebunJpaEntity entity = new KebunJpaEntity();
        when(kebunRepo.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(new KebunDTO(id, "A", "K", 10, List.of()));
        assertThat(adapter.findById(id)).isNotNull();

        // 2. Not Found (Mencakup .orElse(null))
        when(kebunRepo.findById(id)).thenReturn(Optional.empty());
        assertThat(adapter.findById(id)).isNull();
    }

    @Test
    void findAll_returnsList() {
        // Mencakup stream().map().collect()
        when(kebunRepo.findAll()).thenReturn(List.of(new KebunJpaEntity()));
        when(mapper.toDto(any())).thenReturn(new KebunDTO(UUID.randomUUID(), "A", "K", 10, List.of()));
        assertThat(adapter.findAll()).hasSize(1);
    }

    @Test
    void findByNamaContainingOrKodeContaining_callsRepo() {
        when(kebunRepo.findByNamaContainingIgnoreCaseOrKodeContainingIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of(new KebunJpaEntity()));
        adapter.findByNamaContainingOrKodeContaining("nama", "kode");
        verify(kebunRepo).findByNamaContainingIgnoreCaseOrKodeContainingIgnoreCase("nama", "kode");
    }

    @Test
    void hasMandorAssigned_scenarios() {
        UUID id = UUID.randomUUID();
        KebunJpaEntity entity = new KebunJpaEntity();

        // 1. Found and Has Mandor
        entity.setMandorId(UUID.randomUUID());
        when(kebunRepo.findById(id)).thenReturn(Optional.of(entity));
        assertThat(adapter.hasMandorAssigned(id)).isTrue();

        // 2. Found but No Mandor
        entity.setMandorId(null);
        assertThat(adapter.hasMandorAssigned(id)).isFalse();

        // 3. Not Found (Mencakup e != null)
        when(kebunRepo.findById(id)).thenReturn(Optional.empty());
        assertThat(adapter.hasMandorAssigned(id)).isFalse();
    }

    @Test
    void deleteById_callsRepo() {
        UUID id = UUID.randomUUID();
        adapter.deleteById(id);
        verify(kebunRepo).deleteById(id);
    }

    @Test
    void findSupirIdsByKebunId_returnsList() {
        UUID kebunId = UUID.randomUUID();
        KebunSupirJpaEntity entity = new KebunSupirJpaEntity(kebunId, UUID.randomUUID());
        when(supirRepo.findByKebunId(kebunId)).thenReturn(List.of(entity));

        List<UUID> result = adapter.findSupirIdsByKebunId(kebunId);
        assertThat(result).hasSize(1);
    }

    @Test
    void findAllCoordinates_returnsNestedList() {
        KebunDTO dto = new KebunDTO(UUID.randomUUID(), "A", "K", 10, List.of(new CoordinateDTO(1,1)));
        when(kebunRepo.findAll()).thenReturn(List.of(new KebunJpaEntity()));
        when(mapper.toDto(any())).thenReturn(dto);

        List<List<id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO>> result = adapter.findAllCoordinates();
        assertThat(result.get(0)).hasSize(1);
    }

    @Test
    void assignMandor_validId_savesEntity() {
        UUID kebunId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        KebunJpaEntity entity = new KebunJpaEntity();
        when(kebunRepo.findById(kebunId)).thenReturn(Optional.of(entity));

        adapter.assignMandor(mandorId, kebunId);

        assertThat(entity.getMandorId()).isEqualTo(mandorId);
        verify(kebunRepo).save(entity);
    }

    @Test
    void existsByKode_callsRepo() {
        adapter.existsByKode("K-01");
        verify(kebunRepo).existsByKode("K-01");
    }

    @Test
    void findKebunIdByMandorId_scenarios() {
        UUID mandorId = UUID.randomUUID();
        UUID kebunId = UUID.randomUUID();

        // 1. Found
        KebunJpaEntity entity = mock(KebunJpaEntity.class);
        when(kebunRepo.findByMandorId(mandorId)).thenReturn(Optional.of(entity));
        when(entity.getKebunId()).thenReturn(kebunId);
        assertThat(adapter.findKebunIdByMandorId(mandorId)).isEqualTo(kebunId);

        // 2. Not Found
        when(kebunRepo.findByMandorId(mandorId)).thenReturn(Optional.empty());
        assertThat(adapter.findKebunIdByMandorId(mandorId)).isNull();
    }

    @Test
    void findMandorIdByKebunId_success_returnsId() {
        UUID id = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        KebunJpaEntity entity = new KebunJpaEntity();
        entity.setMandorId(mandorId);

        when(kebunRepo.findById(id)).thenReturn(Optional.of(entity));
        assertThat(adapter.findMandorIdByKebunId(id)).isEqualTo(mandorId);
    }

    @Test
    void assignSupir_newAssignment_savesEntity() {
        UUID supirId = UUID.randomUUID();
        when(supirRepo.findBySupirId(supirId)).thenReturn(Optional.empty());
        adapter.assignSupir(supirId, UUID.randomUUID());
        verify(supirRepo).save(any());
    }

    @Test
    void moveSupir_updatesId_savesEntity() {
        UUID supirId = UUID.randomUUID();
        UUID newKebunId = UUID.randomUUID();
        KebunSupirJpaEntity row = new KebunSupirJpaEntity(UUID.randomUUID(), supirId);
        when(supirRepo.findBySupirId(supirId)).thenReturn(Optional.of(row));

        adapter.moveSupir(supirId, newKebunId);
        assertThat(row.getKebunId()).isEqualTo(newKebunId);
        verify(supirRepo).save(row);
    }

    @Test
    void moveSupir_updatesKebunId_savesEntity() {
        // Mencakup: row.setKebunId(newKebunId); dan kebunSupirJpaRepository.save(row);
        UUID supirId = UUID.randomUUID();
        UUID newKebunId = UUID.randomUUID();
        KebunSupirJpaEntity row = new KebunSupirJpaEntity(UUID.randomUUID(), supirId);

        when(supirRepo.findBySupirId(supirId)).thenReturn(Optional.of(row));

        adapter.moveSupir(supirId, newKebunId);

        assertThat(row.getKebunId()).isEqualTo(newKebunId);
        verify(supirRepo).save(row);
    }

    @Test
    void assignSupir_new_savesEntity() {
        // Mencakup: kebunSupirJpaRepository.save(new KebunSupirJpaEntity(kebunId, supirId));
        UUID supirId = UUID.randomUUID();
        when(supirRepo.findBySupirId(supirId)).thenReturn(Optional.empty());
        adapter.assignSupir(supirId, UUID.randomUUID());
        verify(supirRepo).save(any());
    }

    @Test
    void moveSupir_updates_savesEntity() {
        // Mencakup: row.setKebunId(newKebunId);
        UUID supirId = UUID.randomUUID();
        UUID newKebunId = UUID.randomUUID();
        KebunSupirJpaEntity row = new KebunSupirJpaEntity(UUID.randomUUID(), supirId);
        when(supirRepo.findBySupirId(supirId)).thenReturn(Optional.of(row));

        adapter.moveSupir(supirId, newKebunId);
        assertThat(row.getKebunId()).isEqualTo(newKebunId);
        verify(supirRepo).save(row);
    }

    @Test
    void assignMandor_notFound_throwsException() {
        UUID kebunId = UUID.randomUUID();
        when(kebunRepo.findById(kebunId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adapter.assignMandor(UUID.randomUUID(), kebunId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void moveMandor_newKebunNotFound_throwsException() {
        UUID kebunId = UUID.randomUUID();
        when(kebunRepo.findById(kebunId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adapter.moveMandor(UUID.randomUUID(), kebunId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}