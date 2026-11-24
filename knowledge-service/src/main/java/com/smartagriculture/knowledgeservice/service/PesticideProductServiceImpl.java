package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.PesticideProductDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.entity.PesticideProduct;
import com.smartagriculture.knowledgeservice.exception.DuplicateResourceException;
import com.smartagriculture.knowledgeservice.exception.ResourceNotFoundException;
import com.smartagriculture.knowledgeservice.repository.PesticideProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PesticideProductServiceImpl implements PesticideProductService {

    private final PesticideProductRepository pesticideRepository;
    private final SourceResolver sourceResolver;

    @Override
    @Transactional
    public PesticideProductDto.Response createPesticide(PesticideProductDto.Request request) {
        String crop = KnowledgeText.crop(request.getCrop());
        log.info("Creating pesticide use: tradeName={}, crop={}, target={}", request.getTradeName(), crop, request.getTarget());
        sourceResolver.requireSource(request.getSourceId());
        if (pesticideRepository.existsByTradeNameIgnoreCaseAndCropAndTargetIgnoreCaseAndDeletedFalse(
                request.getTradeName(), crop, request.getTarget())) {
            throw new DuplicateResourceException(duplicateMessage(request.getTradeName(), crop, request.getTarget()));
        }
        PesticideProduct product = PesticideProduct.builder()
                .tradeName(request.getTradeName())
                .activeIngredient(request.getActiveIngredient())
                .formulation(request.getFormulation())
                .registrationNo(request.getRegistrationNo())
                .crop(crop)
                .target(request.getTarget())
                .doseValue(request.getDoseValue())
                .doseUnit(request.getDoseUnit())
                .doseBasis(request.getDoseBasis())
                .preHarvestIntervalDays(request.getPreHarvestIntervalDays())
                .toxicityClass(request.getToxicityClass())
                .notes(request.getNotes())
                .sourceId(request.getSourceId())
                .sourcePage(request.getSourcePage())
                .build();
        if (request.getRegistrationStatus() != null) product.setRegistrationStatus(request.getRegistrationStatus());
        return mapToResponse(pesticideRepository.saveAndFlush(product));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PesticideProductDto.Response> searchPesticides(String crop, String target, String activeIngredient,
                                                               KnowledgeEnums.RegistrationStatus registrationStatus,
                                                               Pageable pageable) {
        return pesticideRepository.search(KnowledgeText.crop(KnowledgeText.filter(crop)), KnowledgeText.filter(target),
                KnowledgeText.filter(activeIngredient), registrationStatus, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PesticideProductDto.Response> findByName(String name, String crop) {
        return pesticideRepository.findByName(name.trim(), KnowledgeText.crop(KnowledgeText.filter(crop))).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PesticideProductDto.Response getPesticideById(String id) {
        return mapToResponse(findActiveById(id));
    }

    @Override
    @Transactional
    public PesticideProductDto.Response updatePesticide(String id, PesticideProductDto.Request request) {
        log.info("Updating pesticide use id={}", id);
        PesticideProduct product = findActiveById(id);
        String crop = KnowledgeText.crop(request.getCrop());
        sourceResolver.requireSource(request.getSourceId());
        if (pesticideRepository.existsByTradeNameIgnoreCaseAndCropAndTargetIgnoreCaseAndDeletedFalseAndIdNot(
                request.getTradeName(), crop, request.getTarget(), id)) {
            throw new DuplicateResourceException(duplicateMessage(request.getTradeName(), crop, request.getTarget()));
        }

        // Required by validation, so always set
        product.setTradeName(request.getTradeName());
        product.setActiveIngredient(request.getActiveIngredient());
        product.setCrop(crop);
        product.setTarget(request.getTarget());
        product.setSourceId(request.getSourceId());

        if (request.getFormulation() != null)            product.setFormulation(request.getFormulation());
        if (request.getRegistrationNo() != null)         product.setRegistrationNo(request.getRegistrationNo());
        if (request.getDoseValue() != null)              product.setDoseValue(request.getDoseValue());
        if (request.getDoseUnit() != null)               product.setDoseUnit(request.getDoseUnit());
        if (request.getDoseBasis() != null)              product.setDoseBasis(request.getDoseBasis());
        if (request.getPreHarvestIntervalDays() != null) product.setPreHarvestIntervalDays(request.getPreHarvestIntervalDays());
        if (request.getToxicityClass() != null)          product.setToxicityClass(request.getToxicityClass());
        if (request.getRegistrationStatus() != null)     product.setRegistrationStatus(request.getRegistrationStatus());
        if (request.getNotes() != null)                  product.setNotes(request.getNotes());
        if (request.getSourcePage() != null)             product.setSourcePage(request.getSourcePage());

        return mapToResponse(pesticideRepository.saveAndFlush(product));
    }

    @Override
    @Transactional
    public void deletePesticide(String id) {
        log.info("Soft-deleting pesticide use id={}", id);
        PesticideProduct product = findActiveById(id);
        product.setDeleted(true);
        product.setStatus(KnowledgeEnums.RecordStatus.INACTIVE);
        pesticideRepository.save(product);
    }

    private PesticideProduct findActiveById(String id) {
        return pesticideRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pesticide use not found with id: " + id));
    }

    private String duplicateMessage(String tradeName, String crop, String target) {
        return "Pesticide use already exists: " + tradeName + " on " + crop + " against " + target;
    }

    private PesticideProductDto.Response mapToResponse(PesticideProduct p) {
        return PesticideProductDto.Response.builder()
                .id(p.getId())
                .tradeName(p.getTradeName())
                .activeIngredient(p.getActiveIngredient())
                .formulation(p.getFormulation())
                .registrationNo(p.getRegistrationNo())
                .crop(p.getCrop())
                .target(p.getTarget())
                .doseValue(p.getDoseValue())
                .doseUnit(p.getDoseUnit())
                .doseBasis(p.getDoseBasis())
                .preHarvestIntervalDays(p.getPreHarvestIntervalDays())
                .toxicityClass(p.getToxicityClass())
                .registrationStatus(p.getRegistrationStatus())
                .notes(p.getNotes())
                .source(sourceResolver.toRef(p.getSourceId(), p.getSourcePage()))
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
