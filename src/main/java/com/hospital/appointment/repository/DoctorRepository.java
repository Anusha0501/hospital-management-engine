package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    
    Optional<Doctor> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT d FROM Doctor d WHERE d.isActive = true")
    List<Doctor> findActiveDoctors();
    
    @Query("SELECT d FROM Doctor d WHERE " +
           "d.isActive = true AND " +
           "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(CONCAT(d.firstName, ' ', d.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Doctor> findActiveDoctorsByNameContaining(@Param("name") String name);
    
    @Query("SELECT d FROM Doctor d WHERE " +
           "d.isActive = true AND " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :specialization, '%'))")
    List<Doctor> findActiveDoctorsBySpecialization(@Param("specialization") String specialization);
    
    @Query("SELECT d FROM Doctor d WHERE " +
           "d.isActive = true AND " +
           "d.id IN (SELECT t.doctor.id FROM TimeSlot t WHERE t.slotDate = :date AND t.isAvailable = true)")
    List<Doctor> findAvailableDoctorsByDate(@Param("date") java.time.LocalDate date);
}
