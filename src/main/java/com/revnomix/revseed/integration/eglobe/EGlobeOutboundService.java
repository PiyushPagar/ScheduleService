package com.revnomix.revseed.integration.eglobe;


import java.util.ArrayList;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.JobType;
import com.revnomix.revseed.model.ScheduledJob;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.FinalRecommendationsRepository;
import com.revnomix.revseed.repository.OverrideRepository;
import com.revnomix.revseed.repository.ScheduledJobRepository;
import com.revnomix.revseed.wrapper.OverrideDetailsDto;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeUpdateRateDerivedResponse;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeUpdateRatesDerivedRequest;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeUpdateRatesRequest;
import com.revnomix.revseed.integration.eglobe.dto.RateUpdateStatusResponse;
import com.revnomix.revseed.integration.exception.RevseedException;
import com.revnomix.revseed.integration.service.Response;


@Controller
@RequestMapping("/eglobe/outbound")
@CrossOrigin(origins = "*")
public class EGlobeOutboundService {
	   private final Logger logger = LoggerFactory.getLogger(this.getClass());
	   
	    @Autowired
	    private SystemUtil systemUtil;
	    
	    @Autowired
	    private ClientsRepository clientsRepository;
	    
	    @Autowired
	    private OverrideRepository overrideRepository;
	    
	    @Autowired
	    private FinalRecommendationsRepository finalRecommendationsRepository;
	    
		@Autowired
		private AlertService alertService;
		
	    @Autowired
	    private ScheduledJobRepository scheduledJobRepository;
		   
	    @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/rates/}")
	    private String updateRateUrl;
	    
	    @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/inventory/}")
	    private String statusUrl;
	    
		   @RequestMapping(value = "/updateRate", method = RequestMethod.GET)
		    @ResponseBody
		    public List<String> upateRate(@RequestParam("clientId") Integer clientId) {
			   Response response = new Response();
			   logger.debug("uploading final rates for eglobe completed");
		        com.revnomix.revseed.model.Clients clients = clientsRepository.findById(clientId).orElse(null);
		        List<String> workids = new ArrayList<String>();
		        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.RATE_PUSH));
		        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
		        try {		        
			        if (clients == null) {
			            response.setMessage("Hotel/client not found with id :" + clientId);
			            response.setStatus(Status.FAILED);
			            throw new RevseedException("Client not found with id :" + clientId);
			        } 	    	
			        List<OverrideDetailsDto> latestFinalRates = finalRecommendationsRepository.getLatestFinalRateByClientId(clientId);      
			        List<EGlobeUpdateRatesRequest> eGlobeUpdateRatesRequestList = new ArrayList<EGlobeUpdateRatesRequest>();
			        latestFinalRates.forEach(row -> {
			        	EGlobeUpdateRatesRequest eGlobeUpdateRatesRequest = new EGlobeUpdateRatesRequest();
			        	eGlobeUpdateRatesRequest.setDateFrom(DateUtil.formatDate(row.getCheckin_date(), "dd-MMM-yyyy"));
			        	eGlobeUpdateRatesRequest.setDateTill(DateUtil.formatDate(row.getCheckin_date(), "dd-MMM-yyyy"));
			        	eGlobeUpdateRatesRequest.setRoomCode(clients.getCmMasterRoom().toString());
			        	List<EGlobeUpdateRatesRequest.RatePlanWiseRates> ratePlanWiseRates = new ArrayList<>();
			        	EGlobeUpdateRatesRequest.RatePlanWiseRates ratePlanRates = new EGlobeUpdateRatesRequest.RatePlanWiseRates();
			        	ratePlanRates.setRate((double) row.getRate());
			        	ratePlanRates.setRatePlanCode(clients.getCmMasterRate().toString());
			        	ratePlanWiseRates.add(ratePlanRates);
			        	eGlobeUpdateRatesRequest.setRatePlanWiseRates(ratePlanWiseRates);
			        	eGlobeUpdateRatesRequestList.add(eGlobeUpdateRatesRequest);
		            }); 
			        eGlobeUpdateRatesRequestList.forEach(eurrl->{
			        	String url = getUpdateRateUrl()+clients.getCmPassword()+"/bulkupdate";
				        HttpHeaders headers = new HttpHeaders();
				        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
				        headers.setContentType(MediaType.APPLICATION_JSON);
		            	RestTemplate restTemplate = new RestTemplate();
		                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
				        HttpEntity<EGlobeUpdateRatesRequest> requestObj = new HttpEntity<>(eurrl,headers);
				        ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.POST, requestObj, String.class);
				            byte[] bytes = StringUtils.getBytesUtf8(result.getBody());            
				            String utf8EncodedString = StringUtils.newStringUtf8(bytes);
				            workids.add(utf8EncodedString);
			        });	 
			        changeStatus(runningJob, Status.COMPLETE);
			        alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		        }catch(Exception e) {
		        	changeStatus(runningJob, Status.FAILED);
		        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,e.getMessage()+""+e.getCause());
		        	logger.debug("uploading final rates for eglobe completed");
		        }
		        
		        return workids;

		   }
		   
		   @RequestMapping(value = "/pushAllOverrideRates", method = RequestMethod.PUT)
		    @ResponseBody
		    public List<String> pushAllOverrideRates(@RequestParam("clientId") Integer clientId) {
			   logger.debug("uploading override rates for eglobe started");
			   Response response = new Response();
		        List<String> workids = new ArrayList<String>();
		        Clients clients = clientsRepository.findById(clientId).orElse(null);
		        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.RATE_PUSH_OVERRIDE));
		        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
		        try {
			        if (clients == null) {
			            response.setMessage("Hotel/client not found with id :" + clientId);
			            response.setStatus(com.revnomix.revseed.model.Status.FAILED);
			            throw new RevseedException("Client not found with id :" + clientId);
			        } 	    	
			        Date startDate = DateUtil.localToDate(DateUtil.dateToLocalDate(clients.getSystemToday()).minusDays(1));
			        List<OverrideDetailsDto> latestFinalRates = overrideRepository.getLatestOverrideValue(clientId, startDate);	        List<EGlobeUpdateRatesRequest> eGlobeUpdateRatesRequestList = new ArrayList<EGlobeUpdateRatesRequest>();
			        latestFinalRates.forEach(row -> {
			        	EGlobeUpdateRatesRequest eGlobeUpdateRatesRequest = new EGlobeUpdateRatesRequest();
			        	eGlobeUpdateRatesRequest.setDateFrom(DateUtil.formatDate(row.getCheckin_date(), "dd-MMM-yyyy"));
			        	eGlobeUpdateRatesRequest.setDateTill(DateUtil.formatDate(row.getCheckin_date(), "dd-MMM-yyyy"));
			        	eGlobeUpdateRatesRequest.setRoomCode(clients.getCmMasterRoom().toString());
			        	List<EGlobeUpdateRatesRequest.RatePlanWiseRates> ratePlanWiseRates = new ArrayList<>();
			        	EGlobeUpdateRatesRequest.RatePlanWiseRates ratePlanRates = new EGlobeUpdateRatesRequest.RatePlanWiseRates();
			        	ratePlanRates.setRate((double) row.getRate());
			        	ratePlanRates.setRatePlanCode(clients.getCmMasterRate().toString());
			        	ratePlanWiseRates.add(ratePlanRates);
			        	eGlobeUpdateRatesRequest.setRatePlanWiseRates(ratePlanWiseRates);
			        	eGlobeUpdateRatesRequestList.add(eGlobeUpdateRatesRequest);
		           }); 
			        eGlobeUpdateRatesRequestList.forEach(eurrl->{
			        	String url = getUpdateRateUrl()+clients.getCmPassword()+"/bulkupdate";
				        HttpHeaders headers = new HttpHeaders();
				        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
				        headers.setContentType(MediaType.APPLICATION_JSON);
				        HttpEntity<EGlobeUpdateRatesRequest> requestObj = new HttpEntity<>(eurrl,headers);
		            	RestTemplate restTemplate = new RestTemplate();
		                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
		                ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.POST, requestObj, String.class);
				        //String result = restTemplate.post(url, requestObj,false);
				            byte[] bytes = StringUtils.getBytesUtf8(result.getBody());            
				            String utf8EncodedString = StringUtils.newStringUtf8(bytes);
				            workids.add(utf8EncodedString);
			        });	    
			        changeStatus(runningJob, Status.COMPLETE);
			        alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		        }catch(Exception ce) {
		        	changeStatus(runningJob, Status.FAILED);
		        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,ce.getMessage()+""+ce.getCause());
		        	logger.debug("uploading final rates for eglobe completed");
		        }
		        logger.debug("uploading override rates for eglobe completed");
		        return workids;

		   }
		   
		   @RequestMapping(value = "/updateRate/derived", method = RequestMethod.GET)
		    @ResponseBody
		    public List<String> upateRateDerived(@RequestParam("clientId") Integer clientId) {
			   Response response = new Response();
			   logger.debug("uploading final rates for eglobe completed");
		        Clients clients = clientsRepository.findById(clientId).orElse(null);
		        List<String> workids = new ArrayList<String>();
		        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.RATE_PUSH));
		        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
		        try {		        
			        if (clients == null) {
			            response.setMessage("Hotel/client not found with id :" + clientId);
			            response.setStatus(com.revnomix.revseed.model.Status.FAILED);
			            throw new RevseedException("Client not found with id :" + clientId);
			        } 	    	
			        List<OverrideDetailsDto> latestFinalRates = finalRecommendationsRepository.getLatestFinalRateByClientId(clientId);      
			        List<EGlobeUpdateRatesDerivedRequest> eGlobeUpdateRatesRequestList = new ArrayList<EGlobeUpdateRatesDerivedRequest>();
			        latestFinalRates.forEach(row -> {
			        	EGlobeUpdateRatesDerivedRequest eGlobeUpdateRatesRequest = new EGlobeUpdateRatesDerivedRequest();
			        	eGlobeUpdateRatesRequest.setDateFrom(DateUtil.formatDate(row.getCheckin_date(), "dd-MMM-yyyy"));
			        	eGlobeUpdateRatesRequest.setDateTill(DateUtil.formatDate(row.getCheckin_date(), "dd-MMM-yyyy"));
			        	eGlobeUpdateRatesRequest.setBasePrice((double) row.getRate());
			        	eGlobeUpdateRatesRequestList.add(eGlobeUpdateRatesRequest);
		            }); 
			        eGlobeUpdateRatesRequestList.forEach(eurrl->{
			        	EGlobeUpdateRateDerivedResponse eGlobeUpdateRateDerivedResponse = new EGlobeUpdateRateDerivedResponse();
			        	String url = getUpdateRateUrl()+clients.getCmPassword()+"/bulkupdate/derived";
				        HttpHeaders headers = new HttpHeaders();
				        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
				        headers.setContentType(MediaType.APPLICATION_JSON);
		            	RestTemplate restTemplate = new RestTemplate();
		                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
				        HttpEntity<EGlobeUpdateRatesDerivedRequest> requestObj = new HttpEntity<>(eurrl,headers);
				        ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.POST, requestObj, String.class);
				            byte[] bytes = StringUtils.getBytesUtf8(result.getBody());            
				            String utf8EncodedString = StringUtils.newStringUtf8(bytes);
				            Gson gson = new Gson();
				            try {
				            	eGlobeUpdateRateDerivedResponse = gson.fromJson(utf8EncodedString, EGlobeUpdateRateDerivedResponse.class);
				            	if(eGlobeUpdateRateDerivedResponse!=null) {
				            		if(eGlobeUpdateRateDerivedResponse.getIsError()) {
				            			utf8EncodedString = eGlobeUpdateRateDerivedResponse.getMessage();
				            		}else {
				            			utf8EncodedString = eGlobeUpdateRateDerivedResponse.getResult().toString();
				            		}
				            	}
				            }catch(Exception ce) {
				            	ce.printStackTrace();
				            }
				            workids.add(utf8EncodedString);
			        });	       
			        changeStatus(runningJob, Status.COMPLETE);
			        alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		        }catch(Exception e) {
		        	changeStatus(runningJob, Status.FAILED);
		        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,e.getMessage()+""+e.getCause());
		        	logger.debug("uploading final rates for eglobe completed");
		        }
		        
		        return workids;

		   }
		   
		   @RequestMapping(value = "/pushAllOverrideRates/derived", method = RequestMethod.PUT)
		    @ResponseBody
		    public List<String> pushAllOverrideRatesDerived(@RequestParam("clientId") Integer clientId) {
			   logger.debug("uploading override rates for eglobe started");
			   Response response = new Response();
		        List<String> workids = new ArrayList<String>();
		        Clients clients = clientsRepository.findById(clientId).orElse(null);
		        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.RATE_PUSH_OVERRIDE));
		        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
		        try {
			        if (clients == null) {
			            response.setMessage("Hotel/client not found with id :" + clientId);
			            response.setStatus(com.revnomix.revseed.model.Status.FAILED);
			            throw new RevseedException("Client not found with id :" + clientId);
			        } 	    	
			        Date startDate = DateUtil.localToDate(DateUtil.dateToLocalDate(clients.getSystemToday()).minusDays(1));
			        List<OverrideDetailsDto> latestFinalRates = overrideRepository.getLatestOverrideValue(clientId, startDate);	        
					List<EGlobeUpdateRatesDerivedRequest> eGlobeUpdateRatesRequestList = new ArrayList<EGlobeUpdateRatesDerivedRequest>();
			        latestFinalRates.forEach(row -> {
			        	EGlobeUpdateRatesDerivedRequest eGlobeUpdateRatesRequest = new EGlobeUpdateRatesDerivedRequest();
			        	eGlobeUpdateRatesRequest.setDateFrom(DateUtil.formatDate(row.getCheckin_date(), "dd-MMM-yyyy"));
			        	eGlobeUpdateRatesRequest.setDateTill(DateUtil.formatDate(row.getCheckin_date(), "dd-MMM-yyyy"));
			        	eGlobeUpdateRatesRequest.setBasePrice((double) row.getRate());
			        	eGlobeUpdateRatesRequestList.add(eGlobeUpdateRatesRequest);
		           }); 
			        eGlobeUpdateRatesRequestList.forEach(eurrl->{
			        	EGlobeUpdateRateDerivedResponse eGlobeUpdateRateDerivedResponse = new EGlobeUpdateRateDerivedResponse();
			        	String url = getUpdateRateUrl()+clients.getCmPassword()+"/bulkupdate/derived";
				        HttpHeaders headers = new HttpHeaders();
				        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
				        headers.setContentType(MediaType.APPLICATION_JSON);
				        HttpEntity<EGlobeUpdateRatesDerivedRequest> requestObj = new HttpEntity<>(eurrl,headers);
		            	RestTemplate restTemplate = new RestTemplate();
		                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
		                ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.POST, requestObj, String.class);
				            byte[] bytes = StringUtils.getBytesUtf8(result.getBody());            
				            String utf8EncodedString = StringUtils.newStringUtf8(bytes);
				            Gson gson = new Gson();
				            try {
				            	eGlobeUpdateRateDerivedResponse = gson.fromJson(utf8EncodedString, EGlobeUpdateRateDerivedResponse.class);
				            	if(eGlobeUpdateRateDerivedResponse!=null) {
				            		if(eGlobeUpdateRateDerivedResponse.getIsError()) {
				            			utf8EncodedString = eGlobeUpdateRateDerivedResponse.getMessage();
				            		}else {
				            			utf8EncodedString = eGlobeUpdateRateDerivedResponse.getResult().toString();
				            		}
				            	}
				            }catch(Exception ce) {
				            	ce.printStackTrace();
				            }
				            workids.add(utf8EncodedString);
			        });	        
			        changeStatus(runningJob, Status.COMPLETE);
			        alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		        }catch(Exception ce) {
		        	changeStatus(runningJob, Status.FAILED);
		        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,ce.getMessage()+""+ce.getCause());
		        	logger.debug("uploading final rates for eglobe completed");
		        }
		        logger.debug("uploading override rates for eglobe completed");
		        return workids;

		   }

	    
	    @RequestMapping(value = "/status", method = RequestMethod.GET)
	    @ResponseBody
	    public List<RateUpdateStatusResponse> status(@RequestParam("clientId") Integer clientId, @RequestParam("workId") Integer workId) {
	        logger.debug("Fetching update status for eglobe started");
	        List<RateUpdateStatusResponse> response = null;
	        String response1 = "";
	        try {
	        	 Clients clients = clientsRepository.findById(clientId).orElse(null);
			        if (clients == null) {
			            throw new RevseedException("Client not found with id :" + clientId);
			     } 	    	
			    String url = getStatusUrl()+clients.getCmPassword()+"/updatestatus/{"+workId;
		        HttpHeaders headers = new HttpHeaders();
            	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            	RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
                ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
	            //response1 = (String) restTemplate.get(url,new Object[0]);
	            byte[] bytes = StringUtils.getBytesUtf8(response1);            
	            String utf8EncodedString = StringUtils.newStringUtf8(bytes);
	            Gson gson = new Gson();
	            response = Arrays.asList(gson.fromJson(utf8EncodedString, RateUpdateStatusResponse[].class));
	            logger.debug("Fetching update status for eglobe completed");
	        } catch (Exception e) {
	            logger.debug("Failed to fetch update status", e);
	            e.printStackTrace();
	        }
	        return response;
	    }
	    
	    private ScheduledJob createJob(Clients clients, JobType jobType) {
	        ScheduledJob scheduledJob = new ScheduledJob();
	        scheduledJob.setClientId(clients.getId());
	        scheduledJob.setJobType(jobType);
	        scheduledJob.setStartDateTime(new Date());
	        scheduledJob.setStatus(Status.CREATED);
	        return scheduledJob;
	    }
	    
	    private ScheduledJob changeStatus(ScheduledJob job, Status status) {
	        job.setStatus(status);
	        return scheduledJobRepository.save(job);
	    }
	    

		public String getUpdateRateUrl() {
			return updateRateUrl;
		}

		public void setUpdateRateUrl(String updateRateUrl) {
			this.updateRateUrl = updateRateUrl;
		}

		public String getStatusUrl() {
			return statusUrl;
		}

		public void setStatusUrl(String statusUrl) {
			this.statusUrl = statusUrl;
		}
	    
	    
}
