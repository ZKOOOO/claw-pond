package com.clawpond.platform;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    void shouldServeFrontendWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Claw Pond")));
    }

    @Test
    void shouldCompleteOpenClawPoolWorkJobAndLobsterFlow() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@example.com",
                                  "password": "StrongPass123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("alice"));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alice@example.com",
                                  "password": "StrongPass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String token = loginJson.get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        String createResponse = mockMvc.perform(post("/api/openclaws")
                        .header("Authorization", "Bearer " + token)
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

        mockMvc.perform(get("/api/openclaws")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalId").value("oc-prod-001"));

        mockMvc.perform(get("/api/openclaw-pool")
                        .param("tag", "ocr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("prod-openclaw-1"));

        mockMvc.perform(post("/api/work-jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "nightly-ocr-job",
                                  "description": "run nightly OCR workload",
                                  "openClawId": "%s",
                                  "desiredTags": ["ocr"]
                                }
                                """.formatted(openClawId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("nightly-ocr-job"))
                .andExpect(jsonPath("$.openClawName").value("prod-openclaw-1"));

        mockMvc.perform(get("/api/work-jobs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].desiredTags[0]").value("ocr"));

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
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ocr-lobster"))
                .andExpect(jsonPath("$.tagNames", org.hamcrest.Matchers.hasItems("ocr", "doc-parse")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode lobsterJson = objectMapper.readTree(lobsterResponse);
        String lobsterId = lobsterJson.get("id").asText();

        mockMvc.perform(get("/api/lobsters")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalFilename").value("lobster.txt"));

        mockMvc.perform(get("/api/lobsters/{id}/download", lobsterId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("lobster.txt")));

        mockMvc.perform(put("/api/openclaws/{id}", openClawId)
                        .header("Authorization", "Bearer " + token)
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
                .andExpect(jsonPath("$.name").value("prod-openclaw-1-updated"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.tagNames", org.hamcrest.Matchers.hasItems("ocr", "offline")));

        mockMvc.perform(get("/api/openclaws/{id}", openClawId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseUrl").value("https://openclaw-updated.example.com"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/openclaw-pool")
                        .param("tag", "ocr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(delete("/api/openclaws/{id}", openClawId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/openclaws")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
