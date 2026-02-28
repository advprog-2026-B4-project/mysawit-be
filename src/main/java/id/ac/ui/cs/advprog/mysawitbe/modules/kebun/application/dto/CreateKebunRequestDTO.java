package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateKebunRequestDTO(
    @NotBlank(message = "Nama is required")
    String nama,

    @NotBlank(message = "Kode is required")
    String kode,

    @NotNull(message = "Luas is required")
    @Positive(message = "Luas must be positive")
    Integer luas,

    @NotEmpty(message = "Coordinates are required")
    List<CoordinateDTO> coordinates
) {}
