package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out.PengirimanRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PengirimanQueryUseCaseImpl implements PengirimanQueryUseCase {

    private final PengirimanRepositoryPort repository;

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
}
