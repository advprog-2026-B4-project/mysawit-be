package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.User;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserJpaAdapter")
class UserJpaAdapterTest {

    @Mock UserJpaRepository repository;
    @Mock UserMapper        mapper;

    UserJpaAdapter adapter;

    final UUID   userId   = UUID.randomUUID();
    final UUID   mandorId = UUID.randomUUID();
    final String email    = "user@test.com";
    final String hash     = "$2a$hash";

    UserJpaEntity entity() {
        UserJpaEntity e = new UserJpaEntity();
        e.setUserId(userId);
        e.setUsername("user");
        e.setEmail(email);
        e.setName("Name");
        e.setPassword(hash);
        e.setRole("BURUH");
        return e;
    }

    User domain() {
        User u = new User();
        u.setUserId(userId);
        u.setUsername("user");
        u.setEmail(email);
        u.setName("Name");
        u.setHashedPassword(hash);
        u.setRole(UserRole.BURUH);
        return u;
    }

    UserDTO dto()    { return new UserDTO(userId, "user", "Name", "BURUH", email); }
    UserDTO dtoNew() { return new UserDTO(null,   "user", "Name", "BURUH", email); }

    @BeforeEach
    void setUp() {
        adapter = new UserJpaAdapter(repository, mapper);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("new user (null userId) – creates and persists entity")
        void newUserCreatesAndPersistsEntity() {
            UserDTO input  = dtoNew();
            User    dom    = domain(); dom.setUserId(null);
            UserJpaEntity ent   = entity();
            UserJpaEntity saved = entity();

            when(mapper.toDomain(input)).thenReturn(dom);
            when(mapper.toEntity(dom)).thenReturn(ent);
            when(repository.save(ent)).thenReturn(saved);
            when(mapper.toDomain(saved)).thenReturn(dom);
            when(mapper.toDTO(dom)).thenReturn(dto());

            UserDTO result = adapter.save(input, hash);

            assertThat(result.userId()).isEqualTo(userId);
            verify(repository).save(ent);
        }

        @Test
        @DisplayName("existing user – updates name, email, role on found entity")
        void existingUserUpdatesFields() {
            UserDTO input    = dto();
            User    dom      = domain();
            UserJpaEntity existing = entity();
            UserJpaEntity saved    = entity();

            when(mapper.toDomain(input)).thenReturn(dom);
            when(mapper.toEntity(dom)).thenReturn(existing);
            when(repository.findById(userId)).thenReturn(Optional.of(existing));
            when(repository.save(existing)).thenReturn(saved);
            when(mapper.toDomain(saved)).thenReturn(dom);
            when(mapper.toDTO(dom)).thenReturn(dto());

            UserDTO result = adapter.save(input, hash);

            assertThat(result).isEqualTo(dto());
            verify(repository).save(existing);
        }

        @Test
        @DisplayName("existing user not in DB – falls back to mapper entity")
        void existingUserNotFoundFallsBack() {
            UserDTO input    = dto();
            User    dom      = domain();
            UserJpaEntity fallback = entity();
            UserJpaEntity saved    = entity();

            when(mapper.toDomain(input)).thenReturn(dom);
            when(mapper.toEntity(dom)).thenReturn(fallback);
            when(repository.findById(userId)).thenReturn(Optional.empty());
            when(repository.save(fallback)).thenReturn(saved);
            when(mapper.toDomain(saved)).thenReturn(dom);
            when(mapper.toDTO(dom)).thenReturn(dto());

            assertThat(adapter.save(input, hash)).isNotNull();
        }

        @Test
        @DisplayName("null hashedPassword does not overwrite existing password")
        void nullPasswordIsNotWrittenToEntity() {
            UserDTO input    = dto();
            User    dom      = domain();
            UserJpaEntity existing = entity();
            existing.setPassword("old-hash");
            UserJpaEntity saved = entity();

            when(mapper.toDomain(input)).thenReturn(dom);
            when(mapper.toEntity(dom)).thenReturn(existing);
            when(repository.findById(userId)).thenReturn(Optional.of(existing));
            when(repository.save(existing)).thenReturn(saved);
            when(mapper.toDomain(saved)).thenReturn(dom);
            when(mapper.toDTO(dom)).thenReturn(dto());

            adapter.save(input, null);

            assertThat(existing.getPassword()).isEqualTo("old-hash");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("found – returns mapped DTO")
        void foundReturnsMappedDto() {
            UserJpaEntity ent = entity();
            User          dom = domain();
            when(repository.findById(userId)).thenReturn(Optional.of(ent));
            when(mapper.toDomain(ent)).thenReturn(dom);
            when(mapper.toDTO(dom)).thenReturn(dto());

            assertThat(adapter.findById(userId)).isEqualTo(dto());
        }

        @Test
        @DisplayName("not found – returns null")
        void notFoundReturnsNull() {
            when(repository.findById(userId)).thenReturn(Optional.empty());
            assertThat(adapter.findById(userId)).isNull();
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("found – returns mapped DTO")
        void foundReturnsMappedDto() {
            UserJpaEntity ent = entity();
            User          dom = domain();
            when(repository.findByEmail(email)).thenReturn(Optional.of(ent));
            when(mapper.toDomain(ent)).thenReturn(dom);
            when(mapper.toDTO(dom)).thenReturn(dto());

            assertThat(adapter.findByEmail(email)).isEqualTo(dto());
        }

        @Test
        @DisplayName("not found – returns null")
        void notFoundReturnsNull() {
            when(repository.findByEmail(email)).thenReturn(Optional.empty());
            assertThat(adapter.findByEmail(email)).isNull();
        }
    }

    @Nested
    @DisplayName("findPasswordHashByEmail")
    class FindPasswordHashByEmail {

        @Test
        @DisplayName("found – returns hash string")
        void foundReturnsHash() {
            when(repository.findByEmail(email)).thenReturn(Optional.of(entity()));
            assertThat(adapter.findPasswordHashByEmail(email)).isEqualTo(hash);
        }

        @Test
        @DisplayName("not found – returns null")
        void notFoundReturnsNull() {
            when(repository.findByEmail(email)).thenReturn(Optional.empty());
            assertThat(adapter.findPasswordHashByEmail(email)).isNull();
        }
    }

    @Test
    @DisplayName("findAll – maps all entities to DTOs")
    void findAllMapsAllEntities() {
        UserJpaEntity ent = entity();
        User          dom = domain();
        when(repository.findAll()).thenReturn(List.of(ent, ent));
        when(mapper.toDomain(ent)).thenReturn(dom);
        when(mapper.toDTO(dom)).thenReturn(dto());

        assertThat(adapter.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("findByRole – filters correctly by role string")
    void findByRoleFilters() {
        UserJpaEntity ent = entity();
        User          dom = domain();
        when(repository.findByRole("BURUH")).thenReturn(List.of(ent));
        when(mapper.toDomain(ent)).thenReturn(dom);
        when(mapper.toDTO(dom)).thenReturn(dto());

        assertThat(adapter.findByRole("BURUH")).hasSize(1);
    }

    @Test
    @DisplayName("findBuruhByMandorId – returns buruh list for mandor")
    void findBuruhByMandorIdReturnsList() {
        UserJpaEntity ent = entity();
        User          dom = domain();
        when(repository.findByMandorId(mandorId)).thenReturn(List.of(ent));
        when(mapper.toDomain(ent)).thenReturn(dom);
        when(mapper.toDTO(dom)).thenReturn(dto());

        assertThat(adapter.findBuruhByMandorId(mandorId)).hasSize(1);
    }

    @Test
    @DisplayName("deleteById – delegates to repository")
    void deleteByIdDelegates() {
        adapter.deleteById(userId);
        verify(repository).deleteById(userId);
    }

    @Test
    @DisplayName("existsById – delegates to repository")
    void existsByIdDelegates() {
        when(repository.existsById(userId)).thenReturn(true);
        assertThat(adapter.existsById(userId)).isTrue();
    }

    @Nested
    @DisplayName("saveBuruhMandorAssignment")
    class SaveAssignment {

        @Test
        @DisplayName("buruh found – sets mandorId and saves entity")
        void buruhFoundSetsMandorIdAndSaves() {
            UserJpaEntity ent = entity();
            when(repository.findById(userId)).thenReturn(Optional.of(ent));

            adapter.saveBuruhMandorAssignment(userId, mandorId);

            assertThat(ent.getMandorId()).isEqualTo(mandorId);
            verify(repository).save(ent);
        }

        @Test
        @DisplayName("buruh not found – no save called")
        void buruhNotFoundNoSave() {
            when(repository.findById(userId)).thenReturn(Optional.empty());

            adapter.saveBuruhMandorAssignment(userId, mandorId);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeBuruhMandorAssignment")
    class RemoveAssignment {

        @Test
        @DisplayName("buruh found – clears mandorId and saves entity")
        void buruhFoundClearsMandorIdAndSaves() {
            UserJpaEntity ent = entity();
            ent.setMandorId(mandorId);
            when(repository.findById(userId)).thenReturn(Optional.of(ent));

            adapter.removeBuruhMandorAssignment(userId);

            assertThat(ent.getMandorId()).isNull();
            verify(repository).save(ent);
        }

        @Test
        @DisplayName("buruh not found – no save called")
        void buruhNotFoundNoSave() {
            when(repository.findById(userId)).thenReturn(Optional.empty());

            adapter.removeBuruhMandorAssignment(userId);

            verify(repository, never()).save(any());
        }
    }
}