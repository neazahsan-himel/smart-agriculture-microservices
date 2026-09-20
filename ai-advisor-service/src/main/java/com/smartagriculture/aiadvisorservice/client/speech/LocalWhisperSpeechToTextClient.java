package com.smartagriculture.aiadvisorservice.client.speech;

import com.smartagriculture.aiadvisorservice.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;

/**
 * Calls a local `onerahmet/openai-whisper-asr-webservice` container (see
 * docker-compose.yml / whisper-asr) — {@code POST /asr} accepts a multipart audio file and
 * returns the transcription as plain text when {@code output=txt}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "speech.provider", havingValue = "local", matchIfMissing = true)
public class LocalWhisperSpeechToTextClient implements SpeechToTextClient {

    @Value("${speech.local.asr.base-url}")
    private String baseUrl;

    @Value("${speech.language-code}")
    private String languageCode;

    private final RestTemplate restTemplate;

    @Override
    public String transcribe(String audioBase64) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/asr")
                .queryParam("task", "transcribe")
                .queryParam("language", bcp47ToWhisperLanguage(languageCode))
                .queryParam("output", "txt")
                .toUriString();

        byte[] audioBytes = Base64.getDecoder().decode(audioBase64);
        ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "audio.webm";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio_file", audioResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String text = response.getBody();
            if (text == null || text.isBlank()) {
                throw new ExternalServiceException("Whisper ASR returned empty transcription");
            }
            return text.trim();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Whisper ASR HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceException("Whisper ASR error " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Whisper ASR call failed: {}", e.getMessage());
            throw new ExternalServiceException("Failed to transcribe audio via Whisper: " + e.getMessage());
        }
    }

    private String bcp47ToWhisperLanguage(String bcp47) {
        // Whisper expects a bare ISO-639-1 code (e.g. "bn"), not a region-qualified tag.
        int dash = bcp47.indexOf('-');
        return dash > 0 ? bcp47.substring(0, dash) : bcp47;
    }
}
