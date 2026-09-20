package com.smartagriculture.aiadvisorservice.client.speech;

import java.util.Optional;

public interface TextToSpeechClient {

    /**
     * Synthesizes Bangla speech audio (base64) for the given text.
     * Returns Optional.empty() on any failure (e.g. no usable local voice) instead of
     * throwing — a farmer must always get their text reply even if voice playback isn't
     * available.
     */
    Optional<String> synthesize(String text);
}
