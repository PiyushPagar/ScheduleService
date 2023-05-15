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
import com.revnomix.revseed.schema.ezee.PushBookingError;
import com.revnomix.revseed.schema.ezee.PushBookingError.Errors;
import com.revnomix.revseed.schema.ezee.PushBookingSucess;
import com.revnomix.revseed.schema.ezee.PushBookingSucess.Success;
import com.revnomix.revseed.schema.ezee.PushBookingSucess.Success.Booking;
import com.revnomix.revseed.schema.ezee.RESRequest;
import com.revnomix.revseed.schema.ezee.RESResponse;
import com.revnomix.revseed.schema.ezee.RESResponse.Reservations.Reservation;
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
public class EzeeMultiProcessingService {

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
	private AlertService alertService;
	
    @Autowired
    private ScheduledJobRepository scheduledJobRepository;
	
    @Value("${ezee.url:https://live.ipms247.com/pmsinterface/reservation.php}")
    private String endPointUrl;

    @Value("${ezee.url:https://live.ipms247.com/pmsinterface/getdataAPI.php}")
    private String endPointUrlForGetData;
    
    
    public String roomMapping(List<RESRequest> roomrequestList) throws Exception {
	    logger.info("Fetching roomMapping for ezee started");
    	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
        List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
		for (RESRequest roomrequest: roomrequestList) {
			Clients clients = clientsRepository.findOneByCmHotel(roomrequest.getAuthentication().getHotelCode());
			try {
	            if (clients != null) {
			        String url = getEndPointUrl();
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
    	        StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
				try {
					if(request.get().getEntity().getBody()!=null) {
						GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
						SchedulerConvertedResponse response = xmlWebAPIServiceImpl.convertObjectfromStringEzee(request.get().getEntity().getBody(), ConstantUtil.EZEEROOMMAP,clients);
						if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
							genericIntegrationDto.setResResponse(response.getEzeeRoomMapping());
							genericIntegrationDto.setClient(request.get().getClient());
							staahPopulationDto.setClient(request.get().getClient());
							genericIntegrationDto.setStaahPopulationDto(staahPopulationDto);
							genericIntegrationDto.setIntegerationType(IntegrationType.EZEE.toString());
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
					}
		        }catch(Exception ce) {
		        	ce.printStackTrace();
	            	runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
					runningJob.setEndDateTime(new Date());
					systemUtil.changeStatus(runningJob, Status.FAILED);
					alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,ce.getMessage() + "" + ce.getCause());
		        }
			}
        logger.info("Fetching roomMapping for ezee completed");
    return "done";
    }
    
    public String viewInventory(List<RESRequest> inventoryrequestList) throws Exception {
	    logger.info("Fetching viewInventory for ezee started");
    	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
        List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
		for (RESRequest inventoryrequest: inventoryrequestList) {
			Clients clients = clientsRepository.findOneByCmHotel(inventoryrequest.getAuthentication().getHotelCode());
			try {				
	            if (clients != null) {
			        String url = getEndPointUrlForGetData();
					allFutures.add(xmlWebAPIServiceImpl.xmlRestPostAPIcall(url, null, inventoryrequest, String.class,clients));
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
					if(request.get().getEntity().getBody()!=null) {
						GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
						SchedulerConvertedResponse response = xmlWebAPIServiceImpl.convertObjectfromStringEzee(request.get().getEntity().getBody(), ConstantUtil.EZEEINVENTSYNC,clients);
						if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
							genericIntegrationDto.setResResponse(response.getEzeeInventory());
							genericIntegrationDto.setClient(request.get().getClient());
							genericIntegrationDto.setIntegerationType(IntegrationType.EZEE.toString());
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
					}
		        }catch(Exception ce) {
		        	ce.printStackTrace();
	            	runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
					runningJob.setEndDateTime(new Date());
					systemUtil.changeStatus(runningJob, Status.FAILED);
					alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,ce.getMessage() + "" + ce.getCause());
		        }
			}

        logger.info("Fetching viewInventory for ezee completed");
    return "done";
    }
    
    public String viewRate(List<RESRequest> raterequestList) throws Exception {
	    logger.info("Fetching viewRate for ezee started");
    	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
        List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
		for (RESRequest raterequest: raterequestList) {
			Clients clients = clientsRepository.findOneByCmHotel(raterequest.getAuthentication().getHotelCode());
			try {
	            if (clients != null) {
			        String url = getEndPointUrlForGetData();
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
					if(request.get().getEntity().getBody()!=null) {
						GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
						SchedulerConvertedResponse response = xmlWebAPIServiceImpl.convertObjectfromStringEzee(request.get().getEntity().getBody(), ConstantUtil.EZEERATESYNC,clients);
						if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
							genericIntegrationDto.setResResponse(response.getEzeeRateSync());
							genericIntegrationDto.setClient(request.get().getClient());
							genericIntegrationDto.setIntegerationType(IntegrationType.EZEE.toString());
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
					}
		        }catch(Exception ce) {
		        	ce.printStackTrace();
	            	runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
					runningJob.setEndDateTime(new Date());
					systemUtil.changeStatus(runningJob, Status.FAILED);
					alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,ce.getMessage() + "" + ce.getCause());
		        }
			}

        logger.info("Fetching viewRate for ezee completed");
    return "done";
    }
    
    public String getBooking(List<RESRequest> bookingRequestList) throws Exception {
	    logger.info("Fetching getBooking for ezee started");
    	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
        List<CompletableFuture<ResponseWrapper<String>>> allFutures = new ArrayList<>();
		for (RESRequest bookingrequest: bookingRequestList) { 
			Thread.sleep(ConstantUtil.MIN_Delay_SLEEP);
			Clients clients = clientsRepository.findOneByCmHotel(bookingrequest.getAuthentication().getHotelCode());
			try {
	            if (clients != null) {
			        String url = getEndPointUrlForGetData();
					allFutures.add(xmlWebAPIServiceImpl.xmlRestPostAPIcall(url, null, bookingrequest, String.class,clients));
	            }
	        }catch(Exception ce) {
	        	ce.printStackTrace();
	        	alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage() + "" + ce.getCause());
	        }
		       
		CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
			for(CompletableFuture<ResponseWrapper<String>> request: allFutures) {
    			Clients client = request.get().getClient();
    	        ScheduledJob job = scheduledJobRepository.save(systemUtil.createJob(client, JobType.FETCH_BOOKING_DATA));
    	        ScheduledJob runningJob = systemUtil.changeStatus(job, Status.RUNNING);
				try {
					if(request.get().getEntity().getBody()!=null) {
						GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
						SchedulerConvertedResponse response = xmlWebAPIServiceImpl.convertObjectfromStringEzee(request.get().getEntity().getBody(), ConstantUtil.EZEEBOOKING,client);
						if (response.getResponse().getStatus().equals(Status.SUCCESS)) {
							genericIntegrationDto.setResResponse(response.getEzeeBooking());
							genericIntegrationDto.setClient(request.get().getClient());
							genericIntegrationDto.setIntegerationType(IntegrationType.EZEE.toString());
							populateGenericGateway.populateGenericBooking(genericIntegrationDto);
							runningJob.setResponse(response.getResponse().getMessage().toString());
		       				runningJob.setEndDateTime(new Date());
		    				systemUtil.changeStatus(runningJob, Status.COMPLETE);
		                    alertService.createAlertsforClientRefreshData(client, ConstantUtil.BOOKFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
						}else {
		                	   runningJob.setResponse(response.getResponse().getMessage().toString());
		                	   runningJob.setEndDateTime(new Date());
		                	   systemUtil.changeStatus(runningJob, Status.FAILED);
			                   alertService.createFailureAlertsforClient(client, ConstantUtil.BOOKFAIL,response.getResponse().getMessage().toString());
	     				}
					}
		        }catch(Exception ce) {
		        	ce.printStackTrace();
	            	runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
					runningJob.setEndDateTime(new Date());
					systemUtil.changeStatus(runningJob, Status.FAILED);
					alertService.createFailureAlertsforClient(client, ConstantUtil.BOOKFAIL,ce.getMessage() + "" + ce.getCause());
		        }
			}
		} 
        logger.info("Fetching getBooking for ezee completed");
    return "done";
    }
    
    public  Object savePushBooking(RESResponse ezeeBooking) {
		PushBookingError pushBookingError = new PushBookingError();
		Success success = new Success();
		Errors errors = new Errors();
		com.revnomix.revseed.schema.ezee.PushBookingSucess.Errors successError = new com.revnomix.revseed.schema.ezee.PushBookingSucess.Errors();
		Booking booking =new Booking();
		PushBookingSucess pushBookingSucess = new PushBookingSucess();
		GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
		Clients client = clientsRepository.findOneByCmHotel(
				ezeeBooking.getReservations().getReservation().get(0).getBookByInfo().getLocationId());
		try {
			List<Reservation> totalReservations =ezeeBooking.getReservations().getReservation();
			for(Reservation reservation:totalReservations) {
				booking.setBookingId(reservation.getBookByInfo().getUniqueID()); 
			}
			if (client != null) {
				genericIntegrationDto.setResResponse(ezeeBooking);
				genericIntegrationDto.setClient(client);
				genericIntegrationDto.setIntegerationType(IntegrationType.EZEE.toString());
				populateGenericGateway.populateGenericBooking(genericIntegrationDto);
				success.setBooking(booking);
				successError.setErrorCode(200);
				successError.setErrorMessage("Success");
				pushBookingSucess.setErrors(successError);
				pushBookingSucess.setSuccess(success);
				return pushBookingSucess;
			} else {
				errors.setErrorCode(500);
				errors.setErrorMessage("Booking not inserted");
				pushBookingError.setErrors(errors);
				return pushBookingError;
			}
		} catch (Exception ce) {
			ce.printStackTrace();
			alertService.createFailureAlertsforClient(client, ConstantUtil.BOOKFAIL,
					ce.getMessage() + "" + ce.getCause());
			errors.setErrorCode(500);
			errors.setErrorMessage("Booking not inserted");
			pushBookingError.setErrors(errors);
			return pushBookingError;
		}
	}


	public String getEndPointUrl() {
		return endPointUrl;
	}

	public void setEndPointUrl(String endPointUrl) {
		this.endPointUrl = endPointUrl;
	}

	public String getEndPointUrlForGetData() {
		return endPointUrlForGetData;
	}

	public void setEndPointUrlForGetData(String endPointUrlForGetData) {
		this.endPointUrlForGetData = endPointUrlForGetData;
	}
    
    
    
    
}