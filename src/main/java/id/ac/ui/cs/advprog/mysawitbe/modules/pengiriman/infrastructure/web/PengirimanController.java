package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
