package com.hospital.appointment.consumer;

import com.hospital.appointment.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final EmailService emailService;
    private final SMSService smsService;

    @RabbitListener(queues = "appointment.booked.queue")
    public void handleAppointmentBooked(@Payload NotificationEvent event, 
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Processing appointment booked notification for appointment: {}", event.getAppointmentId());
            
            // Send email notification
            emailService.sendAppointmentBookedEmail(event);
            
            // Send SMS notification
            smsService.sendAppointmentBookedSMS(event);
            
            log.info("Successfully processed appointment booked notification for appointment: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to process appointment booked notification for appointment: {}", event.getAppointmentId(), e);
            throw e; // Re-queue for retry
        }
    }

    @RabbitListener(queues = "appointment.cancelled.queue")
    public void handleAppointmentCancelled(@Payload NotificationEvent event,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Processing appointment cancelled notification for appointment: {}", event.getAppointmentId());
            
            // Send email notification
            emailService.sendAppointmentCancelledEmail(event);
            
            // Send SMS notification
            smsService.sendAppointmentCancelledSMS(event);
            
            log.info("Successfully processed appointment cancelled notification for appointment: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to process appointment cancelled notification for appointment: {}", event.getAppointmentId(), e);
            throw e; // Re-queue for retry
        }
    }

    @RabbitListener(queues = "appointment.reminder.queue")
    public void handleAppointmentReminder(@Payload NotificationEvent event,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Processing appointment reminder notification for appointment: {}", event.getAppointmentId());
            
            // Send email reminder
            emailService.sendAppointmentReminderEmail(event);
            
            // Send SMS reminder
            smsService.sendAppointmentReminderSMS(event);
            
            log.info("Successfully processed appointment reminder notification for appointment: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to process appointment reminder notification for appointment: {}", event.getAppointmentId(), e);
            throw e; // Re-queue for retry
        }
    }

    @RabbitListener(queues = "notification.dlq")
    public void handleDeadLetter(@Payload NotificationEvent event,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.error("Message sent to dead letter queue: {}", event);
        // Here you could implement additional monitoring or alerting
        // For now, just log the failure
    }
}
