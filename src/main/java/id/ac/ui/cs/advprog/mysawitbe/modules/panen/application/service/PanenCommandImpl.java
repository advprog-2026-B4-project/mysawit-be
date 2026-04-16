package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenRejectedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenMapperPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.Panen;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PanenCommandImpl implements PanenCommandUseCase {

    private final PanenRepositoryPort repositoryPort;
    private final PanenMapperPort mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserQueryUseCase userQueryUseCase;
    private final KebunQueryUseCase kebunQueryUseCase;

    @Override
    @Transactional
    public PanenDTO createPanen(UUID buruhId, String description, int weight, List<String> photoUrls) {
        LocalDateTime now = LocalDateTime.now();

        if (repositoryPort.existsByBuruhIdAndDate(buruhId, now.toLocalDate())) {
            throw new IllegalStateException("Pencatatan gagal: Batas harian tercapai (maksimal 1 kali sehari).");
        }

        UUID mandorId = userQueryUseCase.getMandorIdByBuruhId(buruhId);
        if (mandorId == null) {
            throw new IllegalStateException("Buruh belum memiliki Mandor!");
        }

        UUID kebunId = kebunQueryUseCase.findKebunIdByMandorId(mandorId);
        if (kebunId == null) {
            throw new IllegalStateException("Mandor belum di-assign ke kebun manapun!");
        }

        UserDTO buruh = userQueryUseCase.getUserById(buruhId);
        String buruhName = buruh.name();

        Panen domainPanen = Panen.catatBaru(
                buruhId, 
                buruhName, 
                kebunId, 
                description,
                weight,
                now,
                photoUrls
        );

        return repositoryPort.save(mapper.toDTO(domainPanen));
    }

    @Override
    @Transactional
    public PanenDTO approvePanen(UUID panenId, UUID mandorId) {
        PanenDTO dto = repositoryPort.findById(panenId);
        if (dto == null) {
            throw new EntityNotFoundException("Laporan panen tidak ditemukan"); 
        }

        Panen domainPanen = mapper.dtoToDomain(dto);
        domainPanen.approve();

        PanenDTO updatedDto = repositoryPort.save(mapper.toDTO(domainPanen));

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
    public PanenDTO rejectPanen(UUID panenId, UUID mandorId, String rejectionReason) {
        PanenDTO dto = repositoryPort.findById(panenId);
        if (dto == null) {
            throw new EntityNotFoundException("Laporan panen tidak ditemukan");  
        }

        Panen domainPanen = mapper.dtoToDomain(dto);
        
        domainPanen.reject(rejectionReason);
        PanenDTO updatedDto = repositoryPort.save(mapper.toDTO(domainPanen));

        eventPublisher.publishEvent(new PanenRejectedEvent(
                updatedDto.panenId(),
                updatedDto.buruhId(),
                rejectionReason
        ));

        return updatedDto;
    }
}