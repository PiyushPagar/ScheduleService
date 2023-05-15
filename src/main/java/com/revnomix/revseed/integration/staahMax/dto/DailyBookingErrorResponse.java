package com.revnomix.revseed.integration.staahMax.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DailyBookingErrorResponse {

	@JsonProperty("Reservations")
	private String reservations;
}
