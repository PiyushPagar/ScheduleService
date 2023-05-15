package com.revnomix.revseed.integration.eglobe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
	
	@Data
	public class DayWiseInventory{
	    @JsonProperty("AsOnDate") 
	    public String asOnDate;
	    @JsonProperty("DayAvailability") 
	    public int dayAvailability;
	    @JsonProperty("StopSell") 
	    public boolean stopSell;
	}
