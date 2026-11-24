package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.CropCalendarEntryDto;
import com.smartagriculture.knowledgeservice.entity.CropCalendarEntry;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.exception.DuplicateResourceException;
import com.smartagriculture.knowledgeservice.exception.ResourceNotFoundException;
import com.smartagriculture.knowledgeservice.repository.CropCalendarEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.MonthDay;

@Service
@RequiredArgsConstructor
@Slf4j
public class CropCalendarEntryServiceImpl implements CropCalendarEntryService {

    private final CropCalendarEntryRepository calendarRepository;
    private final SourceResolver sourceResolver;

    @Override
    @Transactional
    public CropCalendarEntryDto.Response createEntry(CropCalendarEntryDto.Request request) {
        String crop = KnowledgeText.crop(request.getCrop());
        log.info("Creating calendar entry: crop={}, season={}, activity={}", crop, request.getSeason(), request.getActivity());
        sourceResolver.requireSource(request.getSourceId());
        validateDates(request.getStartMonth(), request.getStartDay(), request.getEndMonth(), request.getEndDay());
        if (calendarRepository.existsByCropAndSeasonAndRegionAndActivityAndDeletedFalse(
                crop, request.getSeason(), request.getRegion(), request.getActivity())) {
            throw new DuplicateResourceException(duplicateMessage(crop, request.getSeason(), request.getRegion(), request.getActivity()));
        }
        CropCalendarEntry entry = CropCalendarEntry.builder()
                .crop(crop)
                .season(request.getSeason())
                .region(request.getRegion())
                .activity(request.getActivity())
                .startMonth(request.getStartMonth())
                .startDay(request.getStartDay())
                .endMonth(request.getEndMonth())
                .endDay(request.getEndDay())
                .notes(request.getNotes())
                .sourceId(request.getSourceId())
                .sourcePage(request.getSourcePage())
                .build();
        return mapToResponse(calendarRepository.saveAndFlush(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CropCalendarEntryDto.Response> searchEntries(String crop, KnowledgeEnums.CropSeason season,
                                                             KnowledgeEnums.CalendarActivity activity, String region,
                                                             Pageable pageable) {
        return calendarRepository.search(KnowledgeText.crop(KnowledgeText.filter(crop)), season, activity,
                KnowledgeText.filter(region), pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CropCalendarEntryDto.Response getEntryById(String id) {
        return mapToResponse(findActiveById(id));
    }

    @Override
    @Transactional
    public CropCalendarEntryDto.Response updateEntry(String id, CropCalendarEntryDto.Request request) {
        log.info("Updating calendar entry id={}", id);
        CropCalendarEntry entry = findActiveById(id);
        String crop = KnowledgeText.crop(request.getCrop());
        sourceResolver.requireSource(request.getSourceId());
        // Start/end month and day are all @NotNull, so the window is always replaced as a whole
        validateDates(request.getStartMonth(), request.getStartDay(), request.getEndMonth(), request.getEndDay());

        KnowledgeEnums.CropSeason season = request.getSeason() != null ? request.getSeason() : entry.getSeason();
        String region = request.getRegion() != null ? request.getRegion() : entry.getRegion();
        if (calendarRepository.existsByCropAndSeasonAndRegionAndActivityAndDeletedFalseAndIdNot(
                crop, season, region, request.getActivity(), id)) {
            throw new DuplicateResourceException(duplicateMessage(crop, season, region, request.getActivity()));
        }

        entry.setCrop(crop);
        entry.setSeason(season);
        entry.setRegion(region);
        entry.setActivity(request.getActivity());
        entry.setStartMonth(request.getStartMonth());
        entry.setStartDay(request.getStartDay());
        entry.setEndMonth(request.getEndMonth());
        entry.setEndDay(request.getEndDay());
        entry.setSourceId(request.getSourceId());
        if (request.getNotes() != null)      entry.setNotes(request.getNotes());
        if (request.getSourcePage() != null) entry.setSourcePage(request.getSourcePage());

        return mapToResponse(calendarRepository.saveAndFlush(entry));
    }

    @Override
    @Transactional
    public void deleteEntry(String id) {
        log.info("Soft-deleting calendar entry id={}", id);
        CropCalendarEntry entry = findActiveById(id);
        entry.setDeleted(true);
        entry.setStatus(KnowledgeEnums.RecordStatus.INACTIVE);
        calendarRepository.save(entry);
    }

    // Bean validation checks each field's range; this catches impossible dates such as 31 April
    private void validateDates(int startMonth, int startDay, int endMonth, int endDay) {
        try {
            MonthDay.of(startMonth, startDay);
            MonthDay.of(endMonth, endDay);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid calendar date: " + e.getMessage());
        }
    }

    private CropCalendarEntry findActiveById(String id) {
        return calendarRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar entry not found with id: " + id));
    }

    private String duplicateMessage(String crop, KnowledgeEnums.CropSeason season, String region,
                                    KnowledgeEnums.CalendarActivity activity) {
        return "Calendar entry already exists: " + crop + " / " + season + " / "
                + (region != null ? region : "country-wide") + " / " + activity;
    }

    private CropCalendarEntryDto.Response mapToResponse(CropCalendarEntry c) {
        return CropCalendarEntryDto.Response.builder()
                .id(c.getId())
                .crop(c.getCrop())
                .season(c.getSeason())
                .region(c.getRegion())
                .activity(c.getActivity())
                .startMonth(c.getStartMonth())
                .startDay(c.getStartDay())
                .endMonth(c.getEndMonth())
                .endDay(c.getEndDay())
                .notes(c.getNotes())
                .source(sourceResolver.toRef(c.getSourceId(), c.getSourcePage()))
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
