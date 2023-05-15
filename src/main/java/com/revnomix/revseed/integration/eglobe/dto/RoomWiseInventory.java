package com.revnomix.revseed.integration.eglobe.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.revnomix.revseed.integration.eglobe.dto.DayWiseInventory;

import lombok.Data;

@Data
public class RoomWiseInventory{
    @JsonProperty("RoomId") 
    public String roomId;
    @JsonProperty("RoomName") 
    public String roomName;
    @JsonProperty("DayWiseInventory") 
    public List<DayWiseInventory> dayWiseInventory;
}
