package com.javaweb.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile({"dev", "test"})
public class LoggingEmailSender implements EmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            LoggingEmailSender.class
    );

    @Override
    public String send(EmailMessage message) {
        String messageId = "log-" + UUID.randomUUID();
        LOGGER.info(
                "Development email sent id={} recipient={} subject={} body={}",
                messageId,
                message.recipient(),
                message.subject(),
                message.body()
        );
        return messageId;
    }
}
