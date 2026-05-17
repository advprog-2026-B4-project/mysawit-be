package id.ac.ui.cs.advprog.mysawitbe.modules.notification.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.dto.NotificationDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.port.in.NotificationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationUseCase notificationUseCase;

    @InjectMocks
    private NotificationController controller;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void testListNotifications() {
        NotificationDTO dto = new NotificationDTO(UUID.randomUUID(), userId, "Test", "Desc", false, LocalDateTime.now());
        when(notificationUseCase.listNotifications(userId)).thenReturn(List.of(dto));

        ResponseEntity<List<NotificationDTO>> response = controller.listNotifications(userId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Test", response.getBody().get(0).title());
    }

    @Test
    void testMarkAsRead() {
        UUID notificationId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.markAsRead(notificationId, userId);

        verify(notificationUseCase, times(1)).markAsRead(notificationId);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testMarkAllAsRead() {
        ResponseEntity<Void> response = controller.markAllAsRead(userId);

        verify(notificationUseCase, times(1)).markAllAsRead(userId);
        assertEquals(200, response.getStatusCode().value());
    }
}
