package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignedSupirDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignablePanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.exception.KebunQueryDependencyUnavailableException;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out.PengirimanRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PengirimanQueryUseCaseImpl implements PengirimanQueryUseCase {

    private static final String SUPIR_ROLE = "SUPIR";
    private static final String KEBUN_QUERY_UNAVAILABLE_MESSAGE =
            "Kebun query dependency is unavailable. Integrasi modul kebun belum siap.";

    private final PengirimanRepositoryPort repository;
    private final ObjectProvider<KebunQueryUseCase> kebunQueryUseCaseProvider;
    private final PanenQueryUseCase panenQueryUseCase;

    @Override
    public PengirimanDTO getPengirimanById(UUID pengirimanId) {
        PengirimanDTO result = repository.findById(pengirimanId);
        if (result == null) {
            throw new EntityNotFoundException("Pengiriman not found: " + pengirimanId);
        }
        return result;
    }

    @Override
    public List<PengirimanDTO> listDeliveriesBySupir(UUID supirId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return repository.findBySupirId(supirId, startDate, endDate);
    }

    @Override
    public List<AssignedSupirDTO> listAssignedSupirForMandor(UUID mandorId, String searchNama) {
        KebunQueryUseCase kebunQueryUseCase = kebunQueryUseCaseProvider.getIfAvailable();
        if (kebunQueryUseCase == null) {
            throw new KebunQueryDependencyUnavailableException(KEBUN_QUERY_UNAVAILABLE_MESSAGE);
        }

        List<UserDTO> supirList = kebunQueryUseCase.getSupirListByMandorId(mandorId);
        if (supirList == null) {
            return List.of();
        }

        return supirList.stream()
                .filter(this::isValidSupir)
                .filter(user -> matchesSearchNama(user.name(), searchNama))
                .map(user -> new AssignedSupirDTO(user.userId(), user.username(), user.name(), user.email()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssignablePanenDTO> listAssignablePanenForMandor(UUID mandorId) {
        KebunQueryUseCase kebunQueryUseCase = requireKebunQueryUseCase();
        UUID kebunId = kebunQueryUseCase.findKebunIdByMandorId(mandorId);
        if (kebunId == null) {
            return List.of();
        }

        List<PanenDTO> approvedPanen = panenQueryUseCase.getApprovedPanenByKebun(kebunId);
        if (approvedPanen == null || approvedPanen.isEmpty()) {
            return List.of();
        }

        List<UUID> panenIds = approvedPanen.stream()
                .map(PanenDTO::panenId)
                .filter(Objects::nonNull)
                .toList();
        List<UUID> assignedPanenIds = repository.findAssignedPanenIds(panenIds);

        return approvedPanen.stream()
                .filter(panen -> panen.panenId() != null)
                .filter(panen -> !assignedPanenIds.contains(panen.panenId()))
                .map(panen -> new AssignablePanenDTO(
                        panen.panenId(),
                        panen.buruhId(),
                        panen.buruhName(),
                        panen.description(),
                        panen.weight(),
                        panen.timestamp()
                ))
                .toList();
    }

    @Override
    public List<PengirimanDTO> listActiveDeliveriesByMandor(UUID mandorId) {
        return repository.findActiveByMandorId(mandorId);
    }

    @Override
    public List<PengirimanDTO> listDeliveriesOfSupirByMandor(UUID mandorId, UUID supirId) {
        return repository.findByMandorIdAndSupirId(mandorId, supirId);
    }

    @Override
    public List<PengirimanDTO> listApprovedDeliveriesForAdmin(String mandorName, LocalDate date) {
        return repository.findApprovedByMandorForAdmin(mandorName, date);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    private boolean isValidSupir(UserDTO user) {
        return user != null
                && user.userId() != null
                && SUPIR_ROLE.equalsIgnoreCase(Objects.toString(user.role(), ""));
    }

    private boolean matchesSearchNama(String supirName, String searchNama) {
        if (searchNama == null || searchNama.isBlank()) {
            return true;
        }
        if (supirName == null || supirName.isBlank()) {
            return false;
        }
        return supirName.toLowerCase(Locale.ROOT)
                .contains(searchNama.trim().toLowerCase(Locale.ROOT));
    }

    private KebunQueryUseCase requireKebunQueryUseCase() {
        KebunQueryUseCase kebunQueryUseCase = kebunQueryUseCaseProvider.getIfAvailable();
        if (kebunQueryUseCase == null) {
            throw new KebunQueryDependencyUnavailableException(KEBUN_QUERY_UNAVAILABLE_MESSAGE);
        }
        return kebunQueryUseCase;
    }
}
