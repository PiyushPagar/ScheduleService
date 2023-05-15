package com.revnomix.revseed.integration.staahMax.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaahMaxPropertStatus {

    @JsonProperty("HotelCode")
    protected Integer hotelCode;

    @JsonProperty("Confirmation")
    protected String confirmation;
}
