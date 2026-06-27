package dev.skymetron.domain.observation;

import java.time.Instant;
import java.util.UUID;

/**
 * Base record for all SkyMetron domain events.
 *
 * <p>All events carry a unique id, timestamp, traceId (for correlation),
 * and the routing key used by the RabbitMQ exchange.
 */
public record SkyEvent(
        UUID eventId,
        Instant timestamp,
        UUID traceId,
        String eventType,
        String routingKey,
        String payload
) {
    public static SkyEvent of(String eventType, String routingKey, String payload, UUID traceId) {
        return new SkyEvent(UUID.randomUUID(), Instant.now(), traceId, eventType, routingKey, payload);
    }

    public static SkyEvent of(String eventType, String routingKey, String payload) {
        return of(eventType, routingKey, payload, UUID.randomUUID());
    }
}
