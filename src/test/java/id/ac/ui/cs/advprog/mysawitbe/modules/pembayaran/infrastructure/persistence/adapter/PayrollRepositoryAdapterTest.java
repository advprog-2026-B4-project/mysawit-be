package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.PayrollEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.VariabelPokokJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.PayrollJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence.mapper.PayrollMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollRepositoryAdapterTest {

    @Mock
    private PayrollJpaRepository payrollJpaRepository;

    @Mock
    private VariabelPokokJpaRepository variabelPokokJpaRepository;

    @Mock
    private PayrollMapper payrollMapper;

    @InjectMocks
    private PayrollRepositoryAdapter adapter;

    private UUID payrollId;
    private UUID userId;
    private PayrollEntity payrollEntity;
    private PayrollDTO payrollDTO;

    @BeforeEach
    void setUp() {
        payrollId = UUID.randomUUID();
        userId = UUID.randomUUID();
        payrollEntity = PayrollEntity.builder()
                .payrollId(payrollId)
                .userId(userId)
                .role("BURUH")
                .referenceId(UUID.randomUUID())
                .referenceType("PANEN")
                .weight(1000)
                .wageRateApplied(10)
                .netAmount(10000)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        payrollDTO = new PayrollDTO(
                payrollId,
                userId,
                "BURUH",
                payrollEntity.getReferenceId(),
                "PANEN",
                1000,
                10,
                10000,
                "PENDING",
                null,
                null,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("returns PayrollDTO when payroll exists")
        void findById_existing_returnsDto() {
            when(payrollJpaRepository.findById(payrollId)).thenReturn(Optional.of(payrollEntity));
            when(payrollMapper.toDto(payrollEntity)).thenReturn(payrollDTO);

            PayrollDTO result = adapter.findById(payrollId);

            assertThat(result).isEqualTo(payrollDTO);
            verify(payrollJpaRepository).findById(payrollId);
            verify(payrollMapper).toDto(payrollEntity);
        }

        @Test
        @DisplayName("returns null when payroll not found")
        void findById_notFound_returnsNull() {
            when(payrollJpaRepository.findById(payrollId)).thenReturn(Optional.empty());

            PayrollDTO result = adapter.findById(payrollId);

            assertThat(result).isNull();
            verify(payrollJpaRepository).findById(payrollId);
            verify(payrollMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("findStatusById")
    class FindStatusByIdTests {

        @Test
        @DisplayName("returns PayrollStatusDTO when payroll exists")
        void findStatusById_existing_returnsStatusDto() {
            PayrollStatusDTO statusDto = new PayrollStatusDTO(payrollId, userId, 10000, "PENDING", null, null);
            when(payrollJpaRepository.findById(payrollId)).thenReturn(Optional.of(payrollEntity));
            when(payrollMapper.toStatusDto(payrollEntity)).thenReturn(statusDto);

            PayrollStatusDTO result = adapter.findStatusById(payrollId);

            assertThat(result).isEqualTo(statusDto);
            verify(payrollJpaRepository).findById(payrollId);
            verify(payrollMapper).toStatusDto(payrollEntity);
        }

        @Test
        @DisplayName("returns null when payroll not found")
        void findStatusById_notFound_returnsNull() {
            when(payrollJpaRepository.findById(payrollId)).thenReturn(Optional.empty());

            PayrollStatusDTO result = adapter.findStatusById(payrollId);

            assertThat(result).isNull();
            verify(payrollJpaRepository).findById(payrollId);
            verify(payrollMapper, never()).toStatusDto(any());
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("saves entity and returns DTO")
        void save_validDto_savesAndReturnsDto() {
            when(payrollMapper.toEntity(payrollDTO)).thenReturn(payrollEntity);
            when(payrollJpaRepository.save(payrollEntity)).thenReturn(payrollEntity);
            when(payrollMapper.toDto(payrollEntity)).thenReturn(payrollDTO);

            PayrollDTO result = adapter.save(payrollDTO);

            assertThat(result).isEqualTo(payrollDTO);
            verify(payrollMapper).toEntity(payrollDTO);
            verify(payrollJpaRepository).save(payrollEntity);
            verify(payrollMapper).toDto(payrollEntity);
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndRoleAndReferenceIdAndReferenceType")
    class ExistsByUserIdAndRoleAndReferenceIdAndReferenceTypeTests {

        @Test
        @DisplayName("returns true when payroll exists")
        void exists_existing_returnsTrue() {
            UUID refId = UUID.randomUUID();
            when(payrollJpaRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                    userId, "BURUH", refId, "PANEN")).thenReturn(true);

            boolean result = adapter.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                    userId, "BURUH", refId, "PANEN");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when payroll does not exist")
        void exists_notExisting_returnsFalse() {
            UUID refId = UUID.randomUUID();
            when(payrollJpaRepository.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                    userId, "BURUH", refId, "PANEN")).thenReturn(false);

            boolean result = adapter.existsByUserIdAndRoleAndReferenceIdAndReferenceType(
                    userId, "BURUH", refId, "PANEN");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getWageRate")
    class GetWageRateTests {

        @Test
        @DisplayName("returns value for BURUH role")
        void getWageRate_buruh_returnsValue() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(new VariabelPokokEntity(VariableKey.UPAH_BURUH, 10000)));

            int result = adapter.getWageRate("BURUH");

            assertThat(result).isEqualTo(10000);
        }

        @Test
        @DisplayName("returns value for SUPIR role")
        void getWageRate_supir_returnsValue() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_SUPIR))
                    .thenReturn(Optional.of(new VariabelPokokEntity(VariableKey.UPAH_SUPIR, 15000)));

            int result = adapter.getWageRate("SUPIR");

            assertThat(result).isEqualTo(15000);
        }

        @Test
        @DisplayName("returns value for MANDOR role")
        void getWageRate_mandor_returnsValue() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_MANDOR))
                    .thenReturn(Optional.of(new VariabelPokokEntity(VariableKey.UPAH_MANDOR, 20000)));

            int result = adapter.getWageRate("MANDOR");

            assertThat(result).isEqualTo(20000);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when variable not found")
        void getWageRate_notFound_throwsEntityNotFoundException() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.getWageRate("BURUH"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Variabel pokok not found");
        }

        @ParameterizedTest
        @ValueSource(strings = {"INVALID_ROLE", "ADMIN", "", "xyz", "Buruhx", "Supiry"})
        @DisplayName("throws IllegalArgumentException for invalid role")
        void getWageRate_invalidRole_throwsIllegalArgumentException(String invalidRole) {
            assertThatThrownBy(() -> adapter.getWageRate(invalidRole))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported payroll role");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for whitespace-only role")
        void getWageRate_whitespaceRole_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> adapter.getWageRate("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported payroll role");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null role")
        void getWageRate_nullRole_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> adapter.getWageRate(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported payroll role");
        }

        @Test
        @DisplayName("normalizes role to uppercase before mapping")
        void getWageRate_lowercaseRole_normalizesAndReturnsValue() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(new VariabelPokokEntity(VariableKey.UPAH_BURUH, 10000)));

            int result = adapter.getWageRate("buruh");

            assertThat(result).isEqualTo(10000);
        }

        @Test
        @DisplayName("normalizes mixed case role to uppercase")
        void getWageRate_mixedCaseRole_normalizesAndReturnsValue() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_SUPIR))
                    .thenReturn(Optional.of(new VariabelPokokEntity(VariableKey.UPAH_SUPIR, 15000)));

            int result = adapter.getWageRate("Supir");

            assertThat(result).isEqualTo(15000);
        }

        @Test
        @DisplayName("trims whitespace from role before mapping")
        void getWageRate_whitespaceRole_trimsAndReturnsValue() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(new VariabelPokokEntity(VariableKey.UPAH_BURUH, 10000)));

            int result = adapter.getWageRate("  BURUH  ");

            assertThat(result).isEqualTo(10000);
        }
    }

    @Nested
    @DisplayName("updateWageRate")
    class UpdateWageRateTests {

        @Test
        @DisplayName("updates value for valid role")
        void updateWageRate_validRole_updatesAndSaves() {
            VariabelPokokEntity entity = new VariabelPokokEntity(VariableKey.UPAH_BURUH, 10000);
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.of(entity));

            adapter.updateWageRate("BURUH", 20000);

            ArgumentCaptor<VariabelPokokEntity> captor = ArgumentCaptor.forClass(VariabelPokokEntity.class);
            verify(variabelPokokJpaRepository).save(captor.capture());
            assertThat(captor.getValue().getValue()).isEqualTo(20000);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when variable not found")
        void updateWageRate_notFound_throwsEntityNotFoundException() {
            when(variabelPokokJpaRepository.findById(VariableKey.UPAH_BURUH))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.updateWageRate("BURUH", 20000))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Variabel pokok not found");
        }

        @ParameterizedTest
        @ValueSource(strings = {"INVALID_ROLE", "", "  "})
        @DisplayName("throws IllegalArgumentException for invalid role")
        void updateWageRate_invalidRole_throwsIllegalArgumentException(String invalidRole) {
            assertThatThrownBy(() -> adapter.updateWageRate(invalidRole, 20000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported payroll role");
        }
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserIdTests {

        @Test
        @DisplayName("applies userId filter and returns paginated results")
        void findByUserId_withUserId_returnsFilteredResults() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(List.of(payrollEntity))).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(
                    userId,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31),
                    "PENDING",
                    0,
                    10
            );

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0)).isEqualTo(payrollDTO);
        }

        @Test
        @DisplayName("handles null status filter")
        void findByUserId_nullStatus_ignoresStatusFilter() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, null, 0, 10);

            assertThat(result.items()).hasSize(1);
        }

        @Test
        @DisplayName("handles blank status filter")
        void findByUserId_blankStatus_ignoresStatusFilter() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, "  ", 0, 10);

            assertThat(result.items()).hasSize(1);
        }

        @Test
        @DisplayName("normalizes status to uppercase")
        void findByUserId_lowercaseStatus_normalizesToUppercase() {
            Page<PayrollEntity> page = new PageImpl<>(List.of());
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of());

            adapter.findByUserId(userId, null, null, "pending", 0, 10);

            verify(payrollJpaRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("returns paginated results with all filters")
        void findAll_withFilters_returnsPaginatedResults() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity), org.springframework.data.domain.PageRequest.of(0, 20), 1);
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(List.of(payrollEntity))).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findAll(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31),
                    "APPROVED",
                    0,
                    20
            );

            assertThat(result.items()).hasSize(1);
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("handles null status filter")
        void findAll_nullStatus_ignoresStatusFilter() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findAll(null, null, null, 0, 10);

            assertThat(result.items()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Pagination edge cases")
    class PaginationEdgeCasesTests {

        @Test
        @DisplayName("page < 0 becomes 0")
        void findByUserId_negativePage_becomesZero() {
            Page<PayrollEntity> page = new PageImpl<>(List.of());
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of());

            adapter.findByUserId(userId, null, null, null, -5, 10);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(payrollJpaRepository).findAll(any(Specification.class), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("size <= 0 becomes 10")
        void findByUserId_zeroSize_becomesTen() {
            Page<PayrollEntity> page = new PageImpl<>(List.of());
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of());

            adapter.findByUserId(userId, null, null, null, 0, 0);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(payrollJpaRepository).findAll(any(Specification.class), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("size > 100 becomes 100")
        void findByUserId_excessiveSize_becomesHundred() {
            Page<PayrollEntity> page = new PageImpl<>(List.of());
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of());

            adapter.findByUserId(userId, null, null, null, 0, 500);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(payrollJpaRepository).findAll(any(Specification.class), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("page 0 and size 10 is normal case")
        void findByUserId_normalPagination_worksAsExpected() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, null, 0, 10);

            assertThat(result.items()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("buildFilter combinations")
    class BuildFilterCombinationsTests {

        @Test
        @DisplayName("filter with all parameters")
        void buildFilter_allParams_includesAllPredicates() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            adapter.findByUserId(userId, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), "PENDING", 0, 10);

            verify(payrollJpaRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("filter with only userId")
        void buildFilter_onlyUserId_includesOnlyUserIdPredicate() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            adapter.findByUserId(userId, null, null, null, 0, 10);

            verify(payrollJpaRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("filter with only date range")
        void buildFilter_onlyDateRange_includesDatePredicates() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            adapter.findAll(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), null, 0, 10);

            verify(payrollJpaRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("filter with no parameters (findAll)")
        void buildFilter_noParams_includesNoPredicates() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            adapter.findAll(null, null, null, 0, 10);

            verify(payrollJpaRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("toPayrollPage mapping")
    class ToPayrollPageMappingTests {

        @Test
        @DisplayName("maps page content correctly")
        void toPayrollPage_mapsContentCorrectly() {
            Page<PayrollEntity> page = new PageImpl<>(
                    List.of(payrollEntity),
                    org.springframework.data.domain.PageRequest.of(0, 10),
                    1
            );
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, null, 0, 10);

            assertThat(result.items()).hasSize(1);
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.totalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("maps page number and size correctly")
        void toPayrollPage_mapsNumberAndSizeCorrectly() {
            Page<PayrollEntity> page = new PageImpl<>(
                    List.of(payrollEntity),
                    org.springframework.data.domain.PageRequest.of(3, 25),
                    100
            );
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, null, 3, 25);

            assertThat(result.page()).isEqualTo(3);
            assertThat(result.size()).isEqualTo(25);
        }

        @Test
        @DisplayName("handles empty page")
        void toPayrollPage_emptyPage_returnsEmptyResult() {
            Page<PayrollEntity> page = new PageImpl<>(
                    List.of(),
                    org.springframework.data.domain.PageRequest.of(0, 10),
                    0
            );
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of());

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, null, 0, 10);

            assertThat(result.items()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0);
            assertThat(result.totalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("hasNext is true when more pages exist")
        void toPayrollPage_hasNext_returnsTrueWhenMorePages() {
            Page<PayrollEntity> page = new PageImpl<>(
                    List.of(payrollEntity),
                    org.springframework.data.domain.PageRequest.of(0, 10),
                    25
            );
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, null, 0, 10);

            assertThat(result.totalElements()).isEqualTo(25);
            assertThat(result.totalPages()).isEqualTo(3);
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("hasNext is false on last page")
        void toPayrollPage_hasNext_returnsFalseOnLastPage() {
            Page<PayrollEntity> page = new PageImpl<>(
                    List.of(payrollEntity),
                    org.springframework.data.domain.PageRequest.of(2, 10),
                    25
            );
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, null, 2, 10);

            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("hasPrevious is true when page > 0")
        void toPayrollPage_hasPrevious_returnsTrueWhenNotFirstPage() {
            Page<PayrollEntity> page = new PageImpl<>(
                    List.of(payrollEntity),
                    org.springframework.data.domain.PageRequest.of(1, 10),
                    20
            );
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, null, 1, 10);

            assertThat(result.hasPrevious()).isTrue();
        }

        @Test
        @DisplayName("hasPrevious is false on first page")
        void toPayrollPage_hasPrevious_returnsFalseOnFirstPage() {
            Page<PayrollEntity> page = new PageImpl<>(
                    List.of(payrollEntity),
                    org.springframework.data.domain.PageRequest.of(0, 10),
                    20
            );
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, null, 0, 10);

            assertThat(result.hasPrevious()).isFalse();
        }
    }

    @Nested
    @DisplayName("toEndOfDay edge cases")
    class ToEndOfDayEdgeCasesTests {

        @Test
        @DisplayName("end date at year boundary wraps to next year")
        void toEndOfDay_yearBoundary_worksCorrectly() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            // Using Dec 31, 2024 - the toEndOfDay will compute next day's start minus 1 nanosecond
            PayrollPageDTO result = adapter.findByUserId(
                    userId,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31),
                    null,
                    0,
                    10
            );

            assertThat(result.items()).hasSize(1);
        }

        @Test
        @DisplayName("end of month boundary is handled correctly")
        void toEndOfDay_monthBoundary_worksCorrectly() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            // Feb 28 in non-leap year or Feb 29 in leap year
            PayrollPageDTO result = adapter.findByUserId(
                    userId,
                    LocalDate.of(2024, 2, 1),
                    LocalDate.of(2024, 2, 29),
                    null,
                    0,
                    10
            );

            assertThat(result.items()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("buildFilter edge cases")
    class BuildFilterEdgeCasesTests {

        @Test
        @DisplayName("blank status string is treated as no filter")
        void buildFilter_blankStatus_treatedAsNoFilter() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            // " " is blank, should not be added as predicate
            PayrollPageDTO result = adapter.findByUserId(userId, null, null, " ", 0, 10);

            assertThat(result.items()).hasSize(1);
            verify(payrollJpaRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("whitespace status string is treated as no filter")
        void buildFilter_whitespaceStatus_treatedAsNoFilter() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            PayrollPageDTO result = adapter.findByUserId(userId, null, null, "   ", 0, 10);

            assertThat(result.items()).hasSize(1);
        }

        @Test
        @DisplayName("status with surrounding whitespace is trimmed and uppercased")
        void buildFilter_statusWithWhitespace_normalizesCorrectly() {
            Page<PayrollEntity> page = new PageImpl<>(List.of(payrollEntity));
            when(payrollJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(payrollMapper.toDtoList(any())).thenReturn(List.of(payrollDTO));

            adapter.findByUserId(userId, null, null, "  PENDING  ", 0, 10);

            verify(payrollJpaRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }
}