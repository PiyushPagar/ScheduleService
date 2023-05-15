package com.revnomix.revseed.integration.eglobe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class EGlobeMasterRoomDto {
    @JsonProperty("RoomTypeCode") 
    public String roomTypeCode;
}
