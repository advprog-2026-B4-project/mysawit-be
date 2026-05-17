package id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.event.BuruhAssignedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.event.MandorAssignedToKebunEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.dto.NotificationDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.port.out.NotificationRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenApprovedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.event.PanenRejectedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.event.PayrollProcessedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanApprovedByMandorEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanProcessedByAdminEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanStatusTibaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationUseCaseImplTest {

    @Mock
    private NotificationRepositoryPort repositoryPort;

    @InjectMocks
    private NotificationUseCaseImpl notificationUseCase;

    @Test
    void testSendNotification() {
        UUID userId = UUID.randomUUID();
        when(repositoryPort.save(any(NotificationDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDTO result = notificationUseCase.sendNotification(userId, "Title", "Desc");

        assertNotNull(result.notificationId());
        assertEquals(userId, result.userId());
        assertEquals("Title", result.title());
        assertEquals("Desc", result.description());
        assertFalse(result.isRead());
    }

    @Test
    void testMarkAsRead() {
        UUID notificationId = UUID.randomUUID();
        notificationUseCase.markAsRead(notificationId);
        verify(repositoryPort, times(1)).markAsRead(notificationId);
    }

    @Test
    void testMarkAllAsRead() {
        UUID userId = UUID.randomUUID();
        notificationUseCase.markAllAsRead(userId);
        verify(repositoryPort, times(1)).markAllAsReadByUserId(userId);
    }

    @Test
    void testListNotifications() {
        UUID userId = UUID.randomUUID();
        NotificationDTO dto = new NotificationDTO(UUID.randomUUID(), userId, "T", "D", false, LocalDateTime.now());
        when(repositoryPort.findByUserId(userId)).thenReturn(List.of(dto));

        List<NotificationDTO> result = notificationUseCase.listNotifications(userId);

        assertEquals(1, result.size());
        assertEquals("T", result.get(0).title());
    }

    @Test
    void testOnBuruhAssignedEvent() {
        BuruhAssignedEvent event = new BuruhAssignedEvent(UUID.randomUUID(), UUID.randomUUID());
        notificationUseCase.onBuruhAssigned(event);
        verifySaveWithTitle("Tugas Baru: Anda Ditugaskan ke Mandor");
    }

    @Test
    void testOnMandorAssignedToKebunEvent() {
        MandorAssignedToKebunEvent event = new MandorAssignedToKebunEvent(UUID.randomUUID(), UUID.randomUUID());
        notificationUseCase.onMandorAssignedToKebun(event);
        verifySaveWithTitle("Penugasan Kebun Baru");
    }

    @Test
    void testOnPanenApprovedEvent() {
        PanenApprovedEvent event = new PanenApprovedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            100,
            LocalDateTime.now()
        );
        notificationUseCase.onPanenApproved(event);
        verifySaveWithTitle("Panen Disetujui");
    }

    @Test
    void testOnPanenRejectedEvent() {
        PanenRejectedEvent event = new PanenRejectedEvent(UUID.randomUUID(), UUID.randomUUID(), "Quality bad");
        notificationUseCase.onPanenRejected(event);
        verifySaveWithTitle("Panen Ditolak");
    }

    @Test
    void testOnPengirimanApprovedByMandor() {
        PengirimanApprovedByMandorEvent event = new PengirimanApprovedByMandorEvent(UUID.randomUUID(), UUID.randomUUID(), 500);
        notificationUseCase.onPengirimanApprovedByMandor(event);
        verifySaveWithTitle("Pengiriman Disetujui Mandor");
    }

    @Test
    void testOnPengirimanStatusTiba() {
        PengirimanStatusTibaEvent event = new PengirimanStatusTibaEvent(UUID.randomUUID(), UUID.randomUUID());
        notificationUseCase.onPengirimanStatusTiba(event);
        verifySaveWithTitle("Truk Tiba");
    }

    @Test
    void testOnPengirimanProcessedByAdmin() {
        PengirimanProcessedByAdminEvent event = new PengirimanProcessedByAdminEvent(UUID.randomUUID(), UUID.randomUUID(), 490, "ACCEPTED");
        notificationUseCase.onPengirimanProcessedByAdmin(event);
        verifySaveWithTitle("Pengiriman Diproses Admin");
    }

    @Test
    void testOnPayrollProcessed() {
        PayrollProcessedEvent event = new PayrollProcessedEvent(UUID.randomUUID(), UUID.randomUUID(), "DONE");
        notificationUseCase.onPayrollProcessed(event);
        verifySaveWithTitle("Payroll DONE");
    }

    private void verifySaveWithTitle(String expectedTitle) {
        ArgumentCaptor<NotificationDTO> captor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(repositoryPort, times(1)).save(captor.capture());
        assertEquals(expectedTitle, captor.getValue().title());
    }
}
