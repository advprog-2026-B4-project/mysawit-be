package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto;

import java.util.List;

public record PanenPageDTO(
        List<PanenDTO> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {}
