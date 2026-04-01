package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenRepositoryPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PanenQueryImpl implements PanenQueryUseCase {
    private final PanenRepositoryPort repositoryPort;

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
        return repositoryPort.findByMandorId(mandorId, buruhName, date);
    }
}
