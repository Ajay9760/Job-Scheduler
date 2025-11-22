package com.example.chronos;

import com.example.chronos.controller.JobController;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
// ⛔ remove @Import(TestConfig.class)
@ActiveProfiles("test")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @Autowired
    private ObjectMapper objectMapper;

    // ✅ Test-only security config, ONLY used for this test class
    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll()
                    );
            return http.build();
        }

        // (Optional) you can also define ObjectMapper here if needed,
        // but Spring Boot usually auto-configures one.
        @Bean
         ObjectMapper objectMapper() {
             return new ObjectMapper();
         }
    }

    @Test
    void createJobReturns201() throws Exception {
        // Given
        JobResponse response = new JobResponse();
        response.setId(1L);
        response.setName("Test Job");

        JobCreateRequest request = new JobCreateRequest();
        request.setName("Test Job");
        request.setTargetUrl("https://example.com");
        request.setHttpMethod(HttpMethodType.GET);

        // When
        when(jobService.create(any(JobCreateRequest.class), anyString())).thenReturn(response);

        // Then
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("ajay").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Job"));
    }
}
