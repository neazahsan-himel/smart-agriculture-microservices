package com.smartagriculture.aiadvisorservice.service.weather;

import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Best-Time Recommendation Engine: picks the earliest hourly slot in which a weather-dependent
 * task is safe to do, from the hourly forecast for the farmer's location.
 *
 * <p>Deliberately rule-based rather than LLM-based — the thresholds are agronomic rules of thumb
 * that must be applied consistently and explained exactly, which a small local model can't
 * guarantee. Pure function of its inputs (no I/O), so it is fully unit-tested.
 *
 * <p>Search window: from 24h before the planned (due) time to 48h after it, never earlier than the
 * next full hour and never past the end of the forecast.
 */
@Component
public class BestTimeEvaluator {

    // ── Thresholds ───────────────────────────────────────────────────────────
    static final double SPRAY_MAX_WIND_KMH = 15.0;      // spray drift
    static final double SPRAY_MAX_TEMP_C = 32.0;        // evaporation / leaf scorch
    static final double SPRAY_MAX_RAIN_MM_7H = 1.0;     // slot + 6h: needs time to dry on the leaf
    static final double FERTILIZE_MAX_RAIN_MM_SLOT = 1.0;
    static final double FERTILIZE_MAX_RAIN_MM_24H = 10.0; // heavy rain washes fertiliser off
    static final double IRRIGATE_MAX_RAIN_MM_24H = 5.0;   // more than this and the rain does the job
    static final double HARVEST_MAX_RAIN_MM_7H = 0.5;
    static final double HARVEST_MAX_WIND_KMH = 30.0;
    static final double SOW_MAX_RAIN_MM_24H = 20.0;       // seed washout

    private static final Set<Integer> SPRAY_HOURS = hours(6, 10, 16, 18);
    private static final Set<Integer> FERTILIZE_HOURS = hours(6, 17);
    private static final Set<Integer> IRRIGATE_HOURS = hours(5, 9, 16, 19);
    private static final Set<Integer> HARVEST_HOURS = hours(7, 16);
    private static final Set<Integer> SOW_HOURS = hours(6, 17);

    private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.ENGLISH);

    public enum Status { RECOMMENDED, NO_WINDOW, NO_FORECAST }

    public record BestTimeResult(Status status, LocalDateTime recommendedStart, String advice) {}

    public BestTimeResult evaluate(WeatherTaskType type, List<WeatherSummary> hourlyForecast,
                                   LocalDateTime now, LocalDateTime dueDate) {
        LocalDateTime due = dueDate != null ? dueDate : now;

        NavigableMap<LocalDateTime, WeatherSummary> forecast = hourlyForecast == null ? new TreeMap<>()
                : hourlyForecast.stream()
                    .filter(w -> w.getRecordedAt() != null)
                    .collect(Collectors.toMap(w -> w.getRecordedAt().truncatedTo(ChronoUnit.HOURS), w -> w,
                            (a, b) -> a, TreeMap::new));

        if (now.isAfter(due.plusHours(48))) {
            return new BestTimeResult(Status.NO_WINDOW, null,
                    "The planned time for this task (" + format(due) + ") has passed. Set a new date.");
        }
        if (forecast.isEmpty()) {
            return new BestTimeResult(Status.NO_FORECAST, null,
                    "No forecast is available for this location. Check the weather yourself before you "
                            + type.verb() + ".");
        }

        LocalDateTime searchStart = max(ceilToHour(now), due.minusHours(24).truncatedTo(ChronoUnit.HOURS));
        LocalDateTime searchEnd = min(due.plusHours(48), forecast.lastKey());
        if (searchStart.isAfter(searchEnd)) {
            return new BestTimeResult(Status.NO_FORECAST, null,
                    "The forecast doesn't reach " + format(due) + " yet. The best time will be checked closer to the date.");
        }

        String firstRejection = null;
        for (LocalDateTime slot = searchStart; !slot.isAfter(searchEnd); slot = slot.plusHours(1)) {
            if (!allowedHours(type).contains(slot.getHour()) || !forecast.containsKey(slot)) continue;

            String rejection = checkWeather(type, slot, forecast);
            if (rejection == null) {
                String advice = "Best time to " + type.verb() + ": " + format(slot) + "."
                        + (firstRejection != null ? " Earlier times were skipped: " + firstRejection + "." : "");
                return new BestTimeResult(Status.RECOMMENDED, slot, advice);
            }
            if (firstRejection == null) firstRejection = rejection;
        }

        String reason = firstRejection != null ? firstRejection : "no suitable hours of the day in the forecast window";
        String advice = type == WeatherTaskType.IRRIGATE
                ? "No need to irrigate before " + format(searchEnd) + ": " + reason + ". This will be checked again as the forecast updates."
                : "No safe time to " + type.verb() + " before " + format(searchEnd) + ": " + reason
                        + ". This will be checked again as the forecast updates.";
        return new BestTimeResult(Status.NO_WINDOW, null, advice);
    }

    /** Returns null when the slot is fine, otherwise a farmer-readable reason. */
    private String checkWeather(WeatherTaskType type, LocalDateTime slot, NavigableMap<LocalDateTime, WeatherSummary> f) {
        WeatherSummary at = f.get(slot);
        return switch (type) {
            case SPRAY -> firstNonNull(
                    windAbove(at, slot, SPRAY_MAX_WIND_KMH),
                    tempAbove(at, slot, SPRAY_MAX_TEMP_C),
                    rainAbove(f, slot, 7, SPRAY_MAX_RAIN_MM_7H, "within 6 h of"));
            case FERTILIZE -> firstNonNull(
                    rainAbove(f, slot, 1, FERTILIZE_MAX_RAIN_MM_SLOT, "at"),
                    rainAbove(f, slot, 24, FERTILIZE_MAX_RAIN_MM_24H, "in the 24 h after (fertiliser would wash off)"));
            case IRRIGATE -> rainAbove(f, slot, 24, IRRIGATE_MAX_RAIN_MM_24H, "in the 24 h after (the rain will water the field)");
            case HARVEST -> firstNonNull(
                    windAbove(at, slot, HARVEST_MAX_WIND_KMH),
                    rainAbove(f, slot, 7, HARVEST_MAX_RAIN_MM_7H, "within 6 h of"));
            case SOW -> rainAbove(f, slot, 24, SOW_MAX_RAIN_MM_24H, "in the 24 h after (seeds may wash away)");
        };
    }

    private String windAbove(WeatherSummary at, LocalDateTime slot, double max) {
        Double wind = at.getWindSpeedKmh();
        return wind != null && wind > max
                ? String.format(Locale.ENGLISH, "wind %.0f km/h at %s (max %.0f)", wind, format(slot), max)
                : null;
    }

    private String tempAbove(WeatherSummary at, LocalDateTime slot, double max) {
        Double temp = at.getTemperatureCelsius();
        return temp != null && temp > max
                ? String.format(Locale.ENGLISH, "%.0f°C at %s (max %.0f°C)", temp, format(slot), max)
                : null;
    }

    /** Rain summed over [slot, slot + hours); null-safe (missing hours count as 0). */
    private String rainAbove(NavigableMap<LocalDateTime, WeatherSummary> f, LocalDateTime slot, int hours,
                             double max, String phrase) {
        double total = f.subMap(slot, true, slot.plusHours(hours), false).values().stream()
                .mapToDouble(w -> w.getRainfallMm() != null ? w.getRainfallMm() : 0.0)
                .sum();
        // The single-hour rule allows up to max (≤); the multi-hour rules require strictly below max (<)
        boolean exceeded = hours == 1 ? total > max : total >= max;
        return exceeded
                ? String.format(Locale.ENGLISH, "%.1f mm rain expected %s %s", total, phrase, format(slot))
                : null;
    }

    private static Set<Integer> allowedHours(WeatherTaskType type) {
        return switch (type) {
            case SPRAY -> SPRAY_HOURS;
            case FERTILIZE -> FERTILIZE_HOURS;
            case IRRIGATE -> IRRIGATE_HOURS;
            case HARVEST -> HARVEST_HOURS;
            case SOW -> SOW_HOURS;
        };
    }

    /** Inclusive hour ranges, given as start/end pairs. */
    private static Set<Integer> hours(int... ranges) {
        return IntStream.range(0, ranges.length / 2)
                .flatMap(i -> IntStream.rangeClosed(ranges[2 * i], ranges[2 * i + 1]))
                .boxed()
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String firstNonNull(String... values) {
        for (String v : values) if (v != null) return v;
        return null;
    }

    static LocalDateTime ceilToHour(LocalDateTime t) {
        LocalDateTime floor = t.truncatedTo(ChronoUnit.HOURS);
        return floor.equals(t) ? t : floor.plusHours(1);
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) { return a.isAfter(b) ? a : b; }

    private static LocalDateTime min(LocalDateTime a, LocalDateTime b) { return a.isBefore(b) ? a : b; }

    static String format(LocalDateTime t) {
        return t.format(SLOT_FORMAT);
    }
}
