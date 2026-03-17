package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

    @Mock UserQueryUseCase   userQueryUseCase;
    @Mock AuthCommandUseCase authCommandUseCase;

    UserController controller;

    @BeforeEach
    void setUp() {
        // TODO: instantiate controller with mocked dependencies
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsers {

        @Test
        @DisplayName("no params 200 with all users")
        void noParamsReturns200() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("role and search params forwarded to use case")
        void paramsForwardedToUseCase() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("found 200 with user body")
        void foundReturns200() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("not found EntityNotFoundException propagates")
        void notFoundPropagates() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("editUser")
    class EditUser {

        @Test
        @DisplayName("valid edit 200 with updated user")
        void validEditReturns200() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("editing admin account IllegalStateException propagates")
        void editAdminPropagates() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("valid delete 200 and use case called")
        void validDeleteReturns200() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("self-delete IllegalArgumentException propagates")
        void selfDeletePropagates() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("assignBuruhToMandor")
    class AssignBuruh {

        @Test
        @DisplayName("valid assignment 200 and use case called")
        void validAssignmentReturns200() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("invalid roles IllegalArgumentException propagates")
        void invalidRolesPropagates() {
            fail("not yet implemented");
        }
    }

    @Test
    @DisplayName("unassignBuruh 200 and use case called")
    void unassignBuruhReturns200() {
        fail("not yet implemented");
    }

    @Test
    @DisplayName("reassignBuruh unassign called before assign (order verified)")
    void reassignBuruhCallsUnassignThenAssignInOrder() {
        fail("not yet implemented");
    }

    @Test
    @DisplayName("getBuruhByMandor 200 with list and use case called")
    void getBuruhByMandorReturns200() {
        fail("not yet implemented");
    }
}