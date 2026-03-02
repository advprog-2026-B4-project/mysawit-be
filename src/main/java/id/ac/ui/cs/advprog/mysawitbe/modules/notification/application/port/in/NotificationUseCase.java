package id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.dto.NotificationDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.event.BuruhAssignedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.event.MandorAssignedToKebunEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenRejectedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanApprovedByMandorEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanStatusTibaEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.event.PayrollProcessedEvent;

import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.UUID;

/**
 * Use case interface for notification operations.
 * Listens to all published domain events and sends corresponding notifications.
 */
public interface NotificationUseCase {

    /**
     * Directly send a notification to a user.
     */
    NotificationDTO sendNotification(UUID userId, String title, String description);

    /**
     * Mark a specific notification as read.
     */
    void markAsRead(UUID notificationId);

    /**
     * Mark all notifications as read for a specific user.
     */
    void markAllAsRead(UUID userId);

    /**
     * Returns all notifications for a user (unread first).
     */
    List<NotificationDTO> listNotifications(UUID userId);

    @EventListener
    void onBuruhAssigned(BuruhAssignedEvent event);

    @EventListener
    void onMandorAssignedToKebun(MandorAssignedToKebunEvent event);

    @EventListener
    void onPanenApproved(PanenApprovedEvent event);

    @EventListener
    void onPanenRejected(PanenRejectedEvent event);

    @EventListener
    void onPengirimanApprovedByMandor(PengirimanApprovedByMandorEvent event);

    @EventListener
    void onPengirimanStatusTiba(PengirimanStatusTibaEvent event);

    @EventListener
    void onPengirimanProcessedByAdmin(PengirimanProcessedByAdminEvent event);

    @EventListener
    void onPayrollProcessed(PayrollProcessedEvent event);
}
