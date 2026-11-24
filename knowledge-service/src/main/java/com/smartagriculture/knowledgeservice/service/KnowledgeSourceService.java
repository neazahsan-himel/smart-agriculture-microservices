package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.KnowledgeSourceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KnowledgeSourceService {

    KnowledgeSourceDto.Response createSource(KnowledgeSourceDto.Request request);

    Page<KnowledgeSourceDto.Response> getAllSources(Pageable pageable);

    KnowledgeSourceDto.Response getSourceById(String id);

    KnowledgeSourceDto.Response updateSource(String id, KnowledgeSourceDto.Request request);

    void deleteSource(String id);
}
