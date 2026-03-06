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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/panen")
@RequiredArgsConstructor
public class PanenController {

    private final PanenCommandUseCase commandUseCase;
    private final PanenQueryUseCase queryUseCase;

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