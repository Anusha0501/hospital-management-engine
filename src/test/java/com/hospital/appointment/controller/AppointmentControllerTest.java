package com.hospital.appointment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hospital.appointment.dto.AppointmentRequest;
import com.hospital.appointment.dto.AppointmentResponse;
import com.hospital.appointment.entity.TimeSlot;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.service.TimeSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private TimeSlotService timeSlotService;

    @Autowired
    private ObjectMapper objectMapper;

    private AppointmentRequest appointmentRequest;
    private AppointmentResponse appointmentResponse;
    private TimeSlot timeSlot;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());

        appointmentRequest = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(1L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .timeSlotId(1L)
                .notes("Regular checkup")
                .build();

        appointmentResponse = AppointmentResponse.builder()
                .appointmentId(1L)
                .patientId(1L)
                .patientName("John Doe")
                .patientEmail("john.doe@example.com")
                .patientPhone("1234567890")
                .doctorId(1L)
                .doctorName("Dr. Smith Johnson")
                .doctorSpecialization("Cardiology")
                .timeSlotId(1L)
                .status("SCHEDULED")
                .notes("Regular checkup")
                .message("Appointment booked successfully")
                .build();

        timeSlot = new TimeSlot();
        timeSlot.setId(1L);
        timeSlot.setIsAvailable(true);
    }

    @Test
    void bookAppointment_Success() throws Exception {
        // Given
        when(appointmentService.bookAppointment(any(AppointmentRequest.class)))
                .thenReturn(appointmentResponse);

        // When & Then
        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentId").value(1))
                .andExpect(jsonPath("$.patientId").value(1))
                .andExpect(jsonPath("$.patientName").value("John Doe"))
                .andExpect(jsonPath("$.doctorName").value("Dr. Smith Johnson"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.message").value("Appointment booked successfully"));

        verify(appointmentService).bookAppointment(any(AppointmentRequest.class));
    }

    @Test
    void bookAppointment_ValidationError() throws Exception {
        // Given
        AppointmentRequest invalidRequest = AppointmentRequest.builder()
                .patientId(null) // Invalid
                .doctorId(1L)
                .appointmentDate(LocalDate.now().minusDays(1)) // Past date
                .timeSlotId(1L)
                .build();

        // When & Then
        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verifyNoInteractions(appointmentService);
    }

    @Test
    void cancelAppointment_Success() throws Exception {
        // Given
        AppointmentResponse cancelResponse = AppointmentResponse.builder()
                .appointmentId(1L)
                .status("CANCELLED")
                .message("Appointment cancelled successfully")
                .build();

        when(appointmentService.cancelAppointment(1L, "Patient requested"))
                .thenReturn(cancelResponse);

        // When & Then
        mockMvc.perform(put("/appointments/1/cancel")
                        .param("cancellationReason", "Patient requested"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(1))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("Appointment cancelled successfully"));

        verify(appointmentService).cancelAppointment(1L, "Patient requested");
    }

    @Test
    void getAvailableSlots_Success() throws Exception {
        // Given
        List<TimeSlot> slots = Arrays.asList(timeSlot);
        when(appointmentService.getAvailableSlots(1L, LocalDate.now().plusDays(1)))
                .thenReturn(slots);

        // When & Then
        mockMvc.perform(get("/appointments/slots/available")
                        .param("doctorId", "1")
                        .param("date", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].isAvailable").value(true));

        verify(appointmentService).getAvailableSlots(1L, LocalDate.now().plusDays(1));
    }

    @Test
    void getDoctorAvailability_Success() throws Exception {
        // Given
        List<TimeSlot> slots = Arrays.asList(timeSlot);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);

        when(timeSlotService.getDoctorSlotsInRange(1L, startDate, endDate))
                .thenReturn(slots);

        // When & Then
        mockMvc.perform(get("/appointments/doctors/1/availability")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(timeSlotService).getDoctorSlotsInRange(1L, startDate, endDate);
    }

    @Test
    void getDoctorAvailability_InvalidDateRange() throws Exception {
        // Given
        LocalDate startDate = LocalDate.now().plusDays(7);
        LocalDate endDate = LocalDate.now(); // End date before start date

        // When & Then
        mockMvc.perform(get("/appointments/doctors/1/availability")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(timeSlotService);
    }

    @Test
    void checkSlotAvailability_Success() throws Exception {
        // Given
        when(timeSlotService.isSlotAvailable(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/appointments/slots/1/availability"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(timeSlotService).isSlotAvailable(1L);
    }

    @Test
    void getAvailableSlotCount_Success() throws Exception {
        // Given
        when(timeSlotService.getAvailableSlotCount(1L, LocalDate.now().plusDays(1)))
                .thenReturn(5L);

        // When & Then
        mockMvc.perform(get("/appointments/slots/count")
                        .param("doctorId", "1")
                        .param("date", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(timeSlotService).getAvailableSlotCount(1L, LocalDate.now().plusDays(1));
    }
}
