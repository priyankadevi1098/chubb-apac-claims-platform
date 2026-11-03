package com.chubb.apac.claims.messaging;

import com.chubb.apac.claims.domain.ClaimAuditEvent;
import com.chubb.apac.claims.repository.ClaimAuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ClaimEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClaimEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ClaimAuditEventRepository auditRepository;
    private final String topicName;

    public ClaimEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            ClaimAuditEventRepository auditRepository,
            @Value("${app.kafka.topic.claim-events}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.auditRepository = auditRepository;
        this.topicName = topicName;
    }

    public void publish(ClaimEventPayload payload) {
        try {
            kafkaTemplate.send(topicName, payload.claimReference(), payload)
                    .get(1500, TimeUnit.MILLISECONDS);
            log.info("[KAFKA-PRODUCER] Published {} for key {}", payload.eventType(), payload.claimReference());
        } catch (Exception ex) {
            log.warn("[KAFKA-FALLBACK] Broker unreachable ({}). Persisting audit event directly.", ex.getMessage());
            ClaimAuditEvent fallbackEvent = new ClaimAuditEvent(
                    payload.claimReference(),
                    payload.eventType(),
                    payload.previousStatus(),
                    payload.newStatus(),
                    payload.actor(),
                    payload.reserveExposureUsd(),
                    payload.summary() + " [Broker offline fallback]",
                    "LOCAL_IN_MEMORY_FALLBACK",
                    payload.occurredAt()
            );
            auditRepository.save(fallbackEvent);
        }
    }
}
