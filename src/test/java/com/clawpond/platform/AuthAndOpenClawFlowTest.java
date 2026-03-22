package com.clawpond.platform;

import com.clawpond.platform.model.Role;
import com.clawpond.platform.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "app.security.jwt.secret=ZGVtb2RlbW9kZW1vZGVtb2RlbW9kZW1vZGVtbw=="
})
class AuthAndOpenClawFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void shouldServeFrontendWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("OpenClaw")));
    }

    @Test
    void shouldCompleteAdminOpenClawWorkJobAndLobsterFlow() throws Exception {
        registerUser("alice", "alice@example.com");
        registerUser("bob", "bob@example.com");

        String aliceToken = login("alice@example.com");
        String bobToken = login("bob@example.com");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.enabled").value(true));

        String createResponse = mockMvc.perform(post("/api/openclaws")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "prod-openclaw-1",
                                  "baseUrl": "https://openclaw.example.com",
                                  "externalId": "oc-prod-001",
                                  "description": "production cluster",
                                  "apiToken": "secret-token",
                                  "tagNames": ["ocr", "high-throughput"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("prod-openclaw-1"))
                .andExpect(jsonPath("$.ownerUsername").value("alice"))
                .andExpect(jsonPath("$.tagNames", org.hamcrest.Matchers.hasItems("ocr", "high-throughput")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createJson = objectMapper.readTree(createResponse);
        String openClawId = createJson.get("id").asText();

        mockMvc.perform(get("/api/openclaw-pool")
                        .param("tag", "ocr")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("prod-openclaw-1"));

        MockMultipartFile lobsterFile = new MockMultipartFile(
                "file",
                "lobster.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "lobster-binary".getBytes()
        );

        String lobsterResponse = mockMvc.perform(multipart("/api/lobsters")
                        .file(lobsterFile)
                        .param("name", "ocr-lobster")
                        .param("description", "good at OCR jobs")
                        .param("tagText", "ocr, doc-parse")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ocr-lobster"))
                .andExpect(jsonPath("$.tagNames", org.hamcrest.Matchers.hasItems("ocr", "doc-parse")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode lobsterJson = objectMapper.readTree(lobsterResponse);
        String lobsterId = lobsterJson.get("id").asText();

        mockMvc.perform(get("/api/lobsters/{id}/download", lobsterId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("lobster.txt")));

        String workJobResponse = mockMvc.perform(post("/api/work-jobs")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "nightly-ocr-job",
                                  "description": "run nightly OCR workload",
                                  "openClawId": "%s",
                                  "lobsterAssetId": "%s",
                                  "desiredTags": ["ocr"]
                                }
                                """.formatted(openClawId, lobsterId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("nightly-ocr-job"))
                .andExpect(jsonPath("$.openClawName").value("prod-openclaw-1"))
                .andExpect(jsonPath("$.lobsterAssetName").value("ocr-lobster"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode workJobJson = objectMapper.readTree(workJobResponse);
        String workJobId = workJobJson.get("id").asText();

        mockMvc.perform(put("/api/work-jobs/{id}/status", workJobId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RUNNING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        mockMvc.perform(put("/api/work-jobs/{id}/status", workJobId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(put("/api/work-jobs/{id}/status", workJobId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RUNNING"
                                }
                                """))
                .andExpect(status().isBadRequest());

        userAccountRepository.findByEmail("alice@example.com").ifPresent(user -> {
            user.setRole(Role.ADMIN);
            userAccountRepository.save(user);
        });

        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalOpenClaws").value(1))
                .andExpect(jsonPath("$.totalWorkJobs").value(1))
                .andExpect(jsonPath("$.totalLobsters").value(1))
                .andExpect(jsonPath("$.recentWorkJobs[0].lobsterAssetName").value("ocr-lobster"));

        String bobUserId = userAccountRepository.findByEmail("bob@example.com")
                .orElseThrow()
                .getId()
                .toString();

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='alice')]").exists())
                .andExpect(jsonPath("$[?(@.username=='bob')]").exists());

        mockMvc.perform(put("/api/admin/users/{id}", bobUserId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2));

        mockMvc.perform(put("/api/admin/users/{id}", bobUserId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN",
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/openclaws/{id}", openClawId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "prod-openclaw-1-updated",
                                  "baseUrl": "https://openclaw-updated.example.com",
                                  "externalId": "oc-prod-001",
                                  "description": "updated cluster",
                                  "apiToken": "",
                                  "tagNames": ["ocr", "offline"],
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/openclaw-pool")
                        .param("tag", "ocr")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(delete("/api/openclaws/{id}", openClawId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());
    }

    private void registerUser(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "StrongPass123"
                                }
                                """.formatted(username, email)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "StrongPass123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
}
