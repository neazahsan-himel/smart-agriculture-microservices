package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.CropCalendarEntryDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CropCalendarEntryService {

    CropCalendarEntryDto.Response createEntry(CropCalendarEntryDto.Request request);

    Page<CropCalendarEntryDto.Response> searchEntries(String crop, KnowledgeEnums.CropSeason season,
                                                      KnowledgeEnums.CalendarActivity activity, String region,
                                                      Pageable pageable);

    CropCalendarEntryDto.Response getEntryById(String id);

    CropCalendarEntryDto.Response updateEntry(String id, CropCalendarEntryDto.Request request);

    void deleteEntry(String id);
}
