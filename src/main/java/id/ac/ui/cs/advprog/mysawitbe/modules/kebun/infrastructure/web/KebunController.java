package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.AssignPersonRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CreateKebunRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.EditKebunRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("${api.kebun.path:/api/kebun}")
public class KebunController {

    private final KebunCommandUseCase kebunCommandUseCase;
    private final KebunQueryUseCase kebunQueryUseCase;

    @Value("${app.name:MySawit}")
    private String appName;

    public KebunController(KebunCommandUseCase kebunCommandUseCase,
                           KebunQueryUseCase kebunQueryUseCase) {
        this.kebunCommandUseCase = kebunCommandUseCase;
        this.kebunQueryUseCase = kebunQueryUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<List<KebunDTO>>> listKebun(
            @RequestParam(required = false, name = "nama") String searchNama,
            @RequestParam(required = false, name = "kode") String searchKode
    ) {
        log.info("Fetching kebun list with filters - nama: {}, kode: {}", searchNama, searchKode);
        List<KebunDTO> result = kebunQueryUseCase.listKebun(searchNama, searchKode);
        log.debug("Found {} kebun(s)", result.size());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{kebunId}")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<KebunDTO>> getKebun(@PathVariable UUID kebunId) {
        log.info("Fetching kebun details for ID: {}", kebunId);
        KebunDTO result = kebunQueryUseCase.getKebunById(kebunId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<KebunDTO>> create(@Valid @RequestBody CreateKebunRequestDTO body) {
        log.info("Creating new kebun with nama: {}, kode: {}", body.nama(), body.kode());
        KebunDTO created = kebunCommandUseCase.createKebun(
                body.nama(),
                body.kode(),
                body.luas(),
                body.coordinates()
        );
        log.info("Successfully created kebun with ID: {}", created.kebunId());
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/{kebunId}")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<KebunDTO>> edit(
            @PathVariable UUID kebunId,
            @Valid @RequestBody EditKebunRequestDTO body) {
        log.info("Editing kebun ID: {} with nama: {}", kebunId, body.nama());
        KebunDTO updated = kebunCommandUseCase.editKebun(
                kebunId,
                body.nama(),
                body.luas(),
                body.coordinates()
        );
        log.info("Successfully updated kebun ID: {}", kebunId);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{kebunId}")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID kebunId) {
        log.warn("Deleting kebun ID: {}", kebunId);
        kebunCommandUseCase.deleteKebun(kebunId);
        log.info("Successfully deleted kebun ID: {}", kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{kebunId}/mandor")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<MandorResponse>> getMandor(@PathVariable UUID kebunId) {
        log.info("Fetching mandor for kebun ID: {}", kebunId);
        UUID mandorId = kebunQueryUseCase.getMandorIdByKebun(kebunId);
        return ResponseEntity.ok(ApiResponse.success(new MandorResponse(mandorId)));
    }

    @GetMapping("/{kebunId}/supir")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getSupirList(
            @PathVariable UUID kebunId,
            @RequestParam(required = false, name = "nama") String searchNama
    ) {
        log.info("Fetching supir list for kebun ID: {}, search: {}", kebunId, searchNama);
        List<UserDTO> supirList = kebunQueryUseCase.getSupirList(kebunId);

        if (searchNama != null && !searchNama.isBlank()) {
            supirList = supirList.stream()
                    .filter(u -> u.name().toLowerCase().contains(searchNama.toLowerCase()))
                    .collect(Collectors.toList());
        }

        log.debug("Found {} supir(s)", supirList.size());
        return ResponseEntity.ok(ApiResponse.success(supirList));
    }

    @GetMapping("/{kebunId}/buruh")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getBuruhList(
            @PathVariable UUID kebunId,
            @RequestParam(required = false, name = "nama") String searchNama
    ) {
        log.info("Fetching buruh list for kebun ID: {}, search: {}", kebunId, searchNama);
        List<UserDTO> buruhList = kebunQueryUseCase.getBuruhList(kebunId);

        if (searchNama != null && !searchNama.isBlank()) {
            buruhList = buruhList.stream()
                    .filter(u -> u.name().toLowerCase().contains(searchNama.toLowerCase()))
                    .collect(Collectors.toList());
        }

        log.debug("Found {} buruh(s)", buruhList.size());
        return ResponseEntity.ok(ApiResponse.success(buruhList));
    }

    @PostMapping("/{kebunId}/assign/mandor")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<Void>> assignMandor(
            @PathVariable UUID kebunId,
            @Valid @RequestBody AssignPersonRequestDTO body) {
        log.info("Assigning mandor ID: {} to kebun ID: {}", body.personId(), kebunId);
        kebunCommandUseCase.assignMandorToKebun(body.personId(), kebunId);
        log.info("Successfully assigned mandor to kebun ID: {}", kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{kebunId}/move/mandor")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<Void>> moveMandor(
            @PathVariable UUID kebunId,
            @Valid @RequestBody AssignPersonRequestDTO body) {
        log.info("Moving mandor ID: {} to kebun ID: {}", body.personId(), kebunId);
        kebunCommandUseCase.moveMandorToKebun(body.personId(), kebunId);
        log.info("Successfully moved mandor to kebun ID: {}", kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{kebunId}/assign/supir")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<Void>> assignSupir(
            @PathVariable UUID kebunId,
            @Valid @RequestBody AssignPersonRequestDTO body) {
        log.info("Assigning supir ID: {} to kebun ID: {}", body.personId(), kebunId);
        kebunCommandUseCase.assignSupirToKebun(body.personId(), kebunId);
        log.info("Successfully assigned supir to kebun ID: {}", kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{kebunId}/move/supir")
    @PreAuthorize("hasRole('ADMIN_UTAMA')")
    public ResponseEntity<ApiResponse<Void>> moveSupir(
            @PathVariable UUID kebunId,
            @Valid @RequestBody AssignPersonRequestDTO body) {
        log.info("Moving supir ID: {} to kebun ID: {}", body.personId(), kebunId);
        kebunCommandUseCase.moveSupirToKebun(body.personId(), kebunId);
        log.info("Successfully moved supir to kebun ID: {}", kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private record MandorResponse(UUID mandorId) {}
}
