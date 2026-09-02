package com.smartagriculture.aiadvisorservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartagriculture.aiadvisorservice.exception.ExternalServiceException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaApiClient {

    @Value("${ollama.api.base-url}")
    private String baseUrl;

    @Value("${ollama.api.model}")
    private String model;

    @Value("${ollama.api.vision-model}")
    private String visionModel;

    private final RestTemplate restTemplate;

    public String getAdvice(String systemPrompt, String userMessage) {
        return getAdvice(systemPrompt, userMessage, List.of());
    }

    public String getAdvice(String systemPrompt, String userMessage, List<String> images) {
        String url = baseUrl + "/api/chat";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        boolean hasImages = images != null && !images.isEmpty();
        String effectiveModel = hasImages ? visionModel : model;
        OllamaRequest request = new OllamaRequest(effectiveModel, systemPrompt, userMessage,
                hasImages ? images : null);
        HttpEntity<OllamaRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<OllamaResponse> response =
                    restTemplate.postForEntity(url, entity, OllamaResponse.class);

            OllamaResponse body = response.getBody();
            if (body == null || body.getMessage() == null) {
                throw new ExternalServiceException("Ollama returned an empty response");
            }

            String text = body.getMessage().getContent();
            if (text == null || text.isBlank()) {
                throw new ExternalServiceException("Ollama returned empty content");
            }
            return text;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Ollama HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceException("Ollama error " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ollama call failed: {}", e.getMessage());
            throw new ExternalServiceException("Failed to get advice from Ollama: " + e.getMessage());
        }
    }

    // ── Request DTOs ──────────────────────────────────────────────────────────

    @Getter
    static class OllamaRequest {
        private final String model;
        private final List<Message> messages;
        private final boolean stream = false;

        OllamaRequest(String model, String systemPrompt, String userMessage, List<String> userImages) {
            this.model = model;
            this.messages = List.of(
                    new Message("system", systemPrompt, null),
                    new Message("user", userMessage, userImages)
            );
        }

        @Getter
        @AllArgsConstructor
        static class Message {
            private final String role;
            private final String content;
            @JsonInclude(JsonInclude.Include.NON_NULL)
            private final List<String> images;
        }
    }

    // ── Response DTOs ─────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OllamaResponse {
        private Message message;

        @Data
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Message {
            private String role;
            private String content;
        }
    }
}