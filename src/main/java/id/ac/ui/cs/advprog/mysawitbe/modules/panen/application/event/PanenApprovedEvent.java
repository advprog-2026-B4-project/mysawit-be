package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published when a mandor approves a buruh's harvest record.
 * Consumed by: pembayaran module (triggers payroll creation), notification module.
 */
public record PanenApprovedEvent(
        UUID panenId,
        UUID buruhId,
        UUID kebunId,
        int weight,
        LocalDateTime timestamp
) {}
