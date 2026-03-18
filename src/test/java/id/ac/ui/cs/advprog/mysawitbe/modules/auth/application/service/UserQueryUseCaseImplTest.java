package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.UserRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryUseCaseImpl")
class UserQueryUseCaseImplTest {

    @Mock UserRepositoryPort userRepository;

    UserQueryUseCaseImpl service;

    final UUID userId   = UUID.randomUUID();
    final UUID mandorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UserQueryUseCaseImpl(userRepository);
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("found – returns user DTO")
        void foundReturnsUser() {
            UserDTO user = new UserDTO(userId, "u", "Name", "BURUH", "u@t.com");
            when(userRepository.findById(userId)).thenReturn(user);

            assertThat(service.getUserById(userId)).isEqualTo(user);
        }

        @Test
        @DisplayName("not found – throws EntityNotFoundException with userId")
        void notFoundThrowsWithUserId() {
            when(userRepository.findById(userId)).thenReturn(null);

            assertThatThrownBy(() -> service.getUserById(userId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(userId.toString());
        }
    }

    @Nested
    @DisplayName("getUserRole")
    class GetUserRole {

        @Test
        @DisplayName("returns role string of found user")
        void returnsRoleString() {
            UserDTO user = new UserDTO(userId, "u", "Name", "MANDOR", "u@t.com");
            when(userRepository.findById(userId)).thenReturn(user);

            assertThat(service.getUserRole(userId)).isEqualTo("MANDOR");
        }

        @Test
        @DisplayName("user not found – throws EntityNotFoundException")
        void userNotFoundThrows() {
            when(userRepository.findById(userId)).thenReturn(null);

            assertThatThrownBy(() -> service.getUserRole(userId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Test
    @DisplayName("verifyUserExists – delegates true/false to repository")
    void verifyUserExistsDelegates() {
        when(userRepository.existsById(userId)).thenReturn(true);
        assertThat(service.verifyUserExists(userId)).isTrue();

        when(userRepository.existsById(userId)).thenReturn(false);
        assertThat(service.verifyUserExists(userId)).isFalse();
    }

    @Test
    @DisplayName("getBuruhByMandorId – returns list from repository")
    void getBuruhByMandorIdReturnsList() {
        List<UserDTO> list = List.of(
                new UserDTO(UUID.randomUUID(), "b1", "Buruh 1", "BURUH", "b1@t.com"),
                new UserDTO(UUID.randomUUID(), "b2", "Buruh 2", "BURUH", "b2@t.com")
        );
        when(userRepository.findBuruhByMandorId(mandorId)).thenReturn(list);

        assertThat(service.getBuruhByMandorId(mandorId)).hasSize(2);
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsers {

        final List<UserDTO> all = List.of(
                new UserDTO(UUID.randomUUID(), "a",  "Admin User",  "ADMIN",  "a@t.com"),
                new UserDTO(UUID.randomUUID(), "b",  "Buruh One",   "BURUH",  "b@t.com"),
                new UserDTO(UUID.randomUUID(), "m",  "Mandor Budi", "MANDOR", "budi@t.com")
        );

        @Test
        @DisplayName("null filter – returns all users from repository")
        void nullFilterReturnsAll() {
            when(userRepository.findAll()).thenReturn(all);

            assertThat(service.listUsers(null)).hasSize(3);
            verify(userRepository).findAll();
            verify(userRepository, never()).findByRole(any());
        }

        @Test
        @DisplayName("blank filter – returns all users from repository")
        void blankFilterReturnsAll() {
            when(userRepository.findAll()).thenReturn(all);

            assertThat(service.listUsers("  ")).hasSize(3);
        }

        @Test
        @DisplayName("role filter applied correctly")
        void roleFilterApplied() {
            when(userRepository.findByRole("BURUH")).thenReturn(List.of(all.get(1)));

            List<UserDTO> result = service.listUsers("BURUH");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).role()).isEqualTo("BURUH");
        }

        @Test
        @DisplayName("role filter uppercased before repository query")
        void roleFilterUppercased() {
            when(userRepository.findByRole("MANDOR")).thenReturn(List.of());

            service.listUsers("mandor");

            verify(userRepository).findByRole("MANDOR");
        }

        @Test
        @DisplayName("search filters by name (case-insensitive)")
        void searchFiltersByName() {
            when(userRepository.findAll()).thenReturn(all);

            List<UserDTO> result = service.listUsers(null, "budi");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Mandor Budi");
        }

        @Test
        @DisplayName("search filters by email")
        void searchFiltersByEmail() {
            when(userRepository.findAll()).thenReturn(all);

            List<UserDTO> result = service.listUsers(null, "b@t.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).email()).isEqualTo("b@t.com");
        }

        @Test
        @DisplayName("search is case-insensitive")
        void searchIsCaseInsensitive() {
            when(userRepository.findAll()).thenReturn(all);

            assertThat(service.listUsers(null, "BURUH ONE")).hasSize(1);
        }

        @Test
        @DisplayName("blank search returns full list without filtering")
        void blankSearchReturnsAll() {
            when(userRepository.findAll()).thenReturn(all);

            assertThat(service.listUsers(null, "   ")).hasSize(3);
        }

        @Test
        @DisplayName("role and search filters combined")
        void roleAndSearchCombined() {
            when(userRepository.findByRole("BURUH")).thenReturn(List.of(all.get(1)));

            List<UserDTO> result = service.listUsers("BURUH", "One");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("no search match returns empty list")
        void noMatchReturnsEmpty() {
            when(userRepository.findAll()).thenReturn(all);

            assertThat(service.listUsers(null, "zzznomatch")).isEmpty();
        }
    }
}