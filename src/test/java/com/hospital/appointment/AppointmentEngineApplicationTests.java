package com.hospital.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AppointmentEngineApplicationTests {

    @Test
    void contextLoads() {
        // Test to ensure the Spring Boot application context loads successfully
    }
}
