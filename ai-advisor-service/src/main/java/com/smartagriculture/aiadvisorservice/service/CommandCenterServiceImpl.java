package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.dto.CommandCenterDto;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.FarmEvent;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.repository.DiagnosticCaseRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmEventRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmTaskRepository;
import com.smartagriculture.aiadvisorservice.service.weather.WeatherTaskAdvisor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandCenterServiceImpl implements CommandCenterService {

    static final int MAX_DAYS = 14;
    static final int RECENT_EVENT_DAYS = 7;

    private final FarmTaskRepository farmTaskRepository;
    private final DiagnosticCaseRepository diagnosticCaseRepository;
    private final FarmEventRepository farmEventRepository;
    private final WeatherTaskAdvisor weatherTaskAdvisor;

    @Override
    @Transactional(readOnly = true)
    public CommandCenterDto.Response getWeek(String farmerId, int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new IllegalArgumentException("days must be between 1 and " + MAX_DAYS);
        }
        LocalDateTime now = LocalDateTime.now();
        // +1 day past the window: a weather task's best time can be up to 24h before its due date
        LocalDateTime dueBefore = now.toLocalDate().plusDays(days + 1L).atStartOfDay();

        List<FarmTask> tasks = farmTaskRepository.findByFarmerIdAndStatusAndDeletedFalseAndDueDateLessThanOrderByDueDateAsc(
                farmerId, FarmTask.TaskStatus.PENDING, dueBefore);

        List<String> caseIds = tasks.stream().map(FarmTask::getSourceDiagnosticCaseId).filter(Objects::nonNull).distinct().toList();
        Map<String, DiagnosticCase> casesById = diagnosticCaseRepository.findAllById(caseIds).stream()
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .collect(Collectors.toMap(DiagnosticCase::getId, Function.identity()));

        // Never throws: empty when the farmer has no location or weather-service is down
        List<WeatherSummary> forecast = weatherTaskAdvisor.forecastFor(farmerId);

        List<FarmEvent> events = farmEventRepository.findTop10ByFarmerIdAndOccurredAtGreaterThanEqualOrderByOccurredAtDesc(
                farmerId, now.minusDays(RECENT_EVENT_DAYS));

        log.info("Command center for farmer {}: {} pending tasks, {} forecast hours, {} recent events",
                farmerId, tasks.size(), forecast.size(), events.size());
        return WeekPlanBuilder.build(farmerId, now, days, tasks, casesById, forecast, events);
    }
}
