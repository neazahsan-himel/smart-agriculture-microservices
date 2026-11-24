package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.KnowledgeSourceDto;
import com.smartagriculture.knowledgeservice.exception.ResourceNotFoundException;
import com.smartagriculture.knowledgeservice.repository.KnowledgeSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Enforces "every fact cites a source" and builds the short citation embedded in fact responses. */
@Component
@RequiredArgsConstructor
public class SourceResolver {

    private final KnowledgeSourceRepository sourceRepository;

    public void requireSource(String sourceId) {
        if (!sourceRepository.existsByIdAndDeletedFalse(sourceId)) {
            throw new ResourceNotFoundException("Source not found with id: " + sourceId);
        }
    }

    // One lookup per row is fine at pilot scale (hundreds of rows per page at most)
    public KnowledgeSourceDto.SourceRef toRef(String sourceId, String page) {
        return sourceRepository.findById(sourceId)
                .map(s -> KnowledgeSourceDto.SourceRef.builder()
                        .id(s.getId())
                        .title(s.getTitle())
                        .publisher(s.getPublisher())
                        .edition(s.getEdition())
                        .publicationYear(s.getPublicationYear())
                        .page(page)
                        .build())
                .orElse(KnowledgeSourceDto.SourceRef.builder().id(sourceId).page(page).build());
    }
}
