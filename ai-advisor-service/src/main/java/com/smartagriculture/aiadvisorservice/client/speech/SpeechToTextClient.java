package com.smartagriculture.aiadvisorservice.client.speech;

public interface SpeechToTextClient {

    /**
     * Transcribes a base64-encoded audio clip (Bangla) into text.
     * Throws ExternalServiceException on failure — unlike TTS, ASR failure has no safe
     * fallback within this call, since callers use it to derive the farmer's message.
     */
    String transcribe(String audioBase64);
}
