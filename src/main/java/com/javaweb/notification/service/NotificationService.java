package com.javaweb.notification.service;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import com.javaweb.notification.dto.MarkAllReadResponse;
import com.javaweb.notification.dto.NotificationResponse;
import com.javaweb.notification.dto.UnreadCountResponse;
import com.javaweb.notification.entity.Notification;
import com.javaweb.notification.mapper.NotificationMapper;
import com.javaweb.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(
            Boolean unread,
            int pageNumber,
            int pageSize,
            AuthUserPrincipal actor
    ) {
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        Page<Notification> page;
        if (Boolean.TRUE.equals(unread)) {
            page = notificationRepository
                    .findAllByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(
                            actor.id(),
                            pageable
                    );
        } else if (Boolean.FALSE.equals(unread)) {
            page = notificationRepository
                    .findAllByRecipientIdAndReadAtIsNotNullOrderByCreatedAtDesc(
                            actor.id(),
                            pageable
                    );
        } else {
            page = notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(
                    actor.id(),
                    pageable
            );
        }
        return PageResponse.from(
                page,
                page.getContent().stream()
                        .map(notificationMapper::toResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(AuthUserPrincipal actor) {
        return new UnreadCountResponse(
                notificationRepository.countByRecipientIdAndReadAtIsNull(actor.id())
        );
    }

    @Transactional
    public NotificationResponse markRead(
            Long notificationId,
            AuthUserPrincipal actor
    ) {
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, actor.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found"
                ));
        if (!notification.isRead()) {
            notification.setReadAt(Instant.now());
        }
        return notificationMapper.toResponse(
                notificationRepository.saveAndFlush(notification)
        );
    }

    @Transactional
    public MarkAllReadResponse markAllRead(AuthUserPrincipal actor) {
        return new MarkAllReadResponse(
                notificationRepository.markAllRead(actor.id(), Instant.now())
        );
    }
}
