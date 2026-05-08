package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.TopUpResponseDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanApprovedByMandorEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;

import org.springframework.context.event.EventListener;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

/**
 * Inbound port for pembayaran write operations.
 * RBAC enforced on HTTP-facing methods via @PreAuthorize.
 * @EventListener methods are excluded: they run in an internal Spring context with no HTTP principal.
 * handlePaymentCallback is excluded: it is a webhook guarded by Midtrans signature verification.
 */
public interface PembayaranCommandUseCase {

    /**
     * Triggered when a panen is approved; creates a pending payroll for the buruh.
     * Internal event - no HTTP principal, do not add @PreAuthorize.
     */
    @EventListener
    void onPanenApproved(PanenApprovedEvent event);

    /**
     * Triggered when a mandor approves a pengiriman; creates a pending payroll for the assigned supir.
     * Internal event - no HTTP principal, do not add @PreAuthorize.
     */
    @EventListener
    void onPengirimanApprovedByMandor(PengirimanApprovedByMandorEvent event);

    /**
     * Triggered when admin processes a pengiriman; creates pending payroll for mandor (and supir if applicable).
     * Internal event - no HTTP principal, do not add @PreAuthorize.
     */
    @EventListener
    void onPengirimanProcessedByAdmin(PengirimanProcessedByAdminEvent event);

    /**
     * Admin approves a pending payroll entry. ADMIN only.
     * Debits admin wallet and credits target user wallet.
     * Publishes PayrollProcessedEvent.
     */
    @PreAuthorize("hasRole('ADMIN')")
    PayrollDTO approvePayroll(UUID payrollId, UUID adminId);

    /**
     * Admin rejects a payroll entry with a reason. ADMIN only.
     * Publishes PayrollProcessedEvent with REJECTED status.
     */
    @PreAuthorize("hasRole('ADMIN')")
    PayrollDTO rejectPayroll(UUID payrollId, UUID adminId, String reason);

    /**
     * Handle incoming payment gateway callback (Midtrans).
     * No JWT is expected; Midtrans signature is verified inside the implementation.
     */
    void handlePaymentCallback(PaymentCallbackDTO payload);

    /**
     * Update wage rate configuration. ADMIN only.
     * type: BURUH, SUPIR, or MANDOR.
     */
    @PreAuthorize("hasRole('ADMIN')")
    void updateWageRate(String type, int newRatePerGram);

    /**
     * Initiate a wallet top-up for the admin via Midtrans Snap.
     * Returns a redirect URL to the Midtrans Snap payment page.
     * ADMIN only.
     */
    @PreAuthorize("hasRole('ADMIN')")
    TopUpResponseDTO initiateTopUp(UUID adminId, int amount);
}
