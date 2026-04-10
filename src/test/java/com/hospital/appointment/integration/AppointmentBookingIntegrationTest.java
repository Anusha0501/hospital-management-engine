package com.hospital.appointment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hospital.appointment.dto.AppointmentRequest;
import com.hospital.appointment.entity.*;
import com.hospital.appointment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AppointmentBookingIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Patient patient;
    private Doctor doctor;
    private TimeSlot timeSlot;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper.registerModule(new JavaTimeModule());

        // Setup test data
        patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setEmail("john.doe@example.com");
        patient.setPhone("1234567890");
        patient = patientRepository.save(patient);

        doctor = new Doctor();
        doctor.setFirstName("Dr. Smith");
        doctor.setLastName("Johnson");
        doctor.setEmail("smith.johnson@hospital.com");
        doctor.setSpecialization("Cardiology");
        doctor.setIsActive(true);
        doctor = doctorRepository.save(doctor);

        timeSlot = new TimeSlot();
        timeSlot.setDoctor(doctor);
        timeSlot.setSlotDate(LocalDate.now().plusDays(1));
        timeSlot.setStartTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0));
        timeSlot.setEndTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(30));
        timeSlot.setIsAvailable(true);
        timeSlot = timeSlotRepository.save(timeSlot);
    }

    @Test
    void concurrentBooking_PreventsDoubleBooking() throws InterruptedException {
        int numberOfThreads = 10;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(patient.getId())
                .doctorId(doctor.getId())
                .appointmentDate(timeSlot.getSlotDate())
                .timeSlotId(timeSlot.getId())
                .notes("Concurrent test booking")
                .build();

        int[] successCount = {0};
        int[] failureCount = {0};

        // Submit multiple booking requests concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/appointments")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isCreated());
                    successCount[0]++;
                } catch (Exception e) {
                    failureCount[0]++;
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify that only one appointment was created
        long appointmentCount = appointmentRepository.count();
        assertEquals(1, appointmentCount, "Only one appointment should be created");

        // Verify the time slot is no longer available
        TimeSlot updatedSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        assertFalse(updatedSlot.getIsAvailable(), "Time slot should not be available after booking");

        // Verify that exactly one request succeeded and the rest failed
        assertEquals(1, successCount[0], "Exactly one booking should succeed");
        assertEquals(numberOfThreads - 1, failureCount[0], "All other bookings should fail");
    }

    @Test
    void bookingWorkflow_EndToEnd() throws Exception {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(patient.getId())
                .doctorId(doctor.getId())
                .appointmentDate(timeSlot.getSlotDate())
                .timeSlotId(timeSlot.getId())
                .notes("End-to-end test")
                .build();

        // Book appointment
        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.doctorId").value(doctor.getId()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        // Verify appointment was created
        assertEquals(1, appointmentRepository.count());

        // Verify time slot is no longer available
        TimeSlot updatedSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        assertFalse(updatedSlot.getIsAvailable());

        // Cancel appointment
        mockMvc.perform(put("/appointments/1/cancel")
                        .param("cancellationReason", "Test cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify appointment was cancelled
        Appointment cancelledAppointment = appointmentRepository.findById(1L).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, cancelledAppointment.getStatus());

        // Verify time slot is available again
        TimeSlot releasedSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        assertTrue(releasedSlot.getIsAvailable());
    }

    @Test
    void idempotencyKey_PreventsDuplicateBookings() throws Exception {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(patient.getId())
                .doctorId(doctor.getId())
                .appointmentDate(timeSlot.getSlotDate())
                .timeSlotId(timeSlot.getId())
                .notes("Idempotency test")
                .idempotencyKey("unique-key-123")
                .build();

        // First booking request
        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patient.getId()));

        // Second request with same idempotency key
        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patient.getId()));

        // Verify only one appointment was created
        assertEquals(1, appointmentRepository.count());
    }
}
