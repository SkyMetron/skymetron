package dev.skymetron.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.skymetron.domain.observation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to the RabbitMQ topic exchange as JSON strings.
 *
 * <p>All events are published with a routing key matching their type
 * (e.g. "agent.invoked", "memory.stored"). The trace queue binds with "#"
 * to receive all events.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishAgentInvoked(AgentInvokedEvent event) {
        publish(AgentInvokedEvent.ROUTING_KEY, event);
    }

    public void publishMemoryStored(MemoryStoredEvent event) {
        publish(MemoryStoredEvent.ROUTING_KEY, event);
    }

    public void publishMemoryConsolidated(MemoryConsolidatedEvent event) {
        publish(MemoryConsolidatedEvent.ROUTING_KEY, event);
    }

    public void publishToolExecuted(ToolExecutedEvent event) {
        publish(ToolExecutedEvent.ROUTING_KEY, event);
    }

    public void publishResearchCompleted(ResearchCompletedEvent event) {
        publish(ResearchCompletedEvent.ROUTING_KEY, event);
    }

    public void publishProviderFallback(ProviderFallbackEvent event) {
        publish(ProviderFallbackEvent.ROUTING_KEY, event);
    }

    public void publishQaTestExecuted(QaTestExecutedEvent event) {
        publish(QaTestExecutedEvent.ROUTING_KEY, event);
    }

    public void publishSecurityAnalysis(SecurityAnalysisEvent event) {
        publish(SecurityAnalysisEvent.ROUTING_KEY, event);
    }

    public void publishSwarmCompleted(SwarmCompletedEvent event) {
        publish(SwarmCompletedEvent.ROUTING_KEY, event);
    }

    private void publish(String routingKey, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            var node = objectMapper.readTree(json);
            var enhanced = ((com.fasterxml.jackson.databind.node.ObjectNode) node);
            enhanced.put("eventType", routingKey);
            enhanced.put("routingKey", routingKey);
            String enhancedJson = objectMapper.writeValueAsString(node);
            rabbitTemplate.convertAndSend(
                    dev.skymetron.infrastructure.messaging.rabbitmq.RabbitMqConfig.SKY_EXCHANGE,
                    routingKey,
                    enhancedJson);
            log.debug("Published event: {} -> {}", event.getClass().getSimpleName(), routingKey);
        } catch (Exception e) {
            log.warn("Failed to publish event {}: {}", event.getClass().getSimpleName(), e.getMessage());
        }
    }
}
