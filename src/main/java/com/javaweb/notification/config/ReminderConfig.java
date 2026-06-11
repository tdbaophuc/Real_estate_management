package com.javaweb.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ReminderProperties.class)
public class ReminderConfig {

    @Bean
    public Clock reminderClock() {
        return Clock.systemUTC();
    }
}
