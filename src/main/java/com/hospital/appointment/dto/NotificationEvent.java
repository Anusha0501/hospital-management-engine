package com.hospital.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    
    private String eventType;
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    private String doctorName;
    private String doctorSpecialization;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appointmentDateTime;
    
    private String cancellationReason;
    private LocalDateTime timestamp;
    
    public static NotificationEvent appointmentBooked(Long appointmentId, Long patientId, 
                                                    String patientName, String patientEmail, String patientPhone,
                                                    Long doctorId, String doctorName, String doctorSpecialization,
                                                    LocalDateTime appointmentDateTime) {
        return NotificationEvent.builder()
                .eventType("APPOINTMENT_BOOKED")
                .appointmentId(appointmentId)
                .patientId(patientId)
                .patientName(patientName)
                .patientEmail(patientEmail)
                .patientPhone(patientPhone)
                .doctorId(doctorId)
                .doctorName(doctorName)
                .doctorSpecialization(doctorSpecialization)
                .appointmentDateTime(appointmentDateTime)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static NotificationEvent appointmentCancelled(Long appointmentId, Long patientId, 
                                                       String patientName, String patientEmail, String patientPhone,
                                                       Long doctorId, String doctorName, String doctorSpecialization,
                                                       LocalDateTime appointmentDateTime, String cancellationReason) {
        return NotificationEvent.builder()
                .eventType("APPOINTMENT_CANCELLED")
                .appointmentId(appointmentId)
                .patientId(patientId)
                .patientName(patientName)
                .patientEmail(patientEmail)
                .patientPhone(patientPhone)
                .doctorId(doctorId)
                .doctorName(doctorName)
                .doctorSpecialization(doctorSpecialization)
                .appointmentDateTime(appointmentDateTime)
                .cancellationReason(cancellationReason)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static NotificationEvent appointmentReminder(Long appointmentId, Long patientId, 
                                                      String patientName, String patientEmail, String patientPhone,
                                                      Long doctorId, String doctorName, String doctorSpecialization,
                                                      LocalDateTime appointmentDateTime) {
        return NotificationEvent.builder()
                .eventType("APPOINTMENT_REMINDER")
                .appointmentId(appointmentId)
                .patientId(patientId)
                .patientName(patientName)
                .patientEmail(patientEmail)
                .patientPhone(patientPhone)
                .doctorId(doctorId)
                .doctorName(doctorName)
                .doctorSpecialization(doctorSpecialization)
                .appointmentDateTime(appointmentDateTime)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
