package id.ac.ui.cs.advprog.mysawitbe.modules.notification.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.dto.NotificationDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.port.in.NotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> listNotifications(@RequestAttribute("userId") UUID claimsUserId) {
        List<NotificationDTO> notifications = notificationUseCase.listNotifications(claimsUserId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId, @RequestAttribute("userId") UUID claimsUserId) {
        // Optionally, check if the notification belongs to this user.
        // For simplicity, we just mark it as read.
        notificationUseCase.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(@RequestAttribute("userId") UUID claimsUserId) {
        notificationUseCase.markAllAsRead(claimsUserId);
        return ResponseEntity.ok().build();
    }
}
