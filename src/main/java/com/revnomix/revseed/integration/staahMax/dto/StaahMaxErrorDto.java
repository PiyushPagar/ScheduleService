package com.revnomix.revseed.integration.staahMax.dto;

import lombok.Data;

@Data
public class StaahMaxErrorDto {

	private String error_desc;
	private String status;
	private String trackingId;
	private Integer version;
}
