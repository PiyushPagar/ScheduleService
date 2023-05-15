package com.revnomix.revseed.integration.staahMax.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomRateRequest {
    private Integer propertyid;
    private String apikey;
    private String action;
}
