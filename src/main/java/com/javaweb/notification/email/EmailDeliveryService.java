package com.javaweb.notification.email;

import com.javaweb.notification.entity.EmailLog;
import com.javaweb.notification.enums.EmailDeliveryStatus;
import com.javaweb.notification.repository.EmailLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class EmailDeliveryService {
    private final EmailLogRepository emailLogRepository;
    private final EmailSender emailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final Clock clock;

    public EmailDeliveryService(
            EmailLogRepository emailLogRepository,
            EmailSender emailSender,
            EmailTemplateRenderer templateRenderer,
            Clock clock
    ) {
        this.emailLogRepository = emailLogRepository;
        this.emailSender = emailSender;
        this.templateRenderer = templateRenderer;
        this.clock = clock;
    }

    @Transactional
    public boolean deliver(EmailDeliveryRequest request) {
        if (request.recipientEmail() == null || request.recipientEmail().isBlank()) {
            return false;
        }
        if (emailLogRepository
                .existsByTemplateIdAndRecipientEmailIgnoreCaseAndReferenceTypeAndReferenceId(
                        request.template().getId(),
                        request.recipientEmail(),
                        request.referenceType(),
                        request.referenceId()
                )) {
            return false;
        }

        String subject = templateRenderer.render(
                request.template().getSubjectTemplate(),
                request.variables()
        );
        String body = templateRenderer.render(
                request.template().getBodyTemplate(),
                request.variables()
        );
        EmailLog log = new EmailLog(request.recipientEmail(), subject, body);
        log.setTemplate(request.template());
        log.setRecipientUser(request.recipientUser());
        log.setReferenceType(request.referenceType());
        log.setReferenceId(request.referenceId());
        log.setScheduledAt(request.scheduledAt());
        log.setAttemptCount(1);

        Instant now = clock.instant();
        try {
            String providerMessageId = emailSender.send(
                    new EmailMessage(request.recipientEmail(), subject, body)
            );
            log.setProviderMessageId(providerMessageId);
            log.setStatus(EmailDeliveryStatus.SENT);
            log.setSentAt(now);
        } catch (RuntimeException exception) {
            log.setStatus(EmailDeliveryStatus.FAILED);
            log.setFailedAt(now);
            log.setLastError(limitError(exception.getMessage()));
        }
        emailLogRepository.saveAndFlush(log);
        return true;
    }

    private String limitError(String message) {
        if (message == null) {
            return "Email delivery failed";
        }
        return message.substring(0, Math.min(message.length(), 4000));
    }
}
