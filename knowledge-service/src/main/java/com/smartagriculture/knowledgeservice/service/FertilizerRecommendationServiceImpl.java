package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.FertilizerRecommendationDto;
import com.smartagriculture.knowledgeservice.entity.FertilizerRecommendation;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.exception.DuplicateResourceException;
import com.smartagriculture.knowledgeservice.exception.ResourceNotFoundException;
import com.smartagriculture.knowledgeservice.repository.FertilizerRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FertilizerRecommendationServiceImpl implements FertilizerRecommendationService {

    private final FertilizerRecommendationRepository fertilizerRepository;
    private final SourceResolver sourceResolver;

    @Override
    @Transactional
    public FertilizerRecommendationDto.Response createRecommendation(FertilizerRecommendationDto.Request request) {
        String crop = KnowledgeText.crop(request.getCrop());
        log.info("Creating fertiliser recommendation: crop={}, season={}, region={}", crop, request.getSeason(), request.getRegion());
        sourceResolver.requireSource(request.getSourceId());
        if (fertilizerRepository.existsByCropAndSeasonAndRegionAndSoilFertilityClassAndDeletedFalse(
                crop, request.getSeason(), request.getRegion(), request.getSoilFertilityClass())) {
            throw new DuplicateResourceException(duplicateMessage(crop, request));
        }
        FertilizerRecommendation rec = FertilizerRecommendation.builder()
                .crop(crop)
                .season(request.getSeason())
                .region(request.getRegion())
                .soilFertilityClass(request.getSoilFertilityClass())
                .ureaKgPerHa(request.getUreaKgPerHa())
                .tspKgPerHa(request.getTspKgPerHa())
                .mopKgPerHa(request.getMopKgPerHa())
                .gypsumKgPerHa(request.getGypsumKgPerHa())
                .zincSulphateKgPerHa(request.getZincSulphateKgPerHa())
                .splitSchedule(request.getSplitSchedule())
                .notes(request.getNotes())
                .sourceId(request.getSourceId())
                .sourcePage(request.getSourcePage())
                .build();
        return mapToResponse(fertilizerRepository.saveAndFlush(rec));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FertilizerRecommendationDto.Response> searchRecommendations(String crop, KnowledgeEnums.CropSeason season,
                                                                            String region, Pageable pageable) {
        return fertilizerRepository.search(KnowledgeText.crop(KnowledgeText.filter(crop)), season,
                KnowledgeText.filter(region), pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FertilizerRecommendationDto.Response getRecommendationById(String id) {
        return mapToResponse(findActiveById(id));
    }

    @Override
    @Transactional
    public FertilizerRecommendationDto.Response updateRecommendation(String id, FertilizerRecommendationDto.Request request) {
        log.info("Updating fertiliser recommendation id={}", id);
        FertilizerRecommendation rec = findActiveById(id);
        String crop = KnowledgeText.crop(request.getCrop());
        sourceResolver.requireSource(request.getSourceId());

        // Effective key values after this null-safe update, so the duplicate check is accurate
        KnowledgeEnums.CropSeason season = request.getSeason() != null ? request.getSeason() : rec.getSeason();
        String region = request.getRegion() != null ? request.getRegion() : rec.getRegion();
        String fertility = request.getSoilFertilityClass() != null ? request.getSoilFertilityClass() : rec.getSoilFertilityClass();
        if (fertilizerRepository.existsByCropAndSeasonAndRegionAndSoilFertilityClassAndDeletedFalseAndIdNot(
                crop, season, region, fertility, id)) {
            throw new DuplicateResourceException("Fertiliser recommendation already exists: " + crop + " / " + season
                    + " / " + region + " / " + fertility);
        }

        rec.setCrop(crop);                       // required by validation
        rec.setSourceId(request.getSourceId());  // required by validation
        rec.setSeason(season);
        rec.setRegion(region);
        rec.setSoilFertilityClass(fertility);
        if (request.getUreaKgPerHa() != null)         rec.setUreaKgPerHa(request.getUreaKgPerHa());
        if (request.getTspKgPerHa() != null)          rec.setTspKgPerHa(request.getTspKgPerHa());
        if (request.getMopKgPerHa() != null)          rec.setMopKgPerHa(request.getMopKgPerHa());
        if (request.getGypsumKgPerHa() != null)       rec.setGypsumKgPerHa(request.getGypsumKgPerHa());
        if (request.getZincSulphateKgPerHa() != null) rec.setZincSulphateKgPerHa(request.getZincSulphateKgPerHa());
        if (request.getSplitSchedule() != null)       rec.setSplitSchedule(request.getSplitSchedule());
        if (request.getNotes() != null)               rec.setNotes(request.getNotes());
        if (request.getSourcePage() != null)          rec.setSourcePage(request.getSourcePage());

        return mapToResponse(fertilizerRepository.saveAndFlush(rec));
    }

    @Override
    @Transactional
    public void deleteRecommendation(String id) {
        log.info("Soft-deleting fertiliser recommendation id={}", id);
        FertilizerRecommendation rec = findActiveById(id);
        rec.setDeleted(true);
        rec.setStatus(KnowledgeEnums.RecordStatus.INACTIVE);
        fertilizerRepository.save(rec);
    }

    private FertilizerRecommendation findActiveById(String id) {
        return fertilizerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fertiliser recommendation not found with id: " + id));
    }

    private String duplicateMessage(String crop, FertilizerRecommendationDto.Request r) {
        return "Fertiliser recommendation already exists: " + crop + " / " + r.getSeason() + " / "
                + r.getRegion() + " / " + r.getSoilFertilityClass();
    }

    private FertilizerRecommendationDto.Response mapToResponse(FertilizerRecommendation f) {
        return FertilizerRecommendationDto.Response.builder()
                .id(f.getId())
                .crop(f.getCrop())
                .season(f.getSeason())
                .region(f.getRegion())
                .soilFertilityClass(f.getSoilFertilityClass())
                .ureaKgPerHa(f.getUreaKgPerHa())
                .tspKgPerHa(f.getTspKgPerHa())
                .mopKgPerHa(f.getMopKgPerHa())
                .gypsumKgPerHa(f.getGypsumKgPerHa())
                .zincSulphateKgPerHa(f.getZincSulphateKgPerHa())
                .splitSchedule(f.getSplitSchedule())
                .notes(f.getNotes())
                .source(sourceResolver.toRef(f.getSourceId(), f.getSourcePage()))
                .status(f.getStatus())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
