package id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.port.out;

import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.dto.NotificationDTO;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for notification persistence.
 * Implemented by infrastructure/persistence/NotificationJpaAdapter.
 */
public interface NotificationRepositoryPort {

    NotificationDTO save(NotificationDTO notificationDTO);

    NotificationDTO findById(UUID notificationId);

    /**
     * Returns all notifications for a user; unread entries sorted first.
     */
    List<NotificationDTO> findByUserId(UUID userId);

    void markAsRead(UUID notificationId);
}
