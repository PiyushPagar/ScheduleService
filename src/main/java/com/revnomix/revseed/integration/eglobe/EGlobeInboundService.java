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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.JobType;
import com.revnomix.revseed.model.ScheduledJob;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.ScheduledJobRepository;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeBookingResponse;
import com.revnomix.revseed.integration.eglobe.dto.EGlobePopulationDto;
import com.revnomix.revseed.integration.eglobe.dto.EglobeRoomMapping;
import com.revnomix.revseed.integration.eglobe.dto.Root;
import com.revnomix.revseed.integration.exception.RevseedException;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.service.Response;
import com.revnomix.revseed.integration.staah.transformer.PopulateGenericGateway;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingRequest;
import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;
import com.revnomix.revseed.integration.staahMax.dto.InventoryRequest;
import com.revnomix.revseed.integration.staahMax.dto.RoomRateRequest;
import com.revnomix.revseed.integration.staahMax.dto.StaahMaxPopulationDto;




@Controller
@RequestMapping("/eglobe/inbound")
@CrossOrigin(origins = "*")
public class EGlobeInboundService {
	
	   private final Logger logger = LoggerFactory.getLogger(this.getClass());
	    
	    @Autowired
	    private PopulateEGlobeGateway populateEGlobeGateway;
	    
	    @Autowired
	    private ScheduledJobRepository scheduledJobRepository;
	    
	    @Autowired
	    private ClientsRepository clientsRepository;

	    @Autowired
	    private PopulateGenericGateway populateGenericGateway;
		
	    @Autowired
	    private SystemUtil systemUtil;

	   @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/bookings/}")
	   private String getBookingUrl;
	   
	   @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/rooms/}")
	   private String getRoomsUrl;
	   
	   @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/inventory/}")
	   private String inventoryUrl;
	   
	   @Value("${eglobe.url:https://www.eglobe-solutions.com/webapichannelmanager/rates/}")
	   private String updateRateUrl;
	   
	   public static final String EGLOBE_API_KEY = "GQ03IwoOvXyVMixyepTH";
	   
	   @RequestMapping(value = "/roommapping", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	   @ResponseBody
	    public Response roomMapping(@RequestBody RoomRateRequest roomrequest) throws Exception {
		   Response resp = new Response();
	        List<EglobeRoomMapping> response = null;
	        EGlobePopulationDto eGlobePopulationDto = new EGlobePopulationDto();
	        logger.debug("Fetching roomMapping for eglobe started" + roomrequest.getPropertyid());
	        Clients clients = clientsRepository.findByCmHotel(roomrequest.getPropertyid());		       
		        if (clients == null) {
		            throw new RevseedException("Hotel/client not found with id :" + roomrequest.getPropertyid());
		        }
		        eGlobePopulationDto.setClient(clients);
		        String url = getGetRoomsUrl()+clients.getCmPassword();
		        HttpHeaders headers = new HttpHeaders();
            	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            	RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            	ResponseEntity<String> response1 = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
	            byte[] bytes = StringUtils.getBytesUtf8(response1.getBody());            
	            String utf8EncodedString = StringUtils.newStringUtf8(bytes);
	            ObjectMapper objectMapper = new ObjectMapper();
	            try {
		            response = objectMapper.readValue(utf8EncodedString, new TypeReference<List<EglobeRoomMapping>>(){});
		            eGlobePopulationDto.setEglobeRoomMappingList(response);
		            populateEGlobeGateway.populateEGlobeRoomMapping(eGlobePopulationDto);
	            	resp.setMessage(utf8EncodedString);
	            	resp.setStatus(Status.SUCCESS);
	            }catch(Exception ce) {
	            	ce.printStackTrace();
	            	resp.setMessage(utf8EncodedString);
	            	resp.setStatus(Status.FAILED);
	            }
	            logger.debug("Fetching roomMapping for eglobe completed");
	        return resp;
	    }
	   

	    @RequestMapping(value = "/pushbookings", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	    @ResponseBody
	    public Response reservation(@RequestBody EGlobeBookingResponse eGlobeBookingResponse) {
	        logger.error("Push Booking for client started : " + eGlobeBookingResponse.getBookingId());	
	        Response resp = new Response();
	        StaahMaxPopulationDto staahPopulationDto = new StaahMaxPopulationDto();
	        Clients clients = clientsRepository.findBycmPassword(eGlobeBookingResponse.getBookingId());
	        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.STAAH_ALL_PUSH));
	        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
	        staahPopulationDto.setClient(clients);
	        	List<EGlobeBookingResponse> response = new ArrayList<EGlobeBookingResponse>();
	            EGlobePopulationDto eGlobePopulationDto= new EGlobePopulationDto();
	            eGlobePopulationDto.setClient(clients);
	            response.add(eGlobeBookingResponse);
	            eGlobePopulationDto.setEGlobeBookingResponseList(response);
	            if(response.size()>0) {
	            	populateEGlobeGateway.populateEGlobeBooking(eGlobePopulationDto);
	            }
	            changeStatus(runningJob, Status.COMPLETE);	            
	        	resp.setMessage("Added Booking Successfully !!");
            	resp.setStatus(Status.SUCCESS);
	        return resp;
	    }
	   
	   @RequestMapping(value = "/getbookings", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	    @ResponseBody
	    public Response getBookings(@RequestBody DailyBookingRequest bookingRequest) throws Exception{
	        logger.debug("Fetching roomMapping for eglobe started");
	        Response resp = new Response();
	        List<EGlobeBookingResponse> response = null;
	        	Clients clients = clientsRepository.findById(bookingRequest.getHotelCode()).orElse(null);
	            if (clients == null) {
	                throw new RevseedException("Client not found with id :" + bookingRequest.getHotelCode());
	            }
	        	String url = new String (getGetBookingUrl()+clients.getCmPassword()+"?searchBy=BookingDate&fromDate="+bookingRequest.getCheckIn_At().getStart()+"&tillDate="+bookingRequest.getCheckIn_At().getEnd());
	        	HttpHeaders headers = new HttpHeaders();
            	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            	RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            	ResponseEntity<String> response1 = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
	            byte[] bytes = StringUtils.getBytesUtf8(response1.getBody());            
	            String utf8EncodedString = StringUtils.newStringUtf8(bytes);
	            ObjectMapper objectMapper = new ObjectMapper();
	            try {
		            response = objectMapper.readValue(utf8EncodedString, new TypeReference<List<EGlobeBookingResponse>>(){});
		            EGlobePopulationDto eGlobePopulationDto= new EGlobePopulationDto();
		            eGlobePopulationDto.setClient(clients);
		            eGlobePopulationDto.setEGlobeBookingResponseList(response);
		            GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
    				genericIntegrationDto.setClient(clients);
    				genericIntegrationDto.setEGlobeBookingResponseList(response);
    				genericIntegrationDto.setIntegerationType(IntegrationType.EGLOBE.toString());
    				if(response.size()>0) {
    					 populateGenericGateway.populateGenericBooking(genericIntegrationDto);		           
		            	//populateEGlobeGateway.populateEGlobeBooking(eGlobePopulationDto);
		            }
		        	resp.setMessage(utf8EncodedString);
	            	resp.setStatus(Status.SUCCESS);
	            }catch(Exception ce) {
	            	ce.printStackTrace();
	            	resp.setMessage(utf8EncodedString);
	            	resp.setStatus(Status.FAILED);
	            }
	            logger.debug("Fetching roomMapping for eglobe completed");
	            return resp;
	    }
	   
	   @RequestMapping(value = "/getInventory", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	    @ResponseBody
	    public Response getInventory(@RequestBody InventoryRequest inventoryRequest) throws Exception{
	        logger.debug("Fetching roomMapping for eglobe started");
	        String response1 = "";
	        Response resp = new Response();
	        Clients clients = clientsRepository.findById(inventoryRequest.getPropertyid()).orElse(null);
	        	logger.error("Fetching Rate Inventory By Year for client started :" + inventoryRequest.getPropertyid());	            	            
	            if (clients == null) {
	                throw new RevseedException("Client not found with id :" + inventoryRequest.getPropertyid());
	            }
            	String url = new String(getInventoryUrl()+clients.getCmPassword()+"/channels/1006/v2?fromDate="+inventoryRequest.getFrom_date()+"&tillDate="+inventoryRequest.getTo_date());
            	HttpHeaders headers = new HttpHeaders();
            	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            	RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            	ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            	byte[] bytes = StringUtils.getBytesUtf8(response.getBody());            
 	            String utf8EncodedString = StringUtils.newStringUtf8(bytes);
 	            ObjectMapper om = new ObjectMapper();
 	            try {
	 	            Root root = om.readValue(utf8EncodedString, Root.class);
	 	            EGlobePopulationDto eGlobePopulationDto= new EGlobePopulationDto();
	 	            eGlobePopulationDto.setClient(clients);
	 	            eGlobePopulationDto.setRoot(root);
	 	            populateEGlobeGateway.populateEGlobeInventoryAndRate(eGlobePopulationDto);
		        	resp.setMessage(utf8EncodedString);
	            	resp.setStatus(Status.SUCCESS);
 	            }catch(Exception ce) {
 	            	ce.printStackTrace();
		        	resp.setMessage(utf8EncodedString);
	            	resp.setStatus(Status.FAILED);
 	            }
	            logger.debug("Fetching roomMapping for eglobe completed");
	        	return resp;
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
	
    private ScheduledJob createJob(Clients clients, com.revnomix.revseed.model.JobType jobType) {
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
    

}
