package com.revnomix.revseed.Util;

import java.util.Arrays;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.wrapper.ResponseWrapper;
import com.revnomix.revseed.Service.EmailReportServiceImpl;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeBookingResponse;
import com.revnomix.revseed.integration.eglobe.dto.EglobeRoomMapping;
import com.revnomix.revseed.integration.eglobe.dto.Root;
import com.revnomix.revseed.integration.service.Response;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingErrorResponse;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingResponse;
import com.revnomix.revseed.integration.staahMax.dto.RateInventoryMapping;
import com.revnomix.revseed.integration.staahMax.dto.RoomMapping;


@Service
public class JsonWebAPIServiceImpl{
	RestTemplate restTemplate;
	
	@Autowired
	SystemUtil systemUtil;
	
    @Autowired
    private ParameterRepository parameterRepository;
    
    @Autowired
    private EmailReportServiceImpl emailReportServiceImpl;
	
	public JsonWebAPIServiceImpl(RestTemplateBuilder restTemplateBuilder) {
	    this.restTemplate = restTemplateBuilder
	        .errorHandler(new RestTemplateResponseErrorHandler())
	        .build();
	    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
	    requestFactory.setOutputStreaming(false);
	    restTemplate.setRequestFactory(requestFactory);
	  }
	
	@Async
	public <R, T> CompletableFuture<ResponseWrapper<T>> jsonRestGetAPIcall(String url, Map<String, String> headers, R body, Class<T> responseType,Clients client) {
	        HttpHeaders header = new HttpHeaders();
	        ResponseWrapper<T> response = new ResponseWrapper<T>();
	        if(headers!=null) {
		        headers.forEach((String name, String value) -> {
		            header.add(name, value);
		        });
	        }
	        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
	        header.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	        header.setContentType(MediaType.APPLICATION_JSON);
	        HttpEntity<?> httpEntity = null;
	        if(body!=null) {
	        	httpEntity = new HttpEntity<>(body, header);
	        }
	        else {
	        	httpEntity = new HttpEntity<>(headers);
	        }
	        ResponseEntity<T> entity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, responseType);
	        response.setEntity(entity);
	        response.setClient(client);
	        return CompletableFuture.completedFuture(response);
	}
	
	@Async
	public <R, T> CompletableFuture<ResponseWrapper<T>> jsonRestPostAPIcall(String url, Map<String, String> headers, R body, Class<T> responseType,Clients client) {
	        HttpHeaders header = new HttpHeaders();
	        ResponseWrapper<T> response = new ResponseWrapper<T>();
	        if(headers!=null) {
		        headers.forEach((String name, String value) -> {
		            header.add(name, value);
		        });
	        }
	        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
	        header.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	        header.setContentType(MediaType.APPLICATION_JSON);
	        
	        HttpEntity<R> httpEntity = null;
	        if(body!=null) {
	        	httpEntity = new HttpEntity<>(body, header);
	        }else {
	        	httpEntity = new HttpEntity<>(header);
	        }
	        ResponseEntity<T> entity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, responseType);
	        response.setEntity(entity);
	        response.setClient(client);
	        return CompletableFuture.completedFuture(response);
	}
	
	public SchedulerConvertedResponse convertObjectfromStringEGlobe(String body, String type, Clients clients){
		ObjectMapper objectMapper = new ObjectMapper();
		SchedulerConvertedResponse schedulerConvertedResponse = new SchedulerConvertedResponse();
        String isMonitoringData = "";
        Parameter paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA).orElse(null);
        if(paramOpt!=null) {
        	isMonitoringData = paramOpt.getParamValue();
        }
        String response = "";
        try {
			 switch(type) {
				 case ConstantUtil.EGLOBEROOMMAP :
					 schedulerConvertedResponse.setEGlobeRoomMapping(objectMapper.readValue(body.replaceAll("&", "&amp;"), new TypeReference<List<EglobeRoomMapping>>(){}));
				 	break;
				 case ConstantUtil.EGLOBEINVENTSYNC :
					 schedulerConvertedResponse.setEGlobeInventory(objectMapper.readValue(body.replaceAll("&", "&amp;"), Root.class));
				 	break;
				 case ConstantUtil.EGLOBEBOOKING :
					 schedulerConvertedResponse.setEGlobeBooking(objectMapper.readValue(body.replaceAll("&", "&amp;"), new TypeReference<List<EGlobeBookingResponse>>(){}));
				 	break;
				 default : 
					 
				 	break;
			 }
            if(isMonitoringData.equals(ConstantUtil.YES)) {
            	response = body;
            }else {
            	response = "";
            }
			 schedulerConvertedResponse.setResponse(new Response(Status.SUCCESS, response));
         }catch(Exception ce) {
         	ce.printStackTrace();
         	schedulerConvertedResponse.setResponse(new Response(Status.FAILED, body));
         	emailReportServiceImpl.sendCMStructralErrorMail(ConstantUtil.EGLOBE, body, clients.getPropertyName());
         }
		 return schedulerConvertedResponse;
	}
	
	public SchedulerConvertedResponse convertObjectfromStringStaahMax(String body, String type, Clients clients){
		ObjectMapper objectMapper = new ObjectMapper();
		SchedulerConvertedResponse schedulerConvertedResponse = new SchedulerConvertedResponse();
        String isMonitoringData = "";
        Parameter paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA).orElse(null);
        if(paramOpt!=null) {
        	isMonitoringData = paramOpt.getParamValue();
        }
        String response = "";
		 try {
			 switch(type) {
				 case ConstantUtil.STAAHMAXROOMMAP :
					 schedulerConvertedResponse.setStaahMaxRoomMapping(objectMapper.readValue(body.replaceAll("&", "&amp;"), RoomMapping.class));
				 	break;
				 case ConstantUtil.STAAHMAXINVENTSYNC :
					 schedulerConvertedResponse.setStaahMaxInventory(objectMapper.readValue(body.replaceAll("&", "&amp;"), RateInventoryMapping.class));
				 	break;
				 case ConstantUtil.STAAHMAXBOOKING :
						 schedulerConvertedResponse.setStaahMaxBooking(objectMapper.readValue(body.replaceAll("&", "&amp;"), DailyBookingResponse.class));
				 	break;
				 default : 					 
				 	break;
			 }
			 if(isMonitoringData.equals(ConstantUtil.YES)) {
	            response = body;
	         }
			 else {
	            response = "";
	         }
			 schedulerConvertedResponse.setResponse(new Response(Status.SUCCESS, response));
         }catch(MismatchedInputException ce) {
        	 try {
        		 if(type.equals(ConstantUtil.STAAHMAXBOOKING)) {
					 DailyBookingErrorResponse dresponse = objectMapper.readValue(body.replaceAll("&", "&amp;"), DailyBookingErrorResponse.class);
					 if(dresponse!=null) {
						 schedulerConvertedResponse.setResponse(new Response(Status.FAILED, dresponse.getReservations()));
						 schedulerConvertedResponse.setStaahMaxBooking(new DailyBookingResponse());
					 }
        		 }
        	 }catch(Exception e) {
        		 schedulerConvertedResponse.setResponse(new Response(Status.FAILED, body));
        		 e.printStackTrace();
        		 emailReportServiceImpl.sendCMStructralErrorMail(ConstantUtil.STAAH_ALL, body, clients.getPropertyName());
        	 }
		 }catch(Exception ce) {
         	ce.printStackTrace();
         	schedulerConvertedResponse.setResponse(new Response(Status.FAILED, body));
         	emailReportServiceImpl.sendCMStructralErrorMail(ConstantUtil.STAAH_ALL, body, clients.getPropertyName());
         }
		 return schedulerConvertedResponse;
	}
}
