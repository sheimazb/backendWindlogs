package com.windlogs.notification.service;

import com.windlogs.notification.notification.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebsocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotification(String email, Notification notification) {
        log.info("Sending notification to " + email);

        // Send user-specific notification
        messagingTemplate.convertAndSendToUser(
                email,
                "/topic/notifications", // Match frontend USER_NOTIFICATION_TOPIC
                notification
        );

        // Optionally send global notification if needed
        messagingTemplate.convertAndSend(
                "/topic/notifications", // Match frontend NOTIFICATION_TOPIC
                notification
        );
    }
}
