package id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable data transfer object for user notification entries.
 * isRead: false by default when created.
 */
public record NotificationDTO(
        UUID notificationId,
        UUID userId,
        String title,
        String description,
        boolean isRead,
        LocalDateTime timestamp
) {}
