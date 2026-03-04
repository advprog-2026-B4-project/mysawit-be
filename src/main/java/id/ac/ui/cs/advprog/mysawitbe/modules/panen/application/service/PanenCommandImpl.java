package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenRejectedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenMapperPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.Panen;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PanenCommandImpl implements PanenCommandUseCase {

    private final PanenRepositoryPort repositoryPort;
    private final PanenMapperPort mapper; // MapStruct mapper dari infrastructure
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PanenDTO createPanen(UUID buruhId, UUID kebunId, int weight, List<String> photoUrls) {
        LocalDateTime now = LocalDateTime.now();

        if (repositoryPort.existsByBuruhIdAndDate(buruhId, now.toLocalDate())) {
            throw new IllegalStateException("Pencatatan gagal: Batas harian tercapai (maksimal 1 kali sehari).");
        }

        Panen domainPanen = Panen.catatBaru(
                buruhId,
                "Buruh Name", // TODO = pake dto nanti ya
                kebunId,
                weight,
                now,
                photoUrls
        );

        return repositoryPort.save(mapper.toDTO(domainPanen));
    }

    @Override
    @Transactional
    public PanenDTO approvePanen(UUID panenId, UUID mandorId) {
        // 1. Ambil data (Port Out)
        PanenDTO dto = repositoryPort.findById(panenId);
        if (dto == null) throw new IllegalArgumentException("Laporan panen tidak ditemukan");

        // 2. Ubah DTO ke Domain untuk menjalankan Logika Bisnis
        Panen domainPanen = mapper.dtoToDomain(dto);
        domainPanen.approve();

        // 3. Simpan Perubahan
        PanenDTO updatedDto = repositoryPort.save(mapper.toDTO(domainPanen));

        // 4. Publish Event (Asinkron - Sesuai agent.md)
        eventPublisher.publishEvent(new PanenApprovedEvent(
                updatedDto.panenId(),
                updatedDto.buruhId(),
                updatedDto.kebunId(),
                updatedDto.weight(),
                updatedDto.timestamp()
        ));

        return updatedDto;
    }

    @Override
    @Transactional
    public PanenDTO rejectPanen(UUID panenId, UUID mandorId, String reason) {
        PanenDTO dto = repositoryPort.findById(panenId);
        if (dto == null) throw new IllegalArgumentException("Laporan panen tidak ditemukan");

        Panen domainPanen = mapper.dtoToDomain(dto);
        
        // 2. Delegasi ke Domain: Reject dengan alasan (Metode Bisnis)
        domainPanen.reject(reason);

        PanenDTO updatedDto = repositoryPort.save(mapper.toDTO(domainPanen));

        // 3. Publish Event untuk Modul Notifikasi
        eventPublisher.publishEvent(new PanenRejectedEvent(
                updatedDto.panenId(),
                updatedDto.buruhId(),
                reason
        ));

        return updatedDto;
    }
}