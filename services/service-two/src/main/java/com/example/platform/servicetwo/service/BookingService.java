package com.example.platform.servicetwo.service;

import com.example.platform.servicetwo.dto.BookingDTO;
import com.example.platform.servicetwo.model.Booking;
import com.example.platform.servicetwo.model.BookingStatus;
import com.example.platform.servicetwo.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {
    
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    
    private final BookingRepository bookingRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;
    
    @Value("${services.resource-service.url:http://file-service:8081}")
    private String resourceServiceUrl;
    
    @Value("${app.queue:data-tasks}")
    private String queueName;
    
    public BookingService(BookingRepository bookingRepository,
                         RabbitTemplate rabbitTemplate,
                         RestTemplate restTemplate) {
        this.bookingRepository = bookingRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
    }
    
    @CacheEvict(value = "bookings", allEntries = true)
    public BookingDTO createBooking(BookingDTO dto, String userId, String bearerToken) {
        // Валидация времени
        if (dto.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start time must be in the future");
        }
        if (dto.getEndTime().isBefore(dto.getStartTime()) || dto.getEndTime().isEqual(dto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        // Проверяем доступность ресурса
        if (!isResourceAvailable(dto.getResourceId(), dto.getStartTime(), dto.getEndTime())) {
            throw new IllegalArgumentException("Resource is not available for the selected time period");
        }
        
        // Получаем информацию о ресурсе для расчета цены
        BigDecimal pricePerHour = getResourcePrice(dto.getResourceId(), bearerToken);
        if (pricePerHour == null) {
            throw new IllegalArgumentException("Resource not found");
        }
        
        // Рассчитываем общую стоимость
        long hours = Duration.between(dto.getStartTime(), dto.getEndTime()).toHours();
        if (hours <= 0) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        BigDecimal totalPrice = pricePerHour.multiply(BigDecimal.valueOf(hours));
        
        // Создаем бронирование
        Booking booking = new Booking();
        booking.setResourceId(dto.getResourceId());
        booking.setUserId(userId);
        booking.setStartTime(dto.getStartTime());
        booking.setEndTime(dto.getEndTime());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setNotes(dto.getNotes());
        
        Booking saved = bookingRepository.save(booking);
        
        // Отправляем уведомление в очередь
        sendBookingNotification(saved);
        
        return toDTO(saved);
    }
    
    @Cacheable(value = "bookings", key = "#a0")
    @Transactional(readOnly = true)
    public Optional<BookingDTO> getBookingById(Long id) {
        return bookingRepository.findById(id)
                .map(this::toDTO);
    }
    
    @Transactional(readOnly = true)
    public List<BookingDTO> getUserBookings(String userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<BookingDTO> getResourceBookings(Long resourceId) {
        return bookingRepository.findByResourceId(resourceId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @CacheEvict(value = "bookings", key = "#id", allEntries = true)
    public Optional<BookingDTO> cancelBooking(Long id, String userId) {
        Optional<Booking> bookingOpt = bookingRepository.findByIdAndUserId(id, userId);
        if (bookingOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Booking booking = bookingOpt.get();
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed booking");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);
        
        try {
            sendBookingNotification(updated);
        } catch (Exception e) {
            log.warn("Failed to send booking notification: {}", e.getMessage());
            // Не прерываем операцию, если уведомление не отправилось
        }
        
        return Optional.of(toDTO(updated));
    }
    
    @CacheEvict(value = "bookings", key = "#id", allEntries = true)
    public Optional<BookingDTO> cancelBookingAsAdmin(Long id) {
        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Booking booking = bookingOpt.get();
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed booking");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);
        
        try {
            sendBookingNotification(updated);
        } catch (Exception e) {
            log.warn("Failed to send booking notification: {}", e.getMessage());
            // Не прерываем операцию, если уведомление не отправилось
        }
        
        return Optional.of(toDTO(updated));
    }
    
    @Transactional(readOnly = true)
    public boolean isResourceAvailable(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> conflicts = bookingRepository.findConflictingBookings(resourceId, startTime, endTime);
        return conflicts.isEmpty();
    }
    
    private BigDecimal getResourcePrice(Long resourceId, String bearerToken) {
        try {
            String url = resourceServiceUrl + "/resources/" + resourceId;
            HttpHeaders headers = new HttpHeaders();
            if (bearerToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> resource = response.getBody();
                Object priceObj = resource.get("pricePerHour");
                if (priceObj instanceof Number) {
                    return BigDecimal.valueOf(((Number) priceObj).doubleValue());
                } else if (priceObj instanceof String) {
                    return new BigDecimal((String) priceObj);
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.error("Resource not found: {}", resourceId);
        } catch (Exception e) {
            log.error("Error fetching resource price: {}", e.getMessage());
        }
        return null;
    }
    
    private void sendBookingNotification(Booking booking) {
        Map<String, Object> message = new HashMap<>();
        message.put("bookingId", booking.getId());
        message.put("userId", booking.getUserId());
        message.put("resourceId", booking.getResourceId());
        message.put("startTime", booking.getStartTime().toString());
        message.put("endTime", booking.getEndTime().toString());
        message.put("status", booking.getStatus().toString());
        message.put("totalPrice", booking.getTotalPrice().toString());
        
        rabbitTemplate.convertAndSend(queueName, message);
        log.info("Sent booking notification: {}", message);
    }
    
    private BookingDTO toDTO(Booking booking) {
        return new BookingDTO(
                booking.getId(),
                booking.getResourceId(),
                booking.getUserId(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getNotes(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
    
}

