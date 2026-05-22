package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PayrollDTOMissedBranchTest {

    @Test
    void constructorWithNullEvidencePhotoUrls_initializesEmptyList() {
        UUID payrollId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        PayrollDTO dto = new PayrollDTO(
                payrollId,
                userId,
                "BURUH",
                referenceId,
                "PANEN",
                1000,
                10,
                10000,
                "PENDING",
                null,
                now,
                now
        );

        assertNotNull(dto.evidencePhotoUrls());
        assertTrue(dto.evidencePhotoUrls().isEmpty());
    }

    @Test
    void constructorWithEvidencePhotoUrlsList_preservesList() {
        UUID payrollId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        List<String> urls = new ArrayList<>();
        urls.add("http://example.com/photo1.jpg");
        urls.add("http://example.com/photo2.jpg");

        PayrollDTO dto = new PayrollDTO(
                payrollId,
                userId,
                "BURUH",
                referenceId,
                "PANEN",
                1000,
                10,
                10000,
                "PENDING",
                null,
                now,
                now,
                urls
        );

        assertEquals(2, dto.evidencePhotoUrls().size());
        assertEquals("http://example.com/photo1.jpg", dto.evidencePhotoUrls().get(0));
        assertEquals("http://example.com/photo2.jpg", dto.evidencePhotoUrls().get(1));
    }

    @Test
    void compactConstructorDelegatesToCanonical() {
        UUID payrollId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        PayrollDTO dto = new PayrollDTO(
                payrollId,
                userId,
                "SUPIR",
                referenceId,
                "PENGIRIMAN",
                2000,
                15,
                30000,
                "APPROVED",
                null,
                now,
                now
        );

        // The compact constructor is called, which should call canonical with empty list
        assertNotNull(dto.evidencePhotoUrls());
        assertTrue(dto.evidencePhotoUrls().isEmpty());
        assertEquals(payrollId, dto.payrollId());
        assertEquals(userId, dto.userId());
        assertEquals("SUPIR", dto.role());
        assertEquals(referenceId, dto.referenceId());
        assertEquals("PENGIRIMAN", dto.referenceType());
        assertEquals(2000, dto.weight());
        assertEquals(15, dto.wageRateApplied());
        assertEquals(30000, dto.netAmount());
        assertEquals("APPROVED", dto.status());
    }

    @Test
    void evidencePhotoUrlsIsImmutable() {
        UUID payrollId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        List<String> urls = new ArrayList<>();
        urls.add("http://example.com/photo1.jpg");

        PayrollDTO dto = new PayrollDTO(
                payrollId,
                userId,
                "BURUH",
                referenceId,
                "PANEN",
                1000,
                10,
                10000,
                "PENDING",
                null,
                now,
                now,
                urls
        );

        // Verify the list is copied, not same reference
        assertNotSame(urls, dto.evidencePhotoUrls());
    }

    @Test
    void evidencePhotoUrlsCopiedFromNull() {
        UUID payrollId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        PayrollDTO dto = new PayrollDTO(
                payrollId,
                userId,
                "BURUH",
                referenceId,
                "PANEN",
                1000,
                10,
                10000,
                "PENDING",
                null,
                now,
                now,
                null
        );

        // Should be an empty list, not null
        assertNotNull(dto.evidencePhotoUrls());
        assertTrue(dto.evidencePhotoUrls().isEmpty());
    }

    @Test
    void compactConstructorCallsCanonicalWithEmptyList() {
        UUID payrollId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        PayrollDTO dto = new PayrollDTO(
                payrollId,
                userId,
                "MANDOR",
                referenceId,
                "SUPERVISI",
                500,
                20,
                10000,
                "APPROVED",
                "All good",
                now,
                now
        );

        // evidencePhotoUrls should be initialized to empty list via compact constructor
        assertNotNull(dto.evidencePhotoUrls());
        assertEquals(0, dto.evidencePhotoUrls().size());
    }

    @Test
    void allFieldsAccessible() {
        UUID payrollId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID referenceId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        LocalDateTime createdAt = LocalDateTime.of(2024, 6, 15, 10, 0, 0);
        LocalDateTime processedAt = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
        List<String> urls = List.of("url1", "url2");

        PayrollDTO dto = new PayrollDTO(
                payrollId,
                userId,
                "BURUH",
                referenceId,
                "PANEN",
                1000,
                10,
                10000,
                "PENDING",
                "rejection reason",
                processedAt,
                createdAt,
                urls
        );

        assertEquals(payrollId, dto.payrollId());
        assertEquals(userId, dto.userId());
        assertEquals("BURUH", dto.role());
        assertEquals(referenceId, dto.referenceId());
        assertEquals("PANEN", dto.referenceType());
        assertEquals(1000, dto.weight());
        assertEquals(10, dto.wageRateApplied());
        assertEquals(10000, dto.netAmount());
        assertEquals("PENDING", dto.status());
        assertEquals("rejection reason", dto.rejectionReason());
        assertEquals(processedAt, dto.processedAt());
        assertEquals(createdAt, dto.createdAt());
        assertEquals(2, dto.evidencePhotoUrls().size());
    }
}