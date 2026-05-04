package com.lending.notification.adapter.in;

import com.lending.notification.adapter.out.LoggingNotificationChannel;
import com.lending.notification.adapter.out.NotificationRecordJpaEntity;
import com.lending.notification.adapter.out.NotificationRecordJpaRepository;
import com.lending.notification.domain.model.NotificationRecord;
import com.lending.notification.domain.model.NotificationStatus;
import com.lending.origination.domain.event.LoanApplicationRejectedEvent;
import com.lending.servicing.domain.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final LoggingNotificationChannel channel;
    private final NotificationRecordJpaRepository recordRepository;

    public NotificationEventListener(LoggingNotificationChannel channel,
                                      NotificationRecordJpaRepository recordRepository) {
        this.channel = channel;
        this.recordRepository = recordRepository;
    }

    @EventListener
    public void onLoanDisbursed(LoanDisbursedEvent event) {
        NotificationRecord record = new NotificationRecord(
                event.customerId(), event.loanAccountId(), "EMAIL",
                "LOAN_DISBURSED", "loan-disbursed",
                "Your loan has been disbursed. Amount: " + event.principalDisbursed());
        channel.send(record);
        persist(record);
    }

    @EventListener
    public void onRepaymentReceived(RepaymentReceivedEvent event) {
        NotificationRecord record = new NotificationRecord(
                event.customerId(), event.loanAccountId(), "EMAIL",
                "REPAYMENT_RECEIVED", "repayment-received",
                "Payment received — thank you. Amount: " + event.amount());
        channel.send(record);
        persist(record);
    }

    @EventListener
    public void onLoanOverdue(LoanOverdueEvent event) {
        NotificationRecord record = new NotificationRecord(
                event.customerId(), event.loanAccountId(), "EMAIL",
                "LOAN_OVERDUE", "loan-overdue",
                "Your loan is overdue — please pay immediately.");
        channel.send(record);
        persist(record);
    }

    @EventListener
    public void onLoanClosed(LoanClosedEvent event) {
        NotificationRecord record = new NotificationRecord(
                event.customerId(), event.loanAccountId(), "EMAIL",
                "LOAN_CLOSED", "loan-closed",
                "Congratulations — your loan is fully repaid.");
        channel.send(record);
        persist(record);
    }

    @EventListener
    public void onApplicationRejected(LoanApplicationRejectedEvent event) {
        NotificationRecord record = new NotificationRecord(
                event.customerId(), null, "EMAIL",
                "APPLICATION_REJECTED", "application-rejected",
                "Your loan application was not approved. Reason: " + event.reason());
        channel.send(record);
        persist(record);
    }

    private void persist(NotificationRecord record) {
        try {
            NotificationRecordJpaEntity entity = new NotificationRecordJpaEntity();
            entity.setId(record.getId());
            entity.setCustomerId(record.getCustomerId());
            entity.setLoanAccountId(record.getLoanAccountId());
            entity.setChannel(record.getChannel());
            entity.setEventType(record.getEventType());
            entity.setTemplateKey(record.getTemplateKey());
            entity.setPayload(record.getPayload());
            entity.setStatus(record.getStatus().name());
            entity.setCreatedAt(record.getCreatedAt());
            recordRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to persist notification record: {}", e.getMessage());
        }
    }
}
