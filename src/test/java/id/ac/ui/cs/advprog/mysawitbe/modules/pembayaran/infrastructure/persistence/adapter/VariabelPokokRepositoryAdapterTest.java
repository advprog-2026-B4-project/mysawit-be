package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VariabelPokokRepositoryAdapterTest {

    @Mock
    private VariabelPokokJpaRepository variabelPokokJpaRepository;

    @InjectMocks
    private VariabelPokokRepositoryAdapter adapter;

    private VariabelPokokEntity variabelPokokEntity;

    @BeforeEach
    void setUp() {
        variabelPokokEntity = new VariabelPokokEntity();
        variabelPokokEntity.setKey(VariableKey.UPAH_BURUH);
        variabelPokokEntity.setValue(100000);
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("returns list of VariabelPokokDTO when entities exist")
        void findAll_existingEntities_returnsDtoList() {
            VariabelPokokEntity entity1 = new VariabelPokokEntity();
            entity1.setKey(VariableKey.UPAH_BURUH);
            entity1.setValue(100000);

            VariabelPokokEntity entity2 = new VariabelPokokEntity();
            entity2.setKey(VariableKey.UPAH_SUPIR);
            entity2.setValue(150000);

            when(variabelPokokJpaRepository.findAll()).thenReturn(List.of(entity1, entity2));

            List<VariabelPokokDTO> result = adapter.findAll();

            assertThat(result).hasSize(2);
            verify(variabelPokokJpaRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when no entities exist")
        void findAll_noEntities_returnsEmptyList() {
            when(variabelPokokJpaRepository.findAll()).thenReturn(List.of());

            List<VariabelPokokDTO> result = adapter.findAll();

            assertThat(result).isEmpty();
            verify(variabelPokokJpaRepository).findAll();
        }

        @Test
        @DisplayName("maps each entity to DTO using mapper")
        void findAll_mapsEntitiesToDto() {
            VariabelPokokEntity entity = new VariabelPokokEntity();
            entity.setKey(VariableKey.UPAH_MANDOR);
            entity.setValue(200000);

            when(variabelPokokJpaRepository.findAll()).thenReturn(List.of(entity));

            List<VariabelPokokDTO> result = adapter.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).key()).isEqualTo(VariableKey.UPAH_MANDOR);
            assertThat(result.get(0).value()).isEqualTo(200000);
            assertThat(result.get(0).label()).isEqualTo(VariableKey.UPAH_MANDOR.getLabel());
            assertThat(result.get(0).description()).isEqualTo(VariableKey.UPAH_MANDOR.getDescription());
        }
    }

    @Nested
    @DisplayName("findByKey")
    class FindByKeyTests {

        @Test
        @DisplayName("returns Optional with VariabelPokokDTO when entity exists")
        void findByKey_existing_returnsDtoInOptional() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(variabelPokokEntity));

            Optional<VariabelPokokDTO> result = adapter.findByKey(VariableKey.UPAH_BURUH);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEqualTo(VariableKey.UPAH_BURUH);
            assertThat(result.get().value()).isEqualTo(100000);
            verify(variabelPokokJpaRepository).findById(VariableKey.UPAH_BURUH);
        }

        @Test
        @DisplayName("returns empty Optional when entity not found")
        void findByKey_notFound_returnsEmptyOptional() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.empty());

            Optional<VariabelPokokDTO> result = adapter.findByKey(VariableKey.UPAH_BURUH);

            assertThat(result).isEmpty();
            verify(variabelPokokJpaRepository).findById(VariableKey.UPAH_BURUH);
        }

        @Test
        @DisplayName("queries correct VariableKey enum value for SUPIR")
        void findByKey_supir_queriesCorrectKey() {
            VariabelPokokEntity supirEntity = new VariabelPokokEntity();
            supirEntity.setKey(VariableKey.UPAH_SUPIR);
            supirEntity.setValue(150000);

            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_SUPIR))
                    .thenReturn(Optional.of(supirEntity));

            Optional<VariabelPokokDTO> result = adapter.findByKey(VariableKey.UPAH_SUPIR);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEqualTo(VariableKey.UPAH_SUPIR);
            verify(variabelPokokJpaRepository).findById(VariableKey.UPAH_SUPIR);
        }

        @Test
        @DisplayName("queries correct VariableKey enum value for MANDOR")
        void findByKey_mandor_queriesCorrectKey() {
            VariabelPokokEntity mandorEntity = new VariabelPokokEntity();
            mandorEntity.setKey(VariableKey.UPAH_MANDOR);
            mandorEntity.setValue(200000);

            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_MANDOR))
                    .thenReturn(Optional.of(mandorEntity));

            Optional<VariabelPokokDTO> result = adapter.findByKey(VariableKey.UPAH_MANDOR);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEqualTo(VariableKey.UPAH_MANDOR);
            verify(variabelPokokJpaRepository).findById(VariableKey.UPAH_MANDOR);
        }

        @Test
        @DisplayName("maps entity to DTO correctly with label and description")
        void findByKey_mapsEntityWithLabelAndDescription() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(variabelPokokEntity));

            Optional<VariabelPokokDTO> result = adapter.findByKey(VariableKey.UPAH_BURUH);

            assertThat(result).isPresent();
            assertThat(result.get().label()).isEqualTo(VariableKey.UPAH_BURUH.getLabel());
            assertThat(result.get().description()).isEqualTo(VariableKey.UPAH_BURUH.getDescription());
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("creates new entity when key not found - create path")
        void save_keyNotFound_createsNewEntity() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_MANDOR))
                    .thenReturn(Optional.empty());
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            VariabelPokokDTO result = adapter.save(VariableKey.UPAH_MANDOR, 200000);

            assertThat(result.key()).isEqualTo(VariableKey.UPAH_MANDOR);
            assertThat(result.value()).isEqualTo(200000);

            ArgumentCaptor<VariabelPokokEntity> captor = ArgumentCaptor.forClass(VariabelPokokEntity.class);
            verify(variabelPokokJpaRepository).save(captor.capture());
            assertThat(captor.getValue().getKey()).isEqualTo(VariableKey.UPAH_MANDOR);
            assertThat(captor.getValue().getValue()).isEqualTo(200000);
        }

        @Test
        @DisplayName("updates existing entity when key found - update path")
        void save_keyFound_updatesExistingEntity() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(variabelPokokEntity));
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            VariabelPokokDTO result = adapter.save(VariableKey.UPAH_BURUH, 120000);

            assertThat(result.key()).isEqualTo(VariableKey.UPAH_BURUH);
            assertThat(result.value()).isEqualTo(120000);

            ArgumentCaptor<VariabelPokokEntity> captor = ArgumentCaptor.forClass(VariabelPokokEntity.class);
            verify(variabelPokokJpaRepository).save(captor.capture());
            assertThat(captor.getValue().getValue()).isEqualTo(120000);
        }

        @Test
        @DisplayName("new entity is constructed with key and newValue on create path")
        void save_createPath_usesCorrectConstructorValues() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_SUPIR))
                    .thenReturn(Optional.empty());
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            adapter.save(VariableKey.UPAH_SUPIR, 150000);

            ArgumentCaptor<VariabelPokokEntity> captor = ArgumentCaptor.forClass(VariabelPokokEntity.class);
            verify(variabelPokokJpaRepository).save(captor.capture());
            VariabelPokokEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getKey()).isEqualTo(VariableKey.UPAH_SUPIR);
            assertThat(savedEntity.getValue()).isEqualTo(150000);
        }

        @Test
        @DisplayName("existing entity value is updated via setter on update path")
        void save_updatePath_updatesValue() {
            VariabelPokokEntity existingEntity = new VariabelPokokEntity();
            existingEntity.setKey(VariableKey.UPAH_BURUH);
            existingEntity.setValue(100000);

            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(existingEntity));
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            adapter.save(VariableKey.UPAH_BURUH, 110000);

            ArgumentCaptor<VariabelPokokEntity> captor = ArgumentCaptor.forClass(VariabelPokokEntity.class);
            verify(variabelPokokJpaRepository).save(captor.capture());
            assertThat(captor.getValue().getValue()).isEqualTo(110000);
        }

        @Test
        @DisplayName("returns correct DTO with label and description after save")
        void save_returnsCorrectDtoWithLabelAndDescription() {
            // Build a complete UPAH_SUPIR entity that the static mapper can process without NPE
            VariabelPokokEntity supirEntity = new VariabelPokokEntity(VariableKey.UPAH_SUPIR, 160000);

            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_SUPIR))
                    .thenReturn(Optional.of(supirEntity));
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            VariabelPokokDTO result = adapter.save(VariableKey.UPAH_SUPIR, 160000);

            assertThat(result.key()).isEqualTo(VariableKey.UPAH_SUPIR);
            assertThat(result.value()).isEqualTo(160000);
            assertThat(result.label()).isEqualTo(VariableKey.UPAH_SUPIR.getLabel());
            assertThat(result.description()).isEqualTo(VariableKey.UPAH_SUPIR.getDescription());
        }

        @Test
        @DisplayName("create path - entity is saved with value set via setter")
        void save_createPath_valueSetViaSetter() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_MANDOR))
                    .thenReturn(Optional.empty());
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            adapter.save(VariableKey.UPAH_MANDOR, 250000);

            ArgumentCaptor<VariabelPokokEntity> captor = ArgumentCaptor.forClass(VariabelPokokEntity.class);
            verify(variabelPokokJpaRepository).save(captor.capture());
            assertThat(captor.getValue().getValue()).isEqualTo(250000);
        }

        @Test
        @DisplayName("update path - existing entity is retrieved and modified")
        void save_updatePath_retrievesExistingAndModifies() {
            // Need a complete entity + stub save so the static mapper toDto() doesn't NPE
            VariabelPokokEntity completeEntity = new VariabelPokokEntity(VariableKey.UPAH_BURUH, 130000);
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(completeEntity));
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            adapter.save(VariableKey.UPAH_BURUH, 130000);

            verify(variabelPokokJpaRepository).findById(VariableKey.UPAH_BURUH);
            verify(variabelPokokJpaRepository).save(any(VariabelPokokEntity.class));
        }
    }

    @Nested
    @DisplayName("VariableKey enum coverage")
    class VariableKeyEnumCoverageTests {

        @Test
        @DisplayName("findByKey works with UPAH_BURUH")
        void findByKey_upahBuruh_works() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(variabelPokokEntity));

            Optional<VariabelPokokDTO> result = adapter.findByKey(VariableKey.UPAH_BURUH);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("findByKey works with UPAH_SUPIR")
        void findByKey_upahSupir_works() {
            VariabelPokokEntity entity = new VariabelPokokEntity();
            entity.setKey(VariableKey.UPAH_SUPIR);
            entity.setValue(150000);

            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_SUPIR))
                    .thenReturn(Optional.of(entity));

            Optional<VariabelPokokDTO> result = adapter.findByKey(VariableKey.UPAH_SUPIR);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("findByKey works with UPAH_MANDOR")
        void findByKey_upahMandor_works() {
            VariabelPokokEntity entity = new VariabelPokokEntity();
            entity.setKey(VariableKey.UPAH_MANDOR);
            entity.setValue(200000);

            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_MANDOR))
                    .thenReturn(Optional.of(entity));

            Optional<VariabelPokokDTO> result = adapter.findByKey(VariableKey.UPAH_MANDOR);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("save works with UPAH_BURUH create path")
        void save_upahBuruh_createPath_works() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.empty());
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            VariabelPokokDTO result = adapter.save(VariableKey.UPAH_BURUH, 100000);

            assertThat(result.key()).isEqualTo(VariableKey.UPAH_BURUH);
        }

        @Test
        @DisplayName("save works with UPAH_SUPIR update path")
        void save_upahSupir_updatePath_works() {
            VariabelPokokEntity entity = new VariabelPokokEntity();
            entity.setKey(VariableKey.UPAH_SUPIR);
            entity.setValue(150000);

            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_SUPIR))
                    .thenReturn(Optional.of(entity));
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            VariabelPokokDTO result = adapter.save(VariableKey.UPAH_SUPIR, 160000);

            assertThat(result.key()).isEqualTo(VariableKey.UPAH_SUPIR);
        }

        @Test
        @DisplayName("save works with UPAH_MANDOR create path")
        void save_upahMandor_createPath_works() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_MANDOR))
                    .thenReturn(Optional.empty());
            when(variabelPokokJpaRepository.save(any(VariabelPokokEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            VariabelPokokDTO result = adapter.save(VariableKey.UPAH_MANDOR, 200000);

            assertThat(result.key()).isEqualTo(VariableKey.UPAH_MANDOR);
        }
    }
}