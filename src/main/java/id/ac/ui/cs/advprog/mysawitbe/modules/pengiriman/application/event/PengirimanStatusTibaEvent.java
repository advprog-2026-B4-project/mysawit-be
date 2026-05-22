package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event;

import java.util.UUID;

/**
 * Published when supir marks the delivery status as TIBA (arrived at destination).
 * Consumed by: pembayaran module (triggers mandor payroll), notification module.
 */
public record PengirimanStatusTibaEvent(
        UUID pengirimanId,
        UUID mandorId
) {}
