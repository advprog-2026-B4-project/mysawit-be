package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.event.MandorAssignedToKebunEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.out.KebunRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.domain.BoundingBox;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.domain.KebunGeometry;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class KebunUseCaseService implements KebunCommandUseCase, KebunQueryUseCase {

    private final KebunRepositoryPort kebunRepository;
    private final UserQueryUseCase userQueryUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public KebunUseCaseService(KebunRepositoryPort kebunRepository,
                               UserQueryUseCase userQueryUseCase,
                               ApplicationEventPublisher eventPublisher) {
        this.kebunRepository = kebunRepository;
        this.userQueryUseCase = userQueryUseCase;
        this.eventPublisher = eventPublisher;
    }

    // ---------------- Command Use Cases ----------------

    @Override
    public KebunDTO createKebun(String nama, String kode, int luas, List<CoordinateDTO> coordinates) {
        // Trim input
        nama = (nama != null) ? nama.trim() : null;
        kode = (kode != null) ? kode.trim() : null;

        validateBasic(nama, kode, luas, coordinates);
        KebunGeometry.validateSquareCorners(coordinates);
        validateNoOverlap(null, coordinates);

        // Check for duplicate kode
        List<KebunDTO> existing = kebunRepository.findByNamaContainingOrKodeContaining("", kode);
        if (!existing.isEmpty()) {
            throw new IllegalStateException("Kode kebun sudah digunakan");
        }

        KebunDTO dto = new KebunDTO(null, nama, kode, luas, coordinates);
        return kebunRepository.save(dto);
    }

    @Override
    public KebunDTO editKebun(UUID kebunId, String nama, int luas, List<CoordinateDTO> coordinates) {
        if (kebunId == null) throw new IllegalArgumentException("kebunId wajib diisi");

        KebunDTO existing = kebunRepository.findById(kebunId);
        if (existing == null) throw new EntityNotFoundException("Kebun not found: " + kebunId);

        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama kebun wajib diisi");
        if (luas <= 0) throw new IllegalArgumentException("Luas kebun harus > 0");
        KebunGeometry.validateSquareCorners(coordinates);
        validateNoOverlap(kebunId, coordinates);

        KebunDTO updated = new KebunDTO(existing.kebunId(), nama, existing.kode(), luas, coordinates);
        return kebunRepository.save(updated);
    }

    @Override
    public void deleteKebun(UUID kebunId) {
        if (kebunId == null) throw new IllegalArgumentException("kebunId wajib diisi");
        if (kebunRepository.findById(kebunId) == null) {
            throw new EntityNotFoundException("Kebun not found: " + kebunId);
        }
        if (kebunRepository.hasMandorAssigned(kebunId)) {
            throw new IllegalStateException("Tidak bisa menghapus kebun karena masih ada mandor yang terikat");
        }
        kebunRepository.deleteById(kebunId);
    }

    @Override
    public void assignMandorToKebun(UUID mandorId, UUID kebunId) {
        requireIds(mandorId, kebunId);
        ensureKebunExists(kebunId);

        // Validate user exists and has MANDOR role
        UserDTO user = userQueryUseCase.getUserById(mandorId);
        if (user == null) {
            throw new EntityNotFoundException("Mandor not found: " + mandorId);
        }
        if (!"MANDOR".equals(user.role())) {
            throw new IllegalArgumentException("User is not a MANDOR: " + mandorId);
        }

        // Check if mandor already assigned to another kebun
        UUID currentKebunId = kebunRepository.findKebunIdByMandorId(mandorId);
        if (currentKebunId != null) {
            throw new IllegalStateException("Mandor sudah terikat pada kebun lain: " + currentKebunId);
        }

        kebunRepository.assignMandor(mandorId, kebunId);
        eventPublisher.publishEvent(new MandorAssignedToKebunEvent(mandorId, kebunId));
    }

    @Override
    public void moveMandorToKebun(UUID mandorId, UUID newKebunId) {
        requireIds(mandorId, newKebunId);
        ensureKebunExists(newKebunId);

        // Validate user exists and has MANDOR role
        UserDTO user = userQueryUseCase.getUserById(mandorId);
        if (user == null) {
            throw new EntityNotFoundException("Mandor not found: " + mandorId);
        }
        if (!"MANDOR".equals(user.role())) {
            throw new IllegalArgumentException("User is not a MANDOR: " + mandorId);
        }

        // Ensure mandor is currently assigned
        UUID currentKebunId = kebunRepository.findKebunIdByMandorId(mandorId);
        if (currentKebunId == null) {
            throw new IllegalStateException("Mandor belum terikat ke kebun manapun");
        }

        kebunRepository.moveMandor(mandorId, newKebunId);
        eventPublisher.publishEvent(new MandorAssignedToKebunEvent(mandorId, newKebunId));
    }

    @Override
    public void assignSupirToKebun(UUID supirId, UUID kebunId) {
        requireIds(supirId, kebunId);
        ensureKebunExists(kebunId);

        // Validate user exists and has SUPIR role
        UserDTO user = userQueryUseCase.getUserById(supirId);
        if (user == null) {
            throw new EntityNotFoundException("Supir not found: " + supirId);
        }
        if (!"SUPIR".equals(user.role())) {
            throw new IllegalArgumentException("User is not a SUPIR: " + supirId);
        }

        kebunRepository.assignSupir(supirId, kebunId);
    }

    @Override
    public void moveSupirToKebun(UUID supirId, UUID newKebunId) {
        requireIds(supirId, newKebunId);
        ensureKebunExists(newKebunId);

        // Validate user exists and has SUPIR role
        UserDTO user = userQueryUseCase.getUserById(supirId);
        if (user == null) {
            throw new EntityNotFoundException("Supir not found: " + supirId);
        }
        if (!"SUPIR".equals(user.role())) {
            throw new IllegalArgumentException("User is not a SUPIR: " + supirId);
        }

        kebunRepository.moveSupir(supirId, newKebunId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getBuruhList(UUID kebunId) {
        if (kebunId == null) throw new IllegalArgumentException("kebunId wajib diisi");
        ensureKebunExists(kebunId);

        return kebunRepository.findBuruhIdsByKebunId(kebunId).stream()
                .map(userQueryUseCase::getUserById)
                .sorted(Comparator.comparing(UserDTO::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    // ---------------- Query Use Cases ----------------

    @Override
    @Transactional(readOnly = true)
    public KebunDTO getKebunById(UUID kebunId) {
        if (kebunId == null) throw new IllegalArgumentException("kebunId wajib diisi");
        KebunDTO kebun = kebunRepository.findById(kebunId);
        if (kebun == null) throw new EntityNotFoundException("Kebun not found: " + kebunId);
        return kebun;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getMandorIdByKebun(UUID kebunId) {
        if (kebunId == null) throw new IllegalArgumentException("kebunId wajib diisi");
        ensureKebunExists(kebunId);
        return kebunRepository.findMandorIdByKebunId(kebunId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getSupirList(UUID kebunId) {
        if (kebunId == null) throw new IllegalArgumentException("kebunId wajib diisi");
        ensureKebunExists(kebunId);

        return kebunRepository.findSupirIdsByKebunId(kebunId).stream()
                .map(userQueryUseCase::getUserById)
                .sorted(Comparator.comparing(UserDTO::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KebunDTO> listKebun(String searchNama, String searchKode) {
        String nama = (searchNama == null) ? "" : searchNama.trim();
        String kode = (searchKode == null) ? "" : searchKode.trim();

        if (nama.isBlank() && kode.isBlank()) {
            return kebunRepository.findAll();
        }
        return kebunRepository.findByNamaContainingOrKodeContaining(nama, kode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateCoordinates(int lat, int lng) {
        List<List<CoordinateDTO>> polygons = kebunRepository.findAllCoordinates();
        for (List<CoordinateDTO> coords : polygons) {
            BoundingBox b = KebunGeometry.toBoundingBox(coords);
            if (b.containsPoint(lat, lng)) return false;
        }
        return true;
    }

    // ---------------- Helpers ----------------

    private void validateBasic(String nama, String kode, int luas, List<CoordinateDTO> coordinates) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama kebun wajib diisi");
        if (kode == null || kode.isBlank()) throw new IllegalArgumentException("Kode kebun wajib diisi");
        if (luas <= 0) throw new IllegalArgumentException("Luas kebun harus > 0");
        if (coordinates == null) throw new IllegalArgumentException("Koordinat wajib diisi");
    }

    private void validateNoOverlap(UUID selfKebunId, List<CoordinateDTO> newCoords) {
        BoundingBox incoming = KebunGeometry.toBoundingBox(newCoords);

        for (KebunDTO existing : kebunRepository.findAll()) {
            if (selfKebunId != null && selfKebunId.equals(existing.kebunId())) continue;

            BoundingBox ex = KebunGeometry.toBoundingBox(existing.coordinates());
            if (incoming.overlaps(ex)) {
                throw new IllegalStateException("Koordinat kebun overlap dengan kebun lain: " + existing.kode());
            }
        }
    }

    private void ensureKebunExists(UUID kebunId) {
        if (kebunRepository.findById(kebunId) == null) {
            throw new EntityNotFoundException("Kebun not found: " + kebunId);
        }
    }

    private void requireIds(UUID a, UUID b) {
        if (a == null) throw new IllegalArgumentException("personId wajib diisi");
        if (b == null) throw new IllegalArgumentException("kebunId wajib diisi");
    }
}