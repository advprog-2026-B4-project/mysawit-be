package id.ac.ui.cs.advprog.mysawitbe.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds deterministic test data for payroll features:
 * - [Admin] list + process payroll (approve/reject)
 * - [Worker] view own wallet balance + payroll history
 *
 * Usage:
 *   gradlew seedPayrollTestData
 *
 * Reads DB settings from env vars:
 *   DB_URL, DB_USERNAME, DB_PASSWORD
 */
public class SeedPayrollTestData {

    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/mysawit";
    private static final String DEFAULT_DB_USERNAME = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "postgres";

    private static final String TEST_PASSWORD = "Worker@12345";

    private static final SeedUser MANDOR = new SeedUser(
            stableUuid("user-mandor"),
            "mandor_seed",
            "mandor.seed@mysawit.id",
            "Mandor Seed",
            "MANDOR",
            null
    );

    private static final SeedUser BURUH_1 = new SeedUser(
            stableUuid("user-buruh-1"),
            "buruh_seed_1",
            "buruh.seed1@mysawit.id",
            "Buruh Seed 1",
            "BURUH",
            MANDOR.userId()
    );

    private static final SeedUser BURUH_2 = new SeedUser(
            stableUuid("user-buruh-2"),
            "buruh_seed_2",
            "buruh.seed2@mysawit.id",
            "Buruh Seed 2",
            "BURUH",
            MANDOR.userId()
    );

    private static final SeedUser SUPIR = new SeedUser(
            stableUuid("user-supir"),
            "supir_seed",
            "supir.seed@mysawit.id",
            "Supir Seed",
            "SUPIR",
            null
    );

    private static final List<SeedUser> WORKER_USERS = List.of(MANDOR, BURUH_1, BURUH_2, SUPIR);

    public static void main(String[] args) throws Exception {
        String dbUrl = env("DB_URL", DEFAULT_DB_URL);
        String dbUsername = env("DB_USERNAME", DEFAULT_DB_USERNAME);
        String dbPassword = env("DB_PASSWORD", DEFAULT_DB_PASSWORD);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hashedPassword = encoder.encode(TEST_PASSWORD);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
            conn.setAutoCommit(false);
            try {
                verifyAdminExists(conn);
                upsertSeedUsers(conn, hashedPassword);
                resetSeedFeatureData(conn);

                List<SeedPayroll> payrolls = buildSeedPayrolls();
                upsertPayrolls(conn, payrolls);
                upsertWalletTransactions(conn, payrolls);
                upsertWallets(conn, payrolls);

                conn.commit();
                printSummary(payrolls);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void verifyAdminExists(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE role = 'ADMIN' LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException("No ADMIN user found. Ensure base migrations have been applied.");
            }
        }
    }

    private static void upsertSeedUsers(Connection conn, String hashedPassword) throws Exception {
        String sql = """
                INSERT INTO users (user_id, username, email, name, password, role, mandor_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                ON CONFLICT (user_id) DO UPDATE SET
                    username = EXCLUDED.username,
                    email = EXCLUDED.email,
                    name = EXCLUDED.name,
                    password = EXCLUDED.password,
                    role = EXCLUDED.role,
                    mandor_id = EXCLUDED.mandor_id,
                    updated_at = NOW()
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SeedUser user : WORKER_USERS) {
                ps.setObject(1, user.userId());
                ps.setString(2, user.username());
                ps.setString(3, user.email());
                ps.setString(4, user.name());
                ps.setString(5, hashedPassword);
                ps.setString(6, user.role());
                ps.setObject(7, user.mandorId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void resetSeedFeatureData(Connection conn) throws Exception {
        List<UUID> userIds = WORKER_USERS.stream().map(SeedUser::userId).toList();

        try (PreparedStatement deleteTransactions = conn.prepareStatement(
                "DELETE FROM wallet_transactions WHERE user_id = ?")) {
            for (UUID userId : userIds) {
                deleteTransactions.setObject(1, userId);
                deleteTransactions.addBatch();
            }
            deleteTransactions.executeBatch();
        }

        try (PreparedStatement deletePayrolls = conn.prepareStatement(
                "DELETE FROM payrolls WHERE user_id = ?")) {
            for (UUID userId : userIds) {
                deletePayrolls.setObject(1, userId);
                deletePayrolls.addBatch();
            }
            deletePayrolls.executeBatch();
        }
    }

    private static List<SeedPayroll> buildSeedPayrolls() {
        LocalDateTime now = LocalDateTime.now();
        List<SeedPayroll> rows = new ArrayList<>();

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-1-approved"),
                BURUH_1.userId(),
                "BURUH",
                stableUuid("ref-panen-1"),
                "PANEN",
                120,
                10_000,
                "APPROVED",
                null,
                now.minusDays(6),
                now.minusDays(7),
                "PAY-SEED-001"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-1-pending"),
                BURUH_1.userId(),
                "BURUH",
                stableUuid("ref-panen-2"),
                "PANEN",
                80,
                10_000,
                "PENDING",
                null,
                null,
                now.minusDays(2),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-1-rejected"),
                BURUH_1.userId(),
                "BURUH",
                stableUuid("ref-panen-3"),
                "PANEN",
                60,
                10_000,
                "REJECTED",
                "Data panen tidak valid",
                now.minusDays(4),
                now.minusDays(5),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-2-approved"),
                BURUH_2.userId(),
                "BURUH",
                stableUuid("ref-panen-4"),
                "PANEN",
                95,
                10_000,
                "APPROVED",
                null,
                now.minusDays(3),
                now.minusDays(4),
                "PAY-SEED-004"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-supir-approved"),
                SUPIR.userId(),
                "SUPIR",
                stableUuid("ref-pengiriman-1"),
                "PENGIRIMAN",
                200,
                8_000,
                "APPROVED",
                null,
                now.minusDays(2),
                now.minusDays(3),
                "PAY-SEED-002"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-supir-pending"),
                SUPIR.userId(),
                "SUPIR",
                stableUuid("ref-pengiriman-2"),
                "PENGIRIMAN",
                150,
                8_000,
                "PENDING",
                null,
                null,
                now.minusDays(1),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-mandor-approved"),
                MANDOR.userId(),
                "MANDOR",
                stableUuid("ref-pengiriman-3"),
                "PENGIRIMAN",
                220,
                12_000,
                "APPROVED",
                null,
                now.minusDays(7),
                now.minusDays(8),
                "PAY-SEED-003"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-mandor-rejected"),
                MANDOR.userId(),
                "MANDOR",
                stableUuid("ref-pengiriman-4"),
                "PENGIRIMAN",
                180,
                12_000,
                "REJECTED",
                "Dokumen pendukung pengiriman belum lengkap",
                now.minusDays(5),
                now.minusDays(6),
                null
        ));

        return rows;
    }

    private static void upsertPayrolls(Connection conn, List<SeedPayroll> payrolls) throws Exception {
        String sql = """
                INSERT INTO payrolls (
                    payroll_id, user_id, role, reference_id, reference_type,
                    weight, wage_rate_applied, net_amount, status, rejection_reason,
                    processed_at, created_at, payment_reference
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (payroll_id) DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    role = EXCLUDED.role,
                    reference_id = EXCLUDED.reference_id,
                    reference_type = EXCLUDED.reference_type,
                    weight = EXCLUDED.weight,
                    wage_rate_applied = EXCLUDED.wage_rate_applied,
                    net_amount = EXCLUDED.net_amount,
                    status = EXCLUDED.status,
                    rejection_reason = EXCLUDED.rejection_reason,
                    processed_at = EXCLUDED.processed_at,
                    created_at = EXCLUDED.created_at,
                    payment_reference = EXCLUDED.payment_reference
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SeedPayroll row : payrolls) {
                ps.setObject(1, row.payrollId());
                ps.setObject(2, row.userId());
                ps.setString(3, row.role());
                ps.setObject(4, row.referenceId());
                ps.setString(5, row.referenceType());
                ps.setInt(6, row.weight());
                ps.setInt(7, row.wageRateApplied());
                ps.setInt(8, row.netAmount());
                ps.setString(9, row.status());
                ps.setString(10, row.rejectionReason());
                ps.setTimestamp(11, toTimestamp(row.processedAt()));
                ps.setTimestamp(12, toTimestamp(row.createdAt()));
                ps.setString(13, row.paymentReference());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void upsertWalletTransactions(Connection conn, List<SeedPayroll> payrolls) throws Exception {
        String sql = """
                INSERT INTO wallet_transactions (
                    transaction_id, user_id, payroll_id, amount, type, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (transaction_id) DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    payroll_id = EXCLUDED.payroll_id,
                    amount = EXCLUDED.amount,
                    type = EXCLUDED.type,
                    created_at = EXCLUDED.created_at
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SeedPayroll row : payrolls) {
                if (!"APPROVED".equals(row.status())) {
                    continue;
                }

                UUID txId = stableUuid("tx-" + row.payrollId());
                ps.setObject(1, txId);
                ps.setObject(2, row.userId());
                ps.setObject(3, row.payrollId());
                ps.setInt(4, row.netAmount());
                ps.setString(5, "CREDIT");
                ps.setTimestamp(6, toTimestamp(row.processedAt() == null ? row.createdAt() : row.processedAt()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void upsertWallets(Connection conn, List<SeedPayroll> payrolls) throws Exception {
        record BalanceRow(UUID userId, int balance) {}

        List<BalanceRow> balances = WORKER_USERS.stream()
                .map(user -> new BalanceRow(
                        user.userId(),
                        payrolls.stream()
                                .filter(payroll -> user.userId().equals(payroll.userId()))
                                .filter(payroll -> "APPROVED".equals(payroll.status()))
                                .mapToInt(SeedPayroll::netAmount)
                                .sum()
                ))
                .toList();

        String sql = """
                INSERT INTO wallets (user_id, balance, updated_at)
                VALUES (?, ?, NOW())
                ON CONFLICT (user_id) DO UPDATE SET
                    balance = EXCLUDED.balance,
                    updated_at = NOW()
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (BalanceRow row : balances) {
                ps.setObject(1, row.userId());
                ps.setInt(2, row.balance());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void printSummary(List<SeedPayroll> payrolls) {
        System.out.println("Seed payroll test data generated successfully.");
        System.out.println();
        System.out.println("Worker login password: " + TEST_PASSWORD);
        System.out.println();

        for (SeedUser user : WORKER_USERS) {
            System.out.println("User " + user.role() + ":");
            System.out.println("  email: " + user.email());
            System.out.println("  username: " + user.username());
            System.out.println("  userId: " + user.userId());
        }

        System.out.println();
        System.out.println("Pending payroll IDs for ADMIN process testing:");
        payrolls.stream()
                .filter(payroll -> "PENDING".equals(payroll.status()))
                .forEach(payroll -> System.out.println("  - " + payroll.payrollId()));
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static UUID stableUuid(String key) {
        String value = "mysawit-seed-" + key;
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    private record SeedUser(
            UUID userId,
            String username,
            String email,
            String name,
            String role,
            UUID mandorId
    ) {}

    private record SeedPayroll(
            UUID payrollId,
            UUID userId,
            String role,
            UUID referenceId,
            String referenceType,
            int weight,
            int wageRateApplied,
            String status,
            String rejectionReason,
            LocalDateTime processedAt,
            LocalDateTime createdAt,
            String paymentReference
    ) {
        int netAmount() {
            long value = (long) weight * wageRateApplied;
            if (value > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Seed payroll amount overflow for " + payrollId);
            }
            return (int) value;
        }
    }
}
