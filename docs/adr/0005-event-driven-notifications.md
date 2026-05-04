# ADR-0005: Event-Driven Notifications via Spring ApplicationEventPublisher

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

The brief states: "How would you design this module such that adding new channels or
event triggers doesn't require re-engineering the core?"

The notification module must:

1. React to business events (loan disbursed, repayment received, loan overdue, etc.).
2. Be decoupled from the core loan lifecycle — adding a notification must not require
   changing `LoanAccount` or `RepaymentService`.
3. Support multiple channels (email, SMS, in-app) with easy extensibility.
4. Be stubbable — the case study uses a logging-based channel, but the design must
   make it clear how a real channel would be plugged in.

## Decision

Use **Spring's `ApplicationEventPublisher`** for in-process domain events. The
notification module is an `@EventListener` that reacts to events published by the
Loan Servicing context.

### Architecture:

```
LoanServicingService
  ├── publishes LoanDisbursedEvent via ApplicationEventPublisher
  │
  └── NotificationEventListener (@EventListener)
        ├── maps event → NotificationRecord
        ├── resolves template
        └── dispatches via NotificationChannel (interface)
              ├── LoggingNotificationChannel (stub — logs to console)
              ├── EmailNotificationChannel (future — sends via SMTP/SES)
              └── SmsNotificationChannel (future — sends via Twilio)
```

### Key design elements:

- **`NotificationChannel` interface** with a single method:
  `void send(NotificationRecord record)`. Each channel is a Spring bean. Multiple
  channels can be active simultaneously.
- **`NotificationEventListener`** is `@TransactionalEventListener(phase = AFTER_COMMIT)`
  — notifications are only dispatched after the business transaction commits. This
  prevents sending "loan disbursed" if the disbursement transaction rolls back.
- **`NotificationRecord`** is persisted before dispatch, ensuring we have an audit trail
  of every notification attempted.
- **Template resolution** is a simple enum-to-string mapping in v1. A real system would
  use a template engine (Thymeleaf, Mustache).

## Consequences

### Positive

- Zero coupling between core domain and notifications. `LoanAccount` does not know
  notifications exist.
- Adding a new channel = implementing `NotificationChannel` + registering as `@Component`.
  No changes to existing code.
- Adding a new event trigger = publishing a new event from the domain + adding a handler
  in the listener. No changes to the channel layer.
- `AFTER_COMMIT` guarantees no phantom notifications.
- In-process events have zero infrastructure overhead (no message broker to deploy).

### Negative

- In-process events are lost if the application crashes between commit and event delivery.
  Acceptable for a case study. A production system would use a transactional outbox
  pattern (see [06-trade-offs.md](../06-trade-offs.md)).
- `@TransactionalEventListener` executes in the same thread by default. If notification
  dispatch is slow (e.g., real SMTP), it blocks the response. Mitigated by the stub
  being instant. A production system would use `@Async` or a task queue.
- All events are in-process — no cross-service event bus. If the system evolves to
  microservices, the event publishing mechanism must be replaced with a message broker
  (Kafka, RabbitMQ). The `ApplicationEventPublisher` interface is narrow enough that this
  migration is localised to the adapter layer.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **Direct method calls** (e.g., `notificationService.sendLoanDisbursedNotification()` inside `disburseLoan()`) | Tight coupling. Adding a notification type requires modifying the service that publishes it. Violates Open/Closed Principle. |
| **Message broker (Kafka / RabbitMQ)** | Correct for production microservices, but adds infrastructure overhead (broker deployment, consumer groups, dead-letter queues) that is disproportionate for a monolithic case study. The in-process event design is broker-shaped — migrating to Kafka later means replacing the publisher adapter, not rewriting business logic. |
| **Spring Integration** | Powerful but adds a DSL layer and configuration complexity. Overkill for "publish event → listener reacts." |
| **Polling-based approach** (scheduled job queries for unsent notifications) | Adds latency. Events should trigger notifications, not wait for the next poll cycle. Polling is appropriate for overdue detection (batch), not for real-time notifications. |
