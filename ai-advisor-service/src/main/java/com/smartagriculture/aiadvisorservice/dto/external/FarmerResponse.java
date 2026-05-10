package com.smartagriculture.aiadvisorservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FarmerResponse {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String countryCode;
    private String region;
    private Double farmSizeHectares;
    private String soilType;
    private String status;
}