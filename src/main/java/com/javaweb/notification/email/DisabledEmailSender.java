package com.javaweb.notification.email;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"uat", "prod"})
public class DisabledEmailSender implements EmailSender {

    @Override
    public String send(EmailMessage message) {
        throw new IllegalStateException(
                "Email delivery is disabled for the active profile"
        );
    }
}
