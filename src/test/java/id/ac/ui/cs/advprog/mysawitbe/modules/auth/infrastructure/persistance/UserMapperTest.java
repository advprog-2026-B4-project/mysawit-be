package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.User;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserMapper Tests")
class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    @DisplayName("Should map UserDTO to User domain")
    void testMapDTOToDomain() {
        UUID userId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        
        UserDTO dto = new UserDTO(
                userId,
                "testuser",
                "Test User",
                "BURUH",
                "test@example.com",
                null,
                mandorId
        );

        User user = userMapper.toDomain(dto);

        assertNotNull(user);
        assertEquals(userId, user.getUserId());
        assertEquals("testuser", user.getUsername());
        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(UserRole.BURUH, user.getRole());
        assertEquals(mandorId, user.getMandorId());
        assertNull(user.getMandorCertificationNumber());
        assertNull(user.getHashedPassword());
    }

    @Test
    @DisplayName("Should map User domain to UserDTO")
    void testMapDomainToDTO() {
        UUID userId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        User user = new User(
                userId,
                "testuser",
                "test@example.com",
                "Test User",
                "hashed_password",
                UserRole.BURUH,
                mandorId,
                null,
                now,
                now
        );

        UserDTO dto = userMapper.toDTO(user);

        assertNotNull(dto);
        assertEquals(userId, dto.userId());
        assertEquals("testuser", dto.username());
        assertEquals("Test User", dto.name());
        assertEquals("test@example.com", dto.email());
        assertEquals("BURUH", dto.role());
        assertEquals(mandorId, dto.mandorId());
    }

    @Test
    @DisplayName("Should map UserJpaEntity to User domain")
    void testMapEntityToDomain() {
        UUID userId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        UserJpaEntity entity = new UserJpaEntity();
        entity.setUserId(userId);
        entity.setUsername("testuser");
        entity.setEmail("test@example.com");
        entity.setName("Test User");
        entity.setPassword("hashed_password");
        entity.setRole("BURUH");
        entity.setMandorId(mandorId);
        entity.setMandorCertificationNumber(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        User user = userMapper.toDomain(entity);

        assertNotNull(user);
        assertEquals(userId, user.getUserId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getName());
        assertEquals("hashed_password", user.getHashedPassword());
        assertEquals(UserRole.BURUH, user.getRole());
        assertEquals(mandorId, user.getMandorId());
    }

    @Test
    @DisplayName("Should map User domain to UserJpaEntity")
    void testMapDomainToEntity() {
        UUID userId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        User user = new User(
                userId,
                "testuser",
                "test@example.com",
                "Test User",
                "hashed_password",
                UserRole.BURUH,
                mandorId,
                null,
                now,
                now
        );

        UserJpaEntity entity = userMapper.toEntity(user);

        assertNotNull(entity);
        assertEquals(userId, entity.getUserId());
        assertEquals("testuser", entity.getUsername());
        assertEquals("test@example.com", entity.getEmail());
        assertEquals("Test User", entity.getName());
        assertEquals("hashed_password", entity.getPassword());
        assertEquals("BURUH", entity.getRole());
        assertEquals(mandorId, entity.getMandorId());
    }

    @Test
    @DisplayName("Should handle mapping with MANDOR certification")
    void testMapMandorWithCertification() {
        String certNumber = "CERT-12345";
        UUID userId = UUID.randomUUID();
        
        UserDTO dto = new UserDTO(
                userId,
                "mandor",
                "Mandor User",
                "MANDOR",
                "mandor@example.com",
                certNumber,
                null
        );

        User user = userMapper.toDomain(dto);

        assertEquals(UserRole.MANDOR, user.getRole());
        assertEquals(certNumber, user.getMandorCertificationNumber());
    }

    @Test
    @DisplayName("Should handle mapping with ADMIN role")
    void testMapAdminRole() {
        UUID userId = UUID.randomUUID();
        
        UserDTO dto = new UserDTO(
                userId,
                "admin",
                "Admin User",
                "ADMIN",
                "admin@example.com",
                null,
                null
        );

        User user = userMapper.toDomain(dto);

        assertEquals(UserRole.ADMIN, user.getRole());
        assertTrue(user.isAdmin());
    }

    @Test
    @DisplayName("Should handle mapping with SUPIR role")
    void testMapSupirRole() {
        UUID userId = UUID.randomUUID();
        
        UserDTO dto = new UserDTO(
                userId,
                "supir",
                "Supir User",
                "SUPIR",
                "supir@example.com",
                null,
                null
        );

        User user = userMapper.toDomain(dto);

        assertEquals(UserRole.SUPIR, user.getRole());
    }

    @Test
    @DisplayName("Should map OAuth account (no password)")
    void testMapOAuthAccount() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        User oauthUser = new User(
                userId,
                "oauthuser",
                "oauth@example.com",
                "OAuth User",
                null,
                UserRole.BURUH,
                null,
                null,
                now,
                now
        );

        UserJpaEntity entity = userMapper.toEntity(oauthUser);

        assertNotNull(entity);
        assertNull(entity.getPassword());
        assertTrue(oauthUser.isOAuthAccount());
    }

    @Test
    @DisplayName("Should preserve all fields during round-trip mapping")
    void testRoundTripMapping() {
        UUID userId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        
        UserDTO originalDTO = new UserDTO(
                userId,
                "testuser",
                "Test User",
                "BURUH",
                "test@example.com",
                null,
                mandorId
        );

        User domain = userMapper.toDomain(originalDTO);
        UserJpaEntity entity = userMapper.toEntity(domain);
        User domainAgain = userMapper.toDomain(entity);
        UserDTO dtoAgain = userMapper.toDTO(domainAgain);

        assertEquals(originalDTO.userId(), dtoAgain.userId());
        assertEquals(originalDTO.username(), dtoAgain.username());
        assertEquals(originalDTO.name(), dtoAgain.name());
        assertEquals(originalDTO.email(), dtoAgain.email());
        assertEquals(originalDTO.role(), dtoAgain.role());
        assertEquals(originalDTO.mandorId(), dtoAgain.mandorId());
    }

    @Test
    @DisplayName("Should handle null mandor ID")
    void testMapWithNullMandorId() {
        UUID userId = UUID.randomUUID();
        
        UserDTO dto = new UserDTO(
                userId,
                "testuser",
                "Test User",
                "MANDOR",
                "test@example.com",
                "CERT-001",
                null
        );

        User user = userMapper.toDomain(dto);

        assertNull(user.getMandorId());
    }

    @Test
    @DisplayName("Should map entity with null certification number")
    void testMapEntityWithNullCertification() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        UserJpaEntity entity = new UserJpaEntity();
        entity.setUserId(userId);
        entity.setUsername("mandor");
        entity.setEmail("mandor@example.com");
        entity.setName("Mandor User");
        entity.setPassword("hashed");
        entity.setRole("MANDOR");
        entity.setMandorCertificationNumber(null);
        entity.setMandorId(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        User user = userMapper.toDomain(entity);

        assertNull(user.getMandorCertificationNumber());
    }

    @Test
    @DisplayName("Should map DTO with special characters in name")
    void testMapSpecialCharactersInName() {
        UUID userId = UUID.randomUUID();
        
        UserDTO dto = new UserDTO(
                userId,
                "testuser",
                "José María de los Ángeles",
                "BURUH",
                "test@example.com",
                null,
                null
        );

        User user = userMapper.toDomain(dto);

        assertEquals("José María de los Ángeles", user.getName());
    }

    @Test
    @DisplayName("Should map DTO with special characters in email")
    void testMapSpecialCharactersInEmail() {
        UUID userId = UUID.randomUUID();
        
        UserDTO dto = new UserDTO(
                userId,
                "testuser",
                "Test User",
                "BURUH",
                "user+tag@example.co.uk",
                null,
                null
        );

        User user = userMapper.toDomain(dto);

        assertEquals("user+tag@example.co.uk", user.getEmail());
    }

    @Test
    @DisplayName("Should map entity with all roles to DTO")
    void testMapAllRolesFromEntityToDTO() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        for (UserRole role : UserRole.values()) {
            UserJpaEntity entity = new UserJpaEntity();
            entity.setUserId(userId);
            entity.setUsername("user" + role.name().toLowerCase());
            entity.setEmail("user" + role.name().toLowerCase() + "@example.com");
            entity.setName("User " + role.name());
            entity.setPassword("hashed");
            entity.setRole(role.name());
            entity.setMandorId(null);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);

            User user = userMapper.toDomain(entity);
            UserDTO dto = userMapper.toDTO(user);

            assertEquals(role.name(), dto.role());
        }
    }

    @Test
    @DisplayName("Should map DTO with empty mandor certification to entity")
    void testMapDTOWithEmptyCertificationToEntity() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        UserDTO dto = new UserDTO(
                userId,
                "mandor",
                "Mandor",
                "MANDOR",
                "mandor@example.com",
                "",
                null
        );

        User user = userMapper.toDomain(dto);
        UserJpaEntity entity = userMapper.toEntity(user);

        assertNotNull(entity);
        assertEquals(userId, entity.getUserId());
    }

    @Test
    @DisplayName("Should map entity with mandor ID to DTO")
    void testMapEntityWithMandorIdToDTO() {
        UUID userId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        UserJpaEntity entity = new UserJpaEntity();
        entity.setUserId(userId);
        entity.setUsername("buruh");
        entity.setEmail("buruh@example.com");
        entity.setName("Buruh User");
        entity.setPassword("hashed");
        entity.setRole("BURUH");
        entity.setMandorId(mandorId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        User user = userMapper.toDomain(entity);
        UserDTO dto = userMapper.toDTO(user);

        assertEquals(mandorId, dto.mandorId());
    }

    @Test
    @DisplayName("Should map DTO and preserve role enum conversion")
    void testMapDTORoleEnumConversion() {
        UUID userId = UUID.randomUUID();

        for (String roleName : new String[]{"ADMIN", "BURUH", "MANDOR", "SUPIR"}) {
            UserDTO dto = new UserDTO(
                    userId,
                    "user",
                    "User",
                    roleName,
                    "user@example.com",
                    null,
                    null
            );

            User user = userMapper.toDomain(dto);

            assertEquals(UserRole.valueOf(roleName), user.getRole());
            assertNotNull(user.getRole());
        }
    }

    @Test
    @DisplayName("Should map entity to domain and preserve all attributes")
    void testMapEntityToDomainPreservesAllAttributes() {
        UUID userId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        String certNumber = "CERT-999";
        LocalDateTime now = LocalDateTime.now();

        UserJpaEntity entity = new UserJpaEntity();
        entity.setUserId(userId);
        entity.setUsername("complete");
        entity.setEmail("complete@example.com");
        entity.setName("Complete User");
        entity.setPassword("super_hashed");
        entity.setRole("MANDOR");
        entity.setMandorId(mandorId);
        entity.setMandorCertificationNumber(certNumber);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        User user = userMapper.toDomain(entity);

        assertEquals(userId, user.getUserId());
        assertEquals("complete", user.getUsername());
        assertEquals("complete@example.com", user.getEmail());
        assertEquals("Complete User", user.getName());
        assertEquals("super_hashed", user.getHashedPassword());
        assertEquals(UserRole.MANDOR, user.getRole());
        assertEquals(mandorId, user.getMandorId());
        assertEquals(certNumber, user.getMandorCertificationNumber());
    }
}
