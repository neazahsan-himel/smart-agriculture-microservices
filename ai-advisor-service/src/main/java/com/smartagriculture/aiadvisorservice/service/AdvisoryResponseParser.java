package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the trailing CONFIDENCE/RISK/FOLLOW_UP_DAYS/ESCALATE tag block that
 * {@code PromptTemplates.CONVERSATION_SYSTEM_PROMPT} instructs the model to append,
 * and strips it from the farmer-visible text. A small local model won't reliably
 * produce valid JSON, so a fixed-format trailing block + regex is far more robust —
 * any missing/unparseable tag falls back to a safe default instead of failing the request.
 */
@Component
@Slf4j
public class AdvisoryResponseParser {

    private static final Pattern TAG_BLOCK = Pattern.compile("(?s)---\\s*\\n?(.*)$");
    private static final Pattern CONFIDENCE = Pattern.compile("CONFIDENCE:\\s*(LOW|MEDIUM|HIGH)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RISK = Pattern.compile("RISK:\\s*(LOW|MEDIUM|HIGH|CRITICAL)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOLLOW_UP_DAYS = Pattern.compile("FOLLOW_UP_DAYS:\\s*(\\d+|NONE)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ESCALATE = Pattern.compile("ESCALATE:\\s*(YES|NO)", Pattern.CASE_INSENSITIVE);

    public ParsedAdvice parse(String rawReply) {
        if (rawReply == null || rawReply.isBlank()) {
            return new ParsedAdvice("", DiagnosticCase.ConfidenceLevel.MEDIUM, DiagnosticCase.RiskLevel.LOW, null, false);
        }

        Matcher blockMatcher = TAG_BLOCK.matcher(rawReply);
        String tagBlock = "";
        String visibleText = rawReply.trim();

        if (blockMatcher.find()) {
            String candidate = blockMatcher.group(1);
            // Only treat the trailing "---" section as the tag block if it actually contains a known tag,
            // otherwise a farmer question that legitimately contains "---" would get truncated.
            if (CONFIDENCE.matcher(candidate).find() || RISK.matcher(candidate).find()
                    || FOLLOW_UP_DAYS.matcher(candidate).find() || ESCALATE.matcher(candidate).find()) {
                tagBlock = candidate;
                visibleText = rawReply.substring(0, blockMatcher.start()).trim();
            }
        }

        DiagnosticCase.ConfidenceLevel confidence = extract(CONFIDENCE, tagBlock)
                .map(v -> DiagnosticCase.ConfidenceLevel.valueOf(v.toUpperCase()))
                .orElse(DiagnosticCase.ConfidenceLevel.MEDIUM);

        DiagnosticCase.RiskLevel risk = extract(RISK, tagBlock)
                .map(v -> DiagnosticCase.RiskLevel.valueOf(v.toUpperCase()))
                .orElse(DiagnosticCase.RiskLevel.LOW);

        Integer followUpDays = extract(FOLLOW_UP_DAYS, tagBlock)
                .filter(v -> !"NONE".equalsIgnoreCase(v))
                .map(Integer::parseInt)
                .orElse(null);

        boolean escalate = extract(ESCALATE, tagBlock)
                .map(v -> v.equalsIgnoreCase("YES"))
                .orElse(false);

        if (tagBlock.isBlank()) {
            log.warn("Model reply had no parseable CONFIDENCE/RISK tag block — using safe defaults");
        }

        return new ParsedAdvice(visibleText, confidence, risk, followUpDays, escalate);
    }

    private java.util.Optional<String> extract(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? java.util.Optional.of(m.group(1)) : java.util.Optional.empty();
    }

    public record ParsedAdvice(
            String visibleText,
            DiagnosticCase.ConfidenceLevel confidence,
            DiagnosticCase.RiskLevel risk,
            Integer followUpDays,
            boolean escalate
    ) {}
}
