package com.smartagriculture.aiadvisorservice.client.speech;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Optional;

/**
 * Calls a local Piper TTS HTTP wrapper (see docker/piper-tts, docker-compose.yml piper-tts
 * service) — {@code POST /tts} with JSON {@code {"text": "..."}}, returns raw WAV bytes.
 *
 * <p>Piper's Bangla voice coverage is uncertain (community-trained voice catalog) — any
 * failure here (missing voice, container down, timeout) is logged and swallowed, returning
 * Optional.empty() so a turn always still completes with its text reply.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "speech.provider", havingValue = "local", matchIfMissing = true)
public class LocalPiperTextToSpeechClient implements TextToSpeechClient {

    @Value("${speech.local.tts.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    @Override
    public Optional<String> synthesize(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TtsRequest> entity = new HttpEntity<>(new TtsRequest(text), headers);

            ResponseEntity<byte[]> response =
                    restTemplate.postForEntity(baseUrl + "/tts", entity, byte[].class);

            byte[] audio = response.getBody();
            if (audio == null || audio.length == 0) {
                log.warn("Piper TTS returned empty audio — skipping voice reply for this turn");
                return Optional.empty();
            }
            return Optional.of(Base64.getEncoder().encodeToString(audio));
        } catch (Exception e) {
            log.warn("Piper TTS call failed, continuing with text-only reply: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Getter
    @AllArgsConstructor
    private static class TtsRequest {
        private final String text;
    }
}
