package com.revnomix.revseed.integration.eglobe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RatePlans {		
    @JsonProperty("RatePlanCode") 
    public String ratePlanCode;
    @JsonProperty("RatePlanName") 
    public String ratePlanName;
    @JsonProperty("MinAllowedRate") 
    public double minAllowedRate;
    @JsonProperty("MaxAllowedRate") 
    public double maxAllowedRate;
}
