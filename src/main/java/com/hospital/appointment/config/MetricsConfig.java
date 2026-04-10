package com.hospital.appointment.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "hospital-appointment-engine");
    }

    @Bean
    public Timer appointmentBookingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("appointment.booking.duration")
                .description("Time taken to book an appointment")
                .register(meterRegistry);
    }

    @Bean
    public Timer appointmentCancellationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("appointment.cancellation.duration")
                .description("Time taken to cancel an appointment")
                .register(meterRegistry);
    }

    @Bean
    public Timer slotAvailabilityTimer(MeterRegistry meterRegistry) {
        return Timer.builder("slot.availability.check.duration")
                .description("Time taken to check slot availability")
                .register(meterRegistry);
    }
}
