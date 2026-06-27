package dev.skymetron.infrastructure.messaging.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String SKY_EXCHANGE = "sky.exchange";
    public static final String TRACE_QUEUE = "sky.trace";
    public static final String TRACE_DLQ = "sky.trace.dlq";

    @Bean
    public TopicExchange skyExchange() {
        return new TopicExchange(SKY_EXCHANGE, true, false);
    }

    @Bean
    public Queue traceQueue() {
        return QueueBuilder.durable(TRACE_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", TRACE_DLQ)
                .build();
    }

    @Bean
    public Queue traceDeadLetterQueue() {
        return QueueBuilder.durable(TRACE_DLQ).build();
    }

    @Bean
    public Declarables traceBindings() {
        return new Declarables(
                BindingBuilder.bind(traceQueue())
                        .to(skyExchange())
                        .with("#")
        );
    }
}
