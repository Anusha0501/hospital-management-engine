package com.hospital.appointment.service;

import com.hospital.appointment.config.RabbitMQConfig;
import com.hospital.appointment.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Sends appointment booked notification
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendAppointmentBookedNotification(NotificationEvent event) {
        try {
            log.info("Sending appointment booked notification for appointment: {}", event.getAppointmentId());
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.APPOINTMENT_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_BOOKED_ROUTING_KEY,
                event
            );
            log.info("Successfully sent appointment booked notification for appointment: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to send appointment booked notification for appointment: {}", event.getAppointmentId(), e);
            throw e;
        }
    }

    /**
     * Sends appointment cancelled notification
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendAppointmentCancelledNotification(NotificationEvent event) {
        try {
            log.info("Sending appointment cancelled notification for appointment: {}", event.getAppointmentId());
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.APPOINTMENT_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_CANCELLED_ROUTING_KEY,
                event
            );
            log.info("Successfully sent appointment cancelled notification for appointment: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to send appointment cancelled notification for appointment: {}", event.getAppointmentId(), e);
            throw e;
        }
    }

    /**
     * Sends appointment reminder notification
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendAppointmentReminderNotification(NotificationEvent event) {
        try {
            log.info("Sending appointment reminder notification for appointment: {}", event.getAppointmentId());
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_REMINDER_ROUTING_KEY,
                event
            );
            log.info("Successfully sent appointment reminder notification for appointment: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to send appointment reminder notification for appointment: {}", event.getAppointmentId(), e);
            throw e;
        }
    }
}
