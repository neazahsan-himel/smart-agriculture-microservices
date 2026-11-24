package com.smartagriculture.knowledgeservice.service;

import java.util.Locale;

/** Normalisation shared by writes and lookups, so "rice", " Rice " and "RICE" match. */
public final class KnowledgeText {

    private KnowledgeText() {
    }

    public static String crop(String crop) {
        return crop == null ? null : crop.trim().toUpperCase(Locale.ROOT);
    }

    /** Blank request params mean "no filter". */
    public static String filter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
