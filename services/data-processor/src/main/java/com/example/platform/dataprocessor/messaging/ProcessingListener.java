package com.example.platform.dataprocessor.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProcessingListener {

    private static final Logger log = LoggerFactory.getLogger(ProcessingListener.class);

    @RabbitListener(queues = "${app.queue:booking-notifications}")
    public void handleTask(Object payload) {
        log.info("Processing notification: {}", payload);
        // Legacy handler for backward compatibility
    }
}

