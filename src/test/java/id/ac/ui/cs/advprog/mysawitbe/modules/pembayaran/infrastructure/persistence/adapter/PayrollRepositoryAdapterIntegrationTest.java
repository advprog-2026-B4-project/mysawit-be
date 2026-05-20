package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.PayrollEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.PayrollJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper.PayrollMapper;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper.PayrollMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for PayrollRepositoryAdapter that require a real JPA database
 * to evaluate Specification predicates (buildFilter lambda).
 *
 * This addresses the coverage gap in the Mockito-based unit tests where
 * findAll(spec, pageable) mocks never actually execute the Specification lambda bytecode.
 */
@DataJpaTest
@Import(PayrollMapperImpl.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PayrollRepositoryAdapterIntegrationTest {

    @Autowired
    private PayrollJpaRepository payrollJpaRepository;

    @Autowired
    private PayrollMapper payrollMapper;

    private VariabelPokokJpaRepository variabelPokokJpaRepository;

    private PayrollRepositoryAdapter adapter;

    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        variabelPokokJpaRepository = mock(VariabelPokokJpaRepository.class);
        adapter = new PayrollRepositoryAdapter(payrollJpaRepository, variabelPokokJpaRepository, payrollMapper);
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();

        // Stub variabelPokokJpaRepository for wage rate tests
        VariabelPokokEntity vpEntity = new VariabelPokokEntity(VariableKey.UPAH_BURUH, 10);
        when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                .thenReturn(Optional.of(vpEntity));
    }

    private PayrollEntity savePayroll(UUID userId, String role, String status, LocalDate createdAt) {
        PayrollEntity entity = PayrollEntity.builder()
                .userId(userId)
                .role(role)
                .referenceId(UUID.randomUUID())
                .referenceType("PANEN")
                .weight(1000)
                .wageRateApplied(10)
                .netAmount(10000)
                .status(status)
                .createdAt(createdAt.atStartOfDay())
                .build();
        return payrollJpaRepository.saveAndFlush(entity);
    }

    @Nested
    @DisplayName("findByUserId with real Specification (requires JPA)")
    class FindByUserIdWithSpecification {

        @Test
        @DisplayName("buildFilter predicates execute when userId is non-null")
        void findByUserId_withUserId_executesUserIdPredicate() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 15));
            savePayroll(userId2, "BURUH", "PENDING", LocalDate.of(2024, 6, 20));

            PayrollPageDTO result = adapter.findByUserId(userId1, null, null, null, 0, 10);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).userId()).isEqualTo(userId1);
        }

        @Test
        @DisplayName("buildFilter predicates execute when startDate is non-null")
        void findByUserId_withStartDate_executesStartDatePredicate() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 10));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 20));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 7, 1));

            PayrollPageDTO result = adapter.findByUserId(
                    userId1,
                    LocalDate.of(2024, 6, 15),
                    null,
                    null,
                    0,
                    10
            );

            // startDate filter should exclude the June 10 record
            assertThat(result.items()).hasSizeGreaterThanOrEqualTo(1);
            assertThat(result.items()).allMatch(p ->
                    !p.createdAt().toLocalDate().isBefore(LocalDate.of(2024, 6, 15)));
        }

        @Test
        @DisplayName("buildFilter predicates execute when endDate is non-null")
        void findByUserId_withEndDate_executesEndDatePredicate() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 10));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 20));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 7, 1));

            PayrollPageDTO result = adapter.findByUserId(
                    userId1,
                    null,
                    LocalDate.of(2024, 6, 25),
                    null,
                    0,
                    10
            );

            assertThat(result.items()).hasSize(2);
        }

        @Test
        @DisplayName("buildFilter predicates execute when status is blank (should ignore status)")
        void findByUserId_withBlankStatus_ignoresStatusPredicate() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 15));
            savePayroll(userId1, "BURUH", "APPROVED", LocalDate.of(2024, 6, 20));
            PayrollPageDTO result = adapter.findByUserId(userId1, null, null, "   ", 0, 10);
            assertEquals(2, result.items().size());
        }

        @Test
        @DisplayName("buildFilter predicates execute when status is non-null and non-blank")
        void findByUserId_withStatus_executesStatusPredicate() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 15));
            savePayroll(userId1, "BURUH", "APPROVED", LocalDate.of(2024, 6, 20));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 25));

            PayrollPageDTO result = adapter.findByUserId(
                    userId1,
                    null,
                    null,
                    "PENDING",
                    0,
                    10
            );

            assertThat(result.items()).hasSize(2);
            assertThat(result.items()).allMatch(p -> "PENDING".equals(p.status()));
        }

        @Test
        @DisplayName("buildFilter predicates execute when all params are non-null")
        void findByUserId_withAllParams_executesAllPredicates() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 10));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 20));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 7, 1));
            savePayroll(userId1, "BURUH", "APPROVED", LocalDate.of(2024, 6, 20));
            savePayroll(userId2, "BURUH", "PENDING", LocalDate.of(2024, 6, 20));

            PayrollPageDTO result = adapter.findByUserId(
                    userId1,
                    LocalDate.of(2024, 6, 15),
                    LocalDate.of(2024, 6, 25),
                    "PENDING",
                    0,
                    10
            );

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).createdAt().toLocalDate())
                    .isBetween(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 25));
        }
    }

    @Nested
    @DisplayName("findAll with real Specification (requires JPA)")
    class FindAllWithSpecification {

        @Test
        @DisplayName("findAll executes buildFilter with all null params (empty predicate list)")
        void findAll_noParams_executesNoPredicates() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 15));
            savePayroll(userId2, "SUPIR", "APPROVED", LocalDate.of(2024, 6, 20));

            PayrollPageDTO result = adapter.findAll(null, null, null, 0, 10);

            assertThat(result.items()).hasSize(2);
        }

        @Test
        @DisplayName("findAll with date range executes date predicates")
        void findAll_withDateRange_executesDatePredicates() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 10));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 20));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 7, 1));

            PayrollPageDTO result = adapter.findAll(
                    LocalDate.of(2024, 6, 15),
                    LocalDate.of(2024, 6, 25),
                    null,
                    0,
                    10
            );

            assertThat(result.items()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("toEndOfDay computation")
    class ToEndOfDayTests {

        @Test
        @DisplayName("toEndOfDay boundary - end of day before midnight")
        void toEndOfDay_endOfYearBoundary_worksCorrectly() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 12, 31));

            // Query with endDate = Dec 31 should include the Dec 31 record
            PayrollPageDTO result = adapter.findByUserId(
                    userId1,
                    null,
                    LocalDate.of(2024, 12, 31),
                    null,
                    0,
                    10
            );

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).createdAt().toLocalDate())
                    .isEqualTo(LocalDate.of(2024, 12, 31));
        }

        @Test
        @DisplayName("toEndOfDay leap year boundary - Feb 29")
        void toEndOfDay_leapYearFeb29_worksCorrectly() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 2, 28));
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 3, 1));

            // Query with endDate = Feb 29 should NOT include Mar 1
            PayrollPageDTO result = adapter.findByUserId(
                    userId1,
                    null,
                    LocalDate.of(2024, 2, 29),
                    null,
                    0,
                    10
            );

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).createdAt().toLocalDate())
                    .isEqualTo(LocalDate.of(2024, 2, 28));
        }
    }

    @Nested
    @DisplayName("toVariableKey with real JPA role matching")
    class ToVariableKeyTests {

        @Test
        @DisplayName("getWageRate with BURUH role queries correct variable key")
        void getWageRate_buruh_queriesCorrectKey() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(new VariabelPokokEntity(VariableKey.UPAH_BURUH, 10)));

            int wageRate = adapter.getWageRate("BURUH");

            assertThat(wageRate).isEqualTo(10);
        }

        @Test
        @DisplayName("getWageRate normalizes and uppercases role before matching")
        void getWageRate_lowercaseRole_normalizesAndUppercases() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(new VariabelPokokEntity(VariableKey.UPAH_BURUH, 12)));

            int wageRate = adapter.getWageRate("buruh");

            assertThat(wageRate).isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("Pagination paths")
    class PaginationTests {

        @Test
        @DisplayName("toPageable page < 0 returns 0")
        void findByUserId_negativePage_returnsZero() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 15));

            PayrollPageDTO result = adapter.findByUserId(userId1, null, null, null, -5, 10);

            assertThat(result.page()).isEqualTo(0);
        }

        @Test
        @DisplayName("toPageable size <= 0 returns 10")
        void findByUserId_zeroSize_returnsTen() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 15));

            PayrollPageDTO result = adapter.findByUserId(userId1, null, null, null, 0, 0);

            assertThat(result.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("toPageable size > 100 caps at 100")
        void findByUserId_largeSize_cappedAtHundred() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 15));

            PayrollPageDTO result = adapter.findByUserId(userId1, null, null, null, 0, 500);

            assertThat(result.size()).isEqualTo(100);
        }

        @Test
        @DisplayName("hasNext is false when on last page")
        void findByUserId_singlePage_hasNextFalse() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 15));

            PayrollPageDTO result = adapter.findByUserId(userId1, null, null, null, 0, 10);

            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("hasPrevious is false on first page")
        void findByUserId_firstPage_hasPreviousFalse() {
            savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 15));

            PayrollPageDTO result = adapter.findByUserId(userId1, null, null, null, 0, 10);

            assertThat(result.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("hasNext is true when more pages exist")
        void findByUserId_multiplePages_hasNextTrue() {
            // Save 15 records for userId1 with page size 10
            for (int i = 0; i < 15; i++) {
                savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 1).plusDays(i));
            }

            PayrollPageDTO page0 = adapter.findByUserId(userId1, null, null, null, 0, 10);
            assertThat(page0.hasNext()).isTrue();
            assertThat(page0.totalElements()).isEqualTo(15);
        }

        @Test
        @DisplayName("hasPrevious is true when not on first page")
        void findByUserId_secondPage_hasPreviousTrue() {
            for (int i = 0; i < 15; i++) {
                savePayroll(userId1, "BURUH", "PENDING", LocalDate.of(2024, 6, 1).plusDays(i));
            }

            PayrollPageDTO page1 = adapter.findByUserId(userId1, null, null, null, 1, 10);
            assertThat(page1.hasPrevious()).isTrue();
        }
    }
}
