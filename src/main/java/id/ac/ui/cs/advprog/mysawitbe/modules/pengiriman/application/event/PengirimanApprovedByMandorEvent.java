package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event;

import java.util.UUID;

/**
 * Published when a mandor approves a completed delivery.
 * Consumed by: notification and pembayaran modules.
 */
public record PengirimanApprovedByMandorEvent(
        UUID pengirimanId,
        UUID supirId,
        int totalWeight
) {}
