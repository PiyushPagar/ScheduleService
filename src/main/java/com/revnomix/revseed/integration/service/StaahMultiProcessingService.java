package com.revnomix.revseed.integration.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.JobType;
import com.revnomix.revseed.model.ScheduledJob;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.ScheduledJobRepository;
import com.revnomix.revseed.schema.staah.AvailRequestSegments;
import com.revnomix.revseed.schema.staah.RateRequestSegments;
import com.revnomix.revseed.schema.staah.Roomrequest;
import com.revnomix.revseed.wrapper.ResponseWrapper;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.SchedulerConvertedResponse;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.Util.XmlWebAPIServiceImpl;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.staah.transformer.PopulateGenericGateway;
import com.revnomix.revseed.integration.staah.transformer.StaahPopulationDto;
import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;

@Service
public class StaahMultiProcessingService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ClientsRepository clientsRepository;
    
    @Autowired
    private SystemUtil systemUtil;

	@Autowired
	private XmlWebAPIServiceImpl xmlWebAPIServiceImpl;
    
    @Autowired
    private PopulateGenericGateway populateGenericGateway;
    
    @Autowired
    private ScheduledJobRepository scheduledJobRepository;
    
	@Autowired
	private AlertService alertService;
	
    @Value("${staah.url:https://revnomix.staah.net/common-cgi/Services.pl}")
    private String staahUrl;
    
    public String roomMapping(List<Roomrequest> roomrequestList) throws Exception {
	    logger.debug("Fetching roomMapping for staah started");
    	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
        List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
		for (Roomrequest roomrequest: roomrequestList) {
			Clients clients = clientsRepository.findOneByCmHotel(roomrequest.getHotelId());
			try {				
	            if (clients != null) {
			        String url = getStaahUrl();
					allFutures.add(xmlWebAPIServiceImpl.xmlRestPostAPIcall(url, null, roomrequest, String.class,clients));
	            }
			}catch(Exception ce) {
				ce.printStackTrace();
				alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,ce.getMessage() + "" + ce.getCause());
			}
		}        
		CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
			for(CompletableFuture<ResponseWrapper<String>> request: allFutures) {
    			Clients clients = request.get().getClient();
    	        ScheduledJob job = scheduledJobRepository.save(systemUtil.createJob(clients, JobType.FETCH_ROOM_MAPPING));
    	        ScheduledJob runningJob = systemUtil.changeStatus(job, Status.RUNNING);
				try {
					GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
					StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
					SchedulerConvertedResponse response = xmlWebAPIServiceImpl.convertObjectfromStringStaah(request.get().getEntity().getBody(), ConstantUtil.STAAHROOMMAP,clients);
					if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
						staahPopulationDto.setRoomresponse(response.getStaahRoomMapping());
						staahPopulationDto.setClient(request.get().getClient());
						if(staahPopulationDto.getRoomresponse()!=null && staahPopulationDto.getRoomresponse().getRooms()!=null) {
		    				genericIntegrationDto.setStaahPopulationDto(staahPopulationDto);
		    				genericIntegrationDto.setClient(request.get().getClient());
		    				genericIntegrationDto.setIntegerationType(IntegrationType.STAAH.toString());
		    				populateGenericGateway.populateGenericRoomMapping(genericIntegrationDto);
		    				runningJob.setResponse(response.getResponse().getMessage().toString());
		       				runningJob.setEndDateTime(new Date());
		    				systemUtil.changeStatus(runningJob, Status.COMPLETE);
		                    alertService.createAlertsforClientRefreshData(clients, ConstantUtil.ROOMINVFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
						}
					}else {
	                	   runningJob.setResponse(response.getResponse().getMessage().toString());
	                	   runningJob.setEndDateTime(new Date());
	                	   systemUtil.changeStatus(runningJob, Status.FAILED);
		                   alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,response.getResponse().getMessage().toString());
     				}
				}catch(Exception ce) {
					ce.printStackTrace();
	            	runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
					runningJob.setEndDateTime(new Date());
					systemUtil.changeStatus(runningJob, Status.FAILED);
					alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,ce.getMessage() + "" + ce.getCause());
				}
			}
        logger.info("Fetching roomMapping for staah completed");
    return "done";
    }
    
    public String viewInventory(List<AvailRequestSegments> inventoryrequestList) throws Exception {
	    logger.info("Fetching viewInventory for staah started");
    	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
        List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
		for (AvailRequestSegments inventoryrequest: inventoryrequestList) {
			Clients clients = clientsRepository.findOneByCmHotel(inventoryrequest.getHotelId());
			try {			
	            if (clients != null) {
			        String url = getStaahUrl();
					allFutures.add(xmlWebAPIServiceImpl.xmlRestPostAPIcall(url, null, inventoryrequest, String.class,clients));
	            }
			}catch(Exception ce) {
				ce.printStackTrace();
				alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,ce.getMessage() + "" + ce.getCause());
			}
		}        
		CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();		
			for(CompletableFuture<ResponseWrapper<String>> request: allFutures) {
    			Clients clients = request.get().getClient();
    	        ScheduledJob job = scheduledJobRepository.save(systemUtil.createJob(clients, JobType.FETCH_INVENTORY));
    	        ScheduledJob runningJob = systemUtil.changeStatus(job, Status.RUNNING);
				try {
					GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
					StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
					SchedulerConvertedResponse response = xmlWebAPIServiceImpl.convertObjectfromStringStaah(request.get().getEntity().getBody(), ConstantUtil.STAAHINVENTSYNC,clients);
					if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
						staahPopulationDto.setAvailRequestSegments(response.getStaahInventory());
						genericIntegrationDto.setClient(request.get().getClient());
						genericIntegrationDto.setStaahPopulationDto(staahPopulationDto);
						genericIntegrationDto.setIntegerationType(IntegrationType.STAAH.toString());
						populateGenericGateway.populateGenericInventoryAndRate(genericIntegrationDto);
						runningJob.setResponse(response.getResponse().getMessage().toString());
	       				runningJob.setEndDateTime(new Date());
	    				systemUtil.changeStatus(runningJob, Status.COMPLETE);
	                    alertService.createAlertsforClientRefreshData(clients, ConstantUtil.INVENTFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
					}else {
                	    runningJob.setResponse(response.getResponse().getMessage().toString());
                	    runningJob.setEndDateTime(new Date());
                	    systemUtil.changeStatus(runningJob, Status.FAILED);
	                    alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,response.getResponse().getMessage().toString());
	  				}
				}catch(Exception ce) {
					ce.printStackTrace();
	            	runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
					runningJob.setEndDateTime(new Date());
					systemUtil.changeStatus(runningJob, Status.FAILED);
					alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,ce.getMessage() + "" + ce.getCause());
				}
			}
        logger.info("Fetching viewInventory for staah completed");
    return "done";
    }
    
    public String viewRate(List<RateRequestSegments> raterequestList) throws Exception {
	    logger.info("Fetching viewRate for staah started");
    	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
        List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
		for (RateRequestSegments raterequest: raterequestList) {
			Clients clients = clientsRepository.findOneByCmHotel(raterequest.getHotelId());
			try {				
	            if (clients != null) {
			        String url = getStaahUrl();
		        allFutures.add(xmlWebAPIServiceImpl.xmlRestPostAPIcall(url, null, raterequest, String.class,clients));
	            }
			}catch(Exception ce) {
				ce.printStackTrace();
				alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,ce.getMessage() + "" + ce.getCause());
			}
		}        
		CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
		
			for(CompletableFuture<ResponseWrapper<String>> request: allFutures) {
    			Clients clients = request.get().getClient();
    	        ScheduledJob job = scheduledJobRepository.save(systemUtil.createJob(clients, JobType.FETCH_RATE_INVENTORY));
    	        ScheduledJob runningJob = systemUtil.changeStatus(job, Status.RUNNING);
				try {
					GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
					StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
					SchedulerConvertedResponse response = xmlWebAPIServiceImpl.convertObjectfromStringStaah(request.get().getEntity().getBody(), ConstantUtil.STAAHRATESYNC,clients);
					if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
						genericIntegrationDto.setClient(clients);
						staahPopulationDto.setRateRequestSegmentsResponse(response.getStaahRateSync());
						genericIntegrationDto.setIntegerationType(IntegrationType.STAAH.toString());
						genericIntegrationDto.setStaahPopulationDto(staahPopulationDto);
						populateGenericGateway.populateGenericRateMapping(genericIntegrationDto);
						runningJob.setResponse(response.getResponse().getMessage().toString());
        				runningJob.setEndDateTime(new Date());
        				systemUtil.changeStatus(runningJob, Status.COMPLETE);
	                    alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEINVFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
					}else {
	                	   runningJob.setResponse(response.getResponse().getMessage().toString());
	                	   runningJob.setEndDateTime(new Date());
	                	   systemUtil.changeStatus(runningJob, Status.FAILED);
		                   alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,response.getResponse().getMessage().toString());
     				}
				}catch(Exception ce) {
					ce.printStackTrace();
	            	runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
					runningJob.setEndDateTime(new Date());
					systemUtil.changeStatus(runningJob, Status.FAILED);
					alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,ce.getMessage() + "" + ce.getCause());
				}
			}
        logger.info("Fetching viewRate for staah completed");
    return "done";
    }
    
    

	public String getStaahUrl() {
		return staahUrl;
	}

	public void setStaahUrl(String staahUrl) {
		this.staahUrl = staahUrl;
	}
    
    
}
