package id.ac.ui.cs.advprog.mysawitbe.modules.notification.infrastructure.persistence;

import id.ac.ui.cs.advprog.mysawitbe.modules.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaNotificationRepository extends JpaRepository<Notification, UUID> {
    
    // Unread first, then sorted by timestamp descending
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.isRead ASC, n.timestamp DESC")
    List<Notification> findByUserId(UUID userId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
    void markAllAsReadByUserId(UUID userId);
}
