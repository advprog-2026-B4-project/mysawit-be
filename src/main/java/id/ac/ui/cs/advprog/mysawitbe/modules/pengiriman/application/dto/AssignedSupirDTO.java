package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import java.util.UUID;

/**
 * Immutable data transfer object for supir assigned to mandor's kebun.
 */
public record AssignedSupirDTO(
        UUID supirId,
        String username,
        String name,
        String email
) {}
