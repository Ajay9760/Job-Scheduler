package com.example.chronos;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.example.chronos.service.JobService;

@SpringBootTest
@ActiveProfiles("test")
class ApiServiceApplicationTests {

    // 👇 Mock out infrastructure so Spring doesn't try to build real Rabbit stuff
    @MockBean
    private RabbitTemplate rabbitTemplate;

    // 👇 Mock JobService too so its constructor dependencies don't matter
    @MockBean
    private JobService jobService;

    @Test
    void contextLoads() {
        // Just verify the Spring context starts.
        // No assertions needed here.
    }
}
