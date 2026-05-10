package com.smartagriculture.aiadvisorservice.client;

import com.smartagriculture.aiadvisorservice.dto.external.CropSummary;
import com.smartagriculture.aiadvisorservice.dto.external.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "crop-service")
public interface CropServiceClient {

    @GetMapping("/api/v1/crops")
    PageResponse<CropSummary> getCrops(
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "20")         int size,
            @RequestParam(defaultValue = "createdAt")  String sortBy,
            @RequestParam(defaultValue = "desc")       String sortDir
    );
}