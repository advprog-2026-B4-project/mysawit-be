package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import java.util.List;

public record PengirimanPageDTO(
        List<PengirimanDTO> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {}
