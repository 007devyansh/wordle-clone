package com.example.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the settings a hosting provider relies on. The allowed origin is
 * overridden here the same way an environment variable would override it in
 * production, to prove the value really comes from configuration.
 */
@SpringBootTest(properties = "wordle.cors.allowed-origins=https://wordle.example.com")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DeploymentConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reports_healthy_when_the_app_and_database_are_up() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void allows_browser_requests_from_the_configured_origin() throws Exception {
        mockMvc.perform(options("/api/games")
                        .header(HttpHeaders.ORIGIN, "https://wordle.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://wordle.example.com"
                ));
    }

    @Test
    void rejects_browser_requests_from_any_other_origin() throws Exception {
        mockMvc.perform(options("/api/games")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
