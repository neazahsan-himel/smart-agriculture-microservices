package com.smartagriculture.aiadvisorservice.service.weather;

import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Condenses an hourly forecast into 3-hour blocks for the LLM prompt — 48 hourly lines would
 * crowd out the rest of the context for a small local model; ~16 block lines keep the detail that
 * matters for timing advice (rain, wind, temperature).
 */
public final class ForecastFormatter {

    private static final DateTimeFormatter DAY_HOUR = DateTimeFormatter.ofPattern("EEE d MMM HH", Locale.ENGLISH);

    private ForecastFormatter() {}

    public static List<String> toThreeHourBlocks(List<WeatherSummary> hourly) {
        Map<LocalDateTime, List<WeatherSummary>> blocks = new TreeMap<>();
        for (WeatherSummary w : hourly) {
            if (w.getRecordedAt() == null) continue;
            LocalDateTime t = w.getRecordedAt();
            LocalDateTime blockStart = t.toLocalDate().atTime(t.getHour() / 3 * 3, 0);
            blocks.computeIfAbsent(blockStart, k -> new ArrayList<>()).add(w);
        }

        List<String> lines = new ArrayList<>();
        blocks.forEach((start, hours) -> {
            double rain = hours.stream().mapToDouble(w -> w.getRainfallMm() != null ? w.getRainfallMm() : 0.0).sum();
            double maxWind = hours.stream().mapToDouble(w -> w.getWindSpeedKmh() != null ? w.getWindSpeedKmh() : 0.0).max().orElse(0);
            double maxTemp = hours.stream().mapToDouble(w -> w.getTemperatureCelsius() != null ? w.getTemperatureCelsius() : Double.NaN)
                    .filter(v -> !Double.isNaN(v)).max().orElse(Double.NaN);
            // Prefer the condition of a rainy hour so a shower isn't hidden behind a sunny first hour
            String condition = hours.stream()
                    .filter(w -> w.getRainfallMm() != null && w.getRainfallMm() > 0 && w.getWeatherCondition() != null)
                    .map(WeatherSummary::getWeatherCondition)
                    .findFirst()
                    .orElse(hours.getFirst().getWeatherCondition());

            StringBuilder sb = new StringBuilder("- ")
                    .append(start.format(DAY_HOUR)).append(":00-").append(String.format("%02d:00", start.getHour() + 3))
                    .append(" (").append(partOfDay(start.getHour())).append(")")
                    .append(String.format(Locale.ENGLISH, ": rain %.1fmm, wind up to %.0fkm/h", rain, maxWind));
            if (!Double.isNaN(maxTemp)) sb.append(String.format(Locale.ENGLISH, ", up to %.0f°C", maxTemp));
            if (condition != null) sb.append(", ").append(condition);
            lines.add(sb.toString());
        });
        return lines;
    }

    // Spelled out because qwen3:8b misread bare "03-06"/"09-12" labels as morning/late afternoon
    private static String partOfDay(int blockStartHour) {
        return switch (blockStartHour) {
            case 6 -> "early morning";
            case 9 -> "late morning";
            case 12 -> "midday";
            case 15 -> "late afternoon";
            case 18 -> "evening";
            default -> "night";
        };
    }
}
