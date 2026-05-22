package id.ac.ui.cs.advprog.mysawitbe.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Standalone utility - no Spring context required.
 * Usage: pass new password as first argument.
 *   java -cp <classpath> ChangeAdminPassword <newPassword>
 *
 * Reads connection details from the same env vars as the main app:
 *   DB_URL      (default: jdbc:postgresql://localhost:5432/mysawit)
 *   DB_USERNAME (default: postgres)
 *   DB_PASSWORD (default: postgres)
 */
public class ChangeAdminPassword {

    private static final String DEFAULT_DB_URL      = "jdbc:postgresql://localhost:5432/mysawit";
    private static final String DEFAULT_DB_USERNAME  = "postgres";
    private static final String DEFAULT_DB_PASSWORD  = "postgres";
    private static final String ADMIN_EMAIL          = "admin@mysawit.id";

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Usage: changeAdminPassword <newPassword>");
            System.exit(1);
        }

        String newPassword = args[0];
        if (newPassword.length() < 8) {
            System.err.println("Error: password must be at least 8 characters.");
            System.exit(1);
        }

        String dbUrl      = env("DB_URL",      DEFAULT_DB_URL);
        String dbUsername = env("DB_USERNAME",  DEFAULT_DB_USERNAME);
        String dbPassword = env("DB_PASSWORD",  DEFAULT_DB_PASSWORD);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hashed = encoder.encode(newPassword);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
            // Verify admin exists before updating
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT user_id FROM users WHERE email = ?")) {
                check.setString(1, ADMIN_EMAIL);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        System.err.println("Error: admin account (" + ADMIN_EMAIL + ") not found in database.");
                        System.exit(1);
                    }
                }
            }

            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE users SET password = ?, updated_at = NOW() WHERE email = ?")) {
                update.setString(1, hashed);
                update.setString(2, ADMIN_EMAIL);
                int rows = update.executeUpdate();
                if (rows == 1) {
                    System.out.println("Admin password updated successfully.");
                } else {
                    System.err.println("Update affected " + rows + " rows - something unexpected happened.");
                    System.exit(1);
                }
            }
        }
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
