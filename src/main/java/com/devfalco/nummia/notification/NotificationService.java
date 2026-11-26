package com.devfalco.nummia.notification;

import com.devfalco.nummia.automation.AutomationEventPublisher;
import com.devfalco.nummia.common.exception.NotFoundException;
import com.devfalco.nummia.common.web.PageResponse;
import com.devfalco.nummia.notification.dto.NotificationResponse;
import com.devfalco.nummia.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final UserRepository users;
    private final AutomationEventPublisher automation;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, boolean unreadOnly, Pageable pageable) {
        var page = unreadOnly
                ? repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page, NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID id) {
        Notification n = repository.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Notificação"));
        n.setRead(true);
        return NotificationResponse.from(n);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repository.markAllRead(userId);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(repository.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Notificação")));
    }

    /**
     * Cria uma notificação, caso ainda não exista uma com a mesma chave de referência
     * e o usuário não tenha desativado as notificações.
     */
    @Transactional
    public Optional<Notification> notify(UUID userId, NotificationType type, Severity severity,
                                         String title, String message, String referenceKey) {
        boolean enabled = users.findById(userId).map(u -> u.isNotificationsEnabled()).orElse(false);
        if (!enabled || repository.existsByUserIdAndReferenceKey(userId, referenceKey)) {
            return Optional.empty();
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setSeverity(severity);
        n.setTitle(title);
        n.setMessage(message);
        n.setReferenceKey(referenceKey);
        n.setCreatedAt(Instant.now());
        repository.save(n);
        automation.publish("notification.created", userId, Map.of(
                "notificationId", n.getId().toString(),
                "type", type.name(),
                "severity", severity.name(),
                "title", title,
                "message", message));
        return Optional.of(n);
    }
}
