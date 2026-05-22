package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PanenQueryImpl implements PanenQueryUseCase {
    private final PanenRepositoryPort repositoryPort;
    private final UserQueryUseCase userQueryUseCase;
    private final KebunQueryUseCase kebunQueryUseCase;

    @Override
    public PanenDTO getPanenById(UUID panenId) {
        PanenDTO panen = repositoryPort.findById(panenId);
        if (panen == null) {
            throw new IllegalArgumentException("Laporan panen tidak ditemukan.");
        }
        return panen;
    }

    @Override
    public Map<UUID, PanenDTO> getPanenByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return repositoryPort.findAllByIds(ids).stream()
                .collect(Collectors.toMap(PanenDTO::panenId, p -> p));
    }

    @Override
    public List<PanenDTO> getApprovedPanenByKebun(UUID kebunId) {
        return repositoryPort.findByKebunIdAndStatus(kebunId, "APPROVED");
    }

    @Override
    public List<PanenDTO> listPanenByBuruh(UUID buruhId, LocalDate startDate, LocalDate endDate, String status) {
        String buruhName = userQueryUseCase.getUserById(buruhId).name();
        return repositoryPort.findByBuruhId(buruhId, startDate, endDate, status)
                .stream()
                .map(panen -> new PanenDTO(
                        panen.panenId(), panen.buruhId(), buruhName,
                        panen.kebunId(), panen.description(), panen.weight(),
                        panen.status(), panen.rejectionReason(), panen.photos(),
                        panen.timestamp()
                ))
                .toList();
    }

    @Override
    public List<PanenDTO> listPanenByMandor(UUID mandorId, String buruhName, LocalDate date) {
        UUID kebunId = kebunQueryUseCase.findKebunIdByMandorId(mandorId);
        if (kebunId == null) {
            return List.of();
        }

        List<PanenDTO> panenList = repositoryPort.findByKebunIdAndDate(kebunId, date);

        return panenList.stream()
                .map(panen -> {
                    String namaAsli = userQueryUseCase.getUserById(panen.buruhId()).name();
                    return new PanenDTO(
                            panen.panenId(), panen.buruhId(), namaAsli,
                            panen.kebunId(), panen.description(), panen.weight(),
                            panen.status(), panen.rejectionReason(), panen.photos(),
                            panen.timestamp()
                    );
                })
                .filter(panen -> {
                    if (buruhName == null || buruhName.isBlank()) return true;
                    return panen.buruhName().toLowerCase().contains(buruhName.trim().toLowerCase());
                })
                .toList();
    }

    @Override
    public boolean hasPanenToday(UUID buruhId, LocalDate date) {
        if (buruhId == null) throw new IllegalArgumentException("buruhId wajib diisi");
        if (date == null) throw new IllegalArgumentException("date wajib diisi");

        UUID mandorId = userQueryUseCase.getMandorIdByBuruhId(buruhId);
        if (mandorId == null) {
            throw new IllegalStateException("Buruh belum memiliki Mandor!");
        }

        UUID kebunId = kebunQueryUseCase.findKebunIdByMandorId(mandorId);
        if (kebunId == null) {
            throw new IllegalStateException("Mandor belum di-assign ke kebun manapun!");
        }

        return repositoryPort.existsByBuruhIdAndDate(buruhId, date);
    }

    @Override
    public List<PanenDTO> listPanenByBuruhWithAuth(
            UUID buruhId,
            UUID requesterId,
            LocalDate startDate,
            LocalDate endDate,
            String status) throws IllegalAccessException {

        UserDTO buruh = userQueryUseCase.getUserById(buruhId);
        if (buruh == null) {
            throw new EntityNotFoundException(
                    "Buruh dengan ID " + buruhId + " tidak ditemukan.");
        }
        boolean isOwnData = requesterId.equals(buruhId);

        boolean isMandorSupervise = false;
        try {
            UserDTO requester = userQueryUseCase.getUserById(requesterId);
            if ("MANDOR".equals(requester.role())) {
                List<UserDTO> buruhByMandor = userQueryUseCase.getBuruhByMandorId(requesterId);
                isMandorSupervise = buruhByMandor.stream()
                        .anyMatch(b -> b.userId().equals(buruhId));
            } else if ("ADMIN".equals(requester.role())) {
                isMandorSupervise = true;
            }
        } catch (Exception e) {
            isMandorSupervise = false;
        }

        if (!isOwnData && !isMandorSupervise) {
            throw new IllegalAccessException(
                    "Anda tidak memiliki akses untuk melihat data panen buruh ID " + buruhId);
        }

        return listPanenByBuruh(buruhId, startDate, endDate, status);
    }

    @Override
    public PanenPageDTO listPanenForAdmin(String buruhName, LocalDate startDate, LocalDate endDate, String status, int page, int size) {
        PanenPageDTO panenPage = repositoryPort.findAllWithFiltersPaginated(status, startDate, endDate, page, size);

        if (panenPage.items().isEmpty()) {
            return panenPage;
        }
        Set<UUID> uniqueBuruhIds = panenPage.items().stream()
            .map(PanenDTO::buruhId)
            .collect(Collectors.toSet());

        Map<UUID, String> buruhNameMap = new HashMap<>();
        for (UUID id : uniqueBuruhIds) {
            String nama = userQueryUseCase.getUserById(id).name();
            buruhNameMap.put(id, nama);
        }

        List<PanenDTO> enriched = panenPage.items().stream()
            .map(panen -> {
                String namaAsli = buruhNameMap.get(panen.buruhId());

                return new PanenDTO(
                    panen.panenId(), panen.buruhId(), namaAsli,
                    panen.kebunId(), panen.description(), panen.weight(),
                    panen.status(), panen.rejectionReason(),
                    panen.photos(),
                    panen.timestamp()
                );
            })
            .filter(panen -> {
                if (buruhName == null || buruhName.isBlank()) return true;
                return panen.buruhName().toLowerCase().contains(buruhName.trim().toLowerCase());
            })
            .toList();

        return new PanenPageDTO(enriched, panenPage.page(), panenPage.size(),
            panenPage.totalElements(), panenPage.totalPages(),
            panenPage.hasNext(), panenPage.hasPrevious());
    }
}
