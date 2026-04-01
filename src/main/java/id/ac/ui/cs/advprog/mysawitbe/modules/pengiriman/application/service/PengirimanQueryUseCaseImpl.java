package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignedSupirDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.exception.KebunQueryDependencyUnavailableException;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out.PengirimanRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PengirimanQueryUseCaseImpl implements PengirimanQueryUseCase {

    private static final String SUPIR_ROLE = "SUPIR";
    private static final String KEBUN_QUERY_UNAVAILABLE_MESSAGE =
            "Kebun query dependency is unavailable. Integrasi modul kebun belum siap.";

    private final PengirimanRepositoryPort repository;
    private final ObjectProvider<KebunQueryUseCase> kebunQueryUseCaseProvider;

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

        Map<UUID, AssignedSupirDTO> uniqueSupir = new LinkedHashMap<>();
        List<KebunDTO> kebunList = kebunQueryUseCase.listKebun(null, null);
        if (kebunList == null) {
            return List.of();
        }

        for (KebunDTO kebun : kebunList) {
            if (kebun == null || kebun.kebunId() == null) {
                continue;
            }

            UUID assignedMandorId = kebunQueryUseCase.getMandorIdByKebun(kebun.kebunId());
            if (!mandorId.equals(assignedMandorId)) {
                continue;
            }

            List<UserDTO> supirList = kebunQueryUseCase.getSupirList(kebun.kebunId());
            if (supirList == null) {
                continue;
            }

            for (UserDTO user : supirList) {
                if (!isValidSupir(user) || !matchesSearchNama(user.name(), searchNama)) {
                    continue;
                }
                uniqueSupir.putIfAbsent(
                        user.userId(),
                        new AssignedSupirDTO(user.userId(), user.username(), user.name(), user.email())
                );
            }
        }

        return List.copyOf(uniqueSupir.values());
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
}
