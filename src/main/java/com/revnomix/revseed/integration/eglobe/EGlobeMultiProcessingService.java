package com.revnomix.revseed.integration.eglobe;

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
import com.revnomix.revseed.integration.staahMax.dto.InventoryRequest;
import com.revnomix.revseed.integration.staahMax.dto.RoomRateRequest;

@Service
public class EGlobeMultiProcessingService {
	
	   private final Logger logger = LoggerFactory.getLogger(this.getClass());
	   
	    
	    @Autowired
	    private PopulateGenericGateway populateGenericGateway;
	    
	    @Autowired
	    private ScheduledJobRepository scheduledJobRepository;
	    
		@Autowired
		private AlertService alertService;
	    
	    @Autowired
	    private ClientsRepository clientsRepository;
	    
	    @Autowired
	    private SystemUtil systemUtil;

		@Autowired
		JsonWebAPIServiceImpl jsonWebAPIServiceImpl;

	   @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/bookings/}")
	   private String getBookingUrl;
	   
	   @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/rooms/}")
	   private String getRoomsUrl;
	   
	   @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/inventory/}")
	   private String inventoryUrl;
	   
	   @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/rates/}")
	   private String updateRateUrl;
	   
	    public String roomMapping(List<RoomRateRequest> roomrequestList) throws Exception {
	    	    logger.info("Fetching roomMapping for eglobe started");
            	RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
                List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
        		for (RoomRateRequest roomrequest: roomrequestList) {
        			Clients clients = clientsRepository.findById(roomrequest.getPropertyid()).orElse(null);
        			try {	 
        				if (clients!=null) {
		    		        String url = getGetRoomsUrl()+clients.getCmPassword();
		        			allFutures.add(jsonWebAPIServiceImpl.jsonRestGetAPIcall(url, null, null, String.class,clients));
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
	        				SchedulerConvertedResponse response = jsonWebAPIServiceImpl.convertObjectfromStringEGlobe(request.get().getEntity().getBody(), ConstantUtil.EGLOBEROOMMAP,clients);
	        				if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
	        					staahPopulationDto.setClient(request.get().getClient());
		        				genericIntegrationDto.setEglobeRoomMappingList(response.getEGlobeRoomMapping());
		        				genericIntegrationDto.setClient(request.get().getClient());
		        				genericIntegrationDto.setIntegerationType(IntegrationType.EGLOBE.toString());
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
	                	   runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
	                	   runningJob.setEndDateTime(new Date());
	                	   systemUtil.changeStatus(runningJob, Status.FAILED);
	                	   alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,ce.getMessage() + "" + ce.getCause());
	    	               ce.printStackTrace();
	    	            }
        			}
	            logger.info("Fetching roomMapping for eglobe completed");
	        return "done";
	    }
	    
	    public String getBookings(List<DailyBookingRequest> bookingRequestList) throws Exception {
	    	    logger.info("Fetching getBookings for eglobe started");
            	RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
                List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();               
        		for (DailyBookingRequest bookingRequest: bookingRequestList) {
        			Clients clients = clientsRepository.findById(bookingRequest.getHotelCode()).orElse(null);
        			try {	 
        				if (clients!=null) {
		        			String url = new String (getGetBookingUrl()+clients.getCmPassword()+"?searchBy=BookingDate&fromDate="+bookingRequest.getCheckIn_At().getStart()+"&tillDate="+bookingRequest.getCheckIn_At().getEnd());
		        			allFutures.add(jsonWebAPIServiceImpl.jsonRestGetAPIcall(url, null, null, String.class,clients));
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
	        				SchedulerConvertedResponse response = jsonWebAPIServiceImpl.convertObjectfromStringEGlobe(request.get().getEntity().getBody(), ConstantUtil.EGLOBEBOOKING,clients);
	        				if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
		        				genericIntegrationDto.setClient(request.get().getClient());
		        				genericIntegrationDto.setEGlobeBookingResponseList(response.getEGlobeBooking());
		        				genericIntegrationDto.setIntegerationType(IntegrationType.EGLOBE.toString());
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
							runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
							runningJob.setEndDateTime(new Date());
							systemUtil.changeStatus(runningJob, Status.FAILED);
							alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage() + "" + ce.getCause());
	    	            }
        			}
	            logger.info("Fetching getBookings for eglobe completed");
	        return "done";
	    }
	    
	    public String getInventory(List<InventoryRequest> inventoryRequestList) throws Exception {
	    	    logger.info("Fetching getInventory for eglobe started");
            	RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
                List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();              
        		for (InventoryRequest inventoryRequest: inventoryRequestList) {
        			Clients clients = clientsRepository.findById(inventoryRequest.getPropertyid()).orElse(null);
        			try {
        				if (clients!=null) {
		    	            String url = new String(getInventoryUrl()+clients.getCmPassword()+"/channels/1006/v2?fromDate="+inventoryRequest.getFrom_date()+"&tillDate="+inventoryRequest.getTo_date());
		        			allFutures.add(jsonWebAPIServiceImpl.jsonRestGetAPIcall(url, null, null, String.class,clients));
        				}
	        		}catch(Exception ce) {
	        			ce.printStackTrace();
        				alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,ce.getMessage() + "" + ce.getCause());
		            }
        		}        
        		CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();        		
        			for(CompletableFuture<ResponseWrapper<String>> request: allFutures) {
            			Clients clients = request.get().getClient();
            	        ScheduledJob job = scheduledJobRepository.save(systemUtil.createJob(clients, JobType.FETCH_INVENTORY));
            	        ScheduledJob runningJob = systemUtil.changeStatus(job, Status.RUNNING);
        				try {
        					GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
	        				SchedulerConvertedResponse response = jsonWebAPIServiceImpl.convertObjectfromStringEGlobe(request.get().getEntity().getBody(), ConstantUtil.EGLOBEINVENTSYNC,clients);
	        				if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
		        				genericIntegrationDto.setClient(clients);
		        				genericIntegrationDto.setRoot(response.getEGlobeInventory());
		        				genericIntegrationDto.setIntegerationType(IntegrationType.EGLOBE.toString());
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
	            logger.info("Fetching getInventory for eglobe completed");
	        return "done";
	    }


		public String getGetBookingUrl() {
			return getBookingUrl;
		}

		public void setGetBookingUrl(String getBookingUrl) {
			this.getBookingUrl = getBookingUrl;
		}

		public String getGetRoomsUrl() {
			return getRoomsUrl;
		}

		public void setGetRoomsUrl(String getRoomsUrl) {
			this.getRoomsUrl = getRoomsUrl;
		}

		public String getInventoryUrl() {
			return inventoryUrl;
		}

		public void setInventoryUrl(String inventoryUrl) {
			this.inventoryUrl = inventoryUrl;
		}

		public String getUpdateRateUrl() {
			return updateRateUrl;
		}

		public void setUpdateRateUrl(String updateRateUrl) {
			this.updateRateUrl = updateRateUrl;
		}

}
