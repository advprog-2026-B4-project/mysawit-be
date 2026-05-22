package id.ac.ui.cs.advprog.mysawitbe.modules.notification.infrastructure.persistence;

import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.dto.NotificationDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.notification.domain.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryAdapterTest {

    @Mock
    private JpaNotificationRepository jpaRepository;

    @InjectMocks
    private NotificationRepositoryAdapter adapter;

    @Test
    void testSave() {
        NotificationDTO dto = new NotificationDTO(UUID.randomUUID(), UUID.randomUUID(), "T", "D", false, LocalDateTime.now());

        when(jpaRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDTO result = adapter.save(dto);
        assertEquals(dto.notificationId(), result.notificationId());
        assertEquals(dto.userId(), result.userId());
    }

    @Test
    void testFindByIdFound() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder()
                .notificationId(id)
                .userId(UUID.randomUUID())
                .title("Title")
                .description("Desc")
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();

        when(jpaRepository.findById(id)).thenReturn(Optional.of(notification));

        NotificationDTO result = adapter.findById(id);
        assertNotNull(result);
        assertEquals(id, result.notificationId());
    }

    @Test
    void testFindByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());
        assertNull(adapter.findById(id));
    }

    @Test
    void testFindByUserId() {
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID())
                .userId(userId)
                .title("Title")
                .description("Desc")
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();

        when(jpaRepository.findByUserId(userId)).thenReturn(List.of(notification));
        List<NotificationDTO> results = adapter.findByUserId(userId);

        assertEquals(1, results.size());
        assertEquals(userId, results.get(0).userId());
    }

    @Test
    void testMarkAsRead() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder()
                .notificationId(id)
                .isRead(false)
                .build();

        when(jpaRepository.findById(id)).thenReturn(Optional.of(notification));

        adapter.markAsRead(id);

        assertTrue(notification.isRead());
        verify(jpaRepository).save(notification);
    }

    @Test
    void testMarkAllAsReadByUserId() {
        UUID userId = UUID.randomUUID();
        adapter.markAllAsReadByUserId(userId);
        verify(jpaRepository).markAllAsReadByUserId(userId);
    }
}
