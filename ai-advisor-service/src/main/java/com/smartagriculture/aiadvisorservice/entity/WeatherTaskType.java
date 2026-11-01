package com.smartagriculture.aiadvisorservice.entity;

/**
 * Farm tasks whose timing depends on the weather. Stored in {@code FarmTask.taskType} by name —
 * that column stays a String so non-weather tasks (e.g. "FOLLOW_UP") remain valid.
 */
public enum WeatherTaskType {

    SPRAY("spray"),
    FERTILIZE("apply fertiliser"),
    IRRIGATE("irrigate"),
    HARVEST("harvest"),
    SOW("sow");

    private final String verb;

    WeatherTaskType(String verb) {
        this.verb = verb;
    }

    /** Farmer-facing verb, e.g. "Best time to <verb>". */
    public String verb() {
        return verb;
    }

    public static WeatherTaskType fromTaskType(String taskType) {
        if (taskType == null) return null;
        try {
            return valueOf(taskType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
