package com.smartagriculture.knowledgeservice.importer;

import java.util.Arrays;

/** CSV import targets, by URL path segment: POST /api/v1/knowledge/import/{path}. */
public enum ImportType {
    SOURCES("sources"),
    PESTICIDES("pesticides"),
    FERTILIZER_RECOMMENDATIONS("fertilizer-recommendations"),
    CROP_CALENDAR("crop-calendar"),
    PESTS_DISEASES("pests-diseases");

    private final String path;

    ImportType(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public static ImportType fromPath(String path) {
        return Arrays.stream(values())
                .filter(t -> t.path.equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown import type '" + path + "'. Use one of: "
                        + String.join(", ", Arrays.stream(values()).map(ImportType::path).toList())));
    }
}
