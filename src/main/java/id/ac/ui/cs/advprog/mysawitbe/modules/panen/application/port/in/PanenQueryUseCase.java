package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;

public interface PanenQueryUseCase {

    PanenDTO getPanenById(UUID panenId);

    List<PanenDTO> getApprovedPanenByKebun(UUID kebunId);

    List<PanenDTO> listPanenByBuruh(UUID buruhId, LocalDate startDate, LocalDate endDate, String status);

    List<PanenDTO> listPanenByMandor(UUID mandorId, String buruhName, LocalDate date);

    List<PanenDTO> listPanenByBuruhWithAuth(
            UUID buruhId, 
            UUID requesterId, 
            LocalDate startDate, 
            LocalDate endDate, 
            String status) throws IllegalAccessException;

    boolean hasPanenToday(UUID buruhId, LocalDate date);

    List<PanenDTO> listPanenForAdmin(String buruhName, LocalDate startDate, LocalDate endDate, String status);
}
