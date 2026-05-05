package id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PanenTest {

    // -------------------------------------------------------------------------
    // Shared fixtures
    // -------------------------------------------------------------------------
    private UUID buruhId;
    private UUID kebunId;
    private LocalDateTime timestamp;
    private List<String> photoUrls;

    @BeforeEach
    void setUp() {
        buruhId    = UUID.randomUUID();
        kebunId    = UUID.randomUUID();
        timestamp  = LocalDateTime.now();
        photoUrls  = List.of("https://cdn.example.com/foto1.jpg",
                             "https://cdn.example.com/foto2.jpg");
    }

    // =========================================================================
    // catatBaru()
    // =========================================================================
    @Nested
    class CatatBaru {

        @Test
        void shouldCreatePanenWithStatusPending() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "Panen sawit blok A", 150, timestamp, photoUrls);

            assertEquals(PanenStatus.PENDING, panen.getStatus());
        }

        @Test
        void shouldGenerateRandomPanenId() {
            Panen panen1 = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "Panen 1", 100, timestamp, photoUrls);
            Panen panen2 = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "Panen 2", 100, timestamp, photoUrls);

            assertNotNull(panen1.getPanenId());
            assertNotEquals(panen1.getPanenId(), panen2.getPanenId());
        }

        @Test
        void shouldStoreAllFieldsCorrectly() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "Panen sawit blok A", 150, timestamp, photoUrls);

            assertEquals(buruhId,             panen.getBuruhId());
            assertEquals("Budi",              panen.getBuruhName());
            assertEquals(kebunId,             panen.getKebunId());
            assertEquals("Panen sawit blok A",panen.getDescription());
            assertEquals(150,                 panen.getWeight());
            assertEquals(timestamp,           panen.getTimestamp());
        }

        @Test
        void shouldHaveNullRejectionReasonOnCreation() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);

            assertNull(panen.getRejectionReason());
        }

        @Test
        void shouldConvertPhotoUrlsToPanenPhotos() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);

            assertEquals(photoUrls.size(), panen.getPhotos().size());
            List<String> resultUrls = panen.getPhotos().stream()
                    .map(PanenPhoto::getPhotoUrl)
                    .toList();
            assertTrue(resultUrls.containsAll(photoUrls));
        }

        @Test
        void shouldCreatePanenWithEmptyPhotoList() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, List.of());

            assertNotNull(panen.getPhotos());
            assertTrue(panen.getPhotos().isEmpty());
        }
    }

    // =========================================================================
    // approve()
    // =========================================================================
    @Nested
    class Approve {

        @Test
        void shouldChangeStatusToApprovedWhenPending() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);

            panen.approve();

            assertEquals(PanenStatus.APPROVED, panen.getStatus());
        }

        @Test
        void shouldClearRejectionReasonOnApprove() {
            // Buat panen yang sudah pernah di-reject, lalu di-approve
            // (simulasikan via constructor langsung karena state REJECTED
            //  tidak bisa diubah dari APPROVED/REJECTED via domain method)
            Panen panen = new Panen(UUID.randomUUID(), buruhId, "Budi", kebunId,
                    "desc", 100, PanenStatus.PENDING, "alasan sebelumnya",
                    timestamp, List.of());

            panen.approve();

            assertNull(panen.getRejectionReason());
        }

        @Test
        void shouldThrowWhenApprovingApprovedPanen() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);
            panen.approve();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    panen::approve);
            assertEquals("Hanya hasil panen dengan status PENDING yang dapat disetujui.",
                    ex.getMessage());
        }

        @Test
        void shouldThrowWhenApprovingRejectedPanen() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);
            panen.reject("Berat tidak sesuai");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    panen::approve);
            assertEquals("Hanya hasil panen dengan status PENDING yang dapat disetujui.",
                    ex.getMessage());
        }
    }

    // =========================================================================
    // reject()
    // =========================================================================
    @Nested
    class Reject {

        @Test
        void shouldChangeStatusToRejectedWhenPending() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);

            panen.reject("Berat tidak sesuai");

            assertEquals(PanenStatus.REJECTED, panen.getStatus());
        }

        @Test
        void shouldStoreRejectionReason() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);

            panen.reject("Berat tidak sesuai");

            assertEquals("Berat tidak sesuai", panen.getRejectionReason());
        }

        @Test
        void shouldThrowWhenRejectingApprovedPanen() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);
            panen.approve();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> panen.reject("alasan"));
            assertEquals("Hanya hasil panen dengan status PENDING yang dapat ditolak.",
                    ex.getMessage());
        }

        @Test
        void shouldThrowWhenRejectingRejectedPanen() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);
            panen.reject("alasan pertama");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> panen.reject("alasan kedua"));
            assertEquals("Hanya hasil panen dengan status PENDING yang dapat ditolak.",
                    ex.getMessage());
        }

        @Test
        void shouldThrowWhenRejectionReasonIsNull() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> panen.reject(null));
            assertEquals("Alasan penolakan harus ada.", ex.getMessage());
        }

        @Test
        void shouldThrowWhenRejectionReasonIsBlank() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> panen.reject("   "));
            assertEquals("Alasan penolakan harus ada.", ex.getMessage());
        }

        @Test
        void shouldThrowWhenRejectionReasonIsEmptyString() {
            Panen panen = Panen.catatBaru(buruhId, "Budi", kebunId,
                    "desc", 100, timestamp, photoUrls);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> panen.reject(""));
            assertEquals("Alasan penolakan harus ada.", ex.getMessage());
        }
    }
}