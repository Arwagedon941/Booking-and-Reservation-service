package com.example.platform.dataprocessor.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BookingNotificationListener {
    
    private static final Logger log = LoggerFactory.getLogger(BookingNotificationListener.class);
    
    @RabbitListener(queues = "${app.queue:booking-notifications}")
    public void handleBookingNotification(Map<String, Object> message) {
        log.info("Received booking notification: {}", message);
        
        try {
            Long bookingId = Long.valueOf(message.get("bookingId").toString());
            String userId = message.get("userId").toString();
            Long resourceId = Long.valueOf(message.get("resourceId").toString());
            String status = message.get("status").toString();
            
            log.info("Processing booking notification - Booking ID: {}, User ID: {}, Resource ID: {}, Status: {}", 
                    bookingId, userId, resourceId, status);
            
            // Здесь можно добавить логику обработки:
            // - Отправка email уведомлений
            // - Обновление статистики
            // - Генерация отчетов
            // - Интеграция с внешними системами
            
            switch (status) {
                case "CONFIRMED":
                    log.info("Booking confirmed - sending confirmation notification to user {}", userId);
                    // Отправка подтверждения
                    break;
                case "CANCELLED":
                    log.info("Booking cancelled - sending cancellation notification to user {}", userId);
                    // Отправка уведомления об отмене
                    break;
                default:
                    log.info("Booking status changed to {}", status);
            }
            
        } catch (Exception e) {
            log.error("Error processing booking notification: {}", e.getMessage(), e);
        }
    }
}


