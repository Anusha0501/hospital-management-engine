package com.hospital.appointment.controller;

import com.hospital.appointment.dto.AppointmentRequest;
import com.hospital.appointment.dto.AppointmentResponse;
import com.hospital.appointment.entity.TimeSlot;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Appointment Management", description = "APIs for managing hospital appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final TimeSlotService timeSlotService;

    @Operation(summary = "Book a new appointment", description = "Books an appointment with distributed locking to prevent double-booking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Appointment booked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Patient, doctor, or time slot not found"),
        @ApiResponse(responseCode = "409", description = "Time slot not available or already booked"),
        @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded"),
        @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @PostMapping
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody AppointmentRequest request) {
        log.info("Received appointment booking request: {}", request);
        
        AppointmentResponse response = appointmentService.bookAppointment(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cancel an appointment", description = "Cancels an existing appointment and releases the time slot")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Appointment cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "400", description = "Cannot cancel past appointments or already cancelled appointments")
    })
    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @Parameter(description = "ID of the appointment to cancel") 
            @PathVariable Long appointmentId,
            @Parameter(description = "Reason for cancellation") 
            @RequestParam(required = false) String cancellationReason) {
        
        log.info("Received appointment cancellation request for appointment: {}", appointmentId);
        
        AppointmentResponse response = appointmentService.cancelAppointment(appointmentId, cancellationReason);
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get available time slots for a doctor", description = "Retrieves all available time slots for a specific doctor on a given date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available time slots retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Doctor not found"),
        @ApiResponse(responseCode = "400", description = "Invalid date format or past date")
    })
    @GetMapping("/slots/available")
    public ResponseEntity<List<TimeSlot>> getAvailableSlots(
            @Parameter(description = "ID of the doctor") 
            @RequestParam Long doctorId,
            @Parameter(description = "Date to check availability (YYYY-MM-DD)") 
            @RequestParam LocalDate date) {
        
        log.info("Fetching available slots for doctor: {} on date: {}", doctorId, date);
        
        List<TimeSlot> slots = appointmentService.getAvailableSlots(doctorId, date);
        
        return ResponseEntity.ok(slots);
    }

    @Operation(summary = "Get doctor availability", description = "Retrieves all time slots (available and booked) for a doctor in a date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Doctor availability retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Doctor not found"),
        @ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    @GetMapping("/doctors/{doctorId}/availability")
    public ResponseEntity<List<TimeSlot>> getDoctorAvailability(
            @Parameter(description = "ID of the doctor") 
            @PathVariable Long doctorId,
            @Parameter(description = "Start date (YYYY-MM-DD)") 
            @RequestParam LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)") 
            @RequestParam LocalDate endDate) {
        
        log.info("Fetching availability for doctor: {} from {} to {}", doctorId, startDate, endDate);
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        List<TimeSlot> slots = timeSlotService.getDoctorSlotsInRange(doctorId, startDate, endDate);
        
        return ResponseEntity.ok(slots);
    }

    @Operation(summary = "Check slot availability", description = "Checks if a specific time slot is available for booking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Slot availability status retrieved"),
        @ApiResponse(responseCode = "404", description = "Time slot not found")
    })
    @GetMapping("/slots/{slotId}/availability")
    public ResponseEntity<Boolean> checkSlotAvailability(
            @Parameter(description = "ID of the time slot") 
            @PathVariable Long slotId) {
        
        log.info("Checking availability for time slot: {}", slotId);
        
        boolean isAvailable = timeSlotService.isSlotAvailable(slotId);
        
        return ResponseEntity.ok(isAvailable);
    }

    @Operation(summary = "Get available slot count", description = "Returns the count of available slots for a doctor on a specific date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Slot count retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @GetMapping("/slots/count")
    public ResponseEntity<Long> getAvailableSlotCount(
            @Parameter(description = "ID of the doctor") 
            @RequestParam Long doctorId,
            @Parameter(description = "Date to check (YYYY-MM-DD)") 
            @RequestParam LocalDate date) {
        
        log.info("Getting available slot count for doctor: {} on date: {}", doctorId, date);
        
        long count = timeSlotService.getAvailableSlotCount(doctorId, date);
        
        return ResponseEntity.ok(count);
    }
}
