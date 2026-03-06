package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.UpdateVariabelPokokRequest;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.VariabelPokokCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.VariabelPokokQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Thin HTTP adapter for variabel-pokok endpoints.
 * Delegates all logic to use cases; never contains business logic itself.
 */
@RestController
@RequestMapping("/api/pembayaran/variabel-pokok")
@RequiredArgsConstructor
public class VariabelPokokController {

    private final VariabelPokokQueryUseCase   queryUseCase;
    private final VariabelPokokCommandUseCase commandUseCase;

    /** GET /api/pembayaran/variabel-pokok - returns all three wage variables. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VariabelPokokDTO>>> getAll() {
        List<VariabelPokokDTO> result = queryUseCase.getAllVariabelPokok();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** GET /api/pembayaran/variabel-pokok/{key} - returns one variable by key. */
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<VariabelPokokDTO>> getOne(@PathVariable VariableKey key) {
        VariabelPokokDTO result = queryUseCase.getVariabelPokok(key);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * PUT /api/pembayaran/variabel-pokok/{key} - admin updates a wage rate.
     * Validates that path key equals body key to prevent mismatch.
     */
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<VariabelPokokDTO>> update(
            @PathVariable VariableKey key,
            @Valid @RequestBody UpdateVariabelPokokRequest request
    ) {
        if (request.key() != null && request.key() != key) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Path key and body key do not match"));
        }
        VariabelPokokDTO result = commandUseCase.updateVariabelPokok(key, request.newValue());
        return ResponseEntity.ok(ApiResponse.success("Variabel pokok berhasil diperbarui", result));
    }
}
