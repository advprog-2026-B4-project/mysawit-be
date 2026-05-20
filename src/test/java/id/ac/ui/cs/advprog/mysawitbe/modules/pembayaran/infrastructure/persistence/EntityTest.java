package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void testWalletEntity() {
        WalletEntity entity = new WalletEntity();
        
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        entity.setUserId(userId);
        entity.setBalance(1000);
        entity.setUpdatedAt(now);
        
        assertEquals(userId, entity.getUserId());
        assertEquals(1000, entity.getBalance());
        assertEquals(now, entity.getUpdatedAt());

        WalletEntity entity2 = WalletEntity.builder()
                .userId(userId)
                .balance(1000)
                .updatedAt(now)
                .build();
        assertNotNull(entity2.toString());

        WalletEntity e3 = new WalletEntity();
        e3.touchUpdatedAt(); // inside same package, accessible
        assertNotNull(e3.getUpdatedAt());
    }

    @Test
    void testWalletTransactionEntity() {
        WalletTransactionEntity entity = new WalletTransactionEntity();
        
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID payrollId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        entity.setTransactionId(id);
        entity.setUserId(userId);
        entity.setPayrollId(payrollId);
        entity.setReference("ref");
        entity.setAmount(100);
        entity.setType("CREDIT");
        entity.setCreatedAt(now);
        
        assertEquals(id, entity.getTransactionId());
        assertEquals(userId, entity.getUserId());
        assertEquals(payrollId, entity.getPayrollId());
        assertEquals("ref", entity.getReference());
        assertEquals(100, entity.getAmount());
        assertEquals("CREDIT", entity.getType());
        assertEquals(now, entity.getCreatedAt());

        WalletTransactionEntity entity2 = WalletTransactionEntity.builder()
                .transactionId(id)
                .userId(userId)
                .payrollId(payrollId)
                .reference("ref")
                .amount(100)
                .type("CREDIT")
                .createdAt(now)
                .build();
        assertNotNull(entity2.toString());

        WalletTransactionEntity e3 = new WalletTransactionEntity();
        e3.prePersist();
        assertNotNull(e3.getCreatedAt());

        // Cover branch where createdAt is not null
        e3.prePersist();
    }

    @Test
    void testPayrollEntity() {
        PayrollEntity entity = new PayrollEntity();
        
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        entity.setPayrollId(id);
        entity.setUserId(userId);
        entity.setRole("BURUH");
        entity.setReferenceId(refId);
        entity.setReferenceType("PANEN");
        entity.setWeight(1000);
        entity.setWageRateApplied(1);
        entity.setNetAmount(1000);
        entity.setStatus("PENDING");
        entity.setRejectionReason("reason");
        entity.setProcessedAt(now);
        entity.setCreatedAt(now);
        entity.setPaymentReference("ref");
        
        assertEquals(id, entity.getPayrollId());
        assertEquals(userId, entity.getUserId());
        assertEquals("BURUH", entity.getRole());
        assertEquals(refId, entity.getReferenceId());
        assertEquals("PANEN", entity.getReferenceType());
        assertEquals(1000, entity.getWeight());
        assertEquals(1, entity.getWageRateApplied());
        assertEquals(1000, entity.getNetAmount());
        assertEquals("PENDING", entity.getStatus());
        assertEquals("reason", entity.getRejectionReason());
        assertEquals(now, entity.getProcessedAt());
        assertEquals(now, entity.getCreatedAt());
        assertEquals("ref", entity.getPaymentReference());

        PayrollEntity entity2 = PayrollEntity.builder()
                .payrollId(id)
                .userId(userId)
                .role("BURUH")
                .referenceId(refId)
                .referenceType("PANEN")
                .weight(1000)
                .wageRateApplied(1)
                .netAmount(1000)
                .status("PENDING")
                .rejectionReason("reason")
                .processedAt(now)
                .createdAt(now)
                .paymentReference("ref")
                .build();
        assertNotNull(entity2.toString());

        PayrollEntity e3 = new PayrollEntity();
        e3.prePersist();
        assertNotNull(e3.getCreatedAt());

        // Coverage for branch if createdAt != null
        e3.prePersist();
    }
}