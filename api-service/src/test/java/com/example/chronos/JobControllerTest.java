package com.example.chronos;

import com.example.chronos.config.TestSecurityConfig;
import com.example.chronos.controller.JobController;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.service.JobService;
import com.example.chronos.repository.UserRepository; // <--- IMPORT THIS
import com.example.chronos.security.JwtTokenUtil;   // <--- IMPORT THIS
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    // --- THE CRITICAL FIXES ---

    // 1. Your filter needs JwtTokenUtil, so we mock it.
    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    // 2. Your filter needs UserRepository, so we mock it.
    // WITHOUT THIS, THE APP CONTEXT WILL FAIL TO LOAD.
    @MockBean
    private UserRepository userRepository;

    // 3. Spring Security internals often look for this.
    @MockBean
    private UserDetailsService userDetailsService;

    // ---------------------------

    @Test
    @WithMockUser(username = "ajay", roles = "USER")
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
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }
}