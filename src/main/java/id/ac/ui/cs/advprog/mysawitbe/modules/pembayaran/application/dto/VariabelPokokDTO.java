package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;

/**
 * Immutable DTO for a wage variable response.
 * label and description are derived from the VariableKey enum.
 */
public record VariabelPokokDTO(
        VariableKey key,
        String label,
        String description,
        int value
) {}
