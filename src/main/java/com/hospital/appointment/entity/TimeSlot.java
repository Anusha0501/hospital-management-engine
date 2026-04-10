package com.hospital.appointment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "time_slots", indexes = {
    @Index(name = "idx_timeslot_doctor_date", columnList = "doctor_id, slot_date"),
    @Index(name = "idx_timeslot_availability", columnList = "is_available, slot_date")
})
public class TimeSlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Doctor is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;
    
    @NotNull(message = "Slot date is required")
    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;
    
    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;
    
    @Column(name = "booking_lock_expiry")
    private LocalDateTime bookingLockExpiry;
    
    @Column(name = "booked_by_patient_id")
    private Long bookedByPatientId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "timeSlot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appointment> appointments;
    
    // Constructors
    public TimeSlot() {}
    
    public TimeSlot(Doctor doctor, LocalDate slotDate, LocalDateTime startTime, LocalDateTime endTime) {
        this.doctor = doctor;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Doctor getDoctor() {
        return doctor;
    }
    
    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }
    
    public LocalDate getSlotDate() {
        return slotDate;
    }
    
    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Boolean getIsAvailable() {
        return isAvailable;
    }
    
    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
    
    public LocalDateTime getBookingLockExpiry() {
        return bookingLockExpiry;
    }
    
    public void setBookingLockExpiry(LocalDateTime bookingLockExpiry) {
        this.bookingLockExpiry = bookingLockExpiry;
    }
    
    public Long getBookedByPatientId() {
        return bookedByPatientId;
    }
    
    public void setBookedByPatientId(Long bookedByPatientId) {
        this.bookedByPatientId = bookedByPatientId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<Appointment> getAppointments() {
        return appointments;
    }
    
    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }
    
    public boolean isLocked() {
        return bookingLockExpiry != null && bookingLockExpiry.isAfter(LocalDateTime.now());
    }
    
    public void lockForBooking(Long patientId, LocalDateTime lockExpiry) {
        this.bookingLockExpiry = lockExpiry;
        this.bookedByPatientId = patientId;
    }
    
    public void releaseLock() {
        this.bookingLockExpiry = null;
        this.bookedByPatientId = null;
    }
}
