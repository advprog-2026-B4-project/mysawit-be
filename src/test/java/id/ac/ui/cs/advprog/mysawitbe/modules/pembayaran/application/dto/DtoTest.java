package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void testPayrollDTO() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        List<String> urls = List.of("url1");
        
        PayrollDTO dto1 = new PayrollDTO(id, userId, "BURUH", refId, "PANEN", 100, 1, 100, "PENDING", null, null, now, urls);
        PayrollDTO dto2 = new PayrollDTO(id, userId, "BURUH", refId, "PANEN", 100, 1, 100, "PENDING", null, null, now, urls);
        PayrollDTO dto3 = new PayrollDTO(id, userId, "BURUH", refId, "PANEN", 100, 1, 100, "APPROVED", null, null, now, urls);
        
        // Record methods
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotNull(dto1.toString());
        
        // Accessors
        assertEquals(id, dto1.payrollId());
        assertEquals(userId, dto1.userId());
        assertEquals("BURUH", dto1.role());
        assertEquals(refId, dto1.referenceId());
        assertEquals("PANEN", dto1.referenceType());
        assertEquals(100, dto1.weight());
        assertEquals(1, dto1.wageRateApplied());
        assertEquals(100, dto1.netAmount());
        assertEquals("PENDING", dto1.status());
        assertNull(dto1.processedAt());
        assertEquals(now, dto1.createdAt());
        assertEquals(urls, dto1.evidencePhotoUrls());
        assertNull(dto1.rejectionReason());

        PayrollDTO dtoNullEvidence = new PayrollDTO(id, userId, "BURUH", refId, "PANEN", 100, 1, 100, "PENDING", null, null, now, null);
        assertNotNull(dtoNullEvidence.evidencePhotoUrls());
    }

    @Test
    void testUpdateWageRateRequestDTO() {
        UpdateWageRateRequestDTO dto1 = new UpdateWageRateRequestDTO("A", BigDecimal.TEN);
        UpdateWageRateRequestDTO dto2 = new UpdateWageRateRequestDTO("A", BigDecimal.TEN);
        UpdateWageRateRequestDTO dto3 = new UpdateWageRateRequestDTO("B", BigDecimal.TEN);
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotNull(dto1.toString());
        assertEquals("A", dto1.type());
        assertEquals(BigDecimal.TEN, dto1.newRatePerGram());
    }
}