package com.smartagriculture.aiadvisorservice.service.weather;

import com.smartagriculture.aiadvisorservice.dto.external.FarmerResponse;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;
import com.smartagriculture.aiadvisorservice.service.ExternalContextFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Weather-to-Decision glue: fetches the forecast for a task's farmer, runs
 * {@link BestTimeEvaluator}, and writes the result onto the task (the caller saves it).
 * Shared by the farm-task API, the conversation flow and {@code WeatherTaskScheduler}.
 */
@Component
@RequiredArgsConstructor
public class WeatherTaskAdvisor {

    // The evaluator searches up to 48h past the due date; 72h covers that for tasks due within a day
    static final int FORECAST_HOURS = 72;

    private final ExternalContextFetcher contextFetcher;
    private final BestTimeEvaluator bestTimeEvaluator;

    /** Hourly forecast for a farmer's location; empty when unavailable (never throws). */
    public List<WeatherSummary> forecastFor(String farmerId) {
        FarmerResponse farmer = contextFetcher.fetchFarmer(farmerId);
        return contextFetcher.fetchForecast(farmer, FORECAST_HOURS);
    }

    public BestTimeEvaluator.BestTimeResult evaluate(FarmTask task) {
        return evaluate(task, forecastFor(task.getFarmerId()), LocalDateTime.now());
    }

    public BestTimeEvaluator.BestTimeResult evaluate(FarmTask task, List<WeatherSummary> forecast, LocalDateTime now) {
        WeatherTaskType type = WeatherTaskType.fromTaskType(task.getTaskType());
        if (type == null) {
            throw new IllegalArgumentException("Not a weather-dependent task type: " + task.getTaskType());
        }
        BestTimeEvaluator.BestTimeResult result = bestTimeEvaluator.evaluate(type, forecast, now, task.getDueDate());
        task.setRecommendedStartAt(result.recommendedStart());
        task.setWeatherAdvice(result.advice());
        task.setWeatherCheckedAt(now);
        return result;
    }
}
