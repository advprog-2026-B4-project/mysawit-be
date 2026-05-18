package id.ac.ui.cs.advprog.mysawitbe.modules.notification.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testNotificationCreationAndMarkAsRead() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Notification notification = Notification.builder()
                .notificationId(notificationId)
                .userId(userId)
                .title("Test Title")
                .description("Test Description")
                .isRead(false)
                .timestamp(now)
                .build();

        assertEquals(notificationId, notification.getNotificationId());
        assertEquals(userId, notification.getUserId());
        assertEquals("Test Title", notification.getTitle());
        assertEquals("Test Description", notification.getDescription());
        assertFalse(notification.isRead());
        assertEquals(now, notification.getTimestamp());

        // Test mark as read
        notification.markAsRead();
        assertTrue(notification.isRead());
    }

    @Test
    void testSetters() {
        Notification notification = Notification.builder().build();
        UUID newId = UUID.randomUUID();

        notification.setNotificationId(newId);
        notification.setTitle("New Title");

        assertEquals(newId, notification.getNotificationId());
        assertEquals("New Title", notification.getTitle());
    }
}
