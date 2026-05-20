package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.UserRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryUseCaseImpl Tests")
class UserQueryUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepository;

    private UserQueryUseCaseImpl userQueryUseCase;
    private UUID userId;
    private UUID mandorId;
    private UserDTO testUser;
    private UserDTO mandorUser;

    @BeforeEach
    void setUp() {
        userQueryUseCase = new UserQueryUseCaseImpl(userRepository);
        userId = UUID.randomUUID();
        mandorId = UUID.randomUUID();
        
        testUser = new UserDTO(
                userId,
                "testuser",
                "Test User",
                "BURUH",
                "test@example.com",
                null,
                mandorId
        );
        
        mandorUser = new UserDTO(
                mandorId,
                "mandor",
                "Mandor User",
                "MANDOR",
                "mandor@example.com",
                "CERT-12345",
                null
        );
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUserByIdSuccess() {
        when(userRepository.findById(userId)).thenReturn(testUser);

        UserDTO result = userQueryUseCase.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals("testuser", result.username());
        assertEquals("BURUH", result.role());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found")
    void testGetUserByIdNotFound() {
        when(userRepository.findById(userId)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> 
            userQueryUseCase.getUserById(userId)
        );
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should get user role by ID")
    void testGetUserRoleSuccess() {
        when(userRepository.findById(userId)).thenReturn(testUser);

        String role = userQueryUseCase.getUserRole(userId);

        assertEquals("BURUH", role);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when getting role for non-existent user")
    void testGetUserRoleNotFound() {
        when(userRepository.findById(userId)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> 
            userQueryUseCase.getUserRole(userId)
        );
    }

    @Test
    @DisplayName("Should get mandor ID by buruh ID")
    void testGetMandorIdByBuruhIdSuccess() {
        when(userRepository.findMandorIdByBuruhId(userId)).thenReturn(mandorId);

        UUID result = userQueryUseCase.getMandorIdByBuruhId(userId);

        assertEquals(mandorId, result);
        verify(userRepository, times(1)).findMandorIdByBuruhId(userId);
    }

    @Test
    @DisplayName("Should return null when buruh has no mandor")
    void testGetMandorIdByBuruhIdNoMandor() {
        when(userRepository.findMandorIdByBuruhId(userId)).thenReturn(null);

        UUID result = userQueryUseCase.getMandorIdByBuruhId(userId);

        assertNull(result);
        verify(userRepository, times(1)).findMandorIdByBuruhId(userId);
    }

    @Test
    @DisplayName("Should verify user exists")
    void testVerifyUserExistsTrue() {
        when(userRepository.existsById(userId)).thenReturn(true);

        boolean exists = userQueryUseCase.verifyUserExists(userId);

        assertTrue(exists);
        verify(userRepository, times(1)).existsById(userId);
    }

    @Test
    @DisplayName("Should verify user does not exist")
    void testVerifyUserExistsFalse() {
        when(userRepository.existsById(userId)).thenReturn(false);

        boolean exists = userQueryUseCase.verifyUserExists(userId);

        assertFalse(exists);
        verify(userRepository, times(1)).existsById(userId);
    }

    @Test
    @DisplayName("Should get buruh list by mandor ID")
    void testGetBuruhByMandorIdSuccess() {
        UUID buruh1Id = UUID.randomUUID();
        UUID buruh2Id = UUID.randomUUID();
        
        UserDTO buruh1 = new UserDTO(buruh1Id, "buruh1", "Buruh 1", "BURUH", "buruh1@example.com", null, mandorId);
        UserDTO buruh2 = new UserDTO(buruh2Id, "buruh2", "Buruh 2", "BURUH", "buruh2@example.com", null, mandorId);
        List<UserDTO> buruhList = List.of(buruh1, buruh2);

        when(userRepository.findBuruhByMandorId(mandorId)).thenReturn(buruhList);

        List<UserDTO> result = userQueryUseCase.getBuruhByMandorId(mandorId);

        assertEquals(2, result.size());
        assertEquals(buruh1Id, result.get(0).userId());
        assertEquals(buruh2Id, result.get(1).userId());
        verify(userRepository, times(1)).findBuruhByMandorId(mandorId);
    }

    @Test
    @DisplayName("Should return empty list when mandor has no buruh")
    void testGetBuruhByMandorIdEmpty() {
        when(userRepository.findBuruhByMandorId(mandorId)).thenReturn(List.of());

        List<UserDTO> result = userQueryUseCase.getBuruhByMandorId(mandorId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findBuruhByMandorId(mandorId);
    }

    @Test
    @DisplayName("Should list all users without role filter")
    void testListUsersWithoutRoleFilter() {
        UUID admin1Id = UUID.randomUUID();
        UserDTO admin1 = new UserDTO(admin1Id, "admin1", "Admin 1", "ADMIN", "admin1@example.com", null, null);
        List<UserDTO> allUsers = List.of(testUser, mandorUser, admin1);

        when(userRepository.findAll()).thenReturn(allUsers);

        List<UserDTO> result = userQueryUseCase.listUsers(null);

        assertEquals(3, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should list users with role filter")
    void testListUsersWithRoleFilter() {
        UUID buruh2Id = UUID.randomUUID();
        UserDTO buruh2 = new UserDTO(buruh2Id, "buruh2", "Buruh 2", "BURUH", "buruh2@example.com", null, mandorId);
        List<UserDTO> buruhUsers = List.of(testUser, buruh2);

        when(userRepository.findByRole("BURUH")).thenReturn(buruhUsers);

        List<UserDTO> result = userQueryUseCase.listUsers("BURUH");

        assertEquals(2, result.size());
        assertEquals("BURUH", result.get(0).role());
        assertEquals("BURUH", result.get(1).role());
        verify(userRepository, times(1)).findByRole("BURUH");
    }

    @Test
    @DisplayName("Should convert role filter to uppercase")
    void testListUsersRoleFilterToUppercase() {
        when(userRepository.findByRole("MANDOR")).thenReturn(List.of(mandorUser));

        userQueryUseCase.listUsers("mandor");

        verify(userRepository, times(1)).findByRole("MANDOR");
    }

    @Test
    @DisplayName("Should list users with search filter")
    void testListUsersWithSearchFilter() {
        UUID buruh2Id = UUID.randomUUID();
        UserDTO buruh2 = new UserDTO(buruh2Id, "buruh2", "Test Buruh", "BURUH", "buruh2@example.com", null, mandorId);
        List<UserDTO> allUsers = List.of(testUser, buruh2, mandorUser);

        when(userRepository.findAll()).thenReturn(allUsers);

        List<UserDTO> result = userQueryUseCase.listUsers(null, "test");

        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should search by name case-insensitive")
    void testListUsersSearchByNameCaseInsensitive() {
        List<UserDTO> allUsers = List.of(testUser, mandorUser);

        when(userRepository.findAll()).thenReturn(allUsers);

        List<UserDTO> result = userQueryUseCase.listUsers(null, "MANDOR");

        assertEquals(1, result.size());
        assertEquals(mandorId, result.get(0).userId());
    }

    @Test
    @DisplayName("Should search by email")
    void testListUsersSearchByEmail() {
        List<UserDTO> allUsers = List.of(testUser, mandorUser);

        when(userRepository.findAll()).thenReturn(allUsers);

        List<UserDTO> result = userQueryUseCase.listUsers(null, "mandor@");

        assertEquals(1, result.size());
        assertEquals("mandor@example.com", result.get(0).email());
    }

    @Test
    @DisplayName("Should return empty list for non-matching search")
    void testListUsersSearchNoMatch() {
        List<UserDTO> allUsers = List.of(testUser, mandorUser);

        when(userRepository.findAll()).thenReturn(allUsers);

        List<UserDTO> result = userQueryUseCase.listUsers(null, "nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should list users with role filter and search")
    void testListUsersWithRoleFilterAndSearch() {
        UUID buruh2Id = UUID.randomUUID();
        UserDTO buruh2 = new UserDTO(buruh2Id, "buruh2", "Test Buruh", "BURUH", "buruh2@example.com", null, mandorId);
        List<UserDTO> buruhUsers = List.of(testUser, buruh2);

        when(userRepository.findByRole("BURUH")).thenReturn(buruhUsers);

        List<UserDTO> result = userQueryUseCase.listUsers("BURUH", "test");

        assertEquals(2, result.size());
        verify(userRepository, times(1)).findByRole("BURUH");
    }

    @Test
    @DisplayName("Should get any admin ID successfully")
    void testGetAnyAdminIdSuccess() {
        UUID adminId = UUID.randomUUID();
        UserDTO admin = new UserDTO(adminId, "admin", "Admin User", "ADMIN", "admin@example.com", null, null);
        List<UserDTO> adminList = List.of(admin);

        when(userRepository.findByRole("ADMIN")).thenReturn(adminList);

        UUID result = userQueryUseCase.getAnyAdminId();

        assertEquals(adminId, result);
        verify(userRepository, times(1)).findByRole("ADMIN");
    }

    @Test
    @DisplayName("Should get first admin when multiple admins exist")
    void testGetAnyAdminIdWithMultipleAdmins() {
        UUID admin1Id = UUID.randomUUID();
        UUID admin2Id = UUID.randomUUID();
        UserDTO admin1 = new UserDTO(admin1Id, "admin1", "Admin 1", "ADMIN", "admin1@example.com", null, null);
        UserDTO admin2 = new UserDTO(admin2Id, "admin2", "Admin 2", "ADMIN", "admin2@example.com", null, null);
        List<UserDTO> adminList = List.of(admin1, admin2);

        when(userRepository.findByRole("ADMIN")).thenReturn(adminList);

        UUID result = userQueryUseCase.getAnyAdminId();

        assertEquals(admin1Id, result);
        verify(userRepository, times(1)).findByRole("ADMIN");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when no admin exists")
    void testGetAnyAdminIdNotFound() {
        when(userRepository.findByRole("ADMIN")).thenReturn(List.of());

        assertThrows(EntityNotFoundException.class, () -> 
            userQueryUseCase.getAnyAdminId()
        );
        verify(userRepository, times(1)).findByRole("ADMIN");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when admin list is null")
    void testGetAnyAdminIdNull() {
        when(userRepository.findByRole("ADMIN")).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> 
            userQueryUseCase.getAnyAdminId()
        );
    }

    @Test
    @DisplayName("Should handle blank role filter")
    void testListUsersWithBlankRoleFilter() {
        UUID admin1Id = UUID.randomUUID();
        UserDTO admin1 = new UserDTO(admin1Id, "admin1", "Admin 1", "ADMIN", "admin1@example.com", null, null);
        List<UserDTO> allUsers = List.of(testUser, mandorUser, admin1);

        when(userRepository.findAll()).thenReturn(allUsers);

        List<UserDTO> result = userQueryUseCase.listUsers("   ");

        assertEquals(3, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle blank search filter")
    void testListUsersWithBlankSearchFilter() {
        List<UserDTO> allUsers = List.of(testUser, mandorUser);

        when(userRepository.findAll()).thenReturn(allUsers);

        List<UserDTO> result = userQueryUseCase.listUsers(null, "   ");

        assertEquals(2, result.size());
    }
}
