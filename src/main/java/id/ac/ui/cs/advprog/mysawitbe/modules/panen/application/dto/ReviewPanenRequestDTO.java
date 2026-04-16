package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewPanenRequestDTO(
        @NotNull String action,      
        String rejectionReason        
) {}