package com.devfalco.nummia.notification.dto;

import com.devfalco.nummia.notification.Notification;
import com.devfalco.nummia.notification.NotificationType;
import com.devfalco.nummia.notification.Severity;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, NotificationType type, Severity severity, String title, String message,
                                   boolean read, Instant createdAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getSeverity(), n.getTitle(), n.getMessage(),
                n.isRead(), n.getCreatedAt());
    }
}
