package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event;

import java.util.UUID;

/**
 * Published when a mandor approves and submits a delivery assignment.
 * Consumed by: notification module.
 */
public record PengirimanApprovedByMandorEvent(
        UUID pengirimanId,
        UUID supirId,
        int totalWeight
) {}
