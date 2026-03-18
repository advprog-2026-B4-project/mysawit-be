package id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User domain model")
class UserTest {

    private User buildUser(String hashedPassword, UserRole role) {
        return new User(
                UUID.randomUUID(), "username", "user@test.com", "Full Name",
                hashedPassword, role, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("isOAuthAccount")
    class IsOAuthAccount {

        @Test
        @DisplayName("returns true when hashedPassword is null")
        void trueWhenPasswordNull() {
            assertThat(buildUser(null, UserRole.BURUH).isOAuthAccount()).isTrue();
        }

        @Test
        @DisplayName("returns false when hashedPassword is present")
        void falseWhenPasswordPresent() {
            assertThat(buildUser("$2a$hash", UserRole.BURUH).isOAuthAccount()).isFalse();
        }
    }

    @Nested
    @DisplayName("isAdmin")
    class IsAdmin {

        @Test
        @DisplayName("returns true when role is ADMIN")
        void trueForAdmin() {
            assertThat(buildUser("hash", UserRole.ADMIN).isAdmin()).isTrue();
        }

        @Test
        @DisplayName("returns false for BURUH")
        void falseForBuruh() {
            assertThat(buildUser("hash", UserRole.BURUH).isAdmin()).isFalse();
        }

        @Test
        @DisplayName("returns false for MANDOR")
        void falseForMandor() {
            assertThat(buildUser("hash", UserRole.MANDOR).isAdmin()).isFalse();
        }

        @Test
        @DisplayName("returns false for SUPIR")
        void falseForSupir() {
            assertThat(buildUser("hash", UserRole.SUPIR).isAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("constructors and setters")
    class Constructors {

        @Test
        @DisplayName("no-arg constructor and setters map all fields correctly")
        void noArgConstructorAndSetters() {
            UUID id  = UUID.randomUUID();
            UUID mId = UUID.randomUUID();
            User user = new User();

            user.setUserId(id);
            user.setUsername("u");
            user.setEmail("e@t.com");
            user.setName("N");
            user.setHashedPassword("hash");
            user.setRole(UserRole.MANDOR);
            user.setMandorId(mId);

            assertThat(user.getUserId()).isEqualTo(id);
            assertThat(user.getUsername()).isEqualTo("u");
            assertThat(user.getEmail()).isEqualTo("e@t.com");
            assertThat(user.getName()).isEqualTo("N");
            assertThat(user.getHashedPassword()).isEqualTo("hash");
            assertThat(user.getRole()).isEqualTo(UserRole.MANDOR);
            assertThat(user.getMandorId()).isEqualTo(mId);
            assertThat(user.isAdmin()).isFalse();
            assertThat(user.isOAuthAccount()).isFalse();
        }

        @Test
        @DisplayName("all-arg constructor maps all fields")
        void allArgConstructor() {
            UUID id        = UUID.randomUUID();
            UUID mId       = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            User user = new User(id, "u", "e@t.com", "N", "hash", UserRole.SUPIR, mId, now, now);

            assertThat(user.getUserId()).isEqualTo(id);
            assertThat(user.getRole()).isEqualTo(UserRole.SUPIR);
            assertThat(user.getMandorId()).isEqualTo(mId);
            assertThat(user.getCreatedAt()).isEqualTo(now);
            assertThat(user.getUpdatedAt()).isEqualTo(now);
        }
    }
}