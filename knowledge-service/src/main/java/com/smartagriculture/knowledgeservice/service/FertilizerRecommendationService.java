package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.FertilizerRecommendationDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FertilizerRecommendationService {

    FertilizerRecommendationDto.Response createRecommendation(FertilizerRecommendationDto.Request request);

    Page<FertilizerRecommendationDto.Response> searchRecommendations(String crop, KnowledgeEnums.CropSeason season,
                                                                     String region, Pageable pageable);

    FertilizerRecommendationDto.Response getRecommendationById(String id);

    FertilizerRecommendationDto.Response updateRecommendation(String id, FertilizerRecommendationDto.Request request);

    void deleteRecommendation(String id);
}
