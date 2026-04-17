package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PayrollJpaRepositoryTest {

    @Autowired
    private PayrollJpaRepository payrollJpaRepository;

    @Test
    void save_duplicatePayrollReference_throwsDataIntegrityViolationException() {
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        payrollJpaRepository.saveAndFlush(buildPayroll(userId, "SUPIR", referenceId, "PENGIRIMAN"));

        assertThatThrownBy(() ->
                payrollJpaRepository.saveAndFlush(buildPayroll(userId, "SUPIR", referenceId, "PENGIRIMAN"))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_distinctPayrollReferenceFields_succeeds() {
        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        payrollJpaRepository.saveAndFlush(buildPayroll(userId, "SUPIR", referenceId, "PENGIRIMAN"));
        payrollJpaRepository.saveAndFlush(buildPayroll(userId, "MANDOR", referenceId, "PENGIRIMAN"));
        payrollJpaRepository.saveAndFlush(buildPayroll(userId, "SUPIR", UUID.randomUUID(), "PENGIRIMAN"));
        payrollJpaRepository.saveAndFlush(buildPayroll(UUID.randomUUID(), "SUPIR", referenceId, "PENGIRIMAN"));
        payrollJpaRepository.saveAndFlush(buildPayroll(userId, "SUPIR", referenceId, "PANEN"));

        assertThat(payrollJpaRepository.count()).isEqualTo(5);
    }

    private PayrollEntity buildPayroll(UUID userId, String role, UUID referenceId, String referenceType) {
        return PayrollEntity.builder()
                .userId(userId)
                .role(role)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .weight(100_000)
                .wageRateApplied(8)
                .netAmount(800_000)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
