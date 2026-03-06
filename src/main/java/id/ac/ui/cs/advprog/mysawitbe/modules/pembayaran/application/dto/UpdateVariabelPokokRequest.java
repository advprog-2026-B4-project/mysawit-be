package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for updating a single wage variable.
 * key must match the path variable in the controller.
 */
public record UpdateVariabelPokokRequest(

        @NotNull(message = "Key is required")
        VariableKey key,

        @NotNull(message = "newValue is required")
        @Positive(message = "newValue must be a positive integer")
        Integer newValue
) {}
