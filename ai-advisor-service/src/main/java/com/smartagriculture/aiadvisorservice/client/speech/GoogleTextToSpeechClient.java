package com.smartagriculture.aiadvisorservice.client.speech;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.Base64;
import java.util.Optional;

/**
 * Calls Google Cloud Text-to-Speech. Same credential resolution as
 * {@link GoogleSpeechToTextClient}. Failures are logged and swallowed (Optional.empty())
 * so a turn always still completes with its text reply — matching the local Piper client's
 * non-fatal-degradation behavior.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "speech.provider", havingValue = "cloud")
public class GoogleTextToSpeechClient implements com.smartagriculture.aiadvisorservice.client.speech.TextToSpeechClient {

    @Value("${speech.cloud.google.credentials-path:}")
    private String credentialsPath;

    @Value("${speech.language-code}")
    private String languageCode;

    @Override
    public Optional<String> synthesize(String text) {
        try (TextToSpeechClient client = TextToSpeechClient.create(buildSettings())) {
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode(languageCode)
                    .setSsmlGender(SsmlVoiceGender.NEUTRAL)
                    .build();
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.LINEAR16)
                    .build();

            SynthesizeSpeechResponse response = client.synthesizeSpeech(input, voice, audioConfig);
            byte[] audio = response.getAudioContent().toByteArray();
            if (audio.length == 0) {
                log.warn("Google Text-to-Speech returned empty audio — skipping voice reply for this turn");
                return Optional.empty();
            }
            return Optional.of(Base64.getEncoder().encodeToString(audio));
        } catch (Exception e) {
            log.warn("Google Text-to-Speech call failed, continuing with text-only reply: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private TextToSpeechSettings buildSettings() throws Exception {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            return TextToSpeechSettings.newBuilder().build();
        }
        try (FileInputStream credentialsStream = new FileInputStream(credentialsPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            return TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
        }
    }
}
