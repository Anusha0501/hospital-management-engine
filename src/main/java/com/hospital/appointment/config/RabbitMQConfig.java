package com.hospital.appointment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String APPOINTMENT_BOOKED_QUEUE = "appointment.booked.queue";
    public static final String APPOINTMENT_CANCELLED_QUEUE = "appointment.cancelled.queue";
    public static final String APPOINTMENT_REMINDER_QUEUE = "appointment.reminder.queue";
    public static final String NOTIFICATION_DLQ = "notification.dlq";

    public static final String APPOINTMENT_EXCHANGE = "appointment.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    public static final String APPOINTMENT_BOOKED_ROUTING_KEY = "appointment.booked";
    public static final String APPOINTMENT_CANCELLED_ROUTING_KEY = "appointment.cancelled";
    public static final String APPOINTMENT_REMINDER_ROUTING_KEY = "appointment.reminder";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        
        // Enable publisher confirms
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("Message confirmed: " + correlationData);
            } else {
                System.err.println("Message not confirmed: " + correlationData + ", cause: " + cause);
            }
        });
        
        // Enable publisher returns
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage() + 
                             ", reply code: " + returned.getReplyCode() + 
                             ", reply text: " + returned.getReplyText() + 
                             ", exchange: " + returned.getExchange() + 
                             ", routing key: " + returned.getRoutingKey());
        });
        
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        return factory;
    }

    // Queues
    @Bean
    public Queue appointmentBookedQueue() {
        return QueueBuilder.durable(APPOINTMENT_BOOKED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue appointmentCancelledQueue() {
        return QueueBuilder.durable(APPOINTMENT_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue appointmentReminderQueue() {
        return QueueBuilder.durable(APPOINTMENT_REMINDER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue notificationDLQ() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    // Exchanges
    @Bean
    public TopicExchange appointmentExchange() {
        return new TopicExchange(APPOINTMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // Bindings
    @Bean
    public Binding appointmentBookedBinding() {
        return BindingBuilder.bind(appointmentBookedQueue())
                .to(appointmentExchange())
                .with(APPOINTMENT_BOOKED_ROUTING_KEY);
    }

    @Bean
    public Binding appointmentCancelledBinding() {
        return BindingBuilder.bind(appointmentCancelledQueue())
                .to(appointmentExchange())
                .with(APPOINTMENT_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding appointmentReminderBinding() {
        return BindingBuilder.bind(appointmentReminderQueue())
                .to(notificationExchange())
                .with(APPOINTMENT_REMINDER_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(notificationDLQ())
                .to(dlxExchange())
                .with(NOTIFICATION_DLQ);
    }
}
