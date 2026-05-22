package id.ac.ui.cs.advprog.mysawitbe.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(SeedPayrollTestData.class.getName());

    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/mysawit";
    private static final String DEFAULT_DB_USERNAME = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "postgres";

    private static final String ROLE_MANDOR = "MANDOR";
    private static final String ROLE_BURUH = "BURUH";
    private static final String ROLE_SUPIR = "SUPIR";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String REF_PENGIRIMAN = "PENGIRIMAN";
    private static final String REF_PANEN = "PANEN";

    private static final String TEST_PASSWORD =
            System.getenv().getOrDefault("SEED_USER_PASSWORD", "change-me-in-dev-only");

    private static final SeedUser MANDOR = new SeedUser(
            stableUuid("user-mandor"),
            "mandor_seed",
            "mandor.seed@mysawit.id",
            "Mandor Seed",
            ROLE_MANDOR,
            null
    );

    private static final SeedUser BURUH_1 = new SeedUser(
            stableUuid("user-buruh-1"),
            "buruh_seed_1",
            "buruh.seed1@mysawit.id",
            "Buruh Seed 1",
            ROLE_BURUH,
            MANDOR.userId()
    );

    private static final SeedUser BURUH_2 = new SeedUser(
            stableUuid("user-buruh-2"),
            "buruh_seed_2",
            "buruh.seed2@mysawit.id",
            "Buruh Seed 2",
            ROLE_BURUH,
            MANDOR.userId()
    );

    private static final SeedUser SUPIR = new SeedUser(
            stableUuid("user-supir"),
            "supir_seed",
            "supir.seed@mysawit.id",
            "Supir Seed",
            ROLE_SUPIR,
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
                List<SeedPanen> panenRows = buildSeedPanenRows(payrolls);
                upsertPanenWithPhotos(conn, panenRows);
                upsertPayrolls(conn, payrolls);
                upsertWalletTransactions(conn, payrolls);
                upsertWallets(conn, payrolls);

                conn.commit();
                printSummary(payrolls, panenRows);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void verifyAdminExists(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE role = 'ADMIN' LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException("No ADMIN user found. Ensure base migrations have been applied.");
            }
        }
    }

    private static void upsertSeedUsers(Connection conn, String hashedPassword) throws SQLException {
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

    private static void resetSeedFeatureData(Connection conn) throws SQLException {
        List<UUID> userIds = WORKER_USERS.stream().map(SeedUser::userId).toList();
        List<UUID> buruhIds = List.of(BURUH_1.userId(), BURUH_2.userId());

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

        try (PreparedStatement deleteHarvestReports = conn.prepareStatement(
                "DELETE FROM harvest_reports WHERE buruh_id = ?")) {
            for (UUID buruhId : buruhIds) {
                deleteHarvestReports.setObject(1, buruhId);
                deleteHarvestReports.addBatch();
            }
            deleteHarvestReports.executeBatch();
        }
    }

    private static List<SeedPayroll> buildSeedPayrolls() {
        LocalDateTime now = LocalDateTime.now();
        List<SeedPayroll> rows = new ArrayList<>();

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-1-approved"),
                BURUH_1.userId(),
                ROLE_BURUH,
                stableUuid("ref-panen-1"),
                REF_PANEN,
                120,
                10_000,
                STATUS_APPROVED,
                null,
                now.minusDays(6),
                now.minusDays(7),
                "PAY-SEED-001"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-1-pending"),
                BURUH_1.userId(),
                ROLE_BURUH,
                stableUuid("ref-panen-2"),
                REF_PANEN,
                80,
                10_000,
                STATUS_PENDING,
                null,
                null,
                now.minusDays(2),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-1-rejected"),
                BURUH_1.userId(),
                ROLE_BURUH,
                stableUuid("ref-panen-3"),
                REF_PANEN,
                60,
                10_000,
                STATUS_REJECTED,
                "Data panen tidak valid",
                now.minusDays(4),
                now.minusDays(5),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-2-approved"),
                BURUH_2.userId(),
                ROLE_BURUH,
                stableUuid("ref-panen-4"),
                REF_PANEN,
                95,
                10_000,
                STATUS_APPROVED,
                null,
                now.minusDays(3),
                now.minusDays(4),
                "PAY-SEED-004"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-supir-approved"),
                SUPIR.userId(),
                ROLE_SUPIR,
                stableUuid("ref-pengiriman-1"),
                REF_PENGIRIMAN,
                200,
                8_000,
                STATUS_APPROVED,
                null,
                now.minusDays(2),
                now.minusDays(3),
                "PAY-SEED-002"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-supir-pending"),
                SUPIR.userId(),
                ROLE_SUPIR,
                stableUuid("ref-pengiriman-2"),
                REF_PENGIRIMAN,
                150,
                8_000,
                STATUS_PENDING,
                null,
                null,
                now.minusDays(1),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-mandor-approved"),
                MANDOR.userId(),
                ROLE_MANDOR,
                stableUuid("ref-pengiriman-3"),
                REF_PENGIRIMAN,
                220,
                12_000,
                STATUS_APPROVED,
                null,
                now.minusDays(7),
                now.minusDays(8),
                "PAY-SEED-003"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-mandor-rejected"),
                MANDOR.userId(),
                ROLE_MANDOR,
                stableUuid("ref-pengiriman-4"),
                REF_PENGIRIMAN,
                180,
                12_000,
                STATUS_REJECTED,
                "Dokumen pendukung pengiriman belum lengkap",
                now.minusDays(5),
                now.minusDays(6),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-2-pending"),
                BURUH_2.userId(),
                ROLE_BURUH,
                stableUuid("ref-panen-5"),
                REF_PANEN,
                140,
                10_000,
                STATUS_PENDING,
                null,
                null,
                now.minusDays(1),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-2-approved-2"),
                BURUH_2.userId(),
                ROLE_BURUH,
                stableUuid("ref-panen-6"),
                REF_PANEN,
                75,
                10_000,
                STATUS_APPROVED,
                null,
                now.minusDays(8),
                now.minusDays(9),
                "PAY-SEED-005"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-1-approved-2"),
                BURUH_1.userId(),
                ROLE_BURUH,
                stableUuid("ref-panen-7"),
                REF_PANEN,
                155,
                10_000,
                STATUS_APPROVED,
                null,
                now.minusDays(9),
                now.minusDays(10),
                "PAY-SEED-006"
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-buruh-1-pending-2"),
                BURUH_1.userId(),
                ROLE_BURUH,
                stableUuid("ref-panen-8"),
                REF_PANEN,
                90,
                10_000,
                STATUS_PENDING,
                null,
                null,
                now.minusHours(12),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-supir-rejected"),
                SUPIR.userId(),
                ROLE_SUPIR,
                stableUuid("ref-pengiriman-5"),
                REF_PENGIRIMAN,
                160,
                8_000,
                STATUS_REJECTED,
                "Surat jalan belum divalidasi",
                now.minusDays(4),
                now.minusDays(5),
                null
        ));

        rows.add(new SeedPayroll(
                stableUuid("payroll-mandor-pending"),
                MANDOR.userId(),
                ROLE_MANDOR,
                stableUuid("ref-pengiriman-6"),
                REF_PENGIRIMAN,
                205,
                12_000,
                STATUS_PENDING,
                null,
                null,
                now.minusDays(2),
                null
        ));

        return rows;
    }

    private static List<SeedPanen> buildSeedPanenRows(List<SeedPayroll> payrolls) {
        List<SeedPanen> rows = new ArrayList<>();

        for (SeedPayroll payroll : payrolls) {
            if (!REF_PANEN.equals(payroll.referenceType())) {
                continue;
            }

            List<String> photoUrls = new ArrayList<>();
            photoUrls.add(seedPhotoUrl(payroll.referenceId(), 1));
            photoUrls.add(seedPhotoUrl(payroll.referenceId(), 2));

            // Pending payroll gets one extra photo so UI can exercise >2 evidence previews.
            if (STATUS_PENDING.equals(payroll.status())) {
                photoUrls.add(seedPhotoUrl(payroll.referenceId(), 3));
            }

            rows.add(new SeedPanen(
                    payroll.referenceId(),
                    payroll.userId(),
                    payroll.weight(),
                    payroll.status(),
                    payroll.rejectionReason(),
                    "Seed panen untuk payroll " + payroll.payrollId(),
                    payroll.createdAt(),
                    photoUrls
            ));
        }

        return rows;
    }

    private static void upsertPanenWithPhotos(Connection conn, List<SeedPanen> panenRows) throws SQLException {
        if (panenRows.isEmpty()) {
            return;
        }

        UUID kebunId = resolveSeedKebunId(conn);

        String harvestSql = """
                INSERT INTO harvest_reports (
                    id, buruh_id, kebun_id, weight, description,
                    status, rejection_reason, created_at, harvest_date
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    buruh_id = EXCLUDED.buruh_id,
                    kebun_id = EXCLUDED.kebun_id,
                    weight = EXCLUDED.weight,
                    description = EXCLUDED.description,
                    status = EXCLUDED.status,
                    rejection_reason = EXCLUDED.rejection_reason,
                    created_at = EXCLUDED.created_at,
                    harvest_date = EXCLUDED.harvest_date
                """;

        try (PreparedStatement ps = conn.prepareStatement(harvestSql)) {
            for (SeedPanen row : panenRows) {
                ps.setObject(1, row.panenId());
                ps.setObject(2, row.buruhId());
                ps.setObject(3, kebunId);
                ps.setInt(4, row.weight());
                ps.setString(5, row.description());
                ps.setString(6, row.status());
                ps.setString(7, row.rejectionReason());
                ps.setTimestamp(8, toTimestamp(row.createdAt()));
                ps.setDate(9, Date.valueOf(row.createdAt().toLocalDate()));
                ps.addBatch();
            }
            ps.executeBatch();
        }

        try (PreparedStatement deletePhotos = conn.prepareStatement(
                "DELETE FROM harvest_photos WHERE harvest_id = ?")) {
            for (SeedPanen row : panenRows) {
                deletePhotos.setObject(1, row.panenId());
                deletePhotos.addBatch();
            }
            deletePhotos.executeBatch();
        }

        String photoSql = """
                INSERT INTO harvest_photos (id, harvest_id, photo_url, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    harvest_id = EXCLUDED.harvest_id,
                    photo_url = EXCLUDED.photo_url,
                    created_at = EXCLUDED.created_at
                """;

        try (PreparedStatement ps = conn.prepareStatement(photoSql)) {
            for (SeedPanen row : panenRows) {
                int idx = 1;
                for (String photoUrl : row.photoUrls()) {
                    ps.setObject(1, stableUuid("photo-" + row.panenId() + "-" + idx));
                    ps.setObject(2, row.panenId());
                    ps.setString(3, photoUrl);
                    ps.setTimestamp(4, toTimestamp(row.createdAt()));
                    ps.addBatch();
                    idx++;
                }
            }
            ps.executeBatch();
        }
    }

    private static UUID resolveSeedKebunId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT kebun_id FROM kebun ORDER BY kebun_id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return (UUID) rs.getObject(1);
            }
        }

        UUID kebunId = stableUuid("seed-kebun-payroll");
        String sql = """
                INSERT INTO kebun (kebun_id, nama, kode, luas, mandor_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (kebun_id) DO UPDATE SET
                    nama = EXCLUDED.nama,
                    kode = EXCLUDED.kode,
                    luas = EXCLUDED.luas,
                    mandor_id = EXCLUDED.mandor_id
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, kebunId);
            ps.setString(2, "Kebun Seed Payroll");
            ps.setString(3, "KEB-SEED-PAYROLL");
            ps.setInt(4, 250);
            ps.setNull(5, Types.OTHER);
            ps.executeUpdate();
        }

        return kebunId;
    }

    private static void upsertPayrolls(Connection conn, List<SeedPayroll> payrolls) throws SQLException {
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

    private static void upsertWalletTransactions(Connection conn, List<SeedPayroll> payrolls) throws SQLException {
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
                if (!STATUS_APPROVED.equals(row.status())) {
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

    private static void upsertWallets(Connection conn, List<SeedPayroll> payrolls) throws SQLException {
        record BalanceRow(UUID userId, int balance) {}

        List<BalanceRow> balances = WORKER_USERS.stream()
                .map(user -> new BalanceRow(
                        user.userId(),
                        payrolls.stream()
                                .filter(payroll -> user.userId().equals(payroll.userId()))
                                .filter(payroll -> STATUS_APPROVED.equals(payroll.status()))
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

    private static void printSummary(List<SeedPayroll> payrolls, List<SeedPanen> panenRows) {
        LOG.info("Seed payroll test data generated successfully.");
        LOG.info("");
        LOG.info("Total payroll rows seeded: " + payrolls.size());
        LOG.info("Total PANEN rows with evidence photos: " + panenRows.size());
        LOG.info("");
        LOG.info("=== Load test credentials ===");
        LOG.info("");
        LOG.info("ADMIN (from V2 migration — not seeded here):");
        LOG.info("  email:    admin@mysawit.id");
        LOG.info("  password: Admin@12345");
        LOG.info("");
        LOG.info("Worker accounts (password: " + TEST_PASSWORD + "):");
        for (SeedUser user : WORKER_USERS) {
            LOG.info("  [" + user.role() + "] " + user.email() + "  (userId: " + user.userId() + ")");
        }
        LOG.info("");
        LOG.info("Pending payroll IDs for ADMIN process testing:");
        payrolls.stream()
                .filter(payroll -> STATUS_PENDING.equals(payroll.status()))
                .forEach(payroll -> LOG.info("  - " + payroll.payrollId()));
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static UUID stableUuid(String key) {
        String value = "mysawit-seed-" + key;
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String seedPhotoUrl(UUID panenId, int idx) {
        String shortId = panenId.toString().substring(0, 8);
        return "https://placehold.co/640x420/2d6a4f/ffffff?text=Panen+" + shortId + "-" + idx;
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

    private record SeedPanen(
            UUID panenId,
            UUID buruhId,
            int weight,
            String status,
            String rejectionReason,
            String description,
            LocalDateTime createdAt,
            List<String> photoUrls
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
                // weight and wageRateApplied use the same smallest-unit scale, so multiply directly.
                long value = (long) weight * wageRateApplied;

            if (value > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Seed payroll amount overflow for " + payrollId);
            }
            return (int) value;
        }
    }
}
