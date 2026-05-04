package com.lending.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public class NotificationRecord {

    private UUID id;
    private UUID customerId;
    private UUID loanAccountId;
    private String channel;
    private String eventType;
    private String templateKey;
    private String payload;
    private NotificationStatus status;
    private Instant createdAt;

    public NotificationRecord() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.status = NotificationStatus.PENDING;
    }

    public NotificationRecord(UUID customerId, UUID loanAccountId, String channel,
                               String eventType, String templateKey, String payload) {
        this();
        this.customerId = customerId;
        this.loanAccountId = loanAccountId;
        this.channel = channel;
        this.eventType = eventType;
        this.templateKey = templateKey;
        this.payload = payload;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID v) { this.customerId = v; }
    public UUID getLoanAccountId() { return loanAccountId; }
    public void setLoanAccountId(UUID v) { this.loanAccountId = v; }
    public String getChannel() { return channel; }
    public void setChannel(String v) { this.channel = v; }
    public String getEventType() { return eventType; }
    public void setEventType(String v) { this.eventType = v; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String v) { this.templateKey = v; }
    public String getPayload() { return payload; }
    public void setPayload(String v) { this.payload = v; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus v) { this.status = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
