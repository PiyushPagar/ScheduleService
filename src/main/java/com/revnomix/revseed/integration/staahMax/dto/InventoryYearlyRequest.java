package com.revnomix.revseed.integration.staahMax.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryYearlyRequest {
    private Integer propertyid;
    private Long room_id;
    private Long rate_id;
    private String apikey;
    private String action;
}
