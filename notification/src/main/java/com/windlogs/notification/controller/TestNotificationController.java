package com.windlogs.notification.controller;

import com.windlogs.notification.kafka.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-notifications")
@RequiredArgsConstructor
@Slf4j
public class TestNotificationController {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @PostMapping("/send")
    public String sendTestNotification(@RequestBody NotificationEvent event) {
        try {
            kafkaTemplate.send("realtime-notifications", event);
            log.info("Test notification sent successfully: {}", event);
            return "Notification sent successfully";
        } catch (Exception e) {
            log.error("Error sending test notification: {}", e.getMessage(), e);
            return "Error sending notification: " + e.getMessage();
        }
    }
} 