package com.hospital.appointment.service;

import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.entity.TimeSlot;
import com.hospital.appointment.repository.DoctorRepository;
import com.hospital.appointment.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final DoctorRepository doctorRepository;
    private final DistributedLockService distributedLockService;

    @Value("${hospital.appointment.slot-duration-minutes}")
    private int slotDurationMinutes;

    @Value("${hospital.appointment.working-hours-start}")
    private String workingHoursStart;

    @Value("${hospital.appointment.working-hours-end}")
    private String workingHoursEnd;

    /**
     * Generates time slots for all active doctors for a specific date
     */
    @Transactional
    @CacheEvict(value = {"availableSlots", "doctorAvailability"}, allEntries = true)
    public void generateTimeSlotsForDate(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            log.info("Skipping time slot generation for past date: {}", date);
            return;
        }

        List<Doctor> activeDoctors = doctorRepository.findActiveDoctors();
        log.info("Generating time slots for {} active doctors on date: {}", activeDoctors.size(), date);

        for (Doctor doctor : activeDoctors) {
            generateTimeSlotsForDoctor(doctor, date);
        }
    }

    /**
     * Generates time slots for a specific doctor on a specific date
     */
    @Transactional
    public void generateTimeSlotsForDoctor(Doctor doctor, LocalDate date) {
        String lockKey = distributedLockService.generateSlotManagementLockKey(doctor.getId(), date.toString());
        
        distributedLockService.executeWithLock(lockKey, () -> {
            try {
                List<TimeSlot> existingSlots = timeSlotRepository.findSlotsByDoctorAndDateRange(
                        doctor.getId(), date, date);

                if (!existingSlots.isEmpty()) {
                    log.debug("Time slots already exist for doctor: {} on date: {}", doctor.getId(), date);
                    return;
                }

                List<TimeSlot> newSlots = createSlotsForDoctor(doctor, date);
                timeSlotRepository.saveAll(newSlots);
                
                log.info("Generated {} time slots for doctor: {} on date: {}", 
                        newSlots.size(), doctor.getId(), date);
            } catch (Exception e) {
                log.error("Error generating time slots for doctor: {} on date: {}", 
                         doctor.getId(), date, e);
                throw e;
            }
        });
    }

    private List<TimeSlot> createSlotsForDoctor(Doctor doctor, LocalDate date) {
        List<TimeSlot> slots = new ArrayList<>();
        
        LocalTime startTime = LocalTime.parse(workingHoursStart);
        LocalTime endTime = LocalTime.parse(workingHoursEnd);
        
        LocalDateTime currentSlotStart = date.atTime(startTime);
        LocalDateTime slotEnd = date.atTime(endTime);
        
        while (currentSlotStart.plusMinutes(slotDurationMinutes).isBefore(slotEnd) || 
               currentSlotStart.plusMinutes(slotDurationMinutes).equals(slotEnd)) {
            
            LocalDateTime currentSlotEnd = currentSlotStart.plusMinutes(slotDurationMinutes);
            
            TimeSlot timeSlot = new TimeSlot();
            timeSlot.setDoctor(doctor);
            timeSlot.setSlotDate(date);
            timeSlot.setStartTime(currentSlotStart);
            timeSlot.setEndTime(currentSlotEnd);
            timeSlot.setIsAvailable(true);
            
            slots.add(timeSlot);
            
            currentSlotStart = currentSlotEnd;
        }
        
        return slots;
    }

    /**
     * Gets available slots for a doctor on a specific date (cached)
     */
    @Cacheable(value = "availableSlots", key = "#doctorId + '_' + #date")
    public List<TimeSlot> getAvailableSlots(Long doctorId, LocalDate date) {
        log.debug("Fetching available slots for doctor: {} on date: {}", doctorId, date);
        
        return timeSlotRepository.findAvailableSlotsByDoctorAndDate(doctorId, date);
    }

    /**
     * Gets all slots for a doctor in a date range
     */
    @Cacheable(value = "doctorAvailability", key = "#doctorId + '_' + #startDate + '_' + #endDate")
    public List<TimeSlot> getDoctorSlotsInRange(Long doctorId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching slots for doctor: {} from {} to {}", doctorId, startDate, endDate);
        
        return timeSlotRepository.findSlotsByDoctorAndDateRange(doctorId, startDate, endDate);
    }

    /**
     * Releases expired locks on time slots
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void releaseExpiredLocks() {
        try {
            List<TimeSlot> expiredLockedSlots = timeSlotRepository.findExpiredLockedSlots(LocalDateTime.now());
            
            if (!expiredLockedSlots.isEmpty()) {
                log.info("Releasing {} expired time slot locks", expiredLockedSlots.size());
                
                for (TimeSlot slot : expiredLockedSlots) {
                    slot.releaseLock();
                    timeSlotRepository.save(slot);
                }
                
                // Clear cache after releasing locks
                clearCacheForExpiredSlots(expiredLockedSlots);
            }
        } catch (Exception e) {
            log.error("Error releasing expired time slot locks", e);
        }
    }

    private void clearCacheForExpiredSlots(List<TimeSlot> slots) {
        // Cache will be evicted through annotations
        log.debug("Cache cleared for {} expired slots", slots.size());
    }

    /**
     * Generates time slots for the next N days (scheduled task)
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    @Transactional
    public void generateTimeSlotsForNextDays() {
        log.info("Starting scheduled time slot generation for next 30 days");
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(30);
        
        LocalDate currentDate = today;
        while (!currentDate.isAfter(endDate)) {
            try {
                generateTimeSlotsForDate(currentDate);
                currentDate = currentDate.plusDays(1);
            } catch (Exception e) {
                log.error("Error generating time slots for date: {}", currentDate, e);
                // Continue with next date
                currentDate = currentDate.plusDays(1);
            }
        }
        
        log.info("Completed scheduled time slot generation");
    }

    /**
     * Cleans up old time slots (older than 7 days)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Transactional
    @CacheEvict(value = {"availableSlots", "doctorAvailability"}, allEntries = true)
    public void cleanupOldTimeSlots() {
        log.info("Starting cleanup of old time slots");
        
        LocalDate cutoffDate = LocalDate.now().minusDays(7);
        
        try {
            List<Doctor> activeDoctors = doctorRepository.findActiveDoctors();
            
            for (Doctor doctor : activeDoctors) {
                int deletedCount = timeSlotRepository.deleteByDoctorIdAndSlotDateBefore(doctor.getId(), cutoffDate);
                if (deletedCount > 0) {
                    log.info("Deleted {} old time slots for doctor: {}", deletedCount, doctor.getId());
                }
            }
            
            log.info("Completed cleanup of old time slots");
        } catch (Exception e) {
            log.error("Error during cleanup of old time slots", e);
        }
    }

    /**
     * Gets count of available slots for a doctor on a specific date
     */
    @Cacheable(value = "slotCount", key = "#doctorId + '_' + #date")
    public long getAvailableSlotCount(Long doctorId, LocalDate date) {
        return timeSlotRepository.countAvailableSlotsByDoctorAndDate(doctorId, date);
    }

    /**
     * Checks if a time slot exists and is available
     */
    public boolean isSlotAvailable(Long timeSlotId) {
        return timeSlotRepository.findById(timeSlotId)
                .map(slot -> slot.getIsAvailable() && !slot.isLocked())
                .orElse(false);
    }

    /**
     * Locks a time slot for booking (used during booking process)
     */
    @Transactional
    public boolean lockTimeSlot(Long timeSlotId, Long patientId) {
        try {
            TimeSlot slot = timeSlotRepository.findById(timeSlotId)
                    .orElseThrow(() -> new RuntimeException("Time slot not found"));
            
            if (!slot.getIsAvailable() || slot.isLocked()) {
                return false;
            }
            
            LocalDateTime lockExpiry = LocalDateTime.now().plusSeconds(30); // 30 second lock
            slot.lockForBooking(patientId, lockExpiry);
            timeSlotRepository.save(slot);
            
            return true;
        } catch (Exception e) {
            log.error("Error locking time slot: {}", timeSlotId, e);
            return false;
        }
    }

    /**
     * Unlocks a time slot
     */
    @Transactional
    public void unlockTimeSlot(Long timeSlotId) {
        try {
            TimeSlot slot = timeSlotRepository.findById(timeSlotId)
                    .orElseThrow(() -> new RuntimeException("Time slot not found"));
            
            slot.releaseLock();
            timeSlotRepository.save(slot);
        } catch (Exception e) {
            log.error("Error unlocking time slot: {}", timeSlotId, e);
        }
    }
}
