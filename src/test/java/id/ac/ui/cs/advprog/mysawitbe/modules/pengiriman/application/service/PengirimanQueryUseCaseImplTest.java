package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignedSupirDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignmentRecommendationDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignablePanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.exception.KebunQueryDependencyUnavailableException;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out.PengirimanRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PengirimanQueryUseCaseImplTest {

    @Mock
    private PengirimanRepositoryPort repository;

    @Mock
    private ObjectProvider<KebunQueryUseCase> kebunQueryUseCaseProvider;

    @Mock
    private KebunQueryUseCase kebunQueryUseCase;

    @Mock
    private PanenQueryUseCase panenQueryUseCase;

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private PengirimanQueryUseCaseImpl service;

    @Test
    void listDeliveriesBySupir_withoutDateFilter_returnsRepositoryResult() {
        UUID supirId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        PengirimanDTO dto = new PengirimanDTO(
                UUID.randomUUID(),
                supirId,
                mandorId,
                "ASSIGNED",
                120000,
                0,
                LocalDateTime.now()
        );
        when(userQueryUseCase.getUserById(supirId))
                .thenReturn(new UserDTO(supirId, "supir-a", "Supir A", "SUPIR", "supir@example.com"));
        when(userQueryUseCase.getUserById(mandorId))
                .thenReturn(new UserDTO(mandorId, "mandor-a", "Mandor A", "MANDOR", "mandor@example.com"));
        when(repository.findBySupirId(supirId, null, null)).thenReturn(List.of(dto));

        List<PengirimanDTO> result = service.listDeliveriesBySupir(supirId, null, null);

        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(item -> {
                    assertThat(item.pengirimanId()).isEqualTo(dto.pengirimanId());
                    assertThat(item.supirName()).isEqualTo("Supir A");
                    assertThat(item.mandorName()).isEqualTo("Mandor A");
                });
        verify(repository).findBySupirId(supirId, null, null);
    }

    @Test
    void listDeliveriesBySupir_endDateBeforeStartDate_throwsBadRequestError() {
        UUID supirId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 1);

        assertThatThrownBy(() -> service.listDeliveriesBySupir(supirId, startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date cannot be before start date");

        verifyNoInteractions(repository);
    }

    @Test
    void listDeliveriesBySupir_withOnlyEndDate_isValid() {
        UUID supirId = UUID.randomUUID();
        LocalDate endDate = LocalDate.of(2026, 2, 10);
        when(repository.findBySupirId(supirId, null, endDate)).thenReturn(List.of());

        assertThat(service.listDeliveriesBySupir(supirId, null, endDate)).isEmpty();
    }

    @Test
    void listDeliveriesBySupir_withOnlyStartDateAndValidRange_areValid() {
        UUID supirId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);
        when(repository.findBySupirId(supirId, startDate, null)).thenReturn(List.of());
        when(repository.findBySupirId(supirId, startDate, endDate)).thenReturn(List.of());

        assertThat(service.listDeliveriesBySupir(supirId, startDate, null)).isEmpty();
        assertThat(service.listDeliveriesBySupir(supirId, startDate, endDate)).isEmpty();
    }

    @Test
    void getPengirimanById_notFound_throwsEntityNotFoundException() {
        UUID pengirimanId = UUID.randomUUID();
        when(repository.findById(pengirimanId)).thenReturn(null);

        assertThatThrownBy(() -> service.getPengirimanById(pengirimanId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Pengiriman not found");
    }

    @Test
    void listAssignedSupirForMandor_withoutKebunQueryDependency_throwsUnavailableException() {
        UUID mandorId = UUID.randomUUID();
        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.listAssignedSupirForMandor(mandorId, null))
                .isInstanceOf(KebunQueryDependencyUnavailableException.class)
                .hasMessageContaining("Kebun query dependency is unavailable");

        verifyNoInteractions(repository);
    }

    @Test
    void listAssignedSupirForMandor_filtersByRoleAndName() {
        UUID mandorId = UUID.randomUUID();
        UUID supirAId = UUID.randomUUID();
        UUID supirBId = UUID.randomUUID();

        UserDTO supirA = new UserDTO(supirAId, "ega", "Ega Jawa", "SUPIR", "ega@example.com");
        UserDTO supirB = new UserDTO(supirBId, "andi", "Andi Supir", "SUPIR", "andi@example.com");
        UserDTO buruh = new UserDTO(UUID.randomUUID(), "budi", "Budi Buruh", "BURUH", "budi@example.com");

        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.getSupirListByMandorId(mandorId))
                .thenReturn(List.of(supirA, supirB, buruh));

        List<AssignedSupirDTO> result = service.listAssignedSupirForMandor(mandorId, "ega");

        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.supirId()).isEqualTo(supirAId);
                    assertThat(dto.name()).isEqualTo("Ega Jawa");
                });

        verify(kebunQueryUseCase).getSupirListByMandorId(mandorId);
    }

    @Test
    void listAssignedSupirForMandor_nullSupirList_returnsEmptyList() {
        UUID mandorId = UUID.randomUUID();

        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.getSupirListByMandorId(mandorId)).thenReturn(null);

        List<AssignedSupirDTO> result = service.listAssignedSupirForMandor(mandorId, null);

        assertThat(result).isEmpty();
        verify(kebunQueryUseCase).getSupirListByMandorId(mandorId);
    }

    @Test
    void listAssignablePanenForMandor_returnsOnlyApprovedPanenThatAreNotAssigned() {
        UUID mandorId = UUID.randomUUID();
        UUID kebunId = UUID.randomUUID();
        UUID panenA = UUID.randomUUID();
        UUID panenB = UUID.randomUUID();

        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(
                new PanenDTO(panenA, UUID.randomUUID(), "Buruh A", kebunId, "Panen A", 180000, "APPROVED", null, List.of(), LocalDateTime.now()),
                new PanenDTO(panenB, UUID.randomUUID(), "Buruh B", kebunId, "Panen B", 120000, "APPROVED", null, List.of(), LocalDateTime.now())
        ));
        when(repository.findAssignedPanenIds(List.of(panenA, panenB))).thenReturn(List.of(panenB));

        List<AssignablePanenDTO> result = service.listAssignablePanenForMandor(mandorId);

        assertThat(result)
                .extracting(AssignablePanenDTO::panenId)
                .containsExactly(panenA);
    }

    @Test
    void listApprovedDeliveriesForAdmin_filtersByMandorNameAfterEnrichment() {
        UUID mandorA = UUID.randomUUID();
        UUID mandorB = UUID.randomUUID();

        PengirimanDTO deliveryA = new PengirimanDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                mandorA,
                null,
                "APPROVED_MANDOR",
                200000,
                0,
                null,
                List.of(UUID.randomUUID()),
                LocalDateTime.now()
        );
        PengirimanDTO deliveryB = new PengirimanDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                mandorB,
                null,
                "APPROVED_MANDOR",
                150000,
                0,
                null,
                List.of(UUID.randomUUID()),
                LocalDateTime.now()
        );

        when(repository.findApprovedByMandorForAdminPaginated(null, 0, 10))
                .thenReturn(new PengirimanPageDTO(List.of(deliveryA, deliveryB), 0, 10, 2, 1, false, false));
        when(userQueryUseCase.getUserById(deliveryA.supirId()))
                .thenReturn(new UserDTO(deliveryA.supirId(), "supir-a", "Supir A", "SUPIR", "supir-a@example.com"));
        when(userQueryUseCase.getUserById(deliveryB.supirId()))
                .thenReturn(new UserDTO(deliveryB.supirId(), "supir-b", "Supir B", "SUPIR", "supir-b@example.com"));
        when(userQueryUseCase.getUserById(mandorA))
                .thenReturn(new UserDTO(mandorA, "awan", "Awan Mandor", "MANDOR", "awan@example.com"));
        when(userQueryUseCase.getUserById(mandorB))
                .thenReturn(new UserDTO(mandorB, "budi", "Budi Mandor", "MANDOR", "budi@example.com"));

        PengirimanPageDTO result = service.listApprovedDeliveriesForAdmin("awan", null, 0, 10);

        assertThat(result.items())
                .hasSize(1)
                .first()
                .satisfies(dto -> assertThat(dto.mandorName()).isEqualTo("Awan Mandor"));
    }

    @Test
    void getPengirimanById_found_enrichesNames() {
        UUID pengirimanId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        PengirimanDTO dto = delivery(pengirimanId, supirId, mandorId, null, null);
        when(repository.findById(pengirimanId)).thenReturn(dto);
        when(userQueryUseCase.getUserById(supirId))
                .thenReturn(new UserDTO(supirId, "supir", "Supir Nama", "SUPIR", "s@example.com"));
        when(userQueryUseCase.getUserById(mandorId))
                .thenReturn(new UserDTO(mandorId, "mandor", "Mandor Nama", "MANDOR", "m@example.com"));

        PengirimanDTO result = service.getPengirimanById(pengirimanId);

        assertThat(result.supirName()).isEqualTo("Supir Nama");
        assertThat(result.mandorName()).isEqualTo("Mandor Nama");
    }

    @Test
    void listAssignedSupirForMandor_blankSearchAndNullNames_areHandled() {
        UUID mandorId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();
        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.getSupirListByMandorId(mandorId)).thenReturn(java.util.Arrays.asList(
                new UserDTO(supirId, "s", null, "SUPIR", "s@example.com"),
                null,
                new UserDTO(null, "missing-id", "No Id", "SUPIR", "x@example.com"),
                new UserDTO(UUID.randomUUID(), "not-supir", "Buruh", "BURUH", "b@example.com")
        ));

        List<AssignedSupirDTO> blankSearchResult = service.listAssignedSupirForMandor(mandorId, " ");
        List<AssignedSupirDTO> filteredResult = service.listAssignedSupirForMandor(mandorId, "foo");

        assertThat(blankSearchResult).hasSize(1);
        assertThat(filteredResult).isEmpty();
    }

    @Test
    void listAssignedSupirForMandor_nullSearchAndBlankSupirName_areHandled() {
        UUID mandorId = UUID.randomUUID();
        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.getSupirListByMandorId(mandorId)).thenReturn(List.of(
                new UserDTO(UUID.randomUUID(), "blank", " ", "SUPIR", "blank@example.com")
        ));

        assertThat(service.listAssignedSupirForMandor(mandorId, null)).hasSize(1);
        assertThat(service.listAssignedSupirForMandor(mandorId, "x")).isEmpty();
    }

    @Test
    void listAssignablePanenForMandor_withoutKebunQueryDependency_throwsUnavailableException() {
        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.listAssignablePanenForMandor(UUID.randomUUID()))
                .isInstanceOf(KebunQueryDependencyUnavailableException.class)
                .hasMessageContaining("Kebun query dependency is unavailable");
    }

    @Test
    void listAssignablePanenForMandor_withoutKebun_returnsEmptyList() {
        UUID mandorId = UUID.randomUUID();
        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(null);

        assertThat(service.listAssignablePanenForMandor(mandorId)).isEmpty();
    }

    @Test
    void listAssignablePanenForMandor_withoutApprovedPanen_returnsEmptyList() {
        UUID mandorId = UUID.randomUUID();
        UUID kebunId = UUID.randomUUID();
        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(null);

        assertThat(service.listAssignablePanenForMandor(mandorId)).isEmpty();
    }

    @Test
    void listAssignablePanenForMandor_withEmptyApprovedPanen_returnsEmptyList() {
        UUID mandorId = UUID.randomUUID();
        UUID kebunId = UUID.randomUUID();
        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of());

        assertThat(service.listAssignablePanenForMandor(mandorId)).isEmpty();
    }

    @Test
    void listAssignablePanenForMandor_ignoresNullPanenIds() {
        UUID mandorId = UUID.randomUUID();
        UUID kebunId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(
                new PanenDTO(null, UUID.randomUUID(), "No Id", kebunId, "Invalid", 1, "APPROVED", null, List.of(), LocalDateTime.now()),
                new PanenDTO(panenId, UUID.randomUUID(), "Buruh", kebunId, "Valid", 1, "APPROVED", null, List.of(), LocalDateTime.now())
        ));
        when(repository.findAssignedPanenIds(List.of(panenId))).thenReturn(List.of());

        assertThat(service.listAssignablePanenForMandor(mandorId))
                .extracting(AssignablePanenDTO::panenId)
                .containsExactly(panenId);
    }

    @Test
    void recommendAssignmentForMandor_usesKnapsackToMaximizeWeightUnderDefaultCapacity() {
        UUID mandorId = UUID.randomUUID();
        UUID kebunId = UUID.randomUUID();
        UUID panenA = UUID.randomUUID();
        UUID panenB = UUID.randomUUID();
        UUID panenC = UUID.randomUUID();
        UUID panenD = UUID.randomUUID();

        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(
                panen(panenA, kebunId, "A", 240000),
                panen(panenB, kebunId, "B", 210000),
                panen(panenC, kebunId, "C", 190000),
                panen(panenD, kebunId, "D", 160000)
        ));
        when(repository.findAssignedPanenIds(List.of(panenA, panenB, panenC, panenD))).thenReturn(List.of());

        AssignmentRecommendationDTO result = service.recommendAssignmentForMandor(mandorId, null);

        assertThat(result.maxCapacity()).isEqualTo(400000);
        assertThat(result.totalWeight()).isEqualTo(400000);
        assertThat(result.remainingCapacity()).isZero();
        assertThat(result.panenIds()).containsExactly(panenB, panenC);
        assertThat(result.panenItems()).extracting(AssignablePanenDTO::description).containsExactly("B", "C");
    }

    @Test
    void recommendAssignmentForMandor_withCustomCapacity_skipsInvalidAndOverweightPanen() {
        UUID mandorId = UUID.randomUUID();
        UUID kebunId = UUID.randomUUID();
        UUID validA = UUID.randomUUID();
        UUID validB = UUID.randomUUID();
        UUID overweight = UUID.randomUUID();
        UUID zero = UUID.randomUUID();

        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(
                panen(validA, kebunId, "Valid A", 120000),
                panen(validB, kebunId, "Valid B", 80000),
                panen(overweight, kebunId, "Too heavy", 250000),
                panen(zero, kebunId, "Zero", 0)
        ));
        when(repository.findAssignedPanenIds(List.of(validA, validB, overweight, zero))).thenReturn(List.of());

        AssignmentRecommendationDTO result = service.recommendAssignmentForMandor(mandorId, 200000);

        assertThat(result.totalWeight()).isEqualTo(200000);
        assertThat(result.remainingCapacity()).isZero();
        assertThat(result.panenIds()).containsExactly(validA, validB);
    }

    @Test
    void recommendAssignmentForMandor_whenNoCandidateFits_returnsEmptyRecommendation() {
        UUID mandorId = UUID.randomUUID();
        UUID kebunId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();

        when(kebunQueryUseCaseProvider.getIfAvailable()).thenReturn(kebunQueryUseCase);
        when(kebunQueryUseCase.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(panenQueryUseCase.getApprovedPanenByKebun(kebunId)).thenReturn(List.of(
                panen(panenId, kebunId, "Too heavy", 500000)
        ));
        when(repository.findAssignedPanenIds(List.of(panenId))).thenReturn(List.of());

        AssignmentRecommendationDTO result = service.recommendAssignmentForMandor(mandorId, 400000);

        assertThat(result.panenIds()).isEmpty();
        assertThat(result.panenItems()).isEmpty();
        assertThat(result.totalWeight()).isZero();
        assertThat(result.remainingCapacity()).isEqualTo(400000);
    }

    @Test
    void recommendAssignmentForMandor_withInvalidCapacity_throwsBadRequestError() {
        assertThatThrownBy(() -> service.recommendAssignmentForMandor(UUID.randomUUID(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Max capacity must be greater than 0");

        verifyNoInteractions(kebunQueryUseCaseProvider, repository, panenQueryUseCase);
    }

    @Test
    void listActiveDeliveriesByMandor_enrichesFallbackNames() {
        UUID mandorId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();
        PengirimanDTO dto = delivery(UUID.randomUUID(), supirId, mandorId, "Existing Supir", "Existing Mandor");
        when(repository.findActiveByMandorId(mandorId)).thenReturn(List.of(dto));

        List<PengirimanDTO> result = service.listActiveDeliveriesByMandor(mandorId);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.supirName()).isEqualTo("Existing Supir");
            assertThat(item.mandorName()).isEqualTo("Existing Mandor");
        });
        verifyNoInteractions(userQueryUseCase);
    }

    @Test
    void listDeliveriesOfSupirByMandor_withUserLookupFailure_keepsFallbackNull() {
        UUID mandorId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();
        PengirimanDTO dto = delivery(UUID.randomUUID(), supirId, mandorId, null, null);
        when(repository.findByMandorIdAndSupirId(mandorId, supirId)).thenReturn(List.of(dto));
        when(userQueryUseCase.getUserById(supirId)).thenThrow(new RuntimeException("auth down"));
        when(userQueryUseCase.getUserById(mandorId)).thenThrow(new RuntimeException("auth down"));

        List<PengirimanDTO> result = service.listDeliveriesOfSupirByMandor(mandorId, supirId);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.supirName()).isNull();
            assertThat(item.mandorName()).isNull();
        });
    }

    @Test
    void listApprovedDeliveriesForAdmin_blankFilter_keepsAllAndHandlesNullUserId() {
        PengirimanDTO dto = new PengirimanDTO(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                "APPROVED_MANDOR",
                1,
                0,
                null,
                List.of(),
                LocalDateTime.now()
        );
        when(repository.findApprovedByMandorForAdminPaginated(LocalDate.of(2026, 4, 1), 0, 10))
                .thenReturn(new PengirimanPageDTO(List.of(dto), 0, 10, 1, 1, false, false));

        PengirimanPageDTO result = service.listApprovedDeliveriesForAdmin(" ", LocalDate.of(2026, 4, 1), 0, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().mandorName()).isNull();
    }

    @Test
    void listApprovedDeliveriesForAdmin_nullFilter_keepsAll() {
        PengirimanDTO dto = delivery(UUID.randomUUID(), null, null, null, null);
        when(repository.findApprovedByMandorForAdminPaginated(null, 0, 10))
                .thenReturn(new PengirimanPageDTO(List.of(dto), 0, 10, 1, 1, false, false));

        assertThat(service.listApprovedDeliveriesForAdmin(null, null, 0, 10).items()).hasSize(1);
    }

    @Test
    void listApprovedDeliveriesForAdmin_nonMatchingNullMandorName_returnsEmptyList() {
        PengirimanDTO dto = delivery(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Supir", null);
        when(repository.findApprovedByMandorForAdminPaginated(null, 0, 10))
                .thenReturn(new PengirimanPageDTO(List.of(dto), 0, 10, 1, 1, false, false));

        assertThat(service.listApprovedDeliveriesForAdmin("awan", null, 0, 10).items()).isEmpty();
    }

    @Test
    void listApprovedDeliveriesForAdmin_blankExistingNameAndBlankFallback_areHandled() {
        UUID mandorId = UUID.randomUUID();
        PengirimanDTO dto = delivery(UUID.randomUUID(), UUID.randomUUID(), mandorId, " ", " ");
        when(repository.findApprovedByMandorForAdminPaginated(null, 0, 10))
                .thenReturn(new PengirimanPageDTO(List.of(dto), 0, 10, 1, 1, false, false));
        when(userQueryUseCase.getUserById(dto.supirId()))
                .thenReturn(new UserDTO(dto.supirId(), "supir", "Supir", "SUPIR", "s@example.com"));
        when(userQueryUseCase.getUserById(mandorId))
                .thenThrow(new RuntimeException("not found"));

        assertThat(service.listApprovedDeliveriesForAdmin("awan", null, 0, 10).items()).isEmpty();
    }

    private PengirimanDTO delivery(UUID pengirimanId, UUID supirId, UUID mandorId, String supirName, String mandorName) {
        return new PengirimanDTO(
                pengirimanId,
                supirId,
                supirName,
                mandorId,
                mandorName,
                "ASSIGNED",
                100000,
                0,
                null,
                List.of(UUID.randomUUID()),
                LocalDateTime.now()
        );
    }

    private PanenDTO panen(UUID panenId, UUID kebunId, String description, int weight) {
        return new PanenDTO(
                panenId,
                UUID.randomUUID(),
                "Buruh " + description,
                kebunId,
                description,
                weight,
                "APPROVED",
                null,
                List.of(),
                LocalDateTime.now()
        );
    }
}
