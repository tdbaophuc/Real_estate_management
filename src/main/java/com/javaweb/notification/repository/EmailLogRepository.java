package com.javaweb.notification.repository;

import com.javaweb.notification.entity.EmailLog;
import com.javaweb.notification.enums.EmailDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    Page<EmailLog> findAllByStatusOrderByCreatedAtAsc(
            EmailDeliveryStatus status,
            Pageable pageable
    );

    boolean existsByTemplateIdAndRecipientEmailIgnoreCaseAndReferenceTypeAndReferenceId(
            Long templateId,
            String recipientEmail,
            String referenceType,
            Long referenceId
    );
}
