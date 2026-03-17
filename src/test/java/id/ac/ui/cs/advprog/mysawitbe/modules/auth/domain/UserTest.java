package id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("User domain model")
class UserTest {

    @Nested
    @DisplayName("isOAuthAccount")
    class IsOAuthAccount {

        @Test
        @DisplayName("returns true when hashedPassword is null")
        void trueWhenPasswordNull() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("returns false when hashedPassword is present")
        void falseWhenPasswordPresent() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("isAdmin")
    class IsAdmin {

        @Test
        @DisplayName("returns true when role is ADMIN")
        void trueForAdmin() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("returns false for BURUH")
        void falseForBuruh() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("returns false for MANDOR")
        void falseForMandor() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("returns false for SUPIR")
        void falseForSupir() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("constructors and setters")
    class Constructors {

        @Test
        @DisplayName("no-arg constructor and setters map all fields correctly")
        void noArgConstructorAndSetters() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("all-arg constructor maps all fields")
        void allArgConstructor() {
            fail("not yet implemented");
        }
    }
}