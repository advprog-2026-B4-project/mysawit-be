package id.ac.ui.cs.advprog.mysawitbe.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Creates or rotates the admin account password.
 *
 * Usage: {@code java ChangeAdminPassword <newPassword>}
 *
 * Env vars: DB_URL, DB_USERNAME, DB_PASSWORD (required).
 */
public class ChangeAdminPassword {

    private static final Logger LOG = Logger.getLogger(ChangeAdminPassword.class.getName());

    private static final String DEFAULT_DB_URL      = "jdbc:postgresql://localhost:5432/mysawit";
    private static final String DEFAULT_DB_USERNAME  = "postgres";
    private static final String ADMIN_EMAIL          = "admin@mysawit.id";
    private static final String ADMIN_USERNAME       = "admin";
    private static final String ADMIN_NAME           = "Admin";
    private static final String ADMIN_ROLE           = "ADMIN";

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0].isBlank()) {
            LOG.severe("Usage: changeAdminPassword <newPassword>");
            System.exit(1);
        }

        String newPassword = args[0];
        if (newPassword.length() < 8) {
            LOG.severe("Error: password must be at least 8 characters.");
            System.exit(1);
        }

        String dbUrl      = env("DB_URL",      DEFAULT_DB_URL);
        String dbUsername = env("DB_USERNAME",  DEFAULT_DB_USERNAME);
        String dbPassword = env("DB_PASSWORD",  null);

        if (dbPassword == null) {
            LOG.severe("DB_PASSWORD environment variable is required.");
            System.exit(1);
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hashed = encoder.encode(newPassword);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
            boolean created = upsertAdmin(conn, hashed);
            if (created) {
                LOG.info("Admin account created with the provided password.");
            } else {
                LOG.info("Admin password updated successfully.");
            }
        }
    }

    private static boolean upsertAdmin(Connection conn, String hashedPassword) throws SQLException {
        UUID existingId = findAdminId(conn);
        if (existingId != null) {
            updatePassword(conn, hashedPassword);
            return false;
        }
        insertAdmin(conn, hashedPassword);
        return true;
    }

    private static UUID findAdminId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT user_id FROM users WHERE email = ?")) {
            ps.setString(1, ADMIN_EMAIL);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? (UUID) rs.getObject(1) : null;
            }
        }
    }

    private static void updatePassword(Connection conn, String hashedPassword) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET password = ?, updated_at = ? WHERE email = ?")) {
            ps.setString(1, hashedPassword);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setString(3, ADMIN_EMAIL);
            ps.executeUpdate();
        }
    }

    private static void insertAdmin(Connection conn, String hashedPassword) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (user_id, username, email, name, password, role, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, ADMIN_USERNAME);
            ps.setString(3, ADMIN_EMAIL);
            ps.setString(4, ADMIN_NAME);
            ps.setString(5, hashedPassword);
            ps.setString(6, ADMIN_ROLE);
            Timestamp now = Timestamp.from(Instant.now());
            ps.setTimestamp(7, now);
            ps.setTimestamp(8, now);
            ps.executeUpdate();
        }
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
