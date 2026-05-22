package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateWageRateRequestDTO(
    @NotBlank(message = "Type is required")
    String type,

    @NotNull(message = "New rate per gram is required")
    @Positive(message = "Rate per gram must be positive")
    BigDecimal newRatePerGram
) {}
