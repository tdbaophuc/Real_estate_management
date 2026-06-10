package com.javaweb.notification.mapper;

import com.javaweb.notification.dto.NotificationResponse;
import com.javaweb.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActionUrl(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getMetadataJson(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
