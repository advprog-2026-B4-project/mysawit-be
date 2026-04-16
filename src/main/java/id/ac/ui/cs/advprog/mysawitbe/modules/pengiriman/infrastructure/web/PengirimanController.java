package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignedSupirDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignDeliveryRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignablePanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.RejectDeliveryRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.UpdateDeliveryStatusRequestDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.exception.KebunQueryDependencyUnavailableException;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanQueryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pengiriman")
@RequiredArgsConstructor
public class PengirimanController {

    private final PengirimanQueryUseCase queryUseCase;
    private final PengirimanCommandUseCase commandUseCase;

    @PostMapping
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<PengirimanDTO>> assignSupirForDelivery(
            @RequestAttribute("userId") UUID mandorId,
            @Valid @RequestBody AssignDeliveryRequestDTO request
    ) {
        PengirimanDTO result = commandUseCase.assignSupirForDelivery(mandorId, request.supirId(), request.panenIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @GetMapping("/supir")
    @PreAuthorize("hasRole('SUPIR')")
    public ResponseEntity<ApiResponse<List<PengirimanDTO>>> listDeliveriesBySupir(
            @RequestAttribute("userId") UUID supirId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<PengirimanDTO> result = queryUseCase.listDeliveriesBySupir(supirId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{pengirimanId}/status")
    @PreAuthorize("hasRole('SUPIR')")
    public ResponseEntity<ApiResponse<PengirimanDTO>> updateDeliveryStatus(
            @PathVariable UUID pengirimanId,
            @RequestAttribute("userId") UUID supirId,
            @Valid @RequestBody UpdateDeliveryStatusRequestDTO request
    ) {
        PengirimanDTO result = commandUseCase.updateDeliveryStatus(
                pengirimanId,
                supirId,
                request.newStatus()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/mandor/supir")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<List<AssignedSupirDTO>>> listAssignedSupirForMandor(
            @RequestAttribute("userId") UUID mandorId,
            @RequestParam(required = false) String searchNama
    ) {
        List<AssignedSupirDTO> result = queryUseCase.listAssignedSupirForMandor(mandorId, searchNama);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/mandor/panen")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<List<AssignablePanenDTO>>> listAssignablePanenForMandor(
            @RequestAttribute("userId") UUID mandorId
    ) {
        List<AssignablePanenDTO> result = queryUseCase.listAssignablePanenForMandor(mandorId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/mandor/active")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<List<PengirimanDTO>>> listActiveDeliveriesByMandor(
            @RequestAttribute("userId") UUID mandorId
    ) {
        List<PengirimanDTO> result = queryUseCase.listActiveDeliveriesByMandor(mandorId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/supir/{supirId}/mandor")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<List<PengirimanDTO>>> listDeliveriesOfSupirByMandor(
            @RequestAttribute("userId") UUID mandorId,
            @PathVariable UUID supirId
    ) {
        List<PengirimanDTO> result = queryUseCase.listDeliveriesOfSupirByMandor(mandorId, supirId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{pengirimanId}/approve")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<PengirimanDTO>> mandorApproveDelivery(
            @PathVariable UUID pengirimanId,
            @RequestAttribute("userId") UUID mandorId
    ) {
        PengirimanDTO result = commandUseCase.mandorApproveDelivery(pengirimanId, mandorId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{pengirimanId}/reject")
    @PreAuthorize("hasRole('MANDOR')")
    public ResponseEntity<ApiResponse<PengirimanDTO>> mandorRejectDelivery(
            @PathVariable UUID pengirimanId,
            @RequestAttribute("userId") UUID mandorId,
            @Valid @RequestBody RejectDeliveryRequestDTO request
    ) {
        PengirimanDTO result = commandUseCase.mandorRejectDelivery(pengirimanId, mandorId, request.reason());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/admin/approved")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PengirimanDTO>>> listApprovedDeliveriesForAdmin(
            @RequestParam(required = false) String mandorName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<PengirimanDTO> result = queryUseCase.listApprovedDeliveriesForAdmin(mandorName, date);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ExceptionHandler(KebunQueryDependencyUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleKebunQueryDependencyUnavailable(
            KebunQueryDependencyUnavailableException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ex.getMessage()));
    }
}
