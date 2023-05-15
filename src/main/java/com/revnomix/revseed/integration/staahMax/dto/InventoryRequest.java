package com.revnomix.revseed.integration.staahMax.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryRequest {
    private Integer propertyid;
    private Integer room_id;
    private Integer rate_id;
    private String apikey;
    private String action;
    private String from_date;
    private String to_date;
}
