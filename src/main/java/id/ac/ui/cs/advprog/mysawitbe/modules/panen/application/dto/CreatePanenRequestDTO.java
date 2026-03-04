package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePanenRequestDTO(
    @NotNull(message = "Kebun ID is required")
    UUID kebunId,

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    Integer weight,

    @NotEmpty(message = "Photo URLs are required")
    List<String> photoUrls,

    @NotBlank(message = "Panen description is required")
    String description
) {}
