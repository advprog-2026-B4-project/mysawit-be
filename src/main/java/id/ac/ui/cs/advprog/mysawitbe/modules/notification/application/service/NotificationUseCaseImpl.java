package id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.dto.NotificationDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.port.in.NotificationUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.port.out.NotificationRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.event.BuruhAssignedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.event.MandorAssignedToKebunEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenRejectedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanApprovedByMandorEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanStatusTibaEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.event.PayrollProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationUseCaseImpl implements NotificationUseCase {

    private final NotificationRepositoryPort repository;

    @Override
    @Transactional
    public NotificationDTO sendNotification(UUID userId, String title, String description) {
        NotificationDTO notification = new NotificationDTO(
            UUID.randomUUID(),
            userId,
            title,
            description,
            false,
            LocalDateTime.now()
        );
        return repository.save(notification);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        repository.markAsRead(notificationId);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        repository.markAllAsReadByUserId(userId);
    }

    @Override
    public List<NotificationDTO> listNotifications(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Async
    @EventListener
    @Override
    public void onBuruhAssigned(BuruhAssignedEvent event) {
        String title = "Tugas Baru: Anda Ditugaskan ke Mandor";
        String description = String.format("Admin telah menugaskan Anda kepada Mandor dengan ID %s", event.mandorId());
        sendNotification(event.buruhId(), title, description);
    }

    @Async
    @EventListener
    @Override
    public void onMandorAssignedToKebun(MandorAssignedToKebunEvent event) {
        String title = "Penugasan Kebun Baru";
        String description = String.format("Anda telah ditugaskan untuk mengelola kebun dengan ID %s", event.kebunId());
        sendNotification(event.mandorId(), title, description);
    }

    @Async
    @EventListener
    @Override
    public void onPanenApproved(PanenApprovedEvent event) {
        String title = "Panen Disetujui";
        String description = String.format("Laporan panen Anda (ID: %s, %d kg) telah disetujui. Payroll akan segera diproses.", 
            event.panenId(), event.weight());
        sendNotification(event.buruhId(), title, description);
    }

    @Async
    @EventListener
    @Override
    public void onPanenRejected(PanenRejectedEvent event) {
        String title = "Panen Ditolak";
        String description = String.format("Laporan panen Anda (ID: %s) telah ditolak. Alasan: %s", 
            event.panenId(), event.reason());
        sendNotification(event.buruhId(), title, description);
    }

    @Async
    @EventListener
    @Override
    public void onPengirimanApprovedByMandor(PengirimanApprovedByMandorEvent event) {
        String title = "Pengiriman Disetujui Mandor";
        String description = String.format("Pengiriman Anda (ID: %s, %d kg) telah disetujui oleh mandor. Payroll akan segera diproses.", 
            event.pengirimanId(), event.totalWeight());
        sendNotification(event.supirId(), title, description);
    }

    @Async
    @EventListener
    @Override
    public void onPengirimanStatusTiba(PengirimanStatusTibaEvent event) {
        String title = "Truk Tiba";
        String description = String.format("Truk untuk pengiriman %s telah tiba. Silakan tinjau dan proses.", 
            event.pengirimanId());
        sendNotification(event.mandorId(), title, description);
    }

    @Async
    @EventListener
    @Override
    public void onPengirimanProcessedByAdmin(PengirimanProcessedByAdminEvent event) {
        String title = "Pengiriman Diproses Admin";
        String description = String.format("Pengiriman %s telah diproses oleh admin dengan status %s (Diterima: %d kg).", 
            event.pengirimanId(), event.status(), event.acceptedWeight());
        sendNotification(event.mandorId(), title, description);
    }

    @Async
    @EventListener
    @Override
    public void onPayrollProcessed(PayrollProcessedEvent event) {
        String title = "Payroll " + event.status();
        String description = String.format("Payroll Anda (ID: %s) telah diproses dengan status %s.", 
            event.payrollId(), event.status());
        sendNotification(event.userId(), title, description);
    }
}
