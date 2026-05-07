package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.ReviewPanenRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import jakarta.persistence.EntityNotFoundException;
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
        try {
            PanenDTO responseData = queryUseCase.getPanenById(panenId);
            return ResponseEntity.ok(
                    ApiResponse.success("Detail panen", responseData));
                    
        } catch (EntityNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PanenDTO>> createPanen(
            @RequestAttribute("userId") UUID buruhId, 
            @Valid @RequestBody CreatePanenRequestDTO request) {
        
        try {
            PanenDTO responseData = commandUseCase.createPanen(
                    buruhId,
                    request.description(),
                    request.weight(),
                    request.photoUrls()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Panen berhasil dicatat", responseData));
                    
        } catch (EntityNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan: " + e.getMessage()));
        }
    }

    @GetMapping("/buruh")
    public ResponseEntity<ApiResponse<List<PanenDTO>>> getRiwayatPanenBuruh(
            @RequestAttribute("userId") UUID buruhId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status) {
        
        try {
            List<PanenDTO> result = queryUseCase.listPanenByBuruh(
                    buruhId, startDate, endDate, status);
            return ResponseEntity.ok(
                    ApiResponse.success("Riwayat panen buruh", result));
                    
        } catch (EntityNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan: " + e.getMessage()));
        }
    }

    @GetMapping("/mandor")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<List<PanenDTO>>> getRiwayatPanenUntukMandor(
            @RequestAttribute("userId") UUID mandorId,
            @RequestParam(required = false) String buruhName,
            @RequestParam(required = false) LocalDate date) {
        
        try {
            List<PanenDTO> result = queryUseCase.listPanenByMandor(
                    mandorId, buruhName, date);
            return ResponseEntity.ok(
                    ApiResponse.success("Riwayat panen buruh", result));
                    
        } catch (EntityNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan: " + e.getMessage()));
        }
    }

    @GetMapping("/checksubmission")
    public ResponseEntity<ApiResponse<Boolean>> checkPanenToday(
            @RequestAttribute("userId") UUID buruhId) {

        try {
            boolean isSubmitted = queryUseCase.hasPanenToday(buruhId, LocalDate.now());
            return ResponseEntity.ok(
                    ApiResponse.success("Panen sudah disubmit hari ini", isSubmitted));
                    
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan: " + e.getMessage()));
        }
    }

    @GetMapping("/buruh/{buruhId}")
    public ResponseEntity<ApiResponse<List<PanenDTO>>> getPanenByBuruhId(
            @PathVariable UUID buruhId,
            @RequestAttribute("userId") UUID requesterId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status) {
        
        try {
            List<PanenDTO> result = queryUseCase.listPanenByBuruhWithAuth(
                    buruhId, requesterId, startDate, endDate, status);
            
            return ResponseEntity.ok(
                    ApiResponse.success("Daftar panen buruh", result));
                    
        } catch (EntityNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (IllegalAccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Terjadi kesalahan: " + e.getMessage()));
        }
    }

    @PatchMapping("/{panenId}/review")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<PanenDTO>> reviewPanen(
            @PathVariable UUID panenId,
            @RequestAttribute("userId") UUID mandorId,
            @Valid @RequestBody ReviewPanenRequestDTO request) {

        PanenDTO result = switch (request.action().toUpperCase()) {
            case "APPROVE" -> commandUseCase.approvePanen(panenId, mandorId);
            case "REJECT"  -> {
                if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
                    throw new IllegalArgumentException("Alasan penolakan wajib diisi.");
                }
                yield commandUseCase.rejectPanen(panenId, mandorId, request.rejectionReason());
            }
            default -> throw new IllegalArgumentException("Action tidak valid. Gunakan APPROVE atau REJECT.");
        };

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}