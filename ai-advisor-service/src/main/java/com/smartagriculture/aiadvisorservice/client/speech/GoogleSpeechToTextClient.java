package com.smartagriculture.aiadvisorservice.client.speech;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;
import com.smartagriculture.aiadvisorservice.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.Base64;

/**
 * Calls Google Cloud Speech-to-Text. Credentials resolve via
 * {@code speech.cloud.google.credentials-path} when set, otherwise fall back to Application
 * Default Credentials (e.g. the GOOGLE_APPLICATION_CREDENTIALS environment variable).
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "speech.provider", havingValue = "cloud")
public class GoogleSpeechToTextClient implements SpeechToTextClient {

    @Value("${speech.cloud.google.credentials-path:}")
    private String credentialsPath;

    @Value("${speech.language-code}")
    private String languageCode;

    @Override
    public String transcribe(String audioBase64) {
        try (SpeechClient speechClient = SpeechClient.create(buildSettings())) {
            byte[] audioBytes = Base64.getDecoder().decode(audioBase64);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
                    .setLanguageCode(languageCode)
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(ByteString.copyFrom(audioBytes))
                    .build();

            RecognizeResponse response = speechClient.recognize(config, audio);
            StringBuilder transcript = new StringBuilder();
            for (SpeechRecognitionResult result : response.getResultsList()) {
                if (result.getAlternativesCount() == 0) continue;
                SpeechRecognitionAlternative best = result.getAlternatives(0);
                transcript.append(best.getTranscript());
            }

            if (transcript.isEmpty()) {
                throw new ExternalServiceException("Google Speech-to-Text returned no transcription");
            }
            return transcript.toString().trim();
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google Speech-to-Text call failed: {}", e.getMessage());
            throw new ExternalServiceException("Failed to transcribe audio via Google Speech-to-Text: " + e.getMessage());
        }
    }

    private SpeechSettings buildSettings() throws Exception {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            return SpeechSettings.newBuilder().build();
        }
        try (FileInputStream credentialsStream = new FileInputStream(credentialsPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            return SpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
        }
    }
}
