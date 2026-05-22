package id.ac.ui.cs.advprog.mysawitbe.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChangeAdminPassword {

    private static final Logger LOG = Logger.getLogger(ChangeAdminPassword.class.getName());

    private static final String DEFAULT_DB_URL     = "jdbc:postgresql://localhost:5432/mysawit";
    private static final String DEFAULT_DB_USERNAME = "postgres";
    private static final String ADMIN_EMAIL         = "admin@mysawit.id";

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
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT user_id FROM users WHERE email = ?")) {
                check.setString(1, ADMIN_EMAIL);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        LOG.severe("Error: admin account (" + ADMIN_EMAIL + ") not found in database.");
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
                    LOG.info("Admin password updated successfully.");
                } else {
                    LOG.severe("Update affected " + rows + " rows - something unexpected happened.");
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
