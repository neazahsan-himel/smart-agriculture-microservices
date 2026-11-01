package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the CONFIDENCE/RISK/FOLLOW_UP_DAYS/ESCALATE/TASK tags that
 * {@code PromptTemplates.CONVERSATION_SYSTEM_PROMPT} instructs the model to append as a trailing
 * "---" block, and strips them from the farmer-visible text. A small local model won't reliably
 * produce valid JSON, so fixed-format tag lines + regex are far more robust — any
 * missing/unparseable tag falls back to a safe default instead of failing the request.
 *
 * <p>Tags are matched and stripped line-by-line rather than as one contiguous block: some models
 * (observed with llava) don't follow the "prose, then --- separator, then tags at the very end"
 * order — they may omit the "---", put the tag block before the prose instead of after, or repeat
 * it. Stripping individual tag lines wherever they occur, and using the last value seen for each
 * tag, handles all of those without losing the surrounding diagnosis text.
 */
@Component
@Slf4j
public class AdvisoryResponseParser {

    private static final Pattern TAG_LINE =
            Pattern.compile("(?im)^[ \\t]*(?:CONFIDENCE|RISK|FOLLOW_UP_DAYS|ESCALATE|TASK):.*$\\R?");
    private static final Pattern DASH_LINE = Pattern.compile("(?m)^[ \\t]*---[ \\t]*$\\R?");
    private static final Pattern CONFIDENCE = Pattern.compile("CONFIDENCE:\\s*(LOW|MEDIUM|HIGH)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RISK = Pattern.compile("RISK:\\s*(LOW|MEDIUM|HIGH|CRITICAL)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOLLOW_UP_DAYS = Pattern.compile("FOLLOW_UP_DAYS:\\s*(\\d+|NONE)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ESCALATE = Pattern.compile("ESCALATE:\\s*(YES|NO)", Pattern.CASE_INSENSITIVE);
    // Any value is captured (not just valid task types) so a later "TASK: NONE" overrides an earlier task
    private static final Pattern TASK = Pattern.compile("(?im)^[ \\t]*TASK:[ \\t]*(\\w+)(?:[ \\t]+(\\d{1,3}))?");
    static final int MAX_TASK_DAYS = 14;
    private static final Pattern BLANK_LINE_RUN = Pattern.compile("\\n{3,}");

    public ParsedAdvice parse(String rawReply) {
        if (rawReply == null || rawReply.isBlank()) {
            return new ParsedAdvice("", DiagnosticCase.ConfidenceLevel.MEDIUM, DiagnosticCase.RiskLevel.LOW, null, false, null);
        }

        // Gate on tag *lines*, not valid tag values — otherwise a reply whose tags all have
        // unparseable values (e.g. "CONFIDENCE: VERY_HIGH") would leak those lines to the farmer.
        boolean hasAnyTag = TAG_LINE.matcher(rawReply).find();

        String visibleText;
        if (hasAnyTag) {
            String stripped = TAG_LINE.matcher(rawReply).replaceAll("");
            stripped = DASH_LINE.matcher(stripped).replaceAll("");
            visibleText = BLANK_LINE_RUN.matcher(stripped).replaceAll("\n\n").trim();
        } else {
            visibleText = rawReply.trim();
            log.warn("Model reply had no parseable CONFIDENCE/RISK tag block — using safe defaults");
        }

        DiagnosticCase.ConfidenceLevel confidence = extractLast(CONFIDENCE, rawReply)
                .map(v -> DiagnosticCase.ConfidenceLevel.valueOf(v.toUpperCase()))
                .orElse(DiagnosticCase.ConfidenceLevel.MEDIUM);

        DiagnosticCase.RiskLevel risk = extractLast(RISK, rawReply)
                .map(v -> DiagnosticCase.RiskLevel.valueOf(v.toUpperCase()))
                .orElse(DiagnosticCase.RiskLevel.LOW);

        Integer followUpDays = extractLast(FOLLOW_UP_DAYS, rawReply)
                .filter(v -> !"NONE".equalsIgnoreCase(v))
                .map(Integer::parseInt)
                .orElse(null);

        boolean escalate = extractLast(ESCALATE, rawReply)
                .map(v -> v.equalsIgnoreCase("YES"))
                .orElse(false);

        return new ParsedAdvice(visibleText, confidence, risk, followUpDays, escalate, extractTask(rawReply));
    }

    private SuggestedTask extractTask(String text) {
        Matcher m = TASK.matcher(text);
        String type = null;
        String days = null;
        while (m.find()) {
            type = m.group(1);
            days = m.group(2);
        }
        WeatherTaskType taskType = WeatherTaskType.fromTaskType(type);
        if (taskType == null) return null;
        int daysFromNow = days != null ? Math.min(Integer.parseInt(days), MAX_TASK_DAYS) : 0;
        return new SuggestedTask(taskType, daysFromNow);
    }

    private Optional<String> extractLast(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        String last = null;
        while (m.find()) {
            last = m.group(1);
        }
        return Optional.ofNullable(last);
    }

    public record ParsedAdvice(
            String visibleText,
            DiagnosticCase.ConfidenceLevel confidence,
            DiagnosticCase.RiskLevel risk,
            Integer followUpDays,
            boolean escalate,
            SuggestedTask suggestedTask   // null when the model suggested no weather-dependent action
    ) {}

    public record SuggestedTask(WeatherTaskType type, int daysFromNow) {}
}
