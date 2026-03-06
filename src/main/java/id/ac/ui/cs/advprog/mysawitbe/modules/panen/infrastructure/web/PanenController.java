package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/panen")
@RequiredArgsConstructor
public class PanenController {

    private final PanenCommandUseCase commandUseCase;
    private final PanenQueryUseCase queryUseCase;

    // TODO: Implementasi endpoint setelah frontend siap
    // POST   /api/panen              → createPanen
    // GET    /api/panen              → getAllPanen
    // GET    /api/panen/{id}         → getPanenById
    // GET    /api/panen/buruh/{id}   → getPanenByBuruhId
    // PATCH  /api/panen/{id}/approve → approvePanen (sprint berikutnya)
    // PATCH  /api/panen/{id}/reject  → rejectPanen (sprint berikutnya)
}