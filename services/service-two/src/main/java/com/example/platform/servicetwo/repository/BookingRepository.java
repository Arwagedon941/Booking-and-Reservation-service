package com.example.platform.servicetwo.repository;

import com.example.platform.servicetwo.model.Booking;
import com.example.platform.servicetwo.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserId(String userId);
    
    List<Booking> findByResourceId(Long resourceId);
    
    List<Booking> findByUserIdAndStatus(String userId, BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.resourceId = :resourceId " +
           "AND b.status IN ('PENDING', 'CONFIRMED') " +
           "AND ((b.startTime <= :startTime AND b.endTime > :startTime) " +
           "OR (b.startTime < :endTime AND b.endTime >= :endTime) " +
           "OR (b.startTime >= :startTime AND b.endTime <= :endTime))")
    List<Booking> findConflictingBookings(@Param("resourceId") Long resourceId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT b FROM Booking b WHERE b.resourceId = :resourceId " +
           "AND b.status = 'CONFIRMED' " +
           "AND b.startTime >= :startTime " +
           "AND b.endTime <= :endTime")
    List<Booking> findConfirmedBookingsInRange(@Param("resourceId") Long resourceId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    Optional<Booking> findByIdAndUserId(Long id, String userId);
}







