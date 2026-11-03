package com.chubb.apac.claims.messaging;

import com.chubb.apac.claims.domain.ClaimAuditEvent;
import com.chubb.apac.claims.repository.ClaimAuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ClaimEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClaimEventConsumer.class);
    private final ClaimAuditEventRepository auditRepository;

    public ClaimEventConsumer(ClaimAuditEventRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.claim-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "true"
    )
    public void consumeClaimLifecycleEvent(ClaimEventPayload payload) {
        log.info("[KAFKA-CONSUMER] Received event {} for claim {} -> status {}",
                payload.eventType(), payload.claimReference(), payload.newStatus());

        ClaimAuditEvent auditEvent = new ClaimAuditEvent(
                payload.claimReference(),
                payload.eventType(),
                payload.previousStatus(),
                payload.newStatus(),
                payload.actor(),
                payload.reserveExposureUsd(),
                payload.summary(),
                "KAFKA_EVENT_STREAM",
                payload.occurredAt()
        );
        auditRepository.save(auditEvent);
    }
}
