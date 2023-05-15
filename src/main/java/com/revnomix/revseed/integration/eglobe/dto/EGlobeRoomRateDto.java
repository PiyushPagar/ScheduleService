package com.revnomix.revseed.integration.eglobe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeMasterRatePlanDto;

import lombok.Data;

@Data
public class EGlobeRoomRateDto {
    @JsonProperty("RatePlanCode") 
    public String ratePlanCode;
    @JsonProperty("MasterRatePlan") 
    public EGlobeMasterRatePlanDto masterRatePlan;
}
