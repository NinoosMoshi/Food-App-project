package com.ninos.email_notification.services;

import com.ninos.email_notification.dtos.NotificationDTO;

public interface NotificationService {
    void sendEmail(NotificationDTO notificationDTO);
}
