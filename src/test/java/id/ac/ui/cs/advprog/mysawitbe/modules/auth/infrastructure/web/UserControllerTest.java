package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @Mock
    private AuthCommandUseCase authCommandUseCase;

    private UserController userController;
    private UUID userId;
    private UUID mandorId;

    @BeforeEach
    void setUp() {
        userController = new UserController(userQueryUseCase, authCommandUseCase);
        userId = UUID.randomUUID();
        mandorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should get user by ID")
    void testGetUserById() {
        UserDTO user = new UserDTO(userId, "testuser", "Test User", "BURUH", "test@example.com", null, mandorId);

        when(userQueryUseCase.getUserById(userId)).thenReturn(user);

        ResponseEntity<ApiResponse<UserDTO>> result = userController.getUser(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().success());
        assertEquals(userId, result.getBody().data().userId());
        verify(userQueryUseCase, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("Should edit user")
    void testEditUser() {
        String newName = "Updated Name";
        String newRole = "MANDOR";
        String newEmail = "updated@example.com";
        String certNumber = "CERT-001";
        UserDTO inputDto = new UserDTO(userId, "testuser", newName, newRole, newEmail, certNumber, null);
        UserDTO updatedUser = new UserDTO(userId, "testuser", newName, newRole, newEmail, certNumber, null);

        when(authCommandUseCase.editUser(userId, newName, newRole, newEmail, certNumber)).thenReturn(updatedUser);

        ResponseEntity<ApiResponse<UserDTO>> result = userController.editUser(userId, inputDto);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().success());
        assertEquals(newName, result.getBody().data().name());
        assertEquals(newRole, result.getBody().data().role());
        verify(authCommandUseCase, times(1)).editUser(userId, newName, newRole, newEmail, certNumber);
    }

    @Test
    @DisplayName("Should delete user")
    void testDeleteUser() {
        UUID adminId = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> result = userController.deleteUser(userId, adminId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        verify(authCommandUseCase, times(1)).deleteUser(adminId, userId);
    }

    @Test
    @DisplayName("Should assign buruh to mandor")
    void testAssignBuruhToMandor() {
        UUID buruhId = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> result = userController.assignBuruhToMandor(buruhId, mandorId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        verify(authCommandUseCase, times(1)).assignBuruhToMandor(buruhId, mandorId);
    }

    @Test
    @DisplayName("Should unassign buruh from mandor")
    void testUnassignBuruh() {
        UUID buruhId = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> result = userController.unassignBuruh(buruhId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        verify(authCommandUseCase, times(1)).unassignBuruhFromMandor(buruhId);
    }

    @Test
    @DisplayName("Should reassign buruh from current mandor to new mandor")
    void testReassignBuruh() {
        UUID buruhId = UUID.randomUUID();
        UUID newMandorId = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> result = userController.reassignBuruh(buruhId, newMandorId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        verify(authCommandUseCase, times(1)).unassignBuruhFromMandor(buruhId);
        verify(authCommandUseCase, times(1)).assignBuruhToMandor(buruhId, newMandorId);
    }

    @Test
    @DisplayName("Should list all users")
    void testListAllUsers() {
        UserDTO user1 = new UserDTO(UUID.randomUUID(), "user1", "User 1", "BURUH", "user1@example.com", null, mandorId);
        UserDTO user2 = new UserDTO(UUID.randomUUID(), "user2", "User 2", "MANDOR", "user2@example.com", "CERT-001", null);
        List<UserDTO> users = List.of(user1, user2);

        when(userQueryUseCase.listUsers(null, null)).thenReturn(users);

        ResponseEntity<ApiResponse<List<UserDTO>>> result = userController.listUsers(null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().success());
        assertEquals(2, result.getBody().data().size());
        verify(userQueryUseCase, times(1)).listUsers(null, null);
    }

    @Test
    @DisplayName("Should list users by role filter")
    void testListUsersByRole() {
        String role = "BURUH";
        UserDTO user1 = new UserDTO(UUID.randomUUID(), "user1", "User 1", role, "user1@example.com", null, mandorId);
        UserDTO user2 = new UserDTO(UUID.randomUUID(), "user2", "User 2", role, "user2@example.com", null, mandorId);
        List<UserDTO> users = List.of(user1, user2);

        when(userQueryUseCase.listUsers(role, null)).thenReturn(users);

        ResponseEntity<ApiResponse<List<UserDTO>>> result = userController.listUsers(role, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(2, result.getBody().data().size());
        verify(userQueryUseCase, times(1)).listUsers(role, null);
    }

    @Test
    @DisplayName("Should list users with search filter")
    void testListUsersWithSearch() {
        String search = "test";
        UserDTO user1 = new UserDTO(UUID.randomUUID(), "user1", "Test User 1", "BURUH", "test1@example.com", null, mandorId);
        List<UserDTO> users = List.of(user1);

        when(userQueryUseCase.listUsers(null, search)).thenReturn(users);

        ResponseEntity<ApiResponse<List<UserDTO>>> result = userController.listUsers(null, search);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(1, result.getBody().data().size());
        verify(userQueryUseCase, times(1)).listUsers(null, search);
    }

    @Test
    @DisplayName("Should get buruh by mandor")
    void testGetBuruhByMandor() {
        UserDTO buruh1 = new UserDTO(UUID.randomUUID(), "buruh1", "Buruh 1", "BURUH", "buruh1@example.com", null, mandorId);
        UserDTO buruh2 = new UserDTO(UUID.randomUUID(), "buruh2", "Buruh 2", "BURUH", "buruh2@example.com", null, mandorId);
        List<UserDTO> buruhList = List.of(buruh1, buruh2);

        when(userQueryUseCase.getBuruhByMandorId(mandorId)).thenReturn(buruhList);

        ResponseEntity<ApiResponse<List<UserDTO>>> result = userController.getBuruhByMandor(mandorId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(2, result.getBody().data().size());
        verify(userQueryUseCase, times(1)).getBuruhByMandorId(mandorId);
    }

    @Test
    @DisplayName("Should handle user not found")
    void testGetUserNotFound() {
        UUID nonExistentUserId = UUID.randomUUID();

        when(userQueryUseCase.getUserById(nonExistentUserId))
                .thenThrow(new IllegalArgumentException("User not found"));

        assertThrows(IllegalArgumentException.class, () -> userController.getUser(nonExistentUserId));
        verify(userQueryUseCase, times(1)).getUserById(nonExistentUserId);
    }

    @Test
    @DisplayName("Should handle edit user with null name")
    void testEditUserWithNullName() {
        UserDTO inputDto = new UserDTO(userId, "testuser", null, "BURUH", "test@example.com", null, null);

        when(authCommandUseCase.editUser(userId, null, "BURUH", "test@example.com", null))
                .thenThrow(new IllegalArgumentException("Name cannot be null"));

        assertThrows(IllegalArgumentException.class, () -> userController.editUser(userId, inputDto));
    }

    @Test
    @DisplayName("Should handle delete user with invalid admin")
    void testDeleteUserWithInvalidAdmin() {
        UUID invalidAdminId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("User is not an admin"))
                .when(authCommandUseCase).deleteUser(invalidAdminId, userId);

        assertThrows(IllegalArgumentException.class, () -> userController.deleteUser(userId, invalidAdminId));
    }

    @Test
    @DisplayName("Should handle assign buruh to mandor with invalid mandor")
    void testAssignBuruhToInvalidMandor() {
        UUID buruhId = UUID.randomUUID();
        UUID invalidMandorId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Mandor not found"))
                .when(authCommandUseCase).assignBuruhToMandor(buruhId, invalidMandorId);

        assertThrows(IllegalArgumentException.class, 
                () -> userController.assignBuruhToMandor(buruhId, invalidMandorId));
    }

    @Test
    @DisplayName("Should handle unassign buruh not assigned to any mandor")
    void testUnassignBuruhNotAssigned() {
        UUID unassignedBuruhId = UUID.randomUUID();

        doThrow(new IllegalStateException("Buruh is not assigned to any mandor"))
                .when(authCommandUseCase).unassignBuruhFromMandor(unassignedBuruhId);

        assertThrows(IllegalStateException.class, () -> userController.unassignBuruh(unassignedBuruhId));
    }

    @Test
    @DisplayName("Should return empty list when no users found")
    void testListUsersEmpty() {
        when(userQueryUseCase.listUsers(null, null)).thenReturn(List.of());

        ResponseEntity<ApiResponse<List<UserDTO>>> result = userController.listUsers(null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(0, result.getBody().data().size());
    }

    @Test
    @DisplayName("Should get buruh by mandor with no buruh assigned")
    void testGetBuruhByMandorNoBuruh() {
        when(userQueryUseCase.getBuruhByMandorId(mandorId)).thenReturn(List.of());

        ResponseEntity<ApiResponse<List<UserDTO>>> result = userController.getBuruhByMandor(mandorId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(0, result.getBody().data().size());
    }

    @Test
    @DisplayName("Should handle edit user with MANDOR role and certification")
    void testEditUserToMandorWithCertification() {
        String certNumber = "CERT-12345";
        UserDTO inputDto = new UserDTO(userId, "testuser", "Test User", "MANDOR", "test@example.com", certNumber, null);
        UserDTO updatedUser = new UserDTO(userId, "testuser", "Test User", "MANDOR", "test@example.com", certNumber, null);

        when(authCommandUseCase.editUser(userId, "Test User", "MANDOR", "test@example.com", certNumber))
                .thenReturn(updatedUser);

        ResponseEntity<ApiResponse<UserDTO>> result = userController.editUser(userId, inputDto);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals("MANDOR", result.getBody().data().role());
        assertEquals(certNumber, result.getBody().data().mandorCertificationNumber());
    }

    @Test
    @DisplayName("Should list users with both role and search filters")
    void testListUsersWithBothFilters() {
        String role = "BURUH";
        String search = "worker";
        UserDTO user1 = new UserDTO(UUID.randomUUID(), "worker1", "Worker 1", role, "worker1@example.com", null, mandorId);
        List<UserDTO> users = List.of(user1);

        when(userQueryUseCase.listUsers(role, search)).thenReturn(users);

        ResponseEntity<ApiResponse<List<UserDTO>>> result = userController.listUsers(role, search);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(1, result.getBody().data().size());
        verify(userQueryUseCase, times(1)).listUsers(role, search);
    }

    @Test
    @DisplayName("Should handle reassign buruh with unassign failure")
    void testReassignBuruhWithUnassignFailure() {
        UUID buruhId = UUID.randomUUID();
        UUID newMandorId = UUID.randomUUID();

        doThrow(new IllegalStateException("Buruh not assigned"))
                .when(authCommandUseCase).unassignBuruhFromMandor(buruhId);

        assertThrows(IllegalStateException.class, () -> userController.reassignBuruh(buruhId, newMandorId));
        verify(authCommandUseCase, times(1)).unassignBuruhFromMandor(buruhId);
        verify(authCommandUseCase, never()).assignBuruhToMandor(buruhId, newMandorId);
    }

    @Test
    @DisplayName("Should get current user")
    void testGetCurrentUser() {
        UserDTO user = new UserDTO(userId, "testuser", "Test User", "BURUH", "test@example.com", null, mandorId);

        when(userQueryUseCase.getUserById(userId)).thenReturn(user);

        ResponseEntity<ApiResponse<UserDTO>> result = userController.getCurrentUser(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(userId, result.getBody().data().userId());
    }
}
