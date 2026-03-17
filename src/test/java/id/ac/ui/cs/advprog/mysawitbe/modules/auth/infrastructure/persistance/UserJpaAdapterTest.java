package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.persistance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserJpaAdapter")
class UserJpaAdapterTest {

    @Mock UserJpaRepository repository;
    @Mock UserMapper        mapper;

    UserJpaAdapter adapter;

    @BeforeEach
    void setUp() {
        // TODO: instantiate adapter with mocked dependencies
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("new user (null userId) creates and persists entity")
        void newUserCreatesAndPersistsEntity() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("existing user updates name, email, role on found entity")
        void existingUserUpdatesFields() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("existing user not in DB falls back to mapper entity")
        void existingUserNotFoundFallsBack() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("null hashedPassword does not overwrite existing password")
        void nullPasswordIsNotWrittenToEntity() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("found returns mapped DTO")
        void foundReturnsMappedDto() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("not found returns null")
        void notFoundReturnsNull() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("found returns mapped DTO")
        void foundReturnsMappedDto() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("not found returns null")
        void notFoundReturnsNull() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("findPasswordHashByEmail")
    class FindPasswordHashByEmail {

        @Test
        @DisplayName("found returns hash string")
        void foundReturnsHash() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("not found returns null")
        void notFoundReturnsNull() {
            fail("not yet implemented");
        }
    }

    @Test
    @DisplayName("findAll maps all entities to DTOs")
    void findAllMapsAllEntities() {
        fail("not yet implemented");
    }

    @Test
    @DisplayName("findByRole filters correctly by role string")
    void findByRoleFilters() {
        fail("not yet implemented");
    }

    @Test
    @DisplayName("findBuruhByMandorId returns buruh list for mandor")
    void findBuruhByMandorIdReturnsList() {
        fail("not yet implemented");
    }

    @Test
    @DisplayName("deleteById delegates to repository")
    void deleteByIdDelegates() {
        fail("not yet implemented");
    }

    @Test
    @DisplayName("existsById delegates to repository")
    void existsByIdDelegates() {
        fail("not yet implemented");
    }

    @Nested
    @DisplayName("saveBuruhMandorAssignment")
    class SaveAssignment {

        @Test
        @DisplayName("buruh found sets mandorId and saves entity")
        void buruhFoundSetsMandorIdAndSaves() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("buruh not found no save called")
        void buruhNotFoundNoSave() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("removeBuruhMandorAssignment")
    class RemoveAssignment {

        @Test
        @DisplayName("buruh found clears mandorId and saves entity")
        void buruhFoundClearsMandorIdAndSaves() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("buruh not found no save called")
        void buruhNotFoundNoSave() {
            fail("not yet implemented");
        }
    }
}