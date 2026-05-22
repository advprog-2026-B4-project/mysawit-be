package id.ac.ui.cs.advprog.mysawitbe.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Seeds comprehensive dummy data for all modules:
 * kebun, panen, pengiriman, payroll, wallet, notifications.
 *
 * Usage:
 *   gradlew seedDummyData
 *
 * Env vars: DB_URL, DB_USERNAME, DB_PASSWORD
 */
public class SeedDummyData {

    private static final Logger LOG = Logger.getLogger(SeedDummyData.class.getName());

    private static final String DEFAULT_DB_URL      = "jdbc:postgresql://localhost:5432/mysawit";
    private static final String DEFAULT_DB_USERNAME  = "postgres";
    private static final String DEFAULT_DB_PASSWORD  = "postgres";

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

    // ── Users ────────────────────────────────────────────────────────────────

    private static final SeedUser MANDOR = new SeedUser(
            uid("user-mandor"),   "mandor_dummy",   "mandor.dummy@mysawit.id",   "Pak Mandor", ROLE_MANDOR, null);
    private static final SeedUser BURUH_1 = new SeedUser(
            uid("user-buruh-1"), "buruh_dummy_1",  "buruh.dummy1@mysawit.id",   "Budi Buruh", ROLE_BURUH,  MANDOR.userId());
    private static final SeedUser BURUH_2 = new SeedUser(
            uid("user-buruh-2"), "buruh_dummy_2",  "buruh.dummy2@mysawit.id",   "Sari Buruh", ROLE_BURUH,  MANDOR.userId());
    private static final SeedUser SUPIR_1 = new SeedUser(
            uid("user-supir-1"), "supir_dummy_1",  "supir.dummy1@mysawit.id",   "Anton Supir", ROLE_SUPIR,  null);
    private static final SeedUser SUPIR_2 = new SeedUser(
            uid("user-supir-2"), "supir_dummy_2",  "supir.dummy2@mysawit.id",   "Dewi Supir", ROLE_SUPIR,  null);

    private static final List<SeedUser> ALL_USERS = List.of(MANDOR, BURUH_1, BURUH_2, SUPIR_1, SUPIR_2);

    // ── Kebun ────────────────────────────────────────────────────────────────

    // Coordinates stored as integer degrees (lat/lng)
    private static final UUID KEBUN_1_ID = uid("kebun-1");
    private static final UUID KEBUN_2_ID = uid("kebun-2");

    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        String dbUrl      = env("DB_URL",      DEFAULT_DB_URL);
        String dbUsername = env("DB_USERNAME",  DEFAULT_DB_USERNAME);
        String dbPassword = env("DB_PASSWORD",  DEFAULT_DB_PASSWORD);

        String hashedPassword = new BCryptPasswordEncoder(12).encode(TEST_PASSWORD);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
            conn.setAutoCommit(false);
            try {
                verifyAdminExists(conn);
                resetDummyData(conn);
                upsertUsers(conn, hashedPassword);
                upsertKebun(conn);
                List<SeedPanen> panenList = buildPanen();
                upsertPanen(conn, panenList);
                List<SeedPengiriman> pengirimanList = buildPengiriman(panenList);
                upsertPengiriman(conn, pengirimanList);
                List<SeedPayroll> payrolls = buildPayrolls(panenList, pengirimanList);
                upsertPayrolls(conn, payrolls);
                upsertWallets(conn, payrolls);
                upsertNotifications(conn);
                conn.commit();
                printSummary(payrolls, panenList, pengirimanList);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ── Reset (FK-safe order) ─────────────────────────────────────────────────

    private static void resetDummyData(Connection conn) throws SQLException {
        List<UUID> userIds  = ALL_USERS.stream().map(SeedUser::userId).toList();
        List<UUID> buruhIds = List.of(BURUH_1.userId(), BURUH_2.userId());

        exec(conn, "DELETE FROM wallet_transactions WHERE user_id = ?", userIds);
        exec(conn, "DELETE FROM wallets           WHERE user_id = ?", userIds);
        exec(conn, "DELETE FROM payrolls           WHERE user_id = ?", userIds);

        // pengiriman_panen_item → pengiriman
        List<UUID> pgIds = List.of(uid("pg-1"), uid("pg-2"), uid("pg-3"), uid("pg-4"));
        exec(conn, "DELETE FROM pengiriman_panen_item WHERE pengiriman_id = ?", pgIds);
        exec(conn, "DELETE FROM pengiriman            WHERE pengiriman_id = ?", pgIds);

        exec(conn, "DELETE FROM harvest_photos  WHERE harvest_id IN (SELECT id FROM harvest_reports WHERE buruh_id = ?)", buruhIds);
        exec(conn, "DELETE FROM harvest_reports WHERE buruh_id = ?", buruhIds);

        // kebun
        exec(conn, "DELETE FROM kebun_supir      WHERE kebun_id = ?", List.of(KEBUN_1_ID, KEBUN_2_ID));
        exec(conn, "DELETE FROM kebun_coordinate WHERE kebun_id = ?", List.of(KEBUN_1_ID, KEBUN_2_ID));
        exec(conn, "DELETE FROM kebun            WHERE kebun_id = ?", List.of(KEBUN_1_ID, KEBUN_2_ID));

        exec(conn, "DELETE FROM notifications WHERE user_id = ?", userIds);
        exec(conn, "DELETE FROM users          WHERE user_id = ?", userIds);
    }

    private static void exec(Connection conn, String sql, List<UUID> ids) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (UUID id : ids) {
                ps.setObject(1, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private static void upsertUsers(Connection conn, String hashedPassword) throws SQLException {
        String sql = """
                INSERT INTO users (user_id, username, email, name, password, role, mandor_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                ON CONFLICT (user_id) DO UPDATE SET
                    username  = EXCLUDED.username,
                    email     = EXCLUDED.email,
                    name      = EXCLUDED.name,
                    password  = EXCLUDED.password,
                    role      = EXCLUDED.role,
                    mandor_id = EXCLUDED.mandor_id,
                    updated_at = NOW()
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SeedUser u : ALL_USERS) {
                ps.setObject(1, u.userId());
                ps.setString(2, u.username());
                ps.setString(3, u.email());
                ps.setString(4, u.name());
                ps.setString(5, hashedPassword);
                ps.setString(6, u.role());
                ps.setObject(7, u.mandorId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── Kebun ─────────────────────────────────────────────────────────────────

    private static void upsertKebun(Connection conn) throws SQLException {
        // kebun rows
        String kebunSql = """
                INSERT INTO kebun (kebun_id, nama, kode, luas, mandor_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (kebun_id) DO UPDATE SET
                    nama      = EXCLUDED.nama,
                    kode      = EXCLUDED.kode,
                    luas      = EXCLUDED.luas,
                    mandor_id = EXCLUDED.mandor_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(kebunSql)) {
            // kebun 1 — assigned to MANDOR
            ps.setObject(1, KEBUN_1_ID);
            ps.setString(2, "Kebun Dummy Utara");
            ps.setString(3, "KEB-DUMMY-001");
            ps.setInt(4, 350);
            ps.setObject(5, MANDOR.userId());
            ps.addBatch();
            // kebun 2 — no mandor
            ps.setObject(1, KEBUN_2_ID);
            ps.setString(2, "Kebun Dummy Selatan");
            ps.setString(3, "KEB-DUMMY-002");
            ps.setInt(4, 210);
            ps.setObject(5, null);
            ps.addBatch();
            ps.executeBatch();
        }

        // coordinates — simple 4-point polygon
        String coordSql = """
                INSERT INTO kebun_coordinate (kebun_id, idx, lat, lng)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (kebun_id, idx) DO UPDATE SET lat = EXCLUDED.lat, lng = EXCLUDED.lng
                """;
        int[][] kebun1Coords = {{-6, 106}, {-6, 107}, {-7, 107}, {-7, 106}};
        int[][] kebun2Coords = {{-7, 106}, {-7, 107}, {-8, 107}, {-8, 106}};
        try (PreparedStatement ps = conn.prepareStatement(coordSql)) {
            insertCoords(ps, KEBUN_1_ID, kebun1Coords);
            insertCoords(ps, KEBUN_2_ID, kebun2Coords);
            ps.executeBatch();
        }

        // supir assignments
        String supirSql = """
                INSERT INTO kebun_supir (id, kebun_id, supir_id)
                VALUES (?, ?, ?)
                ON CONFLICT (supir_id) DO UPDATE SET kebun_id = EXCLUDED.kebun_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(supirSql)) {
            ps.setObject(1, uid("kebun-supir-1"));
            ps.setObject(2, KEBUN_1_ID);
            ps.setObject(3, SUPIR_1.userId());
            ps.addBatch();
            ps.setObject(1, uid("kebun-supir-2"));
            ps.setObject(2, KEBUN_2_ID);
            ps.setObject(3, SUPIR_2.userId());
            ps.addBatch();
            ps.executeBatch();
        }
    }

    private static void insertCoords(PreparedStatement ps, UUID kebunId, int[][] coords) throws SQLException {
        for (int i = 0; i < coords.length; i++) {
            ps.setObject(1, kebunId);
            ps.setInt(2, i);
            ps.setInt(3, coords[i][0]);
            ps.setInt(4, coords[i][1]);
            ps.addBatch();
        }
    }

    // ── Panen ─────────────────────────────────────────────────────────────────

    private static List<SeedPanen> buildPanen() {
        LocalDateTime now = LocalDateTime.now();
        List<SeedPanen> list = new ArrayList<>();

        // BURUH_1: APPROVED, PENDING, REJECTED
        list.add(panen("panen-b1-1", BURUH_1, KEBUN_1_ID, 130, STATUS_APPROVED, null,  now.minusDays(10)));
        list.add(panen("panen-b1-2", BURUH_1, KEBUN_1_ID, 90,  STATUS_APPROVED, null,  now.minusDays(7)));
        list.add(panen("panen-b1-3", BURUH_1, KEBUN_1_ID, 75,  STATUS_PENDING,  null,  now.minusDays(1)));
        list.add(panen("panen-b1-4", BURUH_1, KEBUN_1_ID, 55,  STATUS_REJECTED, "Berat tidak sesuai timbangan mandor", now.minusDays(5)));

        // BURUH_2: APPROVED, APPROVED, PENDING
        list.add(panen("panen-b2-1", BURUH_2, KEBUN_2_ID, 110, STATUS_APPROVED, null,  now.minusDays(9)));
        list.add(panen("panen-b2-2", BURUH_2, KEBUN_2_ID, 145, STATUS_APPROVED, null,  now.minusDays(6)));
        list.add(panen("panen-b2-3", BURUH_2, KEBUN_2_ID, 80,  STATUS_PENDING,  null,  now.minusDays(2)));

        return list;
    }

    private static SeedPanen panen(String key, SeedUser buruh, UUID kebunId, int weight,
                                   String status, String rejectionReason, LocalDateTime createdAt) {
        List<String> photos = new ArrayList<>();
        photos.add(photoUrl(uid(key), 1));
        photos.add(photoUrl(uid(key), 2));
        if (STATUS_PENDING.equals(status)) photos.add(photoUrl(uid(key), 3));
        return new SeedPanen(uid(key), buruh.userId(), kebunId, weight, status, rejectionReason,
                "Hasil panen " + key, createdAt, photos);
    }

    private static void upsertPanen(Connection conn, List<SeedPanen> list) throws SQLException {
        String harvestSql = """
                INSERT INTO harvest_reports (id, buruh_id, kebun_id, weight, description, status,
                    rejection_reason, created_at, harvest_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    buruh_id         = EXCLUDED.buruh_id,
                    kebun_id         = EXCLUDED.kebun_id,
                    weight           = EXCLUDED.weight,
                    description      = EXCLUDED.description,
                    status           = EXCLUDED.status,
                    rejection_reason = EXCLUDED.rejection_reason,
                    created_at       = EXCLUDED.created_at,
                    harvest_date     = EXCLUDED.harvest_date
                """;
        try (PreparedStatement ps = conn.prepareStatement(harvestSql)) {
            for (SeedPanen p : list) {
                ps.setObject(1, p.panenId());
                ps.setObject(2, p.buruhId());
                ps.setObject(3, p.kebunId());
                ps.setInt(4, p.weight());
                ps.setString(5, p.description());
                ps.setString(6, p.status());
                ps.setString(7, p.rejectionReason());
                ps.setTimestamp(8, ts(p.createdAt()));
                ps.setDate(9, Date.valueOf(p.createdAt().toLocalDate()));
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // photos
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM harvest_photos WHERE harvest_id = ?")) {
            for (SeedPanen p : list) { del.setObject(1, p.panenId()); del.addBatch(); }
            del.executeBatch();
        }
        String photoSql = """
                INSERT INTO harvest_photos (id, harvest_id, photo_url, created_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET harvest_id = EXCLUDED.harvest_id, photo_url = EXCLUDED.photo_url
                """;
        try (PreparedStatement ps = conn.prepareStatement(photoSql)) {
            for (SeedPanen p : list) {
                int i = 1;
                for (String url : p.photoUrls()) {
                    ps.setObject(1, uid("photo-" + p.panenId() + "-" + i));
                    ps.setObject(2, p.panenId());
                    ps.setString(3, url);
                    ps.setTimestamp(4, ts(p.createdAt()));
                    ps.addBatch();
                    i++;
                }
            }
            ps.executeBatch();
        }
    }

    // ── Pengiriman ────────────────────────────────────────────────────────────

    private static List<SeedPengiriman> buildPengiriman(List<SeedPanen> panenList) {
        LocalDateTime now = LocalDateTime.now();

        // Collect approved panen IDs to link
        List<UUID> approvedPanen = panenList.stream()
                .filter(p -> STATUS_APPROVED.equals(p.status()))
                .map(SeedPanen::panenId)
                .toList();

        List<SeedPengiriman> list = new ArrayList<>();

        // SUPIR_1 — APPROVED_MANDOR (link first 2 approved panen)
        list.add(new SeedPengiriman(uid("pg-1"), SUPIR_1.userId(), MANDOR.userId(),
                "APPROVED_MANDOR", 240, 240, null, now.minusDays(8),
                approvedPanen.size() >= 2 ? List.of(approvedPanen.get(0), approvedPanen.get(1)) : List.of()));

        // SUPIR_1 — IN_TRANSIT (link next approved panen)
        list.add(new SeedPengiriman(uid("pg-2"), SUPIR_1.userId(), MANDOR.userId(),
                "IN_TRANSIT", 110, 0, null, now.minusDays(3),
                approvedPanen.size() >= 3 ? List.of(approvedPanen.get(2)) : List.of()));

        // SUPIR_2 — APPROVED_ADMIN (link next approved panen)
        list.add(new SeedPengiriman(uid("pg-3"), SUPIR_2.userId(), MANDOR.userId(),
                "APPROVED_ADMIN", 255, 255, null, now.minusDays(6),
                approvedPanen.size() >= 4 ? List.of(approvedPanen.get(3)) : List.of()));

        // SUPIR_2 — ASSIGNED (no panen linked yet)
        list.add(new SeedPengiriman(uid("pg-4"), SUPIR_2.userId(), MANDOR.userId(),
                "ASSIGNED", 180, 0, null, now.minusDays(1), List.of()));

        return list;
    }

    private static void upsertPengiriman(Connection conn, List<SeedPengiriman> list) throws SQLException {
        String pgSql = """
                INSERT INTO pengiriman (pengiriman_id, supir_id, mandor_id, status,
                    total_weight, accepted_weight, status_reason, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (pengiriman_id) DO UPDATE SET
                    supir_id        = EXCLUDED.supir_id,
                    mandor_id       = EXCLUDED.mandor_id,
                    status          = EXCLUDED.status,
                    total_weight    = EXCLUDED.total_weight,
                    accepted_weight = EXCLUDED.accepted_weight,
                    status_reason   = EXCLUDED.status_reason,
                    timestamp       = EXCLUDED.timestamp
                """;
        try (PreparedStatement ps = conn.prepareStatement(pgSql)) {
            for (SeedPengiriman pg : list) {
                ps.setObject(1, pg.pengirimanId());
                ps.setObject(2, pg.supirId());
                ps.setObject(3, pg.mandorId());
                ps.setString(4, pg.status());
                ps.setInt(5, pg.totalWeight());
                ps.setInt(6, pg.acceptedWeight());
                ps.setString(7, pg.statusReason());
                ps.setTimestamp(8, ts(pg.timestamp()));
                ps.addBatch();
            }
            ps.executeBatch();
        }

        String itemSql = """
                INSERT INTO pengiriman_panen_item (pengiriman_item_id, pengiriman_id, panen_id)
                VALUES (?, ?, ?)
                ON CONFLICT (panen_id) DO UPDATE SET pengiriman_id = EXCLUDED.pengiriman_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
            for (SeedPengiriman pg : list) {
                int i = 1;
                for (UUID panenId : pg.linkedPanenIds()) {
                    ps.setObject(1, uid("pgitem-" + pg.pengirimanId() + "-" + i));
                    ps.setObject(2, pg.pengirimanId());
                    ps.setObject(3, panenId);
                    ps.addBatch();
                    i++;
                }
            }
            ps.executeBatch();
        }
    }

    // ── Payrolls ──────────────────────────────────────────────────────────────

    private static List<SeedPayroll> buildPayrolls(List<SeedPanen> panenList, List<SeedPengiriman> pgList) {
        List<SeedPayroll> rows = new ArrayList<>();

        // onPanenApproved: BURUH PENDING created, then admin approves → BURUH APPROVED.
        // Same event auto-approves MANDOR payroll immediately (autoApprovePayroll).
        // REJECTED panen fires no event → no payroll row at all.
        for (SeedPanen p : panenList) {
            if (!STATUS_APPROVED.equals(p.status())) continue;
            LocalDateTime processedAt = p.createdAt().plusDays(1);
            String buruhRef = "PAY-DUMMY-B-" + p.panenId().toString().substring(0, 8).toUpperCase();
            String mandorRef = "PAY-DUMMY-M-" + p.panenId().toString().substring(0, 8).toUpperCase();

            rows.add(new SeedPayroll(
                    uid("payroll-buruh-" + p.panenId()),
                    p.buruhId(), ROLE_BURUH,
                    p.panenId(), REF_PANEN,
                    p.weight(), 10_000,
                    STATUS_APPROVED, null, processedAt, p.createdAt(), buruhRef));

            // MANDOR payroll auto-approved in same event (autoApprovePayroll)
            rows.add(new SeedPayroll(
                    uid("payroll-mandor-panen-" + p.panenId()),
                    MANDOR.userId(), ROLE_MANDOR,
                    p.panenId(), REF_PANEN,
                    p.weight(), 12_000,
                    STATUS_APPROVED, null, processedAt, p.createdAt(), mandorRef));
        }

        // Pengiriman payrolls mirror the real event handlers:
        //   APPROVED_MANDOR → onPengirimanApprovedByMandor → SUPIR PENDING (admin not yet acted)
        //   APPROVED_ADMIN  → SUPIR APPROVED (admin approved payroll) +
        //                     MANDOR APPROVED (onPengirimanProcessedByAdmin, acceptedWeight)
        //   IN_TRANSIT / ASSIGNED → no payroll (mandor hasn't approved yet)
        for (SeedPengiriman pg : pgList) {
            switch (pg.status()) {
                case "APPROVED_MANDOR" -> {
                    // SUPIR payroll created; admin has not yet approved it
                    rows.add(new SeedPayroll(
                            uid("payroll-supir-" + pg.pengirimanId()),
                            pg.supirId(), ROLE_SUPIR,
                            pg.pengirimanId(), REF_PENGIRIMAN,
                            pg.totalWeight(), 8_000,
                            STATUS_PENDING, null, null, pg.timestamp(), null));
                }
                case "APPROVED_ADMIN" -> {
                    // SUPIR payroll approved (from earlier APPROVED_MANDOR step, totalWeight)
                    LocalDateTime supirProcessed = pg.timestamp().plusDays(1);
                    rows.add(new SeedPayroll(
                            uid("payroll-supir-" + pg.pengirimanId()),
                            pg.supirId(), ROLE_SUPIR,
                            pg.pengirimanId(), REF_PENGIRIMAN,
                            pg.totalWeight(), 8_000,
                            STATUS_APPROVED, null, supirProcessed, pg.timestamp(),
                            "PAY-DUMMY-S-" + pg.pengirimanId().toString().substring(0, 8).toUpperCase()));
                    // MANDOR payroll from onPengirimanProcessedByAdmin (acceptedWeight), approved by admin
                    LocalDateTime mandorProcessed = supirProcessed.plusHours(2);
                    rows.add(new SeedPayroll(
                            uid("payroll-mandor-pg-" + pg.pengirimanId()),
                            MANDOR.userId(), ROLE_MANDOR,
                            pg.pengirimanId(), REF_PENGIRIMAN,
                            pg.acceptedWeight(), 12_000,
                            STATUS_APPROVED, null, mandorProcessed, pg.timestamp(),
                            "PAY-DUMMY-MG-" + pg.pengirimanId().toString().substring(0, 8).toUpperCase()));
                }
                default -> { /* IN_TRANSIT / ASSIGNED — no payroll yet */ }
            }
        }

        return rows;
    }

    private static void upsertPayrolls(Connection conn, List<SeedPayroll> payrolls) throws SQLException {
        String sql = """
                INSERT INTO payrolls (
                    payroll_id, user_id, role, reference_id, reference_type,
                    weight, wage_rate_applied, net_amount, status, rejection_reason,
                    processed_at, created_at, payment_reference)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (payroll_id) DO UPDATE SET
                    user_id           = EXCLUDED.user_id,
                    role              = EXCLUDED.role,
                    reference_id      = EXCLUDED.reference_id,
                    reference_type    = EXCLUDED.reference_type,
                    weight            = EXCLUDED.weight,
                    wage_rate_applied = EXCLUDED.wage_rate_applied,
                    net_amount        = EXCLUDED.net_amount,
                    status            = EXCLUDED.status,
                    rejection_reason  = EXCLUDED.rejection_reason,
                    processed_at      = EXCLUDED.processed_at,
                    created_at        = EXCLUDED.created_at,
                    payment_reference = EXCLUDED.payment_reference
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SeedPayroll r : payrolls) {
                ps.setObject(1, r.payrollId());
                ps.setObject(2, r.userId());
                ps.setString(3, r.role());
                ps.setObject(4, r.referenceId());
                ps.setString(5, r.referenceType());
                ps.setInt(6, r.weight());
                ps.setInt(7, r.wageRateApplied());
                ps.setLong(8, r.netAmount());
                ps.setString(9, r.status());
                ps.setString(10, r.rejectionReason());
                ps.setTimestamp(11, ts(r.processedAt()));
                ps.setTimestamp(12, ts(r.createdAt()));
                ps.setString(13, r.paymentReference());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // wallet_transactions for APPROVED payrolls
        String txSql = """
                INSERT INTO wallet_transactions (transaction_id, user_id, payroll_id, amount, type, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (transaction_id) DO UPDATE SET
                    user_id    = EXCLUDED.user_id,
                    payroll_id = EXCLUDED.payroll_id,
                    amount     = EXCLUDED.amount,
                    type       = EXCLUDED.type,
                    created_at = EXCLUDED.created_at
                """;
        try (PreparedStatement ps = conn.prepareStatement(txSql)) {
            for (SeedPayroll r : payrolls) {
                if (!STATUS_APPROVED.equals(r.status())) continue;
                ps.setObject(1, uid("tx-" + r.payrollId()));
                ps.setObject(2, r.userId());
                ps.setObject(3, r.payrollId());
                ps.setLong(4, r.netAmount());
                ps.setString(5, "CREDIT");
                ps.setTimestamp(6, ts(r.processedAt() != null ? r.processedAt() : r.createdAt()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void upsertWallets(Connection conn, List<SeedPayroll> payrolls) throws SQLException {
        String sql = """
                INSERT INTO wallets (user_id, balance, updated_at)
                VALUES (?, ?, NOW())
                ON CONFLICT (user_id) DO UPDATE SET balance = EXCLUDED.balance, updated_at = NOW()
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SeedUser u : ALL_USERS) {
                long balance = payrolls.stream()
                        .filter(r -> u.userId().equals(r.userId()) && STATUS_APPROVED.equals(r.status()))
                        .mapToLong(SeedPayroll::netAmount)
                        .sum();
                ps.setObject(1, u.userId());
                ps.setLong(2, balance);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── Notifications ──────────────────────────────────────────────────────────

    private static void upsertNotifications(Connection conn) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        String sql = """
                INSERT INTO notifications (notification_id, user_id, title, description, is_read, timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (notification_id) DO UPDATE SET
                    title       = EXCLUDED.title,
                    description = EXCLUDED.description,
                    is_read     = EXCLUDED.is_read,
                    timestamp   = EXCLUDED.timestamp
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            addNotif(ps, "notif-mandor-1", MANDOR,  "Pengiriman Selesai",      "Pengiriman KEB-001 telah selesai diproses.",         true,  now.minusDays(5));
            addNotif(ps, "notif-mandor-2", MANDOR,  "Panen Baru Masuk",        "Terdapat laporan panen baru dari Budi Buruh.",       false, now.minusDays(1));
            addNotif(ps, "notif-mandor-3", MANDOR,  "Payroll Menunggu",        "3 payroll menunggu persetujuan Anda.",               false, now.minusHours(3));

            addNotif(ps, "notif-b1-1",    BURUH_1, "Panen Disetujui",         "Laporan panen Anda tanggal " + now.minusDays(10).toLocalDate() + " disetujui.", true, now.minusDays(9));
            addNotif(ps, "notif-b1-2",    BURUH_1, "Payroll Diproses",        "Payroll Anda sebesar Rp 1.300.000 telah disetujui.", true, now.minusDays(8));
            addNotif(ps, "notif-b1-3",    BURUH_1, "Panen Ditolak",           "Laporan panen Anda ditolak: Berat tidak sesuai.",    false, now.minusDays(4));

            addNotif(ps, "notif-b2-1",    BURUH_2, "Panen Disetujui",         "Laporan panen Anda telah disetujui mandor.",         true,  now.minusDays(8));
            addNotif(ps, "notif-b2-2",    BURUH_2, "Payroll Diproses",        "Payroll Anda sebesar Rp 1.100.000 telah disetujui.", true,  now.minusDays(7));
            addNotif(ps, "notif-b2-3",    BURUH_2, "Laporan Baru",            "Silakan ajukan laporan panen hari ini.",             false, now.minusHours(6));

            addNotif(ps, "notif-s1-1",    SUPIR_1, "Pengiriman Disetujui",    "Pengiriman #pg-1 telah disetujui mandor.",           true,  now.minusDays(7));
            addNotif(ps, "notif-s1-2",    SUPIR_1, "Pengiriman Dalam Proses", "Pengiriman #pg-2 sedang dalam perjalanan.",          false, now.minusDays(2));

            addNotif(ps, "notif-s2-1",    SUPIR_2, "Pengiriman Selesai",      "Pengiriman #pg-3 telah disetujui admin.",            true,  now.minusDays(5));
            addNotif(ps, "notif-s2-2",    SUPIR_2, "Penugasan Baru",          "Anda memiliki penugasan pengiriman baru.",           false, now.minusDays(1));

            ps.executeBatch();
        }
    }

    private static void addNotif(PreparedStatement ps, String key, SeedUser user,
                                  String title, String desc, boolean isRead, LocalDateTime time) throws SQLException {
        ps.setObject(1, uid("notif-" + key));
        ps.setObject(2, user.userId());
        ps.setString(3, title);
        ps.setString(4, desc);
        ps.setBoolean(5, isRead);
        ps.setTimestamp(6, ts(time));
        ps.addBatch();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private static void verifyAdminExists(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("No ADMIN user found. Run migrations first.");
        }
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    private static void printSummary(List<SeedPayroll> payrolls, List<SeedPanen> panenList,
                                      List<SeedPengiriman> pgList) {
        LOG.info("=== seedDummyData complete ===");
        LOG.info("Users    : " + ALL_USERS.size() + " (password: " + TEST_PASSWORD + ")");
        LOG.info("Kebun    : 2");
        LOG.info("Panen    : " + panenList.size());
        LOG.info("Pengiriman: " + pgList.size());
        LOG.info("Payrolls : " + payrolls.size());
        LOG.info("");
        LOG.info("Credentials:");
        LOG.info(String.format("  %-8s  admin@mysawit.id            Admin@12345  (from migration)%n", "[ADMIN]"));
        for (SeedUser u : ALL_USERS) {
            LOG.info(String.format("  %-8s  %-30s  %s%n", "[" + u.role() + "]", u.email(), TEST_PASSWORD));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static UUID uid(String key) {
        return UUID.nameUUIDFromBytes(("mysawit-dummy-" + key).getBytes(StandardCharsets.UTF_8));
    }

    private static Timestamp ts(LocalDateTime dt) {
        return dt == null ? null : Timestamp.valueOf(dt);
    }

    private static String photoUrl(UUID id, int idx) {
        return "https://placehold.co/640x420/2d6a4f/ffffff?text=Panen+" + id.toString().substring(0, 8) + "-" + idx;
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : def;
    }

    // ── Records ───────────────────────────────────────────────────────────────

    private record SeedUser(UUID userId, String username, String email, String name,
                             String role, UUID mandorId) {}

    private record SeedPanen(UUID panenId, UUID buruhId, UUID kebunId, int weight,
                              String status, String rejectionReason, String description,
                              LocalDateTime createdAt, List<String> photoUrls) {}

    private record SeedPengiriman(UUID pengirimanId, UUID supirId, UUID mandorId,
                                   String status, int totalWeight, int acceptedWeight,
                                   String statusReason, LocalDateTime timestamp,
                                   List<UUID> linkedPanenIds) {}

    private record SeedPayroll(UUID payrollId, UUID userId, String role,
                                UUID referenceId, String referenceType,
                                int weight, int wageRateApplied, String status,
                                String rejectionReason, LocalDateTime processedAt,
                                LocalDateTime createdAt, String paymentReference) {
        long netAmount() { return (long) weight * wageRateApplied; }
    }
}
