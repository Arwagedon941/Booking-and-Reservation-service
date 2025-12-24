package com.example.platform.servicetwo.web;

import com.example.platform.servicetwo.dto.BookingDTO;
import com.example.platform.servicetwo.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    
    private final BookingService bookingService;
    
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }
    
    @PostMapping
    public ResponseEntity<BookingDTO> createBooking(@Valid @RequestBody BookingDTO dto,
                                                     @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        dto.setUserId(userId);
        
        try {
            BookingDTO created = bookingService.createBooking(dto, userId, jwt.getTokenValue());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBooking(@PathVariable Long id,
                                                @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        java.util.Optional<BookingDTO> bookingOpt = bookingService.getBookingById(id);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.<BookingDTO>notFound().build();
        }
        BookingDTO booking = bookingOpt.get();
        // Пользователь может видеть только свои бронирования (или админ все)
        if (booking.getUserId().equals(userId) || isAdmin(jwt)) {
            return ResponseEntity.ok(booking);
        }
        return ResponseEntity.<BookingDTO>status(HttpStatus.FORBIDDEN).build();
    }
    
    @GetMapping
    public ResponseEntity<List<BookingDTO>> getMyBookings(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<BookingDTO> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<BookingDTO>> getResourceBookings(@PathVariable Long resourceId) {
        List<BookingDTO> bookings = bookingService.getResourceBookings(resourceId);
        return ResponseEntity.ok(bookings);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable(name = "id") Long id,
                                             @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        boolean isAdmin = isAdmin(jwt);
        
        try {
            Optional<BookingDTO> result = isAdmin 
                ? bookingService.cancelBookingAsAdmin(id)
                : bookingService.cancelBooking(id, userId);
            
            if (result.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            // Бронирование уже отменено или завершено
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/availability")
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam(name = "resourceId") Long resourceId,
            @RequestParam(name = "startTime") String startTime,
            @RequestParam(name = "endTime") String endTime) {
        try {
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime);
            java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime);
            boolean available = bookingService.isResourceAvailable(resourceId, start, end);
            return ResponseEntity.ok(available);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private boolean isAdmin(Jwt jwt) {
        try {
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map<?, ?> map) {
                Object roles = map.get("roles");
                if (roles instanceof java.util.Collection<?> col) {
                    return col.contains("admin");
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга
        }
        return false;
    }
}

