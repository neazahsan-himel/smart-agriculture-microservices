package com.smartagriculture.knowledgeservice.service;

import com.smartagriculture.knowledgeservice.dto.PesticideProductDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesticideProductService {

    PesticideProductDto.Response createPesticide(PesticideProductDto.Request request);

    Page<PesticideProductDto.Response> searchPesticides(String crop, String target, String activeIngredient,
                                                        KnowledgeEnums.RegistrationStatus registrationStatus,
                                                        Pageable pageable);

    /** Exact trade-name or active-ingredient match, for checking a product the farmer or the LLM mentioned. */
    List<PesticideProductDto.Response> findByName(String name, String crop);

    PesticideProductDto.Response getPesticideById(String id);

    PesticideProductDto.Response updatePesticide(String id, PesticideProductDto.Request request);

    void deletePesticide(String id);
}
