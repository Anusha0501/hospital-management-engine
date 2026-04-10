package com.hospital.appointment.service;

import com.hospital.appointment.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    /**
     * Sends appointment booked email notification
     * Mock implementation - in production, integrate with actual email service
     */
    public void sendAppointmentBookedEmail(NotificationEvent event) {
        try {
            // Mock email sending logic
            String subject = "Appointment Confirmed - " + event.getDoctorName();
            String body = buildAppointmentBookedEmailBody(event);
            
            log.info("MOCK: Sending email to {} with subject: {}", event.getPatientEmail(), subject);
            log.info("Email body: {}", body);
            
            // Simulate email processing time
            Thread.sleep(100);
            
            log.info("Successfully sent appointment booked email to: {}", event.getPatientEmail());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending interrupted for patient: {}", event.getPatientEmail());
        } catch (Exception e) {
            log.error("Failed to send appointment booked email to: {}", event.getPatientEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Sends appointment cancelled email notification
     */
    public void sendAppointmentCancelledEmail(NotificationEvent event) {
        try {
            String subject = "Appointment Cancelled - " + event.getDoctorName();
            String body = buildAppointmentCancelledEmailBody(event);
            
            log.info("MOCK: Sending email to {} with subject: {}", event.getPatientEmail(), subject);
            log.info("Email body: {}", body);
            
            Thread.sleep(100);
            
            log.info("Successfully sent appointment cancelled email to: {}", event.getPatientEmail());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending interrupted for patient: {}", event.getPatientEmail());
        } catch (Exception e) {
            log.error("Failed to send appointment cancelled email to: {}", event.getPatientEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Sends appointment reminder email notification
     */
    public void sendAppointmentReminderEmail(NotificationEvent event) {
        try {
            String subject = "Appointment Reminder - " + event.getDoctorName();
            String body = buildAppointmentReminderEmailBody(event);
            
            log.info("MOCK: Sending reminder email to {} with subject: {}", event.getPatientEmail(), subject);
            log.info("Email body: {}", body);
            
            Thread.sleep(100);
            
            log.info("Successfully sent appointment reminder email to: {}", event.getPatientEmail());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending interrupted for patient: {}", event.getPatientEmail());
        } catch (Exception e) {
            log.error("Failed to send appointment reminder email to: {}", event.getPatientEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildAppointmentBookedEmailBody(NotificationEvent event) {
        return String.format(
            "Dear %s,\n\n" +
            "Your appointment has been successfully booked!\n\n" +
            "Appointment Details:\n" +
            "Doctor: Dr. %s (%s)\n" +
            "Date & Time: %s\n" +
            "Appointment ID: %d\n\n" +
            "Please arrive 15 minutes before your scheduled time.\n" +
            "Bring any relevant medical documents.\n\n" +
            "Thank you for choosing our hospital.\n\n" +
            "Best regards,\n" +
            "Hospital Management Team",
            event.getPatientName(),
            event.getDoctorName(),
            event.getDoctorSpecialization(),
            event.getAppointmentDateTime(),
            event.getAppointmentId()
        );
    }

    private String buildAppointmentCancelledEmailBody(NotificationEvent event) {
        return String.format(
            "Dear %s,\n\n" +
            "Your appointment has been cancelled as requested.\n\n" +
            "Cancelled Appointment Details:\n" +
            "Doctor: Dr. %s (%s)\n" +
            "Date & Time: %s\n" +
            "Appointment ID: %d\n" +
            "Reason: %s\n\n" +
            "If you did not request this cancellation, please contact us immediately.\n\n" +
            "To book a new appointment, please visit our website or call us.\n\n" +
            "Best regards,\n" +
            "Hospital Management Team",
            event.getPatientName(),
            event.getDoctorName(),
            event.getDoctorSpecialization(),
            event.getAppointmentDateTime(),
            event.getAppointmentId(),
            event.getCancellationReason() != null ? event.getCancellationReason() : "Not specified"
        );
    }

    private String buildAppointmentReminderEmailBody(NotificationEvent event) {
        return String.format(
            "Dear %s,\n\n" +
            "This is a friendly reminder about your upcoming appointment.\n\n" +
            "Appointment Details:\n" +
            "Doctor: Dr. %s (%s)\n" +
            "Date & Time: %s\n" +
            "Appointment ID: %d\n\n" +
            "Please remember:\n" +
            "- Arrive 15 minutes early\n" +
            "- Bring your ID and insurance documents\n" +
            "- Bring any relevant medical records\n" +
            "- List of current medications\n\n" +
            "If you need to reschedule or cancel, please contact us at least 24 hours in advance.\n\n" +
            "We look forward to seeing you!\n\n" +
            "Best regards,\n" +
            "Hospital Management Team",
            event.getPatientName(),
            event.getDoctorName(),
            event.getDoctorSpecialization(),
            event.getAppointmentDateTime(),
            event.getAppointmentId()
        );
    }
}
