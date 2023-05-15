package com.revnomix.revseed.integration.eglobe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.revnomix.revseed.model.ApplicationParameters;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.JobType;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.ScheduledJob;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.ApplicationParametersRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.ScheduledJobRepository;
import com.revnomix.revseed.repository.StaahRateTypesRepository;
import com.revnomix.revseed.schema.ezee.RESRequest;
import com.revnomix.revseed.schema.staah.AvailRequestSegment;
import com.revnomix.revseed.schema.staah.AvailRequestSegments;
import com.revnomix.revseed.schema.staah.RateRequestSegment;
import com.revnomix.revseed.schema.staah.RateRequestSegments;
import com.revnomix.revseed.schema.staah.Roomrequest;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.integration.service.EzeeMultiProcessingService;
import com.revnomix.revseed.integration.service.EzeeRequestType;
import com.revnomix.revseed.integration.service.StaahAllRequestType;
import com.revnomix.revseed.integration.service.StaahMaxMultiProcessingService;
import com.revnomix.revseed.integration.service.StaahMultiProcessingService;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingRequest;
import com.revnomix.revseed.integration.staahMax.dto.InventoryRequest;
import com.revnomix.revseed.integration.staahMax.dto.InventoryYearlyRequest;
import com.revnomix.revseed.integration.staahMax.dto.RoomRateRequest;


@Service
public class GenericSchedulerService {
	
private final Logger logger = LoggerFactory.getLogger(this.getClass());   

@Autowired
private SystemUtil systemUtil;

@Autowired
private ScheduledJobRepository scheduledJobRepository;

@Autowired
private ClientsRepository clientsRepository;

@Autowired
private ParameterRepository parameterRepository;

@Autowired
private ApplicationParametersRepository appParameterRepository;

@Autowired
private AlertService alertService;

@Autowired
private StaahMultiProcessingService staahMultiProcessingService;

@Autowired
private StaahMaxMultiProcessingService staahMaxMultiProcessingService;

@Autowired
private EGlobeMultiProcessingService eGlobeMultiProcessingService;

@Autowired
private EzeeMultiProcessingService ezeeMultiProcessingService;

@Autowired
private StaahRateTypesRepository staahRateTypesRepository;

public static final String CRM_API_KEY = "U2FsdGVkX1/6pIbvTr4DBIHu9cuU0DQ6smu0OmXTdLY=";
public static final String RMS_API_KEY = "TlNQ9z7rYHR7sCQG4TNItVgfVidcYvAJ";


public boolean runRecommendation(Integer clientId) {
	ResponseEntity<String> result = null;
	String response="";
	ApplicationParameters parameterProp = appParameterRepository.findOneByCode(ConstantUtil.RECOMMENDATION_URL);
	String isMonitoringData = "";
    Optional<Parameter> paramOpt =parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.MONITORING_DATA);
    if(paramOpt!=null && paramOpt.isPresent()) {
    	Parameter param = paramOpt.get();
    	isMonitoringData = param.getParamValue();
    }
	String paramvalue=""; 
	if(parameterProp!=null) {
		paramvalue = parameterProp.getCodeDesc();
	}
    Clients clients = clientsRepository.findById(clientId).get();
    ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.RUN_RECOMMENDATION));
    ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
    try {
    	HttpHeaders headers = new HttpHeaders();
     	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
     	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
     	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
        result = restTemplate.exchange(paramvalue+clientId.toString(), HttpMethod.POST, requestEntity, String.class);
        response = result.getBody();
        }catch(Exception ce) {
        	response = "Failed";
        	ce.printStackTrace();
        }
    if(isMonitoringData.equals(ConstantUtil.YES)) {
    	runningJob.setResponse(response);
    }else {
    	runningJob.setResponse("");
    }
    if (response.contains("Success")) {
        changeStatus(runningJob, Status.COMPLETE);
        alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RECFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
        return true;
    } else {
    	alertService.createFailureAlertsforClient(clients, ConstantUtil.RECFAIL,response);
        changeStatus(runningJob, Status.FAILED);
        return false;
    }
}

public boolean runCalibration(Integer clientId) {
	ResponseEntity<String> result = null;
	String response="";
	ApplicationParameters parameterProp = appParameterRepository.findOneByCode(ConstantUtil.CALIBERATION_URL);
	String isMonitoringData = "";
    Optional<Parameter> paramOpt =parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.MONITORING_DATA);
    if(paramOpt!=null && paramOpt.isPresent()) {
    	Parameter param = paramOpt.get();
    	isMonitoringData = param.getParamValue();
    }
	String paramvalue=""; 
	if(parameterProp!=null) {
		paramvalue = parameterProp.getCodeDesc();
	}
    Clients clients = clientsRepository.findById(clientId).get();
    ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.RUN_CALIBRATION));
    ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
    try {
    	HttpHeaders headers = new HttpHeaders();
     	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
     	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
     	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
        result = restTemplate.exchange(paramvalue+clientId.toString(), HttpMethod.POST, requestEntity, String.class);
        response = result.getBody();
        }catch(Exception ce) {
        	response = "Failed";
        	ce.printStackTrace();
        }
    if(isMonitoringData.equals(ConstantUtil.YES)) {
    	runningJob.setResponse(response);
    }else {
    	runningJob.setResponse("");
    }
    if (response.contains("Success")) {
        changeStatus(runningJob, Status.COMPLETE);
        alertService.createAlertsforClientRefreshData(clients, ConstantUtil.CALFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
        return true;
    } else {       	
    	alertService.createFailureAlertsforClient(clients, ConstantUtil.CALFAIL,response);
        changeStatus(runningJob, Status.FAILED);
        return false;
    }
}

public void createMultipleScheduledJob(List<Clients> clientsList, List<String> scheduledJobTaskList,boolean isschedule) {      	
	scheduledJobTaskList.stream().forEach(jobString -> {
	   if (jobString.equals(JobType.FETCH_ROOM_MAPPING.toString())) {
		   try {
			   List<RoomRateRequest> roomRateReqEGlobeList = new ArrayList<RoomRateRequest>();
			   List<RESRequest> roomRateReqEzeeList = new ArrayList<RESRequest>();
			   List<Roomrequest> roomReqStaahList = new ArrayList<Roomrequest>();
			   List<RoomRateRequest> roomRateRequestList = new ArrayList<RoomRateRequest>();
			   clientsList.forEach(clients->{
					   ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.FETCH_ROOM_MAPPING));
					   ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
					   changeStatus(job, Status.COMPLETE);
		               logger.error("Started FETCH_ROOM_MAPPING Data");
					   //Parameter monitoringParam =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA).orElse(null); 
					   //String isMonitoringData = monitoringParam.getParamValue();
					   if(clients.getChannelManager().equals(ConstantUtil.EZEE)) {
						   RESRequest resRequest = getRoomRequestEzee(clients);
						   roomRateReqEzeeList.add(resRequest);
					   }else if(clients.getChannelManager().equals(ConstantUtil.STAAH)) {
						   Roomrequest roomrequest = getRoomrequestStaah(clients);
						   roomReqStaahList.add(roomrequest);
					   }else if(clients.getChannelManager().equals(ConstantUtil.STAAH_ALL)) {
						   RoomRateRequest roomRateRequest = getRoomRequestStaahAll(clients);
						   roomRateRequestList.add(roomRateRequest);
					   }else if(clients.getChannelManager().equals(ConstantUtil.EGLOBE)) {
						   RoomRateRequest roomRateReq = getRoomRequestEGlobe(clients);
						   roomRateReqEGlobeList.add(roomRateReq);
					   } 
				   });
			   try {
					   if(roomRateReqEGlobeList.size()>0) {
						   eGlobeMultiProcessingService.roomMapping(roomRateReqEGlobeList);
					   }
					   if(roomRateReqEzeeList.size()>0) {
						   ezeeMultiProcessingService.roomMapping(roomRateReqEzeeList);
					   }
					   if(roomReqStaahList.size()>0) {
						   staahMultiProcessingService.roomMapping(roomReqStaahList);
					   }
					   if(roomRateRequestList.size()>0) {
						   staahMaxMultiProcessingService.roomMapping(roomRateRequestList);
					   }
				   }catch(Exception ce) {
					   logger.error(ce.getMessage());
					   ce.printStackTrace();
			       }
				   logger.error("Ended FETCH_ROOM_MAPPING Data");
			   
		   }catch(Exception ce) {
			   logger.error(ce.getMessage());
			   ce.printStackTrace();
	       }
	   }
	   if (jobString.equals(JobType.FETCH_VIEW_RATE.toString())) {
       	try {
               logger.error("Started FETCH_VIEW_RATE Data");
               List<RESRequest> rateRequestEzeeList = new ArrayList<RESRequest>();
               List<RateRequestSegments> raterequestStaahList = new ArrayList<RateRequestSegments>();
               clientsList.forEach(clients->{
				   //Parameter monitoringParam = parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA).orElse(null); 
				   //String isMonitoringData = monitoringParam.getParamValue();
				   Integer dateRange = 364;
				   Parameter paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), "date_range").orElse(null);
				   if(paramOpt!=null) {
					   dateRange = Integer.parseInt(paramOpt.getParamValue());
				   }
	               if(clients.getChannelManager().equals(ConstantUtil.EZEE)) {
		               Date fromDate = new Date();
		               for(int i = 0; i < dateRange; i+=5){ 
		                   Date toDate = DateUtil.addDays(fromDate,5);
		                   RESRequest rateRequestEzee= getRateRequestSegmentsEzee(clients, fromDate, toDate);
		                   rateRequestEzeeList.add(rateRequestEzee);
		                   fromDate = DateUtil.addDays(toDate,1);
		               }
				   }else if(clients.getChannelManager().equals(ConstantUtil.STAAH)) {
					   Date fromDate = new Date();
		               for(int i = 0; i < dateRange; i+=27){ 
		                   Date toDate = DateUtil.addDays(fromDate,27);
		                   RateRequestSegments raterequestStaah = getRateRequestSegmentsStaah(clients, fromDate, toDate);
		                   raterequestStaahList.add(raterequestStaah);
		                   fromDate = DateUtil.addDays(toDate,1);
		               }
				   }
               });
			   if(rateRequestEzeeList.size()>0) {
				   ezeeMultiProcessingService.viewRate(rateRequestEzeeList);
			   }
			   if(raterequestStaahList.size()>0) {
				   staahMultiProcessingService.viewRate(raterequestStaahList);
			   }
               logger.error("Ended FETCH_VIEW_RATE Data");
           }catch(Exception ce) {
        	   ce.printStackTrace();
           }
       }
	   if (jobString.equals(JobType.FETCH_INVENTORY.toString())) {
		   try {
			   List<InventoryYearlyRequest> inventoryYearlyRequestStaahMaxList = new ArrayList<InventoryYearlyRequest>();
			   List<RESRequest> inventoryRequestEzeeList = new ArrayList<RESRequest>();
			   List<AvailRequestSegments> inventoryRequestStaahList = new ArrayList<AvailRequestSegments>();
			   List<InventoryRequest> inventoryRequestEGlobeList = new ArrayList<InventoryRequest>();
			   clientsList.forEach(clients->{
	               logger.error("Started FETCH_RATE_INVENTORY Data");
				   //Parameter monitoringParam =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA).orElse(null); 
				   //String isMonitoringData = monitoringParam.getParamValue();
				   Integer dateRange = 364;
				   Parameter paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), "date_range").orElse(null);
				   if(paramOpt!=null) {
					   dateRange = Integer.parseInt(paramOpt.getParamValue());
				   }
				   String totalResponse ="";
				   ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.FETCH_ROOM_MAPPING));
				   ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
				   changeStatus(job, Status.COMPLETE);

				   if(clients.getChannelManager().equals(ConstantUtil.EZEE)) {
					   Date fromDate = clients.getSystemToday();
					   for(int i = 0; i < dateRange; i+=5){
					       Date toDate = DateUtil.addDays(fromDate,5);
					       RESRequest inventoryRequestEzee  =  getInventoryRequestSegmentsEzee(clients,fromDate,toDate);
					       inventoryRequestEzeeList.add(inventoryRequestEzee);
					       fromDate = toDate;
					   }
				   }else if(clients.getChannelManager().equals(ConstantUtil.STAAH)) {
					   Date fromDate = clients.getSystemToday();
					   for(int i = 0; i < dateRange; i+=27){
					       Date toDate = DateUtil.addDays(fromDate,27);
					       AvailRequestSegments inventoryRequestStaah  =  getInventoryRequestSegmentsStaah(clients,fromDate,toDate);
					       inventoryRequestStaahList.add(inventoryRequestStaah);
					       fromDate = toDate;
					   }
				   }else if(clients.getChannelManager().equals(ConstantUtil.STAAH_ALL)) {
					   inventoryYearlyRequestStaahMaxList.addAll(getInventoryRequestSegmentsStaahMax(clients));
				   }else if(clients.getChannelManager().equals(ConstantUtil.EGLOBE)) {
					   Date fromDate = clients.getSystemToday();
					   for(int i = 0; i < dateRange; i+=20){
					       Date toDate = DateUtil.addDays(fromDate,20);
					       InventoryRequest inventoryRequestEGlobe  =  getInventoryRateRequestSegmentsEGlobe(clients,fromDate,toDate);
					       inventoryRequestEGlobeList.add(inventoryRequestEGlobe);
					       fromDate = toDate;
					   }
				   } 
				   logger.error("Ended FETCH_RATE_INVENTORY Data");
			   });
			   if(inventoryYearlyRequestStaahMaxList.size()>0) {
				   staahMaxMultiProcessingService.viewInventoryRate(inventoryYearlyRequestStaahMaxList);
			   }
			   if(inventoryRequestEzeeList.size()>0) {
				   ezeeMultiProcessingService.viewInventory(inventoryRequestEzeeList);
			   }
			   if(inventoryRequestStaahList.size()>0) {
				   staahMultiProcessingService.viewInventory(inventoryRequestStaahList);
			   }
			   if(inventoryRequestEGlobeList.size()>0) {
				   eGlobeMultiProcessingService.getInventory(inventoryRequestEGlobeList);
			   }
	       }catch(Exception ce) {
	    	   logger.error(ce.getMessage());
			   ce.printStackTrace();
	       }
	   }
	   if (jobString.equals(JobType.FETCH_BOOKING_DATA.toString())) {
		   try {
			   List<DailyBookingRequest> bookingRequestEglobeList = new ArrayList<DailyBookingRequest>();
			   List<DailyBookingRequest> bookingRequestStaahAllList = new ArrayList<DailyBookingRequest>();
			   List<RESRequest> bookingRequestEzeeList = new ArrayList<RESRequest>();
			   			clientsList.forEach(clients->{
		               		logger.error("Started FETCH_BOOKING_DATA");
						   Integer dateRange = 364;
						   Parameter paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), "date_range").orElse(null);
						   if(paramOpt!=null) {
							   dateRange = Integer.parseInt(paramOpt.getParamValue());
						   }
						   String totalResponse ="";
						   Date fromDate = DateUtil.addMonth(clients.getSystemToday(), -1);
						   ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.FETCH_ROOM_MAPPING));
						   ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
						   changeStatus(job, Status.COMPLETE);
						   if(clients.getChannelManager().equals(ConstantUtil.EZEE)) {
							   if(isschedule==true) {
								   List<RESRequest> bookingRequestEzeeList1 = getBookingOfYear(clients);
								   bookingRequestEzeeList.addAll(bookingRequestEzeeList1);
							   }else {
								   RESRequest bookingRequestEzee = getBookingRequestEzee(clients);
								   bookingRequestEzeeList.add(bookingRequestEzee);
							   }
						   } else if(clients.getChannelManager().equals(ConstantUtil.EGLOBE)) {
							   for(int i = 0; i < dateRange; i+=20){
								   Date toDate = DateUtil.addDays(fromDate,20);
								   DailyBookingRequest bookingRequestEglobe = getBookingRequestEGlobe(clients,fromDate,toDate);
								   bookingRequestEglobeList.add(bookingRequestEglobe);
							       fromDate = toDate;
							   }
						   } else if (clients.getChannelManager().equals(ConstantUtil.STAAH_ALL)) {
							   Date fromDatestaahAll = DateUtil.setTimeToZero(new Date());
							   for(int i = 0; i < 5; i++) {
								   DailyBookingRequest bookingRequestStaahAll = getBookingRequestStaahAll(clients, fromDatestaahAll, fromDatestaahAll);
								   bookingRequestStaahAllList.add(bookingRequestStaahAll);
								   fromDatestaahAll = DateUtil.addDays(fromDatestaahAll, 1);
							   }
						   }
						   logger.error("Ended FETCH_BOOKING_DATA");
	        		   });
				   if(bookingRequestEglobeList.size()>0) {
					   eGlobeMultiProcessingService.getBookings(bookingRequestEglobeList);
				   }
				   if(bookingRequestEzeeList.size()>0) {
					   ezeeMultiProcessingService.getBooking(bookingRequestEzeeList);
				   }
				   if(bookingRequestStaahAllList.size()>0) {
					   staahMaxMultiProcessingService.getBooking(bookingRequestStaahAllList);
				   }
               }catch(Exception ce) {
            	   logger.error(ce.getMessage());
        		   ce.printStackTrace();
               }  
        	   
	           
	           }
	       });
	   }


// ROOM REQUEST START

private RoomRateRequest getRoomRequestEGlobe(Clients clients) {
    RoomRateRequest roomrequest = new RoomRateRequest();
    roomrequest.setPropertyid(clients.getId());
    return roomrequest;
}

private Roomrequest getRoomrequestStaah(Clients clients) {
    Roomrequest roomrequest = new Roomrequest();
    roomrequest.setHotelId(clients.getCmHotel());
    roomrequest.setPassword(clients.getCmPassword());
    roomrequest.setUsername(clients.getCmUsername());
    return roomrequest;
}

private RoomRateRequest getRoomRequestStaahAll(Clients clients) {
    RoomRateRequest roomrequest = new RoomRateRequest();
    roomrequest.setPropertyid(clients.getCmHotel());
    roomrequest.setAction(StaahAllRequestType.ROOMRATE_INFO);
    roomrequest.setApikey(RMS_API_KEY);
    return roomrequest;
}

private RESRequest getRoomRequestEzee(Clients clients) {
    RESRequest request = new RESRequest();
    RESRequest.Authentication roomRequest = new RESRequest.Authentication();
    roomRequest.setHotelCode(clients.getCmHotel());
    roomRequest.setAuthCode(clients.getCmPassword());
    request.setRequestType(EzeeRequestType.ROOM_INFO);
    request.setAuthentication(roomRequest);
    return request;
}

//ROOM REQUEST END

//RATE REQUEST START

private RESRequest getRateRequestSegmentsEzee(Clients clients, Date fromDate, Date toDate) {
    RESRequest requestSegments = new RESRequest();
    RESRequest.Authentication roomRequest = new RESRequest.Authentication();
    roomRequest.setHotelCode(clients.getCmHotel());
    roomRequest.setAuthCode(clients.getCmPassword());
    requestSegments.setFromDate(DateUtil.dateToXMLGregorianCalendarByFormatter(fromDate));
    requestSegments.setToDate(DateUtil.dateToXMLGregorianCalendarByFormatter(toDate));
    requestSegments.setAuthentication(roomRequest);
    requestSegments.setRequestType(EzeeRequestType.RATE);
    return requestSegments;
}

private RateRequestSegments getRateRequestSegmentsStaah(Clients clients, Date fromDate, Date toDate) {
    RateRequestSegments rateRequestSegments = new RateRequestSegments();
    rateRequestSegments.setHotelId(clients.getCmHotel());
    rateRequestSegments.setPassword(clients.getCmPassword());
    rateRequestSegments.setUsername(clients.getCmUsername());
    rateRequestSegments.setVersion(1.0f);
    RateRequestSegment rateRequestSegment = new RateRequestSegment();
    rateRequestSegment.setStartDate(DateUtil.dateToXMLGregorianCalendar(fromDate));
    rateRequestSegment.setEndDate(DateUtil.dateToXMLGregorianCalendar(toDate));
    rateRequestSegments.setRateRequestSegment(rateRequestSegment);
    return rateRequestSegments;
}

//RATE REQUEST END

//INVENTORY REQUEST START

private List<InventoryYearlyRequest> getInventoryRequestSegmentsStaahMax(Clients clients) {
	List<InventoryYearlyRequest> inventoryRequestList = new ArrayList<InventoryYearlyRequest>();
	    InventoryYearlyRequest inventoryRequest = new InventoryYearlyRequest();
	    inventoryRequest.setAction(StaahAllRequestType.YEAR_INFO_ARR);
	    inventoryRequest.setPropertyid(clients.getCmHotel());
	    inventoryRequest.setRate_id(clients.getCmMasterRate());
	    inventoryRequest.setRoom_id(clients.getCmMasterRoom());
	    inventoryRequest.setApikey(RMS_API_KEY);
	    inventoryRequestList.add(inventoryRequest);
    return inventoryRequestList;
}

private RESRequest getInventoryRequestSegmentsEzee(Clients clients, Date fromDate, Date toDate) {
    RESRequest requestInventory = new RESRequest();
    RESRequest.Authentication availRequestSegments = new RESRequest.Authentication();
    availRequestSegments.setHotelCode(clients.getCmHotel());
    availRequestSegments.setAuthCode(clients.getCmPassword());
    requestInventory.setFromDate(DateUtil.dateToXMLGregorianCalendarByFormatter(fromDate));
    requestInventory.setToDate(DateUtil.dateToXMLGregorianCalendarByFormatter(toDate));
    requestInventory.setAuthentication(availRequestSegments);
    requestInventory.setRequestType(EzeeRequestType.INVENTORY);
    return requestInventory;
}

private AvailRequestSegments getInventoryRequestSegmentsStaah(Clients clients, Date fromDate, Date toDate) {
    AvailRequestSegments availRequestSegments = new AvailRequestSegments();
    availRequestSegments.setHotelId(clients.getCmHotel());
    availRequestSegments.setPassword(clients.getCmPassword());
    availRequestSegments.setUsername(clients.getCmUsername());
    availRequestSegments.setVersion(1.0f);
    AvailRequestSegment availRequestSegment = new AvailRequestSegment();
    availRequestSegment.setStartDate(DateUtil.dateToXMLGregorianCalendar(fromDate));
    availRequestSegment.setEndDate(DateUtil.dateToXMLGregorianCalendar(toDate));
    availRequestSegments.setAvailRequestSegment(availRequestSegment);
    return availRequestSegments;
}

private InventoryRequest getInventoryRateRequestSegmentsEGlobe(Clients clients, Date fromDate, Date toDate) {
	InventoryRequest inventoryRequest = new InventoryRequest();
    inventoryRequest.setPropertyid(clients.getId());
    inventoryRequest.setFrom_date(DateUtil.formatDate(fromDate,"dd-MMM-yyyy"));
    inventoryRequest.setTo_date(DateUtil.formatDate(toDate,"dd-MMM-yyyy"));
    return inventoryRequest;
}
//INVENTORY REQUEST END

//BOOKING REQUEST START




private RESRequest getBookingRequestEzee(Clients clients) {
    RESRequest requestSegments = new RESRequest();
    RESRequest.Authentication roomRequest = new RESRequest.Authentication();
    roomRequest.setHotelCode(clients.getCmHotel());
    roomRequest.setAuthCode(clients.getCmPassword());
    requestSegments.setFromDate(DateUtil.dateToXMLGregorianCalendarByFormatter(new Date()));
    requestSegments.setToDate(DateUtil.dateToXMLGregorianCalendarByFormatter(DateTime.now().plusDays(3).toDate()));
    requestSegments.setAuthentication(roomRequest);
    requestSegments.setRequestType(EzeeRequestType.BOOKING);
    return requestSegments;
}

private DailyBookingRequest getBookingRequestEGlobe(Clients clients, Date fromDate, Date toDate) {
    DailyBookingRequest dailyBookingRequest = new DailyBookingRequest();
    dailyBookingRequest.setHotelCode(clients.getId());
    DailyBookingRequest.CheckIn checkIn = new DailyBookingRequest.CheckIn();
    checkIn.setStart(DateUtil.formatDate(fromDate,"dd-MMM-yyyy"));
    checkIn.setEnd(DateUtil.formatDate(toDate,"dd-MMM-yyyy"));
    dailyBookingRequest.setCheckIn_At(checkIn);       
    return dailyBookingRequest;
}

private DailyBookingRequest getBookingRequestStaahAll(Clients clients, Date fromDate, Date toDate) {
    DailyBookingRequest dailyBookingRequest = new DailyBookingRequest();
    dailyBookingRequest.setHotelCode(clients.getCmHotel());
    DailyBookingRequest.CheckIn checkIn = new DailyBookingRequest.CheckIn();
    checkIn.setStart(DateUtil.formatDate(fromDate,"yyyy-MM-dd"));
    checkIn.setEnd(DateUtil.formatDate(toDate,"yyyy-MM-dd"));
    dailyBookingRequest.setCheckIn_At(checkIn);       
    return dailyBookingRequest;
}

//BOOKING REQUEST END

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



private List<RESRequest> getBookingOfYear(Clients clients) {
	 RESRequest requestSegments = new RESRequest();
	 List<RESRequest> bookingRequestEzeeList= new ArrayList<RESRequest>();
	    Date startDate=DateUtil.getDate(new Date());
	    Date endDate=DateUtil.addDays(startDate,365);
    int noOfDays = (int) DateUtil.daysBetween(startDate, endDate);
    try {
        for (int i = 0; i <= noOfDays; ) {
        	RESRequest.Authentication roomRequest = new RESRequest.Authentication();
      	    roomRequest.setHotelCode(clients.getCmHotel());
      	    roomRequest.setAuthCode(clients.getCmPassword());
      	    requestSegments.setAuthentication(roomRequest);
      	    requestSegments.setRequestType(EzeeRequestType.BOOKING);
            endDate = DateUtil.addDays(startDate, 5);
            requestSegments.setFromDate(DateUtil.dateToXMLGregorianCalendarByFormatter(startDate));
            requestSegments.setToDate(DateUtil.dateToXMLGregorianCalendarByFormatter(endDate));
            startDate = endDate;
            i = i + 5;
            if(requestSegments!=null) {
            bookingRequestEzeeList.add(requestSegments);
            }
        }
    }catch(Exception ce) {
    	alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage()+" "+ce.getCause());
    }
    return bookingRequestEzeeList;
}

}
