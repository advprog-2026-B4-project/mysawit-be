package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.CreatePanenRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/panen")
@RequiredArgsConstructor
public class PanenController {

    private final PanenCommandUseCase commandUseCase;
    private final PanenQueryUseCase queryUseCase;

    @GetMapping("/{panenId}")
    public ResponseEntity<ApiResponse<PanenDTO>> getPanenById(@PathVariable UUID panenId) {
        PanenDTO responseData = queryUseCase.getPanenById(panenId);
        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PanenDTO>> createPanen(
            @RequestAttribute("userId") UUID buruhId, 
            @Valid @RequestBody CreatePanenRequestDTO request) {
        
        PanenDTO responseData = commandUseCase.createPanen(
                buruhId,
                request.kebunId(),
                request.description(),
                request.weight(),
                request.photoUrls()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(responseData));
    }
}