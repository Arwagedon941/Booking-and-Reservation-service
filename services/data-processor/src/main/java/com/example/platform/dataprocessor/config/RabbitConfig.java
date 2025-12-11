package com.example.platform.dataprocessor.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the required queue so the listener can start even if the broker is empty.
 */
@Configuration
public class RabbitConfig {

    @Value("${app.queue:booking-notifications}")
    private String queueName;

    @Bean
    public Queue dataTasksQueue() {
        // non-durable queue is fine for demo; switch to durable in real env
        return new Queue(queueName, false);
    }
}

