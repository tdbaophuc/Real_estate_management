package com.javaweb.notification.repository;

import com.javaweb.notification.entity.NotificationTemplate;
import com.javaweb.notification.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository
        extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByCodeAndActiveTrue(String code);

    List<NotificationTemplate> findAllByChannelAndActiveTrueOrderByCode(
            NotificationChannel channel
    );
}
