package com.hospital.appointment.service;

import com.hospital.appointment.dto.AppointmentRequest;
import com.hospital.appointment.dto.AppointmentResponse;
import com.hospital.appointment.dto.NotificationEvent;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.AppointmentStatus;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.entity.Patient;
import com.hospital.appointment.entity.TimeSlot;
import com.hospital.appointment.exception.AppointmentException;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.repository.DoctorRepository;
import com.hospital.appointment.repository.PatientRepository;
import com.hospital.appointment.repository.TimeSlotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final DistributedLockService distributedLockService;
    private final NotificationService notificationService;
    private final TimeSlotService timeSlotService;

    @Value("${hospital.appointment.max-booking-days-advance}")
    private int maxBookingDaysAdvance;

    /**
     * Books an appointment with distributed locking to prevent double-booking
     */
    @Transactional
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
    @CircuitBreaker(name = "appointmentService", fallbackMethod = "bookingFallback")
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        log.info("Attempting to book appointment for patient: {}, doctor: {}, slot: {}", 
                request.getPatientId(), request.getDoctorId(), request.getTimeSlotId());

        // Handle idempotency
        if (request.getIdempotencyKey() != null) {
            return handleIdempotentRequest(request);
        }

        // Validate booking constraints
        validateBookingRequest(request);

        // Generate distributed lock key
        String lockKey = distributedLockService.generateBookingLockKey(request.getDoctorId(), request.getTimeSlotId());

        return distributedLockService.executeWithLock(lockKey, () -> {
            try {
                return performBooking(request);
            } catch (Exception e) {
                log.error("Error during appointment booking for patient: {}, doctor: {}", 
                         request.getPatientId(), request.getDoctorId(), e);
                throw new AppointmentException("Failed to book appointment: " + e.getMessage());
            }
        });
    }

    private AppointmentResponse performBooking(AppointmentRequest request) {
        // Fetch entities
        Patient patient = getPatient(request.getPatientId());
        Doctor doctor = getDoctor(request.getDoctorId());
        TimeSlot timeSlot = getTimeSlot(request.getTimeSlotId());

        // Validate slot availability again within lock
        validateSlotAvailability(timeSlot, doctor.getId());

        // Check for existing appointment (idempotency check)
        checkExistingAppointment(patient.getId(), doctor.getId(), timeSlot.getStartTime());

        // Create appointment
        Appointment appointment = createAppointment(patient, doctor, timeSlot, request);

        // Update slot availability
        timeSlot.setIsAvailable(false);
        timeSlotRepository.save(timeSlot);

        // Save appointment
        appointment = appointmentRepository.save(appointment);

        // Send notification asynchronously
        sendBookingNotification(appointment, patient, doctor, timeSlot);

        // Clear cache
        clearRelatedCaches(doctor.getId(), timeSlot.getSlotDate());

        log.info("Successfully booked appointment: {} for patient: {}, doctor: {}", 
                appointment.getId(), patient.getId(), doctor.getId());

        return buildAppointmentResponse(appointment);
    }

    private AppointmentResponse handleIdempotentRequest(AppointmentRequest request) {
        return appointmentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(this::buildAppointmentResponse)
                .orElseGet(() -> {
                    AppointmentRequest requestWithIdempotency = AppointmentRequest.builder()
                            .patientId(request.getPatientId())
                            .doctorId(request.getDoctorId())
                            .appointmentDate(request.getAppointmentDate())
                            .timeSlotId(request.getTimeSlotId())
                            .notes(request.getNotes())
                            .idempotencyKey(request.getIdempotencyKey())
                            .build();
                    return bookAppointment(requestWithIdempotency);
                });
    }

    private void validateBookingRequest(AppointmentRequest request) {
        // Validate booking date is not too far in advance
        LocalDate maxAllowedDate = LocalDate.now().plusDays(maxBookingDaysAdvance);
        if (request.getAppointmentDate().isAfter(maxAllowedDate)) {
            throw new AppointmentException("Appointments can only be booked " + maxBookingDaysAdvance + " days in advance");
        }

        // Validate appointment date is not in the past
        if (request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new AppointmentException("Cannot book appointments in the past");
        }
    }

    private Patient getPatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new AppointmentException("Patient not found with ID: " + patientId));
    }

    private Doctor getDoctor(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new AppointmentException("Doctor not found with ID: " + doctorId));
    }

    private TimeSlot getTimeSlot(Long timeSlotId) {
        return timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new AppointmentException("Time slot not found with ID: " + timeSlotId));
    }

    private void validateSlotAvailability(TimeSlot timeSlot, Long doctorId) {
        if (!timeSlot.getIsAvailable()) {
            throw new AppointmentException("Time slot is not available");
        }

        if (!timeSlot.getDoctor().getId().equals(doctorId)) {
            throw new AppointmentException("Time slot does not belong to the specified doctor");
        }

        if (timeSlot.isLocked()) {
            throw new AppointmentException("Time slot is currently being booked by another user");
        }

        if (timeSlot.getStartTime().isBefore(LocalDateTime.now())) {
            throw new AppointmentException("Cannot book time slots in the past");
        }
    }

    private void checkExistingAppointment(Long patientId, Long doctorId, LocalDateTime appointmentTime) {
        appointmentRepository.findActiveAppointment(patientId, doctorId, appointmentTime)
                .ifPresent(existing -> {
                    throw new AppointmentException("Patient already has an active appointment at this time");
                });
    }

    private Appointment createAppointment(Patient patient, Doctor doctor, TimeSlot timeSlot, AppointmentRequest request) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setTimeSlot(timeSlot);
        appointment.setAppointmentDateTime(timeSlot.getStartTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setNotes(request.getNotes());
        
        // Generate idempotency key if not provided
        String idempotencyKey = request.getIdempotencyKey() != null ? 
                request.getIdempotencyKey() : 
                UUID.randomUUID().toString();
        appointment.setIdempotencyKey(idempotencyKey);
        
        return appointment;
    }

    private void sendBookingNotification(Appointment appointment, Patient patient, Doctor doctor, TimeSlot timeSlot) {
        try {
            NotificationEvent event = NotificationEvent.appointmentBooked(
                    appointment.getId(),
                    patient.getId(),
                    patient.getFullName(),
                    patient.getEmail(),
                    patient.getPhone(),
                    doctor.getId(),
                    doctor.getFullName(),
                    doctor.getSpecialization(),
                    appointment.getAppointmentDateTime()
            );
            notificationService.sendAppointmentBookedNotification(event);
        } catch (Exception e) {
            log.error("Failed to send booking notification for appointment: {}", appointment.getId(), e);
            // Don't fail the booking if notification fails
        }
    }

    private void clearRelatedCaches(Long doctorId, LocalDate slotDate) {
        // Cache eviction will be handled by annotations
        log.debug("Cache eviction triggered for doctor: {}, date: {}", doctorId, slotDate);
    }

    private AppointmentResponse buildAppointmentResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .appointmentId(appointment.getId())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getFullName())
                .patientEmail(appointment.getPatient().getEmail())
                .patientPhone(appointment.getPatient().getPhone())
                .doctorId(appointment.getDoctor().getId())
                .doctorName(appointment.getDoctor().getFullName())
                .doctorSpecialization(appointment.getDoctor().getSpecialization())
                .timeSlotId(appointment.getTimeSlot().getId())
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .status(appointment.getStatus().name())
                .notes(appointment.getNotes())
                .message("Appointment booked successfully")
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    /**
     * Cancels an appointment
     */
    @Transactional
    @CacheEvict(value = {"availableSlots", "doctorAvailability"}, allEntries = true)
    public AppointmentResponse cancelAppointment(Long appointmentId, String cancellationReason) {
        log.info("Attempting to cancel appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentException("Appointment not found with ID: " + appointmentId));

        if (appointment.isCancelled()) {
            throw new AppointmentException("Appointment is already cancelled");
        }

        if (appointment.getAppointmentDateTime().isBefore(LocalDateTime.now())) {
            throw new AppointmentException("Cannot cancel past appointments");
        }

        // Update appointment status
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(cancellationReason);
        appointmentRepository.save(appointment);

        // Release time slot
        TimeSlot timeSlot = appointment.getTimeSlot();
        timeSlot.setIsAvailable(true);
        timeSlot.setBookedByPatientId(null);
        timeSlot.setBookingLockExpiry(null);
        timeSlotRepository.save(timeSlot);

        // Send cancellation notification
        sendCancellationNotification(appointment);

        log.info("Successfully cancelled appointment: {}", appointmentId);

        return AppointmentResponse.builder()
                .appointmentId(appointment.getId())
                .status(appointment.getStatus().name())
                .message("Appointment cancelled successfully")
                .build();
    }

    private void sendCancellationNotification(Appointment appointment) {
        try {
            NotificationEvent event = NotificationEvent.appointmentCancelled(
                    appointment.getId(),
                    appointment.getPatient().getId(),
                    appointment.getPatient().getFullName(),
                    appointment.getPatient().getEmail(),
                    appointment.getPatient().getPhone(),
                    appointment.getDoctor().getId(),
                    appointment.getDoctor().getFullName(),
                    appointment.getDoctor().getSpecialization(),
                    appointment.getAppointmentDateTime(),
                    appointment.getCancellationReason()
            );
            notificationService.sendAppointmentCancelledNotification(event);
        } catch (Exception e) {
            log.error("Failed to send cancellation notification for appointment: {}", appointment.getId(), e);
        }
    }

    /**
     * Gets available slots for a doctor on a specific date
     */
    @Cacheable(value = "availableSlots", key = "#doctorId + '_' + #date")
    public List<TimeSlot> getAvailableSlots(Long doctorId, LocalDate date) {
        log.info("Fetching available slots for doctor: {} on date: {}", doctorId, date);
        
        validateDoctorExists(doctorId);
        
        if (date.isBefore(LocalDate.now())) {
            throw new AppointmentException("Cannot view slots for past dates");
        }

        return timeSlotRepository.findAvailableSlotsByDoctorAndDate(doctorId, date);
    }

    private void validateDoctorExists(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new AppointmentException("Doctor not found with ID: " + doctorId);
        }
    }

    // Fallback methods for circuit breaker and rate limiter
    public AppointmentResponse bookingFallback(AppointmentRequest request, Exception ex) {
        log.error("Circuit breaker triggered for appointment booking", ex);
        throw new AppointmentException("Booking service is temporarily unavailable. Please try again later.");
    }

    public AppointmentResponse rateLimitFallback(AppointmentRequest request, Exception ex) {
        log.error("Rate limit exceeded for appointment booking", ex);
        throw new AppointmentException("Too many booking requests. Please try again later.");
    }
}
