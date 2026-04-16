package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.CreatePanenRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
                request.description(),
                request.weight(),
                request.photoUrls()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(responseData));
    }

    // 2. Link untuk Buruh melihat riwayat panennya sendiri
    // URL: GET /api/panen/buruh?status=APPROVED
    @GetMapping("/buruh")
    public ResponseEntity<ApiResponse<List<PanenDTO>>> getRiwayatPanenBuruh(
            @RequestAttribute("userId") UUID buruhId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status) {
        
        List<PanenDTO> result = queryUseCase.listPanenByBuruh(buruhId, startDate, endDate, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 3. Link untuk Mandor melihat riwayat panen buruh-buruhnya
    // URL: GET /api/panen/mandor?buruhName=Budi
    @GetMapping("/mandor")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<List<PanenDTO>>> getRiwayatPanenUntukMandor(
            @RequestAttribute("userId") UUID mandorId,
            @RequestParam(required = false) String buruhName,
            @RequestParam(required = false) LocalDate date) {
        
        List<PanenDTO> result = queryUseCase.listPanenByMandor(mandorId, buruhName, date);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}