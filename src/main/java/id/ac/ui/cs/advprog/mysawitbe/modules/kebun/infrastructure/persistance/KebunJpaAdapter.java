package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.out.KebunRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional
public class KebunJpaAdapter implements KebunRepositoryPort {

    private final KebunJpaRepository kebunJpaRepository;
    private final KebunSupirJpaRepository kebunSupirJpaRepository;
    private final KebunJpaMapper kebunJpaMapper;

    public KebunJpaAdapter(KebunJpaRepository kebunJpaRepository,
                           KebunSupirJpaRepository kebunSupirJpaRepository,
                           KebunJpaMapper kebunJpaMapper) {
        this.kebunJpaRepository = kebunJpaRepository;
        this.kebunSupirJpaRepository = kebunSupirJpaRepository;
        this.kebunJpaMapper = kebunJpaMapper;
    }

    @Override
    public KebunDTO save(KebunDTO kebunDTO) {
        UUID id = kebunDTO.kebunId() != null ? kebunDTO.kebunId() : UUID.randomUUID();

        KebunJpaEntity existing = kebunJpaRepository.findById(id).orElse(null);
        if (existing == null) {
            KebunDTO withId = new KebunDTO(id, kebunDTO.nama(), kebunDTO.kode(), kebunDTO.luas(), kebunDTO.coordinates());
            KebunJpaEntity created = kebunJpaMapper.toEntity(withId);
            created.setMandorId(null);

            KebunJpaEntity saved = kebunJpaRepository.save(created);
            return kebunJpaMapper.toDto(saved);
        }

        // Update mutable fields only (kode immutable)
        existing.setNama(kebunDTO.nama());
        existing.setLuas(kebunDTO.luas());

        // Replace coordinate collection using MapStruct result
        KebunJpaEntity mapped = kebunJpaMapper.toEntity(kebunDTO);
        existing.setCoordinates(mapped.getCoordinates());

        KebunJpaEntity saved = kebunJpaRepository.save(existing);
        return kebunJpaMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public KebunDTO findById(UUID kebunId) {
        return kebunJpaRepository.findById(kebunId).map(kebunJpaMapper::toDto).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KebunDTO> findAll() {
        return kebunJpaRepository.findAll().stream().map(kebunJpaMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KebunDTO> findByNamaContainingOrKodeContaining(String nama, String kode) {
        String normalizedNama = nama == null ? "" : nama.trim();
        String normalizedKode = kode == null ? "" : kode.trim();

        List<KebunJpaEntity> entities;
        if (!normalizedNama.isBlank() && !normalizedKode.isBlank()) {
            entities = kebunJpaRepository
                    .findByNamaContainingIgnoreCaseAndKodeContainingIgnoreCase(normalizedNama, normalizedKode);
        } else if (!normalizedNama.isBlank()) {
            entities = kebunJpaRepository.findByNamaContainingIgnoreCase(normalizedNama);
        } else if (!normalizedKode.isBlank()) {
            entities = kebunJpaRepository.findByKodeContainingIgnoreCase(normalizedKode);
        } else {
            entities = kebunJpaRepository.findAll();
        }

        return entities
                .stream()
                .map(kebunJpaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UUID findMandorIdByKebunId(UUID kebunId) {
        KebunJpaEntity e = kebunJpaRepository.findById(kebunId)
                .orElseThrow(() -> new EntityNotFoundException("Kebun not found: " + kebunId));
        return e.getMandorId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findSupirIdsByKebunId(UUID kebunId) {
        return kebunSupirJpaRepository.findByKebunId(kebunId).stream()
                .map(KebunSupirJpaEntity::getSupirId)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID kebunId) {
        kebunJpaRepository.deleteById(kebunId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasMandorAssigned(UUID kebunId) {
        KebunJpaEntity e = kebunJpaRepository.findById(kebunId).orElse(null);
        return e != null && e.getMandorId() != null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<List<CoordinateDTO>> findAllCoordinates() {
        return kebunJpaRepository.findAll().stream()
                .map(kebunJpaMapper::toDto)
                .map(KebunDTO::coordinates)
                .collect(Collectors.toList());
    }

    @Override
    public void assignMandor(UUID mandorId, UUID kebunId) {
        KebunJpaEntity target = kebunJpaRepository.findById(kebunId)
                .orElseThrow(() -> new EntityNotFoundException("Kebun not found: " + kebunId));
        target.setMandorId(mandorId);
        kebunJpaRepository.save(target);
    }

    @Override
    public void assignSupir(UUID supirId, UUID kebunId) {
        if (kebunSupirJpaRepository.findBySupirId(supirId).isPresent()) {
            throw new IllegalStateException("Supir sudah terikat pada kebun lain");
        }
        kebunSupirJpaRepository.save(new KebunSupirJpaEntity(kebunId, supirId));
    }

    @Override
    public void moveSupir(UUID supirId, UUID newKebunId) {
        KebunSupirJpaEntity row = kebunSupirJpaRepository.findBySupirId(supirId)
                .orElseThrow(() -> new EntityNotFoundException("Supir belum terikat ke kebun manapun"));
        row.setKebunId(newKebunId);
        kebunSupirJpaRepository.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID findKebunIdByMandorId(UUID mandorId) {
        return kebunJpaRepository.findByMandorId(mandorId)
                .map(KebunJpaEntity::getKebunId)
                .orElse(null);
    }

    @Override
    public void moveMandor(UUID mandorId, UUID newKebunId) {
        // Detach from old kebun (if any)
        kebunJpaRepository.findByMandorId(mandorId).ifPresent(old -> {
            old.setMandorId(null);
            kebunJpaRepository.saveAndFlush(old);
        });

        KebunJpaEntity target = kebunJpaRepository.findById(newKebunId)
                .orElseThrow(() -> new EntityNotFoundException("Kebun not found: " + newKebunId));
        target.setMandorId(mandorId);
        kebunJpaRepository.save(target);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByKode(String kode) {
        return kebunJpaRepository.existsByKode(kode);
    }
}
