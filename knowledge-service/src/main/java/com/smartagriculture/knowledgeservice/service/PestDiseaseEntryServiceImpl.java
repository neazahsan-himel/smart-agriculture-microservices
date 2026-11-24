package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.PestDiseaseEntryDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.entity.PestDiseaseEntry;
import com.smartagriculture.knowledgeservice.exception.DuplicateResourceException;
import com.smartagriculture.knowledgeservice.exception.ResourceNotFoundException;
import com.smartagriculture.knowledgeservice.repository.PestDiseaseEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PestDiseaseEntryServiceImpl implements PestDiseaseEntryService {

    private final PestDiseaseEntryRepository pestDiseaseRepository;
    private final SourceResolver sourceResolver;

    @Override
    @Transactional
    public PestDiseaseEntryDto.Response createEntry(PestDiseaseEntryDto.Request request) {
        String crop = KnowledgeText.crop(request.getCrop());
        log.info("Creating pest/disease entry: crop={}, name={}", crop, request.getNameEn());
        sourceResolver.requireSource(request.getSourceId());
        if (pestDiseaseRepository.existsByCropAndNameEnIgnoreCaseAndDeletedFalse(crop, request.getNameEn())) {
            throw new DuplicateResourceException(duplicateMessage(crop, request.getNameEn()));
        }
        PestDiseaseEntry entry = PestDiseaseEntry.builder()
                .crop(crop)
                .kind(request.getKind())
                .nameEn(request.getNameEn())
                .nameBn(request.getNameBn())
                .symptoms(request.getSymptoms())
                .favourableConditions(request.getFavourableConditions())
                .actionThreshold(request.getActionThreshold())
                .culturalControl(request.getCulturalControl())
                .chemicalControlActiveIngredients(request.getChemicalControlActiveIngredients())
                .sourceId(request.getSourceId())
                .sourcePage(request.getSourcePage())
                .build();
        return mapToResponse(pestDiseaseRepository.saveAndFlush(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PestDiseaseEntryDto.Response> searchEntries(String crop, KnowledgeEnums.PestDiseaseKind kind, String q,
                                                            Pageable pageable) {
        return pestDiseaseRepository.search(KnowledgeText.crop(KnowledgeText.filter(crop)), kind,
                KnowledgeText.filter(q), pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PestDiseaseEntryDto.Response getEntryById(String id) {
        return mapToResponse(findActiveById(id));
    }

    @Override
    @Transactional
    public PestDiseaseEntryDto.Response updateEntry(String id, PestDiseaseEntryDto.Request request) {
        log.info("Updating pest/disease entry id={}", id);
        PestDiseaseEntry entry = findActiveById(id);
        String crop = KnowledgeText.crop(request.getCrop());
        sourceResolver.requireSource(request.getSourceId());
        if (pestDiseaseRepository.existsByCropAndNameEnIgnoreCaseAndDeletedFalseAndIdNot(crop, request.getNameEn(), id)) {
            throw new DuplicateResourceException(duplicateMessage(crop, request.getNameEn()));
        }

        // Required by validation, so always set
        entry.setCrop(crop);
        entry.setKind(request.getKind());
        entry.setNameEn(request.getNameEn());
        entry.setSourceId(request.getSourceId());

        if (request.getNameBn() != null)                           entry.setNameBn(request.getNameBn());
        if (request.getSymptoms() != null)                         entry.setSymptoms(request.getSymptoms());
        if (request.getFavourableConditions() != null)             entry.setFavourableConditions(request.getFavourableConditions());
        if (request.getActionThreshold() != null)                  entry.setActionThreshold(request.getActionThreshold());
        if (request.getCulturalControl() != null)                  entry.setCulturalControl(request.getCulturalControl());
        if (request.getChemicalControlActiveIngredients() != null) entry.setChemicalControlActiveIngredients(request.getChemicalControlActiveIngredients());
        if (request.getSourcePage() != null)                       entry.setSourcePage(request.getSourcePage());

        return mapToResponse(pestDiseaseRepository.saveAndFlush(entry));
    }

    @Override
    @Transactional
    public void deleteEntry(String id) {
        log.info("Soft-deleting pest/disease entry id={}", id);
        PestDiseaseEntry entry = findActiveById(id);
        entry.setDeleted(true);
        entry.setStatus(KnowledgeEnums.RecordStatus.INACTIVE);
        pestDiseaseRepository.save(entry);
    }

    private PestDiseaseEntry findActiveById(String id) {
        return pestDiseaseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pest/disease entry not found with id: " + id));
    }

    private String duplicateMessage(String crop, String nameEn) {
        return "Pest/disease entry already exists: " + nameEn + " on " + crop;
    }

    private PestDiseaseEntryDto.Response mapToResponse(PestDiseaseEntry e) {
        return PestDiseaseEntryDto.Response.builder()
                .id(e.getId())
                .crop(e.getCrop())
                .kind(e.getKind())
                .nameEn(e.getNameEn())
                .nameBn(e.getNameBn())
                .symptoms(e.getSymptoms())
                .favourableConditions(e.getFavourableConditions())
                .actionThreshold(e.getActionThreshold())
                .culturalControl(e.getCulturalControl())
                .chemicalControlActiveIngredients(e.getChemicalControlActiveIngredients())
                .source(sourceResolver.toRef(e.getSourceId(), e.getSourcePage()))
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
