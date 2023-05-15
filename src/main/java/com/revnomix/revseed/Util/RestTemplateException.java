package com.revnomix.revseed.Util;

import org.springframework.http.HttpStatus;

import lombok.Data;

@Data
public class RestTemplateException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HttpStatus statusCode;
	private String errorMessage;
	
	public RestTemplateException() {}
	
	public RestTemplateException(HttpStatus statusCode, String errorMessage) {
		super(errorMessage);
		this.statusCode = statusCode;
		this.errorMessage = errorMessage;
	}



}