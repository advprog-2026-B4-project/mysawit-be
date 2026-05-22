package id.ac.ui.cs.advprog.mysawitbe.modules.notification.infrastructure.persistence;

import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.port.out.NotificationRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.dto.NotificationDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final JpaNotificationRepository repository;

    @Override
    public NotificationDTO save(NotificationDTO notificationDTO) {
        Notification notification = Notification.builder()
            .notificationId(notificationDTO.notificationId())
            .userId(notificationDTO.userId())
            .title(notificationDTO.title())
            .description(notificationDTO.description())
            .isRead(notificationDTO.isRead())
            .timestamp(notificationDTO.timestamp())
            .build();
        Notification saved = repository.save(notification);
        return toDto(saved);
    }

    @Override
    public NotificationDTO findById(UUID notificationId) {
        return repository.findById(notificationId).map(this::toDto).orElse(null);
    }

    @Override
    public List<NotificationDTO> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        repository.findById(notificationId).ifPresent(n -> {
            n.markAsRead();
            repository.save(n);
        });
    }

    @Transactional
    public void markAllAsReadByUserId(UUID userId) {
        repository.markAllAsReadByUserId(userId);
    }

    private NotificationDTO toDto(Notification notification) {
        return new NotificationDTO(
            notification.getNotificationId(),
            notification.getUserId(),
            notification.getTitle(),
            notification.getDescription(),
            notification.isRead(),
            notification.getTimestamp()
        );
    }
}
