package com.hospital.appointment.controller;

import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.repository.DoctorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Doctor Management", description = "APIs for managing doctors and their information")
public class DoctorController {

    private final DoctorRepository doctorRepository;

    @Operation(summary = "Get all active doctors", description = "Retrieves a list of all active doctors")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Doctors retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<Doctor>> getAllActiveDoctors() {
        log.info("Fetching all active doctors");
        
        List<Doctor> doctors = doctorRepository.findActiveDoctors();
        
        return ResponseEntity.ok(doctors);
    }

    @Operation(summary = "Get doctor by ID", description = "Retrieves doctor information by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Doctor retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @GetMapping("/{doctorId}")
    public ResponseEntity<Doctor> getDoctorById(
            @Parameter(description = "ID of the doctor") 
            @PathVariable Long doctorId) {
        
        log.info("Fetching doctor with ID: {}", doctorId);
        
        return doctorRepository.findById(doctorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search doctors by name", description = "Searches for active doctors by name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<Doctor>> searchDoctorsByName(
            @Parameter(description = "Name to search for") 
            @RequestParam String name) {
        
        log.info("Searching doctors by name: {}", name);
        
        List<Doctor> doctors = doctorRepository.findActiveDoctorsByNameContaining(name);
        
        return ResponseEntity.ok(doctors);
    }

    @Operation(summary = "Search doctors by specialization", description = "Searches for active doctors by specialization")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/specialization")
    public ResponseEntity<List<Doctor>> searchDoctorsBySpecialization(
            @Parameter(description = "Specialization to search for") 
            @RequestParam String specialization) {
        
        log.info("Searching doctors by specialization: {}", specialization);
        
        List<Doctor> doctors = doctorRepository.findActiveDoctorsBySpecialization(specialization);
        
        return ResponseEntity.ok(doctors);
    }

    @Operation(summary = "Get available doctors by date", description = "Retrieves doctors who have available slots on a specific date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available doctors retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date format")
    })
    @GetMapping("/available")
    public ResponseEntity<List<Doctor>> getAvailableDoctorsByDate(
            @Parameter(description = "Date to check availability (YYYY-MM-DD)") 
            @RequestParam LocalDate date) {
        
        log.info("Fetching available doctors for date: {}", date);
        
        List<Doctor> doctors = doctorRepository.findAvailableDoctorsByDate(date);
        
        return ResponseEntity.ok(doctors);
    }
}
