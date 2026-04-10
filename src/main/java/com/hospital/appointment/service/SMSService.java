package com.hospital.appointment.service;

import com.hospital.appointment.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SMSService {

    /**
     * Sends appointment booked SMS notification
     * Mock implementation - in production, integrate with actual SMS service
     */
    public void sendAppointmentBookedSMS(NotificationEvent event) {
        try {
            String message = buildAppointmentBookedSMS(event);
            
            log.info("MOCK: Sending SMS to {} with message: {}", event.getPatientPhone(), message);
            
            // Simulate SMS processing time
            Thread.sleep(50);
            
            log.info("Successfully sent appointment booked SMS to: {}", event.getPatientPhone());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SMS sending interrupted for patient: {}", event.getPatientPhone());
        } catch (Exception e) {
            log.error("Failed to send appointment booked SMS to: {}", event.getPatientPhone(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    /**
     * Sends appointment cancelled SMS notification
     */
    public void sendAppointmentCancelledSMS(NotificationEvent event) {
        try {
            String message = buildAppointmentCancelledSMS(event);
            
            log.info("MOCK: Sending SMS to {} with message: {}", event.getPatientPhone(), message);
            
            Thread.sleep(50);
            
            log.info("Successfully sent appointment cancelled SMS to: {}", event.getPatientPhone());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SMS sending interrupted for patient: {}", event.getPatientPhone());
        } catch (Exception e) {
            log.error("Failed to send appointment cancelled SMS to: {}", event.getPatientPhone(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    /**
     * Sends appointment reminder SMS notification
     */
    public void sendAppointmentReminderSMS(NotificationEvent event) {
        try {
            String message = buildAppointmentReminderSMS(event);
            
            log.info("MOCK: Sending reminder SMS to {} with message: {}", event.getPatientPhone(), message);
            
            Thread.sleep(50);
            
            log.info("Successfully sent appointment reminder SMS to: {}", event.getPatientPhone());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SMS sending interrupted for patient: {}", event.getPatientPhone());
        } catch (Exception e) {
            log.error("Failed to send appointment reminder SMS to: {}", event.getPatientPhone(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    private String buildAppointmentBookedSMS(NotificationEvent event) {
        return String.format(
            "Appointment Confirmed! Dr. %s on %s. ID: %d. Arrive 15 mins early. Hospital Team",
            event.getDoctorName(),
            event.getAppointmentDateTime().toLocalDate(),
            event.getAppointmentId()
        );
    }

    private String buildAppointmentCancelledSMS(NotificationEvent event) {
        return String.format(
            "Appointment Cancelled. Dr. %s on %s. ID: %d. Call us to reschedule. Hospital Team",
            event.getDoctorName(),
            event.getAppointmentDateTime().toLocalDate(),
            event.getAppointmentId()
        );
    }

    private String buildAppointmentReminderSMS(NotificationEvent event) {
        return String.format(
            "Reminder: Appointment with Dr. %s tomorrow at %s. ID: %d. Don't forget documents! Hospital Team",
            event.getDoctorName(),
            event.getAppointmentDateTime().toLocalTime(),
            event.getAppointmentId()
        );
    }
}
