package com.example.chronos;

import com.example.chronos.config.TestSecurityConfig;
import com.example.chronos.controller.JobController;
import com.example.chronos.service.JobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(JobController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public class JobControllerValidationTest {

    @Autowired
    MockMvc mockMvc;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Target URL is required")
    private String targetUrl;

    @NotNull(message = "HTTP method is required")
    private HttpMethod httpMethod;

    @MockBean
    JobService jobService;

    @Test
    void createJob_returns400WhenInvalidBody() throws Exception {
        String body = """
            {
              "name": "",
              "targetUrl": "",
              "httpMethod": null
            }
            """;

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-User-Id", "ajay"))
                .andExpect(status().isBadRequest());
    }
}