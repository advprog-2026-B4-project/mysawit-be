package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignPersonRequestDTO(
        @NotNull(message = "Person ID is required")
        UUID personId
) {}