package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.dto.AdvisorDto;

public interface AiAdvisorService {
    AdvisorDto.Response getAdvice(AdvisorDto.Request request);
}