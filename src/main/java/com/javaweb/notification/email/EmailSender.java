package com.javaweb.notification.email;

public interface EmailSender {
    String send(EmailMessage message);
}
