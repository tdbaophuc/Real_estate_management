package com.javaweb.notification.email;

public record EmailMessage(
        String recipient,
        String subject,
        String body
) {
}
