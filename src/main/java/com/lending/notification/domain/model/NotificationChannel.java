package com.lending.notification.domain.model;

public interface NotificationChannel {
    void send(NotificationRecord record);
}
