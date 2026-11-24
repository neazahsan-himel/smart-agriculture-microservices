package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.KnowledgeSourceDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.entity.KnowledgeSource;
import com.smartagriculture.knowledgeservice.exception.DuplicateResourceException;
import com.smartagriculture.knowledgeservice.exception.ResourceNotFoundException;
import com.smartagriculture.knowledgeservice.repository.KnowledgeSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeSourceServiceImpl implements KnowledgeSourceService {

    private final KnowledgeSourceRepository sourceRepository;

    @Override
    @Transactional
    public KnowledgeSourceDto.Response createSource(KnowledgeSourceDto.Request request) {
        log.info("Creating source: title={}, edition={}", request.getTitle(), request.getEdition());
        if (sourceRepository.existsByTitleIgnoreCaseAndEditionAndDeletedFalse(request.getTitle(), request.getEdition())) {
            throw new DuplicateResourceException(duplicateMessage(request.getTitle(), request.getEdition()));
        }
        KnowledgeSource source = KnowledgeSource.builder()
                .title(request.getTitle())
                .publisher(request.getPublisher())
                .edition(request.getEdition())
                .publicationYear(request.getPublicationYear())
                .url(request.getUrl())
                .language(request.getLanguage())
                .licenceNote(request.getLicenceNote())
                .retrievedAt(request.getRetrievedAt())
                .build();
        return mapToResponse(sourceRepository.saveAndFlush(source));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeSourceDto.Response> getAllSources(Pageable pageable) {
        return sourceRepository.findAllByDeletedFalse(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeSourceDto.Response getSourceById(String id) {
        return mapToResponse(findActiveById(id));
    }

    @Override
    @Transactional
    public KnowledgeSourceDto.Response updateSource(String id, KnowledgeSourceDto.Request request) {
        log.info("Updating source id={}", id);
        KnowledgeSource source = findActiveById(id);

        String effectiveEdition = request.getEdition() != null ? request.getEdition() : source.getEdition();
        if (sourceRepository.existsByTitleIgnoreCaseAndEditionAndDeletedFalseAndIdNot(request.getTitle(), effectiveEdition, id)) {
            throw new DuplicateResourceException(duplicateMessage(request.getTitle(), effectiveEdition));
        }

        source.setTitle(request.getTitle());         // required by validation
        source.setPublisher(request.getPublisher()); // required by validation
        if (request.getEdition() != null)         source.setEdition(request.getEdition());
        if (request.getPublicationYear() != null) source.setPublicationYear(request.getPublicationYear());
        if (request.getUrl() != null)             source.setUrl(request.getUrl());
        if (request.getLanguage() != null)        source.setLanguage(request.getLanguage());
        if (request.getLicenceNote() != null)     source.setLicenceNote(request.getLicenceNote());
        if (request.getRetrievedAt() != null)     source.setRetrievedAt(request.getRetrievedAt());

        return mapToResponse(sourceRepository.saveAndFlush(source));
    }

    @Override
    @Transactional
    public void deleteSource(String id) {
        log.info("Soft-deleting source id={}", id);
        KnowledgeSource source = findActiveById(id);
        source.setDeleted(true);
        source.setStatus(KnowledgeEnums.RecordStatus.INACTIVE);
        sourceRepository.save(source);
    }

    private KnowledgeSource findActiveById(String id) {
        return sourceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Source not found with id: " + id));
    }

    private String duplicateMessage(String title, String edition) {
        return "Source already exists: " + title + (edition != null ? " (" + edition + ")" : "");
    }

    private KnowledgeSourceDto.Response mapToResponse(KnowledgeSource s) {
        return KnowledgeSourceDto.Response.builder()
                .id(s.getId())
                .title(s.getTitle())
                .publisher(s.getPublisher())
                .edition(s.getEdition())
                .publicationYear(s.getPublicationYear())
                .url(s.getUrl())
                .language(s.getLanguage())
                .licenceNote(s.getLicenceNote())
                .retrievedAt(s.getRetrievedAt())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
