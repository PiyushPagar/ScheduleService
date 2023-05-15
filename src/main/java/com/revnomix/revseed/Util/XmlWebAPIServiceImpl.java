package com.revnomix.revseed.Util;

import java.util.Arrays;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.schema.ezee.EzeeRoomResponse;
import com.revnomix.revseed.wrapper.ResponseWrapper;
import com.revnomix.revseed.Service.EmailReportServiceImpl;
import com.revnomix.revseed.Util.service.XMLUtilService;
import com.revnomix.revseed.integration.service.Response;

@Service
public class XmlWebAPIServiceImpl {

//	@Autowired
//	RestTemplate restTemplate;

	@Autowired
	SystemUtil systemUtil;

	@Autowired
	private Jaxb2Marshaller jaxb2Marshaller;

	@Autowired
	private XMLUtilService xmlUtilService;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private EmailReportServiceImpl emailReportServiceImpl;

//	public XmlWebAPIServiceImpl(RestTemplateBuilder restTemplateBuilder) {
//	    this.restTemplate = restTemplateBuilder
//	        .errorHandler(new RestTemplateResponseErrorHandler())
//	        .build();
//	  }
//	
//	  public <R, T> CompletableFuture<ResponseWrapper<T>> xmlRestGetAPIcall(String url, Map<String, String> headers, R body, Class<T> responseType, Clients clients) {
//	        HttpHeaders header = new HttpHeaders();
//	        ResponseWrapper<T> response = new ResponseWrapper<T>();
//	        headers.forEach((String name, String value) -> {
//	            header.add(name, value);
//	        });
//	        restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
//	        header.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
//	        HttpEntity<R> httpEntity = null;
//	        if(body!=null) {
//	        	httpEntity = new HttpEntity<>(body, header);
//	        }else {
//	        	httpEntity = new HttpEntity<>(header);
//	        }
//	        ResponseEntity<T> entity =restTemplate.exchange(url, HttpMethod.GET, httpEntity, responseType);
//	        response.setEntity(entity);
//	        response.setClient(clients);	        
//	        return CompletableFuture.completedFuture(response);
//	  }

	public <R, T> CompletableFuture<ResponseWrapper<T>> xmlRestPostAPIcall(String url, Map<String, String> headers,
			R body, Class<T> responseType, Clients clients) {
		HttpHeaders header = new HttpHeaders();
		ResponseWrapper<T> response = new ResponseWrapper<T>();
		if (headers != null) {
			headers.forEach((String name, String value) -> {
				header.add(name, value);
			});
		}
	//	restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
		header.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
		HttpEntity<R> httpEntity = null;
		if (body != null) {
			httpEntity = new HttpEntity<>(body, header);
		} else {
			httpEntity = new HttpEntity<>(header);
		}
		ResponseEntity<T> entity =new ResponseEntity<T>(null);
	//	ResponseEntity<T> entity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, responseType);
		response.setEntity(entity);
		response.setClient(clients);
		return CompletableFuture.completedFuture(response);
	}

	public SchedulerConvertedResponse convertObjectfromStringStaah(String body, String type, Clients clients) {
		SchedulerConvertedResponse schedulerConvertedResponse = new SchedulerConvertedResponse();
		String isMonitoringData = "";
		Parameter paramOpt = parameterRepository
				.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA).orElse(null);
		if (paramOpt != null) {
			isMonitoringData = paramOpt.getParamValue();
		}
		String response = "";
		try {
			switch (type) {
			case ConstantUtil.STAAHROOMMAP:
				schedulerConvertedResponse.setStaahRoomMapping(
						xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, body.replaceAll("&", "&amp;")));
				break;
			case ConstantUtil.STAAHINVENTSYNC:
				schedulerConvertedResponse.setStaahInventory(
						xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, body.replaceAll("&", "&amp;")));
				break;
			case ConstantUtil.STAAHRATESYNC:
				schedulerConvertedResponse.setStaahRateSync(
						xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, body.replaceAll("&", "&amp;")));
				// <roomresponse> <updated>Fail</updated> <error>4</error> <description>Invalid
				// hotel_id or username or password</description> </roomresponse>
				break;
			default:

				break;
			}
			if (isMonitoringData.equals(ConstantUtil.YES)) {
				response = body;
			} else {
				response = "";
			}
			schedulerConvertedResponse.setResponse(new Response(Status.SUCCESS, response));
		} catch (Exception ce) {
			ce.printStackTrace();
			schedulerConvertedResponse.setResponse(new Response(Status.FAILED, body));
			emailReportServiceImpl.sendCMStructralErrorMail(ConstantUtil.STAAH, body, clients.getPropertyName());
		}
		return schedulerConvertedResponse;
	}

	public SchedulerConvertedResponse convertObjectfromStringEzee(String body, String type, Clients clients) {
		SchedulerConvertedResponse schedulerConvertedResponse = new SchedulerConvertedResponse();
		String isMonitoringData = "";
		Parameter paramOpt = parameterRepository
				.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA).orElse(null);
		if (paramOpt != null) {
			isMonitoringData = paramOpt.getParamValue();
		}
		String response = "";
		try {
			switch (type) {
			case ConstantUtil.EZEEROOMMAP:
				body = body.replace("\t<?xml", "<?xml");
				schedulerConvertedResponse.setEzeeRoomMapping(
						xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, body.replaceAll("&", "&amp;")));
				break;
			case ConstantUtil.EZEEINVENTSYNC:
				body = body.replace("\t<?xml", "<?xml");
				schedulerConvertedResponse.setEzeeInventory(
						xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, body.replaceAll("&", "&amp;")));
				break;
			case ConstantUtil.EZEEBOOKING:
				body = body.replace("\t<?xml", "<?xml");
				schedulerConvertedResponse.setEzeeBooking(
						xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, body.replaceAll("&", "&amp;")));
				break;
			case ConstantUtil.EZEERATESYNC:
				body = body.replace("\t<?xml", "<?xml");
				schedulerConvertedResponse.setEzeeRateSync(
						xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, body.replaceAll("&", "&amp;")));
				break;
			default:

				break;
			}
			if (isMonitoringData.equals(ConstantUtil.YES)) {
				response = body;
			} else {
				response = "";
			}
			schedulerConvertedResponse.setResponse(new Response(Status.SUCCESS, response));
		} catch (Exception ce) {
			try {
				body = body.replace("\t<?xml", "<?xml");
				EzeeRoomResponse ezeeRoomResponse = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller,
						body.replaceAll("&", "&amp;"));
				schedulerConvertedResponse.setResponse(new Response(Status.FAILED, ezeeRoomResponse.getDescription()));
			} catch (Exception e) {
				ce.printStackTrace();
				schedulerConvertedResponse.setResponse(new Response(Status.FAILED, body));
				emailReportServiceImpl.sendCMStructralErrorMail(ConstantUtil.EZEE, body, clients.getPropertyName());
			}
		}
		return schedulerConvertedResponse;
	}

}
