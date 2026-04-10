package com.hospital.appointment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hospital Appointment Engine API")
                        .description("Production-grade Hospital Appointment Booking System with distributed locking, async notifications, and scalability for 10,000+ concurrent users")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Hospital Management Team")
                                .email("support@hospital.com")
                                .url("https://hospital.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.hospital.com/api")
                                .description("Production Server")
                ));
    }
}
