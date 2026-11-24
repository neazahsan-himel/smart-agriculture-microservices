package com.smartagriculture.knowledgeservice.importer;

import com.smartagriculture.knowledgeservice.dto.*;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.entity.KnowledgeSource;
import com.smartagriculture.knowledgeservice.repository.KnowledgeSourceRepository;
import com.smartagriculture.knowledgeservice.service.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads CSV files extracted from the official sources (pilot plan Phase 1). Each row goes through
 * the same validation, duplicate and source checks as the REST API, independently: a bad row is
 * reported and the rest still import. Column names are the Request field names (see
 * knowledge-service/import-templates/). Fact rows give either sourceId or sourceTitle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeImportService {

    private final Validator validator;
    private final KnowledgeSourceRepository sourceRepository;
    private final KnowledgeSourceService sourceService;
    private final PesticideProductService pesticideService;
    private final FertilizerRecommendationService fertilizerService;
    private final CropCalendarEntryService calendarService;
    private final PestDiseaseEntryService pestDiseaseService;

    public ImportReport importCsv(ImportType type, String csv) {
        List<CsvParser.CsvRow> rows = CsvParser.parse(csv);
        ImportReport report = ImportReport.builder().type(type.path()).totalRows(rows.size()).build();

        for (CsvParser.CsvRow row : rows) {
            try {
                importRow(type, new RowReader(row.values()));
                report.setImported(report.getImported() + 1);
            } catch (RuntimeException e) {
                // Duplicate (409), missing source (404), bad value or failed validation (400): report and continue
                report.getErrors().add(new ImportReport.RowError(row.number(), e.getMessage()));
                report.setFailed(report.getFailed() + 1);
            }
        }
        log.info("Imported {}: {} of {} rows ({} failed)", type.path(), report.getImported(), report.getTotalRows(), report.getFailed());
        return report;
    }

    private void importRow(ImportType type, RowReader r) {
        switch (type) {
            case SOURCES -> sourceService.createSource(validated(KnowledgeSourceDto.Request.builder()
                    .title(r.str("title"))
                    .publisher(r.str("publisher"))
                    .edition(r.str("edition"))
                    .publicationYear(r.integer("publicationYear"))
                    .url(r.str("url"))
                    .language(r.str("language"))
                    .licenceNote(r.str("licenceNote"))
                    .retrievedAt(r.date("retrievedAt"))
                    .build()));
            case PESTICIDES -> pesticideService.createPesticide(validated(PesticideProductDto.Request.builder()
                    .tradeName(r.str("tradeName"))
                    .activeIngredient(r.str("activeIngredient"))
                    .formulation(r.str("formulation"))
                    .registrationNo(r.str("registrationNo"))
                    .crop(r.str("crop"))
                    .target(r.str("target"))
                    .doseValue(r.dbl("doseValue"))
                    .doseUnit(r.str("doseUnit"))
                    .doseBasis(r.str("doseBasis"))
                    .preHarvestIntervalDays(r.integer("preHarvestIntervalDays"))
                    .toxicityClass(r.str("toxicityClass"))
                    .registrationStatus(r.enumValue("registrationStatus", KnowledgeEnums.RegistrationStatus.class))
                    .notes(r.str("notes"))
                    .sourceId(resolveSourceId(r))
                    .sourcePage(r.str("sourcePage"))
                    .build()));
            case FERTILIZER_RECOMMENDATIONS -> fertilizerService.createRecommendation(validated(FertilizerRecommendationDto.Request.builder()
                    .crop(r.str("crop"))
                    .season(r.enumValue("season", KnowledgeEnums.CropSeason.class))
                    .region(r.str("region"))
                    .soilFertilityClass(r.str("soilFertilityClass"))
                    .ureaKgPerHa(r.dbl("ureaKgPerHa"))
                    .tspKgPerHa(r.dbl("tspKgPerHa"))
                    .mopKgPerHa(r.dbl("mopKgPerHa"))
                    .gypsumKgPerHa(r.dbl("gypsumKgPerHa"))
                    .zincSulphateKgPerHa(r.dbl("zincSulphateKgPerHa"))
                    .splitSchedule(r.str("splitSchedule"))
                    .notes(r.str("notes"))
                    .sourceId(resolveSourceId(r))
                    .sourcePage(r.str("sourcePage"))
                    .build()));
            case CROP_CALENDAR -> calendarService.createEntry(validated(CropCalendarEntryDto.Request.builder()
                    .crop(r.str("crop"))
                    .season(r.enumValue("season", KnowledgeEnums.CropSeason.class))
                    .region(r.str("region"))
                    .activity(r.enumValue("activity", KnowledgeEnums.CalendarActivity.class))
                    .startMonth(r.integer("startMonth"))
                    .startDay(r.integer("startDay"))
                    .endMonth(r.integer("endMonth"))
                    .endDay(r.integer("endDay"))
                    .notes(r.str("notes"))
                    .sourceId(resolveSourceId(r))
                    .sourcePage(r.str("sourcePage"))
                    .build()));
            case PESTS_DISEASES -> pestDiseaseService.createEntry(validated(PestDiseaseEntryDto.Request.builder()
                    .crop(r.str("crop"))
                    .kind(r.enumValue("kind", KnowledgeEnums.PestDiseaseKind.class))
                    .nameEn(r.str("nameEn"))
                    .nameBn(r.str("nameBn"))
                    .symptoms(r.str("symptoms"))
                    .favourableConditions(r.str("favourableConditions"))
                    .actionThreshold(r.str("actionThreshold"))
                    .culturalControl(r.str("culturalControl"))
                    .chemicalControlActiveIngredients(r.str("chemicalControlActiveIngredients"))
                    .sourceId(resolveSourceId(r))
                    .sourcePage(r.str("sourcePage"))
                    .build()));
        }
    }

    // sourceId wins; otherwise sourceTitle must match exactly one active source
    private String resolveSourceId(RowReader r) {
        String sourceId = r.str("sourceId");
        if (sourceId != null) {
            return sourceId;
        }
        String title = r.str("sourceTitle");
        if (title == null) {
            return null; // validation reports "Source ID is required"
        }
        List<KnowledgeSource> matches = sourceRepository.findByTitleIgnoreCaseAndDeletedFalse(title);
        if (matches.size() != 1) {
            throw new IllegalArgumentException(matches.isEmpty()
                    ? "No source titled '" + title + "'. Import the source first."
                    : "Several sources titled '" + title + "' (different editions); use sourceId instead.");
        }
        return matches.get(0).getId();
    }

    private <T> T validated(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return request;
    }

    /** Typed access to one CSV row; a malformed value names its column in the error. */
    record RowReader(Map<String, String> values) {

        String str(String column) {
            return values.get(column);
        }

        Double dbl(String column) {
            String v = str(column);
            if (v == null) return null;
            try {
                return Double.valueOf(v);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(column + ": '" + v + "' is not a number");
            }
        }

        Integer integer(String column) {
            String v = str(column);
            if (v == null) return null;
            try {
                return Integer.valueOf(v);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(column + ": '" + v + "' is not a whole number");
            }
        }

        LocalDate date(String column) {
            String v = str(column);
            if (v == null) return null;
            try {
                return LocalDate.parse(v);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(column + ": '" + v + "' is not a date (use YYYY-MM-DD)");
            }
        }

        <E extends Enum<E>> E enumValue(String column, Class<E> type) {
            String v = str(column);
            if (v == null) return null;
            try {
                return Enum.valueOf(type, v.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(column + ": '" + v + "' is not one of "
                        + java.util.Arrays.toString(type.getEnumConstants()));
            }
        }
    }
}
