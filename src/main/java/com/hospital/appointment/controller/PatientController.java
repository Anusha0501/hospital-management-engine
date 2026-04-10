package com.hospital.appointment.controller;

import com.hospital.appointment.entity.Patient;
import com.hospital.appointment.repository.PatientRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Management", description = "APIs for managing patients and their information")
public class PatientController {

    private final PatientRepository patientRepository;

    @Operation(summary = "Get patient by ID", description = "Retrieves patient information by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Patient retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    @GetMapping("/{patientId}")
    public ResponseEntity<Patient> getPatientById(
            @Parameter(description = "ID of the patient") 
            @PathVariable Long patientId) {
        
        log.info("Fetching patient with ID: {}", patientId);
        
        return patientRepository.findById(patientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get patient by email", description = "Retrieves patient information by email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Patient retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    @GetMapping("/email")
    public ResponseEntity<Patient> getPatientByEmail(
            @Parameter(description = "Email of the patient") 
            @RequestParam String email) {
        
        log.info("Fetching patient by email: {}", email);
        
        Optional<Patient> patient = patientRepository.findByEmail(email);
        
        return patient.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search patients by name", description = "Searches for patients by name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<Patient>> searchPatientsByName(
            @Parameter(description = "Name to search for") 
            @RequestParam String name) {
        
        log.info("Searching patients by name: {}", name);
        
        List<Patient> patients = patientRepository.findByNameContaining(name);
        
        return ResponseEntity.ok(patients);
    }

    @Operation(summary = "Check if patient exists by email", description = "Checks if a patient exists with the given email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed successfully")
    })
    @GetMapping("/exists/email")
    public ResponseEntity<Boolean> checkPatientExistsByEmail(
            @Parameter(description = "Email to check") 
            @RequestParam String email) {
        
        log.info("Checking if patient exists with email: {}", email);
        
        boolean exists = patientRepository.existsByEmail(email);
        
        return ResponseEntity.ok(exists);
    }

    @Operation(summary = "Check if patient exists by phone", description = "Checks if a patient exists with the given phone number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed successfully")
    })
    @GetMapping("/exists/phone")
    public ResponseEntity<Boolean> checkPatientExistsByPhone(
            @Parameter(description = "Phone number to check") 
            @RequestParam String phone) {
        
        log.info("Checking if patient exists with phone: {}", phone);
        
        boolean exists = patientRepository.existsByPhone(phone);
        
        return ResponseEntity.ok(exists);
    }
}
