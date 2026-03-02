package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event;

import java.util.UUID;

/**
 * Published when admin processes a delivery (approve / partial accept / reject).
 * acceptedWeight: 0 if fully rejected; partial value for partial acceptance.
 * status: APPROVED, PARTIAL, REJECTED.
 * Consumed by: pembayaran module (triggers mandor payroll), notification module.
 */
public record PengirimanProcessedByAdminEvent(
        UUID pengirimanId,
        UUID mandorId,
        int acceptedWeight,
        String status
) {}
