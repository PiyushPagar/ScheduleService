package com.revnomix.revseed.integration.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import com.revnomix.revseed.wrapper.ResponseWrapper;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.JsonWebAPIServiceImpl;
import com.revnomix.revseed.Util.SchedulerConvertedResponse;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.staah.transformer.PopulateGenericGateway;
import com.revnomix.revseed.integration.staah.transformer.StaahPopulationDto;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingRequest;
import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;
import com.revnomix.revseed.integration.staahMax.dto.InventoryYearlyRequest;
import com.revnomix.revseed.integration.staahMax.dto.RoomRateRequest;

@Service
public class StaahMaxMultiProcessingService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ClientsRepository clientsRepository;
    
    @Autowired
    private SystemUtil systemUtil;

	@Autowired
	JsonWebAPIServiceImpl jsonWebAPIServiceImpl;
	
    @Autowired
    private PopulateGenericGateway populateGenericGateway;
    
    @Autowired
    private ScheduledJobRepository scheduledJobRepository;
    
	@Autowired
	private AlertService alertService;
	
    @Value("${staah.url:https://metachannel.staah.net/common-cgi/api/Services.pl?version=2}")
    private String getUrl;

    @Value("${staah.url:https://crmsystems.staah.net/common-cgi/Services.pl}")
    private String crmUrl;

    @Value("${staah.url:https://crmsystems.staah.net/common-cgi/confirmation.pl}")
    private String conformationUrl;

    @Value("${staah.url:https://rmsswitch.staah.net/common-cgi/Revseed/Services.pl}")
    private String rmsUrl;
    
    public static final String CRM_API_KEY = "U2FsdGVkX1/6pIbvTr4DBIHu9cuU0DQ6smu0OmXTdLY=";
    public static final String RMS_API_KEY = "TlNQ9z7rYHR7sCQG4TNItVgfVidcYvAJ";
    
    public String roomMapping(List<RoomRateRequest> roomrequestList) throws Exception {
    	    logger.info("Fetching roomMapping for staah max Started");
        	RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
    		for (RoomRateRequest roomrequest: roomrequestList) {
    			Clients clients = clientsRepository.findByCmHotel(roomrequest.getPropertyid());
    			try {	    	        
	    	        if (clients != null) {
		    	        String url = getGetUrl();
		    			allFutures.add(jsonWebAPIServiceImpl.jsonRestPostAPIcall(url, null, roomrequest, String.class,clients));
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
	    				SchedulerConvertedResponse response = jsonWebAPIServiceImpl.convertObjectfromStringStaahMax(request.get().getEntity().getBody(), ConstantUtil.STAAHMAXROOMMAP,clients);
	    				StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
	    				if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
	    					staahPopulationDto.setClient(clients);
	        				genericIntegrationDto.setRoomMapping(response.getStaahMaxRoomMapping());
	        				genericIntegrationDto.setClient(request.get().getClient());
	        				genericIntegrationDto.setIntegerationType(IntegrationType.STAAHMAX.toString());
	        				genericIntegrationDto.setStaahPopulationDto(staahPopulationDto);
	        				populateGenericGateway.populateGenericRoomMapping(genericIntegrationDto);
	        				runningJob.setResponse(response.getResponse().getMessage().toString());
		    				runningJob.setEndDateTime(new Date());
	        				systemUtil.changeStatus(runningJob, Status.COMPLETE);
		                    alertService.createAlertsforClientRefreshData(clients, ConstantUtil.ROOMINVFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
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

            logger.info("Fetching roomMapping for staah max completed");
        return "done";
    }
    
    public String viewInventoryRate(List<InventoryYearlyRequest> inventoryRequestList) throws Exception {
    	    logger.info("Fetching viewInventoryRate for staah max Started");
        	RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
    		for (InventoryYearlyRequest inventoryRequest: inventoryRequestList) {
    			Clients clients = clientsRepository.findByCmHotel(inventoryRequest.getPropertyid());
    			try {    	        
	    	        if (clients != null) {
				        String url = getGetUrl();
		    			allFutures.add(jsonWebAPIServiceImpl.jsonRestPostAPIcall(url, null, inventoryRequest, String.class,clients));
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
	    				SchedulerConvertedResponse response = jsonWebAPIServiceImpl.convertObjectfromStringStaahMax(request.get().getEntity().getBody(), ConstantUtil.STAAHMAXINVENTSYNC,clients);
	    				StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
	    				if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
//		    				ArrayList<StaahMaxPopulationDto> staahMaxPopulationList = new ArrayList<StaahMaxPopulationDto>();
//		    				staahMaxPopulationList.add(staahPopulationDto);
	        				genericIntegrationDto.setClient(clients);
	        				staahPopulationDto.setClient(clients);
	        				genericIntegrationDto.setStaahPopulationDto(staahPopulationDto);
	        				genericIntegrationDto.setRateInventoryMapping(response.getStaahMaxInventory());
	        				genericIntegrationDto.setIntegerationType(IntegrationType.STAAHMAX.toString());
		    				populateGenericGateway.populateGenericInventoryAndRate(genericIntegrationDto);
		    				runningJob.setResponse(response.getResponse().getMessage().toString());
		    				runningJob.setEndDateTime(new Date());
	        				systemUtil.changeStatus(runningJob, Status.COMPLETE);
		                    alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEINVFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
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
	                	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,ce.getMessage() + "" + ce.getCause());
	                }
    			}

            logger.info("Fetching viewInventoryRate for staah max completed");
        return "done";
    }
    
    
    public String getBooking(List<DailyBookingRequest> dailyBookingRequestList) throws Exception {
    	    logger.info("Fetching getBooking for staah max Started");
        	RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
            HashMap<String,String> header = new HashMap<String, String>();
            header.put("STAAH-AUTH-KEY", CRM_API_KEY);
    		for (DailyBookingRequest dailyBookingRequest: dailyBookingRequestList) {
    			Clients clients = clientsRepository.findByCmHotel(dailyBookingRequest.getHotelCode());
    			try {
	    	        if (clients != null) {
				        String url = getCrmUrl();
		    			allFutures.add(jsonWebAPIServiceImpl.jsonRestPostAPIcall(url, header, dailyBookingRequest, String.class,clients));
	    	        }
	    		}catch(Exception ce) {
	            	ce.printStackTrace();
	            	alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage() + "" + ce.getCause());
	            }
    		}        
    		CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
    			for(CompletableFuture<ResponseWrapper<String>> request: allFutures) {
        			Clients clients = request.get().getClient();
        	        ScheduledJob job = scheduledJobRepository.save(systemUtil.createJob(clients, JobType.FETCH_BOOKING_DATA));
        	        ScheduledJob runningJob = systemUtil.changeStatus(job, Status.RUNNING);
    	    		try {
    	    			GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
    	    			StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
    	    			SchedulerConvertedResponse response = jsonWebAPIServiceImpl.convertObjectfromStringStaahMax(request.get().getEntity().getBody(), ConstantUtil.STAAHMAXBOOKING,clients);
	    				if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
	    					staahPopulationDto.setClient(request.get().getClient());
		    				genericIntegrationDto.setClient(request.get().getClient());
		    				genericIntegrationDto.setStaahPopulationDto(staahPopulationDto);
	        				genericIntegrationDto.setDailyBookingResponse(response.getStaahMaxBooking());
	        				genericIntegrationDto.setIntegerationType(IntegrationType.STAAHMAX.toString());
	        				populateGenericGateway.populateGenericBooking(genericIntegrationDto);
	        				runningJob.setResponse(response.getResponse().getMessage().toString());
	        				runningJob.setEndDateTime(new Date());
	        				systemUtil.changeStatus(runningJob, Status.COMPLETE);
		                    alertService.createAlertsforClientRefreshData(clients, ConstantUtil.BOOKFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
	    				}else {
 	                	   runningJob.setResponse(response.getResponse().getMessage().toString());
	                	   runningJob.setEndDateTime(new Date());
	                	   systemUtil.changeStatus(runningJob, Status.FAILED);
		                   alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,response.getResponse().getMessage().toString());
        				}
	                }catch(Exception ce) {
	                	ce.printStackTrace();
	                }
    			}
            logger.info("Fetching getBooking for staah max completed");
        return "done";
    }
    
    
	public String getGetUrl() {
		return getUrl;
	}
	public void setGetUrl(String getUrl) {
		this.getUrl = getUrl;
	}
	public String getCrmUrl() {
		return crmUrl;
	}
	public void setCrmUrl(String crmUrl) {
		this.crmUrl = crmUrl;
	}
	public String getConformationUrl() {
		return conformationUrl;
	}
	public void setConformationUrl(String conformationUrl) {
		this.conformationUrl = conformationUrl;
	}
	public String getRmsUrl() {
		return rmsUrl;
	}
	public void setRmsUrl(String rmsUrl) {
		this.rmsUrl = rmsUrl;
	}
    
    
}
