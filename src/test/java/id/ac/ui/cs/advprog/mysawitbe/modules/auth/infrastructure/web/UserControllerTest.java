package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

    @Mock UserQueryUseCase   userQueryUseCase;
    @Mock AuthCommandUseCase authCommandUseCase;

    UserController controller;

    final UUID userId   = UUID.randomUUID();
    final UUID mandorId = UUID.randomUUID();
    final UUID adminId  = UUID.randomUUID();

    UserDTO sampleUser() {
        return new UserDTO(userId, "user", "Full Name", "BURUH", "u@t.com");
    }

    @BeforeEach
    void setUp() {
        controller = new UserController(userQueryUseCase, authCommandUseCase);
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsers {

        @Test
        @DisplayName("no params – 200 with all users")
        void noParamsReturns200() {
            when(userQueryUseCase.listUsers(null, null)).thenReturn(List.of(sampleUser()));

            ResponseEntity<?> resp = controller.listUsers(null, null);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("role and search params forwarded to use case")
        void paramsForwardedToUseCase() {
            when(userQueryUseCase.listUsers("BURUH", "budi")).thenReturn(List.of());

            controller.listUsers("BURUH", "budi");

            verify(userQueryUseCase).listUsers("BURUH", "budi");
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("found – 200 with user body")
        void foundReturns200() {
            when(userQueryUseCase.getUserById(userId)).thenReturn(sampleUser());

            ResponseEntity<?> resp = controller.getUser(userId);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("not found – EntityNotFoundException propagates")
        void notFoundPropagates() {
            when(userQueryUseCase.getUserById(userId))
                    .thenThrow(new EntityNotFoundException("not found"));

            assertThatThrownBy(() -> controller.getUser(userId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("editUser")
    class EditUser {

        @Test
        @DisplayName("valid edit – 200 with updated user")
        void validEditReturns200() {
            UserDTO body    = new UserDTO(null, null, "New Name", "MANDOR", "u@t.com");
            UserDTO updated = new UserDTO(userId, "user", "New Name", "MANDOR", "u@t.com");
            when(authCommandUseCase.editUser(userId, "New Name", "MANDOR", "u@t.com"))
                    .thenReturn(updated);

            ResponseEntity<?> resp = controller.editUser(userId, body);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("editing admin account – IllegalStateException propagates")
        void editAdminPropagates() {
            UserDTO body = new UserDTO(null, null, "New", "ADMIN", "a@t.com");
            when(authCommandUseCase.editUser(any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("Cannot edit another admin account"));

            assertThatThrownBy(() -> controller.editUser(userId, body))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("valid delete – 200 and use case called")
        void validDeleteReturns200() {
            ResponseEntity<?> resp = controller.deleteUser(userId, adminId);

            verify(authCommandUseCase).deleteUser(adminId, userId);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("self-delete – IllegalArgumentException propagates")
        void selfDeletePropagates() {
            doThrow(new IllegalArgumentException("own account"))
                    .when(authCommandUseCase).deleteUser(adminId, adminId);

            assertThatThrownBy(() -> controller.deleteUser(adminId, adminId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("assignBuruhToMandor")
    class AssignBuruh {

        @Test
        @DisplayName("valid assignment – 200 and use case called")
        void validAssignmentReturns200() {
            ResponseEntity<?> resp = controller.assignBuruhToMandor(userId, mandorId);

            verify(authCommandUseCase).assignBuruhToMandor(userId, mandorId);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("invalid roles – IllegalArgumentException propagates")
        void invalidRolesPropagates() {
            doThrow(new IllegalArgumentException("not a BURUH"))
                    .when(authCommandUseCase).assignBuruhToMandor(userId, mandorId);

            assertThatThrownBy(() -> controller.assignBuruhToMandor(userId, mandorId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("unassignBuruh – 200 and use case called")
    void unassignBuruhReturns200() {
        ResponseEntity<?> resp = controller.unassignBuruh(userId);

        verify(authCommandUseCase).unassignBuruhFromMandor(userId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("reassignBuruh – unassign called before assign (order verified)")
    void reassignBuruhCallsUnassignThenAssignInOrder() {
        InOrder inOrder = inOrder(authCommandUseCase);

        controller.reassignBuruh(userId, mandorId);

        inOrder.verify(authCommandUseCase).unassignBuruhFromMandor(userId);
        inOrder.verify(authCommandUseCase).assignBuruhToMandor(userId, mandorId);
    }

    @Test
    @DisplayName("getBuruhByMandor – 200 with list and use case called")
    void getBuruhByMandorReturns200() {
        when(userQueryUseCase.getBuruhByMandorId(mandorId)).thenReturn(List.of(sampleUser()));

        ResponseEntity<?> resp = controller.getBuruhByMandor(mandorId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userQueryUseCase).getBuruhByMandorId(mandorId);
    }
}