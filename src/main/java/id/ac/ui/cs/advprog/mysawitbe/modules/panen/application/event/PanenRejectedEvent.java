package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event;

import java.util.UUID;

/**
 * Published when a mandor rejects a buruh's harvest record.
 * Consumed by: notification module.
 */
public record PanenRejectedEvent(
        UUID panenId,
        UUID buruhId,
        String reason
) {}
