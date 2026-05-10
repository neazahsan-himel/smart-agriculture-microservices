package com.smartagriculture.aiadvisorservice.client;

import com.smartagriculture.aiadvisorservice.dto.external.FarmerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "farmer-service")
public interface FarmerServiceClient {

    @GetMapping("/api/v1/farmers/{id}")
    FarmerResponse getFarmerById(@PathVariable("id") String id);
}