package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.client.OllamaApiClient;
import com.smartagriculture.aiadvisorservice.dto.AdvisorDto;
import com.smartagriculture.aiadvisorservice.dto.external.CropSummary;
import com.smartagriculture.aiadvisorservice.dto.external.FarmerResponse;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAdvisorServiceImpl implements AiAdvisorService {

    private static final String SYSTEM_PROMPT = """
            You are an expert agricultural advisor for the Smart Agriculture Advisor system.
            You help farmers in developing countries, especially South Asia (Bangladesh, India, Pakistan),
            with practical crop and weather-based farming advice.

            Always provide:
            1. A brief assessment of the farmer's situation (1-2 sentences)
            2. Specific, actionable recommendations (2-4 bullet points)
            3. Any important warnings or risk factors to watch

            Keep responses concise, practical, and in simple language suitable for smallholder farmers.
            Base your advice strictly on the context provided (farmer profile, crop catalog, weather data).
            """;

    private final ExternalContextFetcher contextFetcher;
    private final OllamaApiClient ollamaApiClient;

    @Override
    public AdvisorDto.Response getAdvice(AdvisorDto.Request request) {
        log.info("Getting advice for queryType={}, farmerId={}", request.getQueryType(), request.getFarmerId());

        FarmerResponse farmer = contextFetcher.fetchFarmer(request.getFarmerId());
        List<CropSummary> crops = contextFetcher.fetchCrops();
        List<WeatherSummary> weatherRecords = contextFetcher.fetchWeather();

        String effectiveCountryCode = resolveCountryCode(request, farmer);
        String effectiveRegion = resolveRegion(request, farmer);

        String userMessage = buildUserMessage(request, farmer, crops, weatherRecords);
        String advice = ollamaApiClient.getAdvice(SYSTEM_PROMPT, userMessage);

        return AdvisorDto.Response.builder()
                .id(UUID.randomUUID().toString())
                .question(request.getQuestion())
                .queryType(request.getQueryType())
                .advice(advice)
                .farmerId(request.getFarmerId())
                .countryCode(effectiveCountryCode)
                .region(effectiveRegion)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Prompt Builder ────────────────────────────────────────────────────────

    private String buildUserMessage(AdvisorDto.Request request, FarmerResponse farmer,
                                    List<CropSummary> crops, List<WeatherSummary> weatherRecords) {
        StringBuilder sb = new StringBuilder();

        sb.append("Query Type: ").append(request.getQueryType()).append("\n\n");

        sb.append("== FARMER PROFILE ==\n");
        if (farmer != null) {
            sb.append("Name: ").append(farmer.getName()).append("\n");
            sb.append("Location: ").append(farmer.getRegion()).append(", ").append(farmer.getCountryCode()).append("\n");
            if (farmer.getSoilType() != null)
                sb.append("Soil Type: ").append(farmer.getSoilType()).append("\n");
            if (farmer.getFarmSizeHectares() != null)
                sb.append("Farm Size: ").append(farmer.getFarmSizeHectares()).append(" hectares\n");
        } else {
            String country = request.getCountryCode() != null ? request.getCountryCode() : "Unknown";
            String region = request.getRegion() != null ? request.getRegion() : "Unknown";
            sb.append("Location: ").append(region).append(", ").append(country).append("\n");
            sb.append("(No farmer profile linked)\n");
        }

        sb.append("\n== AVAILABLE CROPS IN CATALOG ==\n");
        if (crops.isEmpty()) {
            sb.append("No crop data available.\n");
        } else {
            crops.stream().limit(15).forEach(crop -> {
                sb.append("- ").append(crop.getName());
                if (crop.getVariety() != null) sb.append(" (").append(crop.getVariety()).append(")");
                sb.append(" | Type: ").append(crop.getCropType());
                sb.append(" | Season: ").append(crop.getSeason());
                if (crop.getIdealSoilType() != null)
                    sb.append(" | Soil: ").append(crop.getIdealSoilType());
                if (crop.getMinTemperatureCelsius() != null && crop.getMaxTemperatureCelsius() != null)
                    sb.append(" | Temp: ").append(crop.getMinTemperatureCelsius())
                      .append("–").append(crop.getMaxTemperatureCelsius()).append("°C");
                if (crop.getGrowingDurationDays() != null)
                    sb.append(" | Duration: ").append(crop.getGrowingDurationDays()).append(" days");
                sb.append("\n");
            });
        }

        sb.append("\n== RECENT WEATHER CONDITIONS ==\n");
        if (weatherRecords.isEmpty()) {
            sb.append("No weather data available.\n");
        } else {
            weatherRecords.forEach(w -> {
                sb.append("- ").append(w.getRecordType()).append(" at ").append(w.getRecordedAt());
                sb.append(" | ").append(w.getWeatherCondition());
                if (w.getTemperatureCelsius() != null)
                    sb.append(" | Temp: ").append(w.getTemperatureCelsius()).append("°C");
                if (w.getHumidityPercent() != null)
                    sb.append(" | Humidity: ").append(w.getHumidityPercent()).append("%");
                if (w.getRainfallMm() != null)
                    sb.append(" | Rainfall: ").append(w.getRainfallMm()).append("mm");
                sb.append(" | Location: ").append(w.getRegion()).append(", ").append(w.getCountryCode());
                sb.append("\n");
            });
        }

        sb.append("\n== FARMER'S QUESTION ==\n");
        sb.append(request.getQuestion()).append("\n");

        return sb.toString();
    }

    private String resolveCountryCode(AdvisorDto.Request request, FarmerResponse farmer) {
        if (request.getCountryCode() != null) return request.getCountryCode();
        if (farmer != null) return farmer.getCountryCode();
        return null;
    }

    private String resolveRegion(AdvisorDto.Request request, FarmerResponse farmer) {
        if (request.getRegion() != null) return request.getRegion();
        if (farmer != null) return farmer.getRegion();
        return null;
    }
}