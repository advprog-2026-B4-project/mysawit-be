package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenRepositoryPort;
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
    public List<PanenDTO> getApprovedPanenByKebun(UUID kebunId) {
        return repositoryPort.findByKebunIdAndStatus(kebunId, "APPROVED");
    }

    @Override
    public List<PanenDTO> listPanenByBuruh(UUID buruhId, LocalDate startDate, LocalDate endDate, String status) {
        return repositoryPort.findByBuruhId(buruhId, startDate, endDate, status);
    }

    @Override 
    public List<PanenDTO> listPanenByMandor(UUID mandorId, String buruhName, LocalDate date) {
        // 1. Tanya modul Kebun: "Mandor ini pegang kebun mana?"
        UUID kebunId = kebunQueryUseCase.findKebunIdByMandorId(mandorId);
        if (kebunId == null) {
            return List.of(); // Jika tidak punya kebun, berarti tidak ada panen
        }

        // 2. Ambil data panen murni dari Database berdasarkan Kebun & Tanggal
        List<PanenDTO> panenList = repositoryPort.findByKebunIdAndDate(kebunId, date);

        // 3. Mapping nama buruh dari modul Auth dan Filter berdasarkan pencarian nama
        return panenList.stream()
                .map(panen -> {
                    // Minta nama buruh yang asli ke modul Auth
                    String namaAsli = userQueryUseCase.getUserById(panen.buruhId()).name();
                    
                    // Re-create DTO karena record di Java bersifat Immutable (SOLID: Functional behavior)
                    return new PanenDTO(
                            panen.panenId(), panen.buruhId(), namaAsli,
                            panen.kebunId(), panen.description(), panen.weight(),
                            panen.status(), panen.rejectionReason(), panen.photos(),
                            panen.timestamp()
                    );
                })
                .filter(panen -> {
                    // Filter berdasarkan substring nama (jika ada input searchNama)
                    if (buruhName == null || buruhName.isBlank()) return true;
                    return panen.buruhName().toLowerCase().contains(buruhName.trim().toLowerCase());
                })
                .toList();
    }

}
