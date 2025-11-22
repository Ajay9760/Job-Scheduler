package com.example.chronos;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.repository.JobRepository;
import com.example.chronos.service.JobMapper;
import com.example.chronos.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobServiceTest {

    @Test
    void createPersistsJobWithOwner() {
        JobRepository repo = mock(JobRepository.class);
        JobMapper mapper = new JobMapper();
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        JobService service = new JobService(repo, mapper, rabbit);

        JobCreateRequest req = new JobCreateRequest();
        req.setName("Test Job");
        req.setTargetUrl("https://example.com");
        req.setHttpMethod(HttpMethodType.GET);

        when(repo.save(any())).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(1L);
            return j;
        });

        var resp = service.create(req, "ajay");

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getName()).isEqualTo("Test Job");
    }
}
