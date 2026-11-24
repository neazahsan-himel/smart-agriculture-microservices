package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.PestDiseaseEntryDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PestDiseaseEntryService {

    PestDiseaseEntryDto.Response createEntry(PestDiseaseEntryDto.Request request);

    Page<PestDiseaseEntryDto.Response> searchEntries(String crop, KnowledgeEnums.PestDiseaseKind kind, String q,
                                                     Pageable pageable);

    PestDiseaseEntryDto.Response getEntryById(String id);

    PestDiseaseEntryDto.Response updateEntry(String id, PestDiseaseEntryDto.Request request);

    void deleteEntry(String id);
}
