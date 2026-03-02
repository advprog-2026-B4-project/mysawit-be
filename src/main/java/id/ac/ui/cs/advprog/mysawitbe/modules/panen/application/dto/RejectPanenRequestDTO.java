package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectPanenRequestDTO(
    @NotBlank(message = "Reason is required")
    String reason
) {}
