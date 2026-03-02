package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;

import org.springframework.context.event.EventListener;

import java.util.UUID;

/**
 * Use case interface for pembayaran write operations.
 * Event listener methods are annotated to allow Spring to wire them as bean methods.
 */
public interface PembayaranCommandUseCase {

    /**
     * Triggered when a panen is approved; creates a pending payroll for the buruh.
     */
    @EventListener
    void onPanenApproved(PanenApprovedEvent event);

    /**
     * Triggered when admin processes a pengiriman; creates pending payroll for mandor (and supir if applicable).
     */
    @EventListener
    void onPengirimanProcessedByAdmin(PengirimanProcessedByAdminEvent event);

    /**
     * Admin approves a payroll entry.
     * Publishes PayrollProcessedEvent (credits the user wallet).
     */
    PayrollDTO approvePayroll(UUID payrollId, UUID adminId);

    /**
     * Admin rejects a payroll entry with a reason.
     * Publishes PayrollProcessedEvent with REJECTED status.
     */
    PayrollDTO rejectPayroll(UUID payrollId, UUID adminId, String reason);

    /**
     * Handle incoming payment gateway callback (Midtrans).
     * Must verify signatureKey before crediting.
     */
    void handlePaymentCallback(PaymentCallbackDTO payload);

    /**
     * Update wage rate configuration (admin-only).
     * type: BURUH, SUPIR, or MANDOR.
     */
    void updateWageRate(String type, int newRatePerGram);
}
