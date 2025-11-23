package com.example.chronos;

import com.example.chronos.config.TestSecurityConfig;
import com.example.chronos.controller.JobController;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.service.JobService;
import com.example.chronos.repository.UserRepository; // ✅ Import 1
import com.example.chronos.security.JwtTokenUtil;   // ✅ Import 2
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService; // ✅ Import 3
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class JobControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    // --- PASTE THESE 3 MOCKS HERE TOO ---
    // Since JobControllerValidationTest loads the same Controller/Security layers,
    // it faces the exact same "Missing Bean" error if these are not mocked.

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    // ------------------------------------

    @Test
    @WithMockUser
    void createJob_returns400WhenInvalidBody() throws Exception {
        // Given an empty request (invalid because @NotNull fields are missing)
        JobCreateRequest request = new JobCreateRequest();

        // When/Then
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())) // CSRF is required for POST/PUT/DELETE
                .andExpect(status().isBadRequest());
    }
}