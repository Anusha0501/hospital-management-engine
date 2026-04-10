package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.patient.id = :patientId AND " +
           "a.doctor.id = :doctorId AND " +
           "a.appointmentDateTime = :dateTime AND " +
           "a.status != 'CANCELLED'")
    Optional<Appointment> findActiveAppointment(@Param("patientId") Long patientId,
                                               @Param("doctorId") Long doctorId,
                                               @Param("dateTime") LocalDateTime dateTime);
    
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.patient.id = :patientId AND " +
           "a.status = :status " +
           "ORDER BY a.appointmentDateTime DESC")
    List<Appointment> findByPatientIdAndStatus(@Param("patientId") Long patientId,
                                               @Param("status") AppointmentStatus status);
    
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.doctor.id = :doctorId AND " +
           "a.status = :status " +
           "ORDER BY a.appointmentDateTime DESC")
    List<Appointment> findByDoctorIdAndStatus(@Param("doctorId") Long doctorId,
                                               @Param("status") AppointmentStatus status);
    
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.patient.id = :patientId AND " +
           "a.appointmentDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY a.appointmentDateTime DESC")
    List<Appointment> findByPatientIdAndDateRange(@Param("patientId") Long patientId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.doctor.id = :doctorId AND " +
           "a.appointmentDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY a.appointmentDateTime DESC")
    List<Appointment> findByDoctorIdAndDateRange(@Param("doctorId") Long doctorId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.appointmentDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY a.appointmentDateTime DESC")
    List<Appointment> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM Appointment a WHERE " +
           "a.doctor.id = :doctorId AND " +
           "DATE(a.appointmentDateTime) = :date AND " +
           "a.status != 'CANCELLED'")
    long countActiveAppointmentsByDoctorAndDate(@Param("doctorId") Long doctorId,
                                                @Param("date") LocalDate date);
    
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.status = 'SCHEDULED' AND " +
           "a.appointmentDateTime < :cutoffTime")
    List<Appointment> findOverdueAppointments(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    Page<Appointment> findByPatientIdOrderByAppointmentDateTimeDesc(Long patientId, Pageable pageable);
    
    Page<Appointment> findByDoctorIdOrderByAppointmentDateTimeDesc(Long doctorId, Pageable pageable);
    
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.status = :status " +
           "ORDER BY a.appointmentDateTime DESC")
    Page<Appointment> findByStatusOrderByAppointmentDateTimeDesc(@Param("status") AppointmentStatus status, 
                                                                 Pageable pageable);
}
