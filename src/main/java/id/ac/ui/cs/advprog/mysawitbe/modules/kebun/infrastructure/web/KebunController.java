package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.AssignPersonRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CreateKebunRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.EditKebunRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/kebun")
public class KebunController {

    private final KebunCommandUseCase kebunCommandUseCase;
    private final KebunQueryUseCase kebunQueryUseCase;

    public KebunController(KebunCommandUseCase kebunCommandUseCase,
                           KebunQueryUseCase kebunQueryUseCase) {
        this.kebunCommandUseCase = kebunCommandUseCase;
        this.kebunQueryUseCase = kebunQueryUseCase;
    }

    /** GET /api/kebun?nama=...&kode=... */
    @GetMapping
    public ResponseEntity<ApiResponse<List<KebunDTO>>> listKebun(
            @RequestParam(required = false, name = "nama") String searchNama,
            @RequestParam(required = false, name = "kode") String searchKode
    ) {
        return ResponseEntity.ok(ApiResponse.success(kebunQueryUseCase.listKebun(searchNama, searchKode)));
    }

    /** GET /api/kebun/{kebunId} */
    @GetMapping("/{kebunId}")
    public ResponseEntity<ApiResponse<KebunDTO>> getKebun(@PathVariable UUID kebunId) {
        return ResponseEntity.ok(ApiResponse.success(kebunQueryUseCase.getKebunById(kebunId)));
    }

    /** POST /api/kebun */
    @PostMapping
    public ResponseEntity<ApiResponse<KebunDTO>> create(@RequestBody CreateKebunRequestDTO body) {
        KebunDTO created = kebunCommandUseCase.createKebun(body.nama(), body.kode(), body.luas(), body.coordinates());
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    /** PUT /api/kebun/{kebunId} */
    @PutMapping("/{kebunId}")
    public ResponseEntity<ApiResponse<KebunDTO>> edit(@PathVariable UUID kebunId,
                                                      @RequestBody EditKebunRequestDTO body) {
        KebunDTO updated = kebunCommandUseCase.editKebun(kebunId, body.nama(), body.luas(), body.coordinates());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /** DELETE /api/kebun/{kebunId} */
    @DeleteMapping("/{kebunId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID kebunId) {
        kebunCommandUseCase.deleteKebun(kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** GET /api/kebun/{kebunId}/mandor */
    @GetMapping("/{kebunId}/mandor")
    public ResponseEntity<ApiResponse<MandorResponse>> getMandor(@PathVariable UUID kebunId) {
        UUID mandorId = kebunQueryUseCase.getMandorIdByKebun(kebunId);
        return ResponseEntity.ok(ApiResponse.success(new MandorResponse(mandorId)));
    }

    /** GET /api/kebun/{kebunId}/supir?search=... */
    @GetMapping("/{kebunId}/supir")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getSupir(
            @PathVariable UUID kebunId,
            @RequestParam(required = false) String search
    ) {
        List<UserDTO> list = kebunQueryUseCase.getSupirList(kebunId);
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            list = list.stream()
                    .filter(u -> u.name() != null && u.name().toLowerCase().contains(q))
                    .toList();
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /** GET /api/kebun/{kebunId}/buruh?search=... */
    @GetMapping("/{kebunId}/buruh")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getBuruh(
            @PathVariable UUID kebunId,
            @RequestParam(required = false) String search
    ) {
        List<UserDTO> list = kebunQueryUseCase.getBuruhList(kebunId);
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            list = list.stream()
                    .filter(u -> u.name() != null && u.name().toLowerCase().contains(q))
                    .toList();
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /** POST /api/kebun/{kebunId}/assign/mandor body: AssignPersonRequestDTO */
    @PostMapping("/{kebunId}/assign/mandor")
    public ResponseEntity<ApiResponse<Void>> assignMandor(@PathVariable UUID kebunId,
                                                          @RequestBody AssignPersonRequestDTO body) {
        kebunCommandUseCase.assignMandorToKebun(body.personId(), kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** POST /api/kebun/{kebunId}/move/mandor body: AssignPersonRequestDTO */
    @PostMapping("/{kebunId}/move/mandor")
    public ResponseEntity<ApiResponse<Void>> moveMandor(@PathVariable UUID kebunId,
                                                        @RequestBody AssignPersonRequestDTO body) {
        kebunCommandUseCase.moveMandorToKebun(body.personId(), kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** POST /api/kebun/{kebunId}/assign/supir body: AssignPersonRequestDTO */
    @PostMapping("/{kebunId}/assign/supir")
    public ResponseEntity<ApiResponse<Void>> assignSupir(@PathVariable UUID kebunId,
                                                         @RequestBody AssignPersonRequestDTO body) {
        kebunCommandUseCase.assignSupirToKebun(body.personId(), kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** POST /api/kebun/{kebunId}/move/supir body: AssignPersonRequestDTO */
    @PostMapping("/{kebunId}/move/supir")
    public ResponseEntity<ApiResponse<Void>> moveSupir(@PathVariable UUID kebunId,
                                                       @RequestBody AssignPersonRequestDTO body) {
        kebunCommandUseCase.moveSupirToKebun(body.personId(), kebunId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private record MandorResponse(UUID mandorId) {}
}