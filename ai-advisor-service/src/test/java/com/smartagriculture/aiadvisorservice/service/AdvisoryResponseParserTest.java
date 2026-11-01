package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase.ConfidenceLevel;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase.RiskLevel;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;
import com.smartagriculture.aiadvisorservice.service.AdvisoryResponseParser.ParsedAdvice;
import com.smartagriculture.aiadvisorservice.service.AdvisoryResponseParser.SuggestedTask;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test (no Spring context). The llava:7b cases reproduce the three reply shapes
 * observed in DAY9-PROGRESS.md §4 that broke the original single-block parser.
 */
class AdvisoryResponseParserTest {

    private static final String DIAGNOSIS =
            "The photo shows yellow spots on the tomato leaves.\nThis looks like early blight.";

    private final AdvisoryResponseParser parser = new AdvisoryResponseParser();

    @Test
    void parsesWellFormedTrailingBlock() {
        String raw = DIAGNOSIS + "\n---\nCONFIDENCE: HIGH\nRISK: MEDIUM\nFOLLOW_UP_DAYS: 3\nESCALATE: NO";

        ParsedAdvice result = parser.parse(raw);

        assertThat(result.visibleText()).isEqualTo(DIAGNOSIS);
        assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        assertThat(result.risk()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.followUpDays()).isEqualTo(3);
        assertThat(result.escalate()).isFalse();
    }

    @Test
    void llavaShape1_tagsWithoutDashSeparator() {
        String raw = DIAGNOSIS + "\n\nCONFIDENCE: LOW\nRISK: MEDIUM\nFOLLOW_UP_DAYS: 2\nESCALATE: NO";

        ParsedAdvice result = parser.parse(raw);

        assertThat(result.visibleText()).isEqualTo(DIAGNOSIS);
        assertThat(result.confidence()).isEqualTo(ConfidenceLevel.LOW);
        assertThat(result.risk()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.followUpDays()).isEqualTo(2);
    }

    @Test
    void llavaShape2_tagBlockRepeated_keepsProseAndUsesLastValues() {
        String raw = "---\nCONFIDENCE: MEDIUM\nRISK: LOW\nFOLLOW_UP_DAYS: 7\nESCALATE: NO\n\n"
                + DIAGNOSIS
                + "\n---\nCONFIDENCE: LOW\nRISK: HIGH\nFOLLOW_UP_DAYS: 1\nESCALATE: YES";

        ParsedAdvice result = parser.parse(raw);

        assertThat(result.visibleText()).isEqualTo(DIAGNOSIS);
        assertThat(result.confidence()).isEqualTo(ConfidenceLevel.LOW);
        assertThat(result.risk()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.followUpDays()).isEqualTo(1);
        assertThat(result.escalate()).isTrue();
    }

    @Test
    void llavaShape3_tagBlockBeforeProse() {
        String raw = "---\nCONFIDENCE: LOW\nRISK: MEDIUM\nFOLLOW_UP_DAYS: NONE\nESCALATE: NO\n\n" + DIAGNOSIS;

        ParsedAdvice result = parser.parse(raw);

        assertThat(result.visibleText()).isEqualTo(DIAGNOSIS);
        assertThat(result.confidence()).isEqualTo(ConfidenceLevel.LOW);
        assertThat(result.risk()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.followUpDays()).isNull();
    }

    @Test
    void lowercaseAndIndentedTagsAreStillParsedAndStripped() {
        String raw = DIAGNOSIS + "\n---\n  confidence: high\n  risk: critical\n  escalate: yes";

        ParsedAdvice result = parser.parse(raw);

        assertThat(result.visibleText()).isEqualTo(DIAGNOSIS);
        assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        assertThat(result.risk()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(result.escalate()).isTrue();
    }

    @Test
    void replyWithNoTags_returnsTextAndSafeDefaults() {
        ParsedAdvice result = parser.parse("  " + DIAGNOSIS + "  ");

        assertThat(result.visibleText()).isEqualTo(DIAGNOSIS);
        assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        assertThat(result.risk()).isEqualTo(RiskLevel.LOW);
        assertThat(result.followUpDays()).isNull();
        assertThat(result.escalate()).isFalse();
    }

    @Test
    void invalidTagValuesFallBackToDefaults() {
        String raw = DIAGNOSIS + "\n---\nCONFIDENCE: VERY_HIGH\nRISK: unknown\nFOLLOW_UP_DAYS: soon";

        ParsedAdvice result = parser.parse(raw);

        assertThat(result.visibleText()).isEqualTo(DIAGNOSIS);
        assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        assertThat(result.risk()).isEqualTo(RiskLevel.LOW);
        assertThat(result.followUpDays()).isNull();
    }

    @Test
    void taskTag_isParsedAndStrippedFromVisibleText() {
        String raw = DIAGNOSIS + "\n---\nCONFIDENCE: HIGH\nRISK: MEDIUM\nFOLLOW_UP_DAYS: 3\nESCALATE: NO\nTASK: SPRAY 1";

        ParsedAdvice result = parser.parse(raw);

        assertThat(result.visibleText()).isEqualTo(DIAGNOSIS);
        assertThat(result.suggestedTask()).isEqualTo(new SuggestedTask(WeatherTaskType.SPRAY, 1));
    }

    @Test
    void taskTag_withoutDays_meansToday_andIsCaseInsensitive() {
        ParsedAdvice result = parser.parse(DIAGNOSIS + "\n---\nCONFIDENCE: HIGH\ntask: irrigate");

        assertThat(result.suggestedTask()).isEqualTo(new SuggestedTask(WeatherTaskType.IRRIGATE, 0));
    }

    @Test
    void taskTag_daysAreCappedAt14() {
        ParsedAdvice result = parser.parse(DIAGNOSIS + "\n---\nCONFIDENCE: HIGH\nTASK: SOW 60");

        assertThat(result.suggestedTask().daysFromNow()).isEqualTo(AdvisoryResponseParser.MAX_TASK_DAYS);
    }

    @Test
    void taskTag_noneOrUnknownType_givesNoTask() {
        assertThat(parser.parse(DIAGNOSIS + "\n---\nCONFIDENCE: HIGH\nTASK: NONE").suggestedTask()).isNull();

        ParsedAdvice unknown = parser.parse(DIAGNOSIS + "\n---\nCONFIDENCE: HIGH\nTASK: WEEDING 2");
        assertThat(unknown.suggestedTask()).isNull();
        assertThat(unknown.visibleText()).isEqualTo(DIAGNOSIS);
    }

    @Test
    void taskTag_lastLineWins_soALaterNoneCancelsAnEarlierTask() {
        String raw = "---\nTASK: SPRAY 1\n\n" + DIAGNOSIS + "\n---\nCONFIDENCE: LOW\nTASK: NONE";

        assertThat(parser.parse(raw).suggestedTask()).isNull();
    }

    @Test
    void taskWordInsideProse_isNotTreatedAsATag() {
        String raw = "Your main TASK: SPRAY 1 litre per plot.\n---\nCONFIDENCE: HIGH";

        ParsedAdvice result = parser.parse(raw);

        assertThat(result.suggestedTask()).isNull();
        assertThat(result.visibleText()).isEqualTo("Your main TASK: SPRAY 1 litre per plot.");
    }

    @Test
    void nullOrBlankReply_returnsEmptyTextAndDefaults() {
        for (String raw : new String[]{null, "", "   \n "}) {
            ParsedAdvice result = parser.parse(raw);

            assertThat(result.visibleText()).isEmpty();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.risk()).isEqualTo(RiskLevel.LOW);
            assertThat(result.escalate()).isFalse();
        }
    }
}
