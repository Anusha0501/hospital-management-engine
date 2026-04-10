package com.hospital.appointment.service;

import com.hospital.appointment.dto.AppointmentRequest;
import com.hospital.appointment.dto.AppointmentResponse;
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
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private DistributedLockService distributedLockService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TimeSlotService timeSlotService;

    @InjectMocks
    private AppointmentService appointmentService;

    private Patient patient;
    private Doctor doctor;
    private TimeSlot timeSlot;
    private AppointmentRequest appointmentRequest;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setEmail("john.doe@example.com");
        patient.setPhone("1234567890");

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setFirstName("Dr. Smith");
        doctor.setLastName("Johnson");
        doctor.setEmail("smith.johnson@hospital.com");
        doctor.setSpecialization("Cardiology");

        timeSlot = new TimeSlot();
        timeSlot.setId(1L);
        timeSlot.setDoctor(doctor);
        timeSlot.setSlotDate(LocalDate.now().plusDays(1));
        timeSlot.setStartTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0));
        timeSlot.setEndTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(30));
        timeSlot.setIsAvailable(true);

        appointmentRequest = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(1L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .timeSlotId(1L)
                .notes("Regular checkup")
                .build();
    }

    @Test
    void bookAppointment_Success() {
        // Given
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(appointmentRepository.findActiveAppointment(anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                    Appointment appointment = invocation.getArgument(0);
                    appointment.setId(1L);
                    return appointment;
                });
        when(distributedLockService.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
                    Runnable task = invocation.getArgument(1);
                    task.run();
                    return true;
                });

        // When
        AppointmentResponse response = appointmentService.bookAppointment(appointmentRequest);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getAppointmentId());
        assertEquals(1L, response.getPatientId());
        assertEquals("John Doe", response.getPatientName());
        assertEquals("Dr. Smith Johnson", response.getDoctorName());
        assertEquals("Cardiology", response.getDoctorSpecialization());
        assertEquals("SCHEDULED", response.getStatus());

        verify(patientRepository).findById(1L);
        verify(doctorRepository).findById(1L);
        verify(timeSlotRepository).findById(1L);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(notificationService).sendAppointmentBookedNotification(any());
    }

    @Test
    void bookAppointment_PatientNotFound_ThrowsException() {
        // Given
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.bookAppointment(appointmentRequest));

        assertEquals("Patient not found with ID: 1", exception.getMessage());
        verify(patientRepository).findById(1L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void bookAppointment_DoctorNotFound_ThrowsException() {
        // Given
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.bookAppointment(appointmentRequest));

        assertEquals("Doctor not found with ID: 1", exception.getMessage());
        verify(doctorRepository).findById(1L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void bookAppointment_TimeSlotNotFound_ThrowsException() {
        // Given
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.bookAppointment(appointmentRequest));

        assertEquals("Time slot not found with ID: 1", exception.getMessage());
        verify(timeSlotRepository).findById(1L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void bookAppointment_SlotNotAvailable_ThrowsException() {
        // Given
        timeSlot.setIsAvailable(false);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(distributedLockService.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
                    Runnable task = invocation.getArgument(1);
                    task.run();
                    return true;
                });

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.bookAppointment(appointmentRequest));

        assertEquals("Time slot is not available", exception.getMessage());
        verifyNoInteractions(notificationService);
    }

    @Test
    void bookAppointment_ExistingAppointment_ThrowsException() {
        // Given
        Appointment existingAppointment = new Appointment();
        existingAppointment.setId(2L);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(appointmentRepository.findActiveAppointment(anyLong(), anyLong(), any()))
                .thenReturn(Optional.of(existingAppointment));
        when(distributedLockService.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
                    Runnable task = invocation.getArgument(1);
                    task.run();
                    return true;
                });

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.bookAppointment(appointmentRequest));

        assertEquals("Patient already has an active appointment at this time", exception.getMessage());
        verifyNoInteractions(notificationService);
    }

    @Test
    void bookAppointment_WithIdempotencyKey_ReturnsExistingAppointment() {
        // Given
        appointmentRequest.setIdempotencyKey("unique-key-123");
        
        Appointment existingAppointment = new Appointment();
        existingAppointment.setId(1L);
        existingAppointment.setPatient(patient);
        existingAppointment.setDoctor(doctor);
        existingAppointment.setTimeSlot(timeSlot);
        existingAppointment.setAppointmentDateTime(timeSlot.getStartTime());
        existingAppointment.setStatus(AppointmentStatus.SCHEDULED);
        existingAppointment.setIdempotencyKey("unique-key-123");

        when(appointmentRepository.findByIdempotencyKey("unique-key-123"))
                .thenReturn(Optional.of(existingAppointment));

        // When
        AppointmentResponse response = appointmentService.bookAppointment(appointmentRequest);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getAppointmentId());
        assertEquals("unique-key-123", response.getIdempotencyKey());

        verify(appointmentRepository).findByIdempotencyKey("unique-key-123");
        verifyNoInteractions(distributedLockService);
        verifyNoInteractions(notificationService);
    }

    @Test
    void bookAppointment_LockAcquisitionFails_ThrowsException() {
        // Given
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(distributedLockService.executeWithLock(anyString(), any())).thenReturn(false);

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.bookAppointment(appointmentRequest));

        assertEquals("Failed to book appointment: Could not acquire distributed lock", exception.getMessage());
        verify(distributedLockService).executeWithLock(anyString(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void cancelAppointment_Success() {
        // Given
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setTimeSlot(timeSlot);
        appointment.setAppointmentDateTime(LocalDateTime.now().plusHours(1));
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        // When
        AppointmentResponse response = appointmentService.cancelAppointment(1L, "Patient requested cancellation");

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getAppointmentId());
        assertEquals("CANCELLED", response.getStatus());
        assertEquals("Appointment cancelled successfully", response.getMessage());

        verify(appointmentRepository).findById(1L);
        verify(appointmentRepository).save(appointment);
        verify(notificationService).sendAppointmentCancelledNotification(any());
    }

    @Test
    void cancelAppointment_AppointmentNotFound_ThrowsException() {
        // Given
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.cancelAppointment(1L, "Test cancellation"));

        assertEquals("Appointment not found with ID: 1", exception.getMessage());
        verify(appointmentRepository).findById(1L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void cancelAppointment_AlreadyCancelled_ThrowsException() {
        // Given
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.cancelAppointment(1L, "Test cancellation"));

        assertEquals("Appointment is already cancelled", exception.getMessage());
        verify(appointmentRepository).findById(1L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void cancelAppointment_PastAppointment_ThrowsException() {
        // Given
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setAppointmentDateTime(LocalDateTime.now().minusHours(1));
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.cancelAppointment(1L, "Test cancellation"));

        assertEquals("Cannot cancel past appointments", exception.getMessage());
        verify(appointmentRepository).findById(1L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void getAvailableSlots_Success() {
        // Given
        LocalDate date = LocalDate.now().plusDays(1);
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(timeSlotRepository.findAvailableSlotsByDoctorAndDate(1L, date))
                .thenReturn(java.util.Arrays.asList(timeSlot));

        // When
        var slots = appointmentService.getAvailableSlots(1L, date);

        // Then
        assertNotNull(slots);
        assertEquals(1, slots.size());
        assertEquals(timeSlot, slots.get(0));

        verify(doctorRepository).existsById(1L);
        verify(timeSlotRepository).findAvailableSlotsByDoctorAndDate(1L, date);
    }

    @Test
    void getAvailableSlots_DoctorNotFound_ThrowsException() {
        // Given
        LocalDate date = LocalDate.now().plusDays(1);
        when(doctorRepository.existsById(1L)).thenReturn(false);

        // When & Then
        AppointmentException exception = assertThrows(AppointmentException.class,
                () -> appointmentService.getAvailableSlots(1L, date));

        assertEquals("Doctor not found with ID: 1", exception.getMessage());
        verify(doctorRepository).existsById(1L);
        verifyNoInteractions(timeSlotRepository);
    }

    @Test
    void bookingFallback_RateLimitExceeded() {
        // Given
        RequestNotPermitted exception = new RequestNotPermitted("Rate limit exceeded");

        // When
        AppointmentException result = appointmentService.bookingFallback(appointmentRequest, exception);

        // Then
        assertNotNull(result);
        assertTrue(result.getMessage().contains("temporarily unavailable"));
    }
}
