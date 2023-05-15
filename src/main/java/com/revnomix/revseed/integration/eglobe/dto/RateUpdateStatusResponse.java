package com.revnomix.revseed.integration.eglobe.dto;

import lombok.Data;

@Data
public class RateUpdateStatusResponse {

	private String ChannelCode;
	private Integer UpdateStatusId;
	private String UpdateStatus;
}
