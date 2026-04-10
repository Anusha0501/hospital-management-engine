package com.hospital.appointment.repository;

import com.hospital.appointment.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TimeSlot t WHERE t.id = :id")
    Optional<TimeSlot> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT t FROM TimeSlot t WHERE " +
           "t.doctor.id = :doctorId AND " +
           "t.slotDate = :date AND " +
           "t.isAvailable = true " +
           "ORDER BY t.startTime")
    List<TimeSlot> findAvailableSlotsByDoctorAndDate(@Param("doctorId") Long doctorId, 
                                                     @Param("date") LocalDate date);
    
    @Query("SELECT t FROM TimeSlot t WHERE " +
           "t.doctor.id = :doctorId AND " +
           "t.slotDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.slotDate, t.startTime")
    List<TimeSlot> findSlotsByDoctorAndDateRange(@Param("doctorId") Long doctorId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t FROM TimeSlot t WHERE " +
           "t.isAvailable = true AND " +
           "t.slotDate >= :currentDate " +
           "ORDER BY t.slotDate, t.startTime")
    List<TimeSlot> findAllAvailableSlotsFrom(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT t FROM TimeSlot t WHERE " +
           "t.slotDate = :date AND " +
           "t.startTime <= :currentTime AND " +
           "t.endTime > :currentTime AND " +
           "t.isAvailable = true")
    List<TimeSlot> findCurrentlyAvailableSlots(@Param("date") LocalDate date,
                                              @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT COUNT(t) FROM TimeSlot t WHERE " +
           "t.doctor.id = :doctorId AND " +
           "t.slotDate = :date AND " +
           "t.isAvailable = true")
    long countAvailableSlotsByDoctorAndDate(@Param("doctorId") Long doctorId,
                                           @Param("date") LocalDate date);
    
    @Query("SELECT t FROM TimeSlot t WHERE " +
           "t.bookingLockExpiry IS NOT NULL AND " +
           "t.bookingLockExpiry < :currentTime")
    List<TimeSlot> findExpiredLockedSlots(@Param("currentTime") LocalDateTime currentTime);
    
    boolean existsByDoctorIdAndSlotDateAndStartTimeAndEndTime(Long doctorId,
                                                           LocalDate slotDate,
                                                           LocalDateTime startTime,
                                                           LocalDateTime endTime);
    
    void deleteByDoctorIdAndSlotDateBefore(Long doctorId, LocalDate date);
}
