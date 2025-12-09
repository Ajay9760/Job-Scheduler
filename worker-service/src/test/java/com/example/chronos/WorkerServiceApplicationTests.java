package com.example.chronos;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = WorkerServiceApplication.class)
class WorkerServiceApplicationTests {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private TaskScheduler taskScheduler;

    @Test
    void contextLoads() {
    }
}
