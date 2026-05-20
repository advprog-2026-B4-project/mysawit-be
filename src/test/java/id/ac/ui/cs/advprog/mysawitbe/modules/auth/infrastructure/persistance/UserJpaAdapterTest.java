package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.User;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserJpaAdapter Tests")
class UserJpaAdapterTest {

    @Mock
    private UserJpaRepository repository;

    @Mock
    private UserMapper mapper;

    private UserJpaAdapter adapter;
    private UUID userId;
    private UUID mandorId;
    private UserDTO userDTO;
    private UserJpaEntity userEntity;
    private User userDomain;

    @BeforeEach
    void setUp() {
        adapter = new UserJpaAdapter(repository, mapper);
        userId = UUID.randomUUID();
        mandorId = UUID.randomUUID();

        userDTO = new UserDTO(
                userId,
                "testuser",
                "Test User",
                "BURUH",
                "test@example.com",
                null,
                mandorId
        );

        userDomain = new User(
                userId,
                "testuser",
                "test@example.com",
                "Test User",
                "hashed_password",
                UserRole.BURUH,
                mandorId,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        userEntity = new UserJpaEntity();
        userEntity.setUserId(userId);
        userEntity.setUsername("testuser");
        userEntity.setEmail("test@example.com");
        userEntity.setName("Test User");
        userEntity.setPassword("hashed_password");
        userEntity.setRole("BURUH");
        userEntity.setMandorId(mandorId);
    }

    @Test
    @DisplayName("Should save new user successfully")
    void testSaveNewUser() {
        String hashedPassword = "hashed_password_123";

        when(mapper.toDomain(userDTO)).thenReturn(userDomain);
        when(mapper.toEntity(userDomain)).thenReturn(userEntity);
        when(repository.save(userEntity)).thenReturn(userEntity);
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);
        when(mapper.toDTO(userDomain)).thenReturn(userDTO);

        UserDTO result = adapter.save(userDTO, hashedPassword);

        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals("testuser", result.username());
        verify(mapper, times(1)).toDomain(userDTO);
        verify(repository, times(1)).save(any(UserJpaEntity.class));
    }

    @Test
    @DisplayName("Should update existing user successfully")
    void testUpdateExistingUser() {
        String hashedPassword = "hashed_password_123";

        when(mapper.toDomain(userDTO)).thenReturn(userDomain);
        when(repository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(repository.save(any(UserJpaEntity.class))).thenReturn(userEntity);
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);
        when(mapper.toDTO(userDomain)).thenReturn(userDTO);

        UserDTO result = adapter.save(userDTO, hashedPassword);

        assertNotNull(result);
        assertEquals(userId, result.userId());
        verify(repository, times(1)).findById(userId);
        verify(repository, times(1)).save(any(UserJpaEntity.class));
    }

    @Test
    @DisplayName("Should save user without password (OAuth account)")
    void testSaveUserWithoutPassword() {
        UserDTO oauthUserDTO = new UserDTO(
                userId,
                "oauthuser",
                "OAuth User",
                "BURUH",
                "oauth@example.com",
                null,
                null
        );

        when(mapper.toDomain(oauthUserDTO)).thenReturn(userDomain);
        when(mapper.toEntity(userDomain)).thenReturn(userEntity);
        when(repository.save(userEntity)).thenReturn(userEntity);
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);
        when(mapper.toDTO(userDomain)).thenReturn(userDTO);

        UserDTO result = adapter.save(oauthUserDTO, null);

        assertNotNull(result);
        verify(repository, times(1)).save(any(UserJpaEntity.class));
    }

    @Test
    @DisplayName("Should find user by ID successfully")
    void testFindByIdSuccess() {
        when(repository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);
        when(mapper.toDTO(userDomain)).thenReturn(userDTO);

        UserDTO result = adapter.findById(userId);

        assertNotNull(result);
        assertEquals(userId, result.userId());
        verify(repository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should return null when user not found by ID")
    void testFindByIdNotFound() {
        when(repository.findById(userId)).thenReturn(Optional.empty());

        UserDTO result = adapter.findById(userId);

        assertNull(result);
        verify(repository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should find user by email successfully")
    void testFindByEmailSuccess() {
        String email = "test@example.com";

        when(repository.findByEmail(email)).thenReturn(Optional.of(userEntity));
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);
        when(mapper.toDTO(userDomain)).thenReturn(userDTO);

        UserDTO result = adapter.findByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.email());
        verify(repository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Should return null when user not found by email")
    void testFindByEmailNotFound() {
        String email = "nonexistent@example.com";

        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        UserDTO result = adapter.findByEmail(email);

        assertNull(result);
        verify(repository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Should find password hash by email")
    void testFindPasswordHashByEmailSuccess() {
        String email = "test@example.com";
        String hashedPassword = "hashed_password_123";

        when(repository.findByEmail(email)).thenReturn(Optional.of(userEntity));

        String result = adapter.findPasswordHashByEmail(email);

        assertNotNull(result);
        verify(repository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Should return null when password hash not found")
    void testFindPasswordHashByEmailNotFound() {
        String email = "nonexistent@example.com";

        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        String result = adapter.findPasswordHashByEmail(email);

        assertNull(result);
    }

    @Test
    @DisplayName("Should find all users")
    void testFindAll() {
        UserJpaEntity user2 = new UserJpaEntity();
        UUID user2Id = UUID.randomUUID();
        user2.setUserId(user2Id);
        List<UserJpaEntity> entities = List.of(userEntity, user2);

        when(repository.findAll()).thenReturn(entities);
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);
        when(mapper.toDTO(userDomain)).thenReturn(userDTO);

        UserDTO user2DTO = new UserDTO(user2Id, "user2", "User 2", "MANDOR", "user2@example.com", "CERT-001", null);
        User user2Domain = new User();
        when(mapper.toDomain(user2)).thenReturn(user2Domain);
        when(mapper.toDTO(user2Domain)).thenReturn(user2DTO);

        List<UserDTO> result = adapter.findAll();

        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find users by role")
    void testFindByRoleSuccess() {
        String role = "BURUH";
        UserJpaEntity user2 = new UserJpaEntity();
        UUID user2Id = UUID.randomUUID();
        user2.setUserId(user2Id);
        List<UserJpaEntity> entities = List.of(userEntity, user2);

        when(repository.findByRole(role)).thenReturn(entities);
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);
        when(mapper.toDTO(userDomain)).thenReturn(userDTO);

        UserDTO user2DTO = new UserDTO(user2Id, "user2", "User 2", role, "user2@example.com", null, mandorId);
        User user2Domain = new User();
        when(mapper.toDomain(user2)).thenReturn(user2Domain);
        when(mapper.toDTO(user2Domain)).thenReturn(user2DTO);

        List<UserDTO> result = adapter.findByRole(role);

        assertEquals(2, result.size());
        verify(repository, times(1)).findByRole(role);
    }

    @Test
    @DisplayName("Should return empty list when no users found by role")
    void testFindByRoleEmpty() {
        String role = "ADMIN";

        when(repository.findByRole(role)).thenReturn(List.of());

        List<UserDTO> result = adapter.findByRole(role);

        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByRole(role);
    }

    @Test
    @DisplayName("Should find buruh by mandor ID")
    void testFindBuruhByMandorIdSuccess() {
        UserJpaEntity buruh2 = new UserJpaEntity();
        UUID buruh2Id = UUID.randomUUID();
        buruh2.setUserId(buruh2Id);
        List<UserJpaEntity> entities = List.of(userEntity, buruh2);

        when(repository.findByMandorId(mandorId)).thenReturn(entities);
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);
        when(mapper.toDTO(userDomain)).thenReturn(userDTO);

        UserDTO buruh2DTO = new UserDTO(buruh2Id, "buruh2", "Buruh 2", "BURUH", "buruh2@example.com", null, mandorId);
        User buruh2Domain = new User();
        when(mapper.toDomain(buruh2)).thenReturn(buruh2Domain);
        when(mapper.toDTO(buruh2Domain)).thenReturn(buruh2DTO);

        List<UserDTO> result = adapter.findBuruhByMandorId(mandorId);

        assertEquals(2, result.size());
        verify(repository, times(1)).findByMandorId(mandorId);
    }

    @Test
    @DisplayName("Should return empty list when no buruh found for mandor")
    void testFindBuruhByMandorIdEmpty() {
        when(repository.findByMandorId(mandorId)).thenReturn(List.of());

        List<UserDTO> result = adapter.findBuruhByMandorId(mandorId);

        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByMandorId(mandorId);
    }

    @Test
    @DisplayName("Should delete user by ID")
    void testDeleteById() {
        adapter.deleteById(userId);

        verify(repository, times(1)).deleteById(userId);
    }

    @Test
    @DisplayName("Should check if user exists")
    void testExistsByIdTrue() {
        when(repository.existsById(userId)).thenReturn(true);

        boolean exists = adapter.existsById(userId);

        assertTrue(exists);
        verify(repository, times(1)).existsById(userId);
    }

    @Test
    @DisplayName("Should check if user does not exist")
    void testExistsByIdFalse() {
        when(repository.existsById(userId)).thenReturn(false);

        boolean exists = adapter.existsById(userId);

        assertFalse(exists);
        verify(repository, times(1)).existsById(userId);
    }

    @Test
    @DisplayName("Should find mandor ID by buruh ID")
    void testFindMandorIdByBuruhIdSuccess() {
        when(repository.findById(userId)).thenReturn(Optional.of(userEntity));

        UUID result = adapter.findMandorIdByBuruhId(userId);

        assertNotNull(result);
        verify(repository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should return null when mandor ID not found")
    void testFindMandorIdByBuruhIdNotFound() {
        UserJpaEntity userWithoutMandor = new UserJpaEntity();
        userWithoutMandor.setUserId(userId);
        userWithoutMandor.setMandorId(null);

        when(repository.findById(userId)).thenReturn(Optional.of(userWithoutMandor));

        UUID result = adapter.findMandorIdByBuruhId(userId);

        assertNull(result);
    }

    @Test
    @DisplayName("Should save buruh-mandor assignment")
    void testSaveBuruhMandorAssignment() {
        UUID buruhId = UUID.randomUUID();
        UUID mandorAssignId = UUID.randomUUID();
        UserJpaEntity buruhEntity = new UserJpaEntity();
        buruhEntity.setUserId(buruhId);

        when(repository.findById(buruhId)).thenReturn(Optional.of(buruhEntity));
        when(repository.save(buruhEntity)).thenReturn(buruhEntity);

        adapter.saveBuruhMandorAssignment(buruhId, mandorAssignId);

        assertEquals(mandorAssignId, buruhEntity.getMandorId());
        verify(repository, times(1)).findById(buruhId);
        verify(repository, times(1)).save(buruhEntity);
    }

    @Test
    @DisplayName("Should remove buruh-mandor assignment")
    void testRemoveBuruhMandorAssignment() {
        UUID buruhId = UUID.randomUUID();
        UserJpaEntity buruhEntity = new UserJpaEntity();
        buruhEntity.setUserId(buruhId);
        buruhEntity.setMandorId(mandorId);

        when(repository.findById(buruhId)).thenReturn(Optional.of(buruhEntity));
        when(repository.save(buruhEntity)).thenReturn(buruhEntity);

        adapter.removeBuruhMandorAssignment(buruhId);

        assertNull(buruhEntity.getMandorId());
        verify(repository, times(1)).findById(buruhId);
        verify(repository, times(1)).save(buruhEntity);
    }

    @Test
    @DisplayName("Should handle saving user with mandor certification")
    void testSaveUserWithMandorCertification() {
        String certNumber = "CERT-12345";
        UserDTO mandorDTO = new UserDTO(
                userId,
                "mandor",
                "Mandor User",
                "MANDOR",
                "mandor@example.com",
                certNumber,
                null
        );

        User mandorDomain = new User(
                userId,
                "mandor",
                "mandor@example.com",
                "Mandor User",
                "hashed_password",
                UserRole.MANDOR,
                null,
                certNumber,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(mapper.toDomain(mandorDTO)).thenReturn(mandorDomain);
        when(mapper.toEntity(mandorDomain)).thenReturn(userEntity);
        when(repository.save(userEntity)).thenReturn(userEntity);
        when(mapper.toDomain(userEntity)).thenReturn(mandorDomain);
        when(mapper.toDTO(mandorDomain)).thenReturn(mandorDTO);

        UserDTO result = adapter.save(mandorDTO, "hashed_password");

        assertNotNull(result);
        verify(repository, times(1)).save(any(UserJpaEntity.class));
    }
}
