package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    Optional<Patient> findByEmail(String email);
    
    Optional<Patient> findByPhone(String phone);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Patient> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT p FROM Patient p WHERE p.id IN " +
           "(SELECT a.patient.id FROM Appointment a WHERE a.doctor.id = :doctorId)")
    List<Patient> findPatientsByDoctorId(@Param("doctorId") Long doctorId);
}
