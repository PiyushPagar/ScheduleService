package com.revnomix.revseed.integration.staahMax;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.JobType;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.ScheduledJob;
import com.revnomix.revseed.model.StaahRateTypes;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.FinalRecommendationsRepository;
import com.revnomix.revseed.repository.OverrideRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.ScheduledJobRepository;
import com.revnomix.revseed.repository.StaahRateTypesRepository;
import com.revnomix.revseed.wrapper.OverrideDetailsDto;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.integration.exception.RevseedException;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.service.Response;
import com.revnomix.revseed.integration.service.StaahAllRequestType;
import com.revnomix.revseed.integration.staah.transformer.PopulateGenericGateway;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingRequest;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingResponse;
import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;
import com.revnomix.revseed.integration.staahMax.dto.InventoryRequest;
import com.revnomix.revseed.integration.staahMax.dto.InventoryYearlyRequest;
import com.revnomix.revseed.integration.staahMax.dto.RateInventoryMapping;
import com.revnomix.revseed.integration.staahMax.dto.RateUpdate;
import com.revnomix.revseed.integration.staahMax.dto.RoomMapping;
import com.revnomix.revseed.integration.staahMax.dto.RoomRateRequest;
import com.revnomix.revseed.integration.staahMax.dto.StaahMaxPopulationDto;
import com.revnomix.revseed.integration.staahMax.dto.StaahMaxPropertStatus;
import com.revnomix.revseed.integration.staahMax.gateway.PopulateStaahMaxGateway;


@Controller
@RequestMapping("/staahAll")
@CrossOrigin(origins = "*")
public class StaahMaxInboundService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;
    @Autowired
    private FinalRecommendationsRepository finalRecommendationsRepository;
    @Autowired
    private ClientsRepository clientsRepository;
    @Autowired
    private PopulateStaahMaxGateway populateStaahGateway;
    @Autowired
    private SystemUtil systemUtil;
    @Autowired
    private ParameterRepository parametersRepository;
    @Autowired
    private OverrideRepository overrideRepository;
    @Autowired
    private ParameterRepository parameterRepository;
    @Autowired
    private ScheduledJobRepository scheduledJobRepository;
	@Autowired
	private AlertService alertService;
    @Autowired
    private PopulateGenericGateway populateGenericGateway;
    
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
    @RequestMapping(value = "/outbound/roomMapping", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response roomMapping(@RequestBody RoomRateRequest roomrequest) throws Exception {
    	Response resp = new Response();
        StaahMaxPopulationDto staahPopulationDto = new StaahMaxPopulationDto();
        Clients clients = clientsRepository.findByCmHotel(roomrequest.getPropertyid());
        if (clients == null) {
            throw new RevseedException("Hotel/client not found with id :" + roomrequest.getPropertyid());
        }
        staahPopulationDto.setClient(clients);
        RoomMapping response = null;
        String response1 = "";
        String utf8EncodedString ="";
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RoomRateRequest> requestObj = new HttpEntity<>(roomrequest,headers);
        try {
	        RestTemplate restTemplate = new RestTemplate();
	        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
	        response1 = restTemplate.postForObject(getGetUrl(), requestObj, String.class, new HashMap<>()).replaceAll("&", "&amp;");
	        byte[] bytes = StringUtils.getBytesUtf8(response1);           
	        utf8EncodedString = StringUtils.newStringUtf8(bytes);
	        Gson gson = new Gson();
	        response = gson.fromJson(utf8EncodedString, RoomMapping.class);
	        staahPopulationDto.setRoomMapping(response);
	        populateStaahGateway.populateStaahMaxRoomMapping(staahPopulationDto);
        	resp.setMessage(utf8EncodedString);
        	resp.setStatus(Status.SUCCESS);
        }catch(Exception ce) {
        	ce.printStackTrace();
        	if(utf8EncodedString.equals("")){
        		resp.setMessage(ce.getMessage()+" "+ce.getCause());
        	}else {
        		resp.setMessage(utf8EncodedString);
        	}
        	resp.setStatus(Status.FAILED);
        }
        return resp;
    }

    @RequestMapping(value = "/outbound/reservation", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String reservation(@RequestBody DailyBookingResponse dailyBookingResponse) {
        logger.error("Push Booking for client started : " + dailyBookingResponse.getReservations().get(0).getProperty_id());
        
        StaahMaxPopulationDto staahPopulationDto = new StaahMaxPopulationDto();
        Clients clients = clientsRepository.findByCmHotel(dailyBookingResponse.getReservations().get(0).getProperty_id());
        if (clients == null) {
            throw new RevseedException("Push Booking Hotel/client not found with id :" + dailyBookingResponse.getReservations().get(0).getProperty_id());
        }
        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.STAAH_ALL_PUSH));
        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
        staahPopulationDto.setClient(clients);
        try {
            staahPopulationDto.setDailyBookingResponse(dailyBookingResponse);
            populateStaahGateway.populateStaahMaxPushBooking(staahPopulationDto);
            changeStatus(runningJob, Status.COMPLETE);
            logger.error("Push Booking Fetching Booking for client completed : " + dailyBookingResponse.getReservations().get(0).getProperty_id());
        } catch (Exception e) {
        	changeStatus(runningJob, Status.FAILED);
            logger.error("Push Booking Failed while parsing response for Booking for hotel {} {} ", clients.getCmHotel(), e);
        }
        return "Added Booking Successfully !!";
    }

    @RequestMapping(value = "/outbound/viewInventoryRateByDate", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response viewInventoryRateByDate(@RequestBody InventoryRequest inventoryRequest)  throws Exception{
        ArrayList<StaahMaxPopulationDto> dtoList = new ArrayList<StaahMaxPopulationDto>();
        Clients clients = clientsRepository.findByCmHotel(inventoryRequest.getPropertyid());
        if (clients == null) {
            throw new RevseedException("Client not found with id :" + inventoryRequest.getPropertyid());
        }
        String response1 = "";
        Response resp = new Response();
        for (StaahRateTypes rate : staahRateTypesRepository.findAllByClientId(clients.getId())) {
       	 StaahMaxPopulationDto staahPopulationDto = new StaahMaxPopulationDto();
       	 staahPopulationDto.setClient(clients);
            inventoryRequest.setRate_id(rate.getStaahRateId().intValue());
            inventoryRequest.setRoom_id(rate.getStaahRoomId().intValue());
            RateInventoryMapping response = new RateInventoryMapping();
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<InventoryRequest> requestObj = new HttpEntity<>(inventoryRequest,headers);
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
                response1 = restTemplate.postForObject(getGetUrl(), requestObj, String.class, new HashMap<>()).replaceAll("&", "&amp;");
                byte[] bytes = StringUtils.getBytesUtf8(response1);           
                String utf8EncodedString = StringUtils.newStringUtf8(bytes);
                Gson gson = new Gson();
                response = gson.fromJson(utf8EncodedString, RateInventoryMapping.class);
                if (response == null) {
                    throw new RevseedException("Failed while getting data for inventory :" + inventoryRequest.getPropertyid());
                }
                resp.setMessage(utf8EncodedString);
            	resp.setStatus(Status.SUCCESS);
                staahPopulationDto.setRateInventoryMapping(response);               
                logger.error("Fetching Rate Inventory By Date for client completed : " + inventoryRequest.getPropertyid());
                }
        	populateStaahGateway.populateStaahMaxInventoryAndRate(dtoList);
        return resp;
    }

    @RequestMapping(value = "/outbound/viewInventoryRateByYear", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response viewInventoryRateByYear(@RequestBody InventoryYearlyRequest inventoryRequest) throws Exception {
        Clients clients = clientsRepository.findByCmHotel(inventoryRequest.getPropertyid());
        if (clients == null) {
            throw new RevseedException("Client not found with id :" + inventoryRequest.getPropertyid());
        }
        ArrayList<StaahMaxPopulationDto> dtoList = new ArrayList<StaahMaxPopulationDto>();
        String response1 = "";
        Response resp = new Response();
        for (StaahRateTypes rate : staahRateTypesRepository.findAllByClientId(clients.getId())) {
        	 StaahMaxPopulationDto staahPopulationDto = new StaahMaxPopulationDto();
        	 staahPopulationDto.setClient(clients);
            inventoryRequest.setRate_id(rate.getStaahRateId());
            inventoryRequest.setRoom_id(rate.getStaahRoomId());
            try {
            	RateInventoryMapping response;
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<InventoryYearlyRequest> requestObj = new HttpEntity<>(inventoryRequest,headers);
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
                response1 = restTemplate.postForObject(getGetUrl(), requestObj, String.class, new HashMap<>()).replaceAll("&", "&amp;");
                byte[] bytes = StringUtils.getBytesUtf8(response1);           
                String utf8EncodedString = StringUtils.newStringUtf8(bytes);
                try {
	                Gson gson = new Gson();
	                response = gson.fromJson(utf8EncodedString, RateInventoryMapping.class);
	                staahPopulationDto.setRateInventoryMapping(response);
	                dtoList.add(staahPopulationDto);
	                resp.setMessage(utf8EncodedString);
                	resp.setStatus(Status.SUCCESS);
                }catch(Exception ce) {
                  	resp.setMessage(utf8EncodedString);
                	resp.setStatus(Status.FAILED);
                }
            }catch(Exception ce) {
            	resp.setMessage(ce.getMessage()+" "+ce.getCause());
            	resp.setStatus(Status.FAILED);
            	ce.printStackTrace();
            }
        }
        populateStaahGateway.populateStaahMaxInventoryAndRate(dtoList);
        return resp;
    }

    @RequestMapping(value = "/outbound/updateRate", method = RequestMethod.GET)
    @ResponseBody
    public Response upateRate(@RequestParam("clientId") Integer clientId) {
        Response response = new Response();
        String response1 ="";
        Clients clients = clientsRepository.findById(clientId).orElse(null);
        Response resp = new Response();
        if (clients == null) {
            response.setMessage("Hotel/client not found with id :" + clientId);
            response.setStatus(com.revnomix.revseed.model.Status.FAILED);
            throw new RevseedException("Client not found with id :" + clientId);
        }
        Parameter par = new Parameter();
        Long masterRateId = null;
    	Optional<Parameter>  isMax = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.ISMAX);
    	if (isMax.isPresent()) {
    		par = isMax.get();
    		if(par.getParamValue().equals(ConstantUtil.TRUE)){
    			Optional<Parameter>  masterRateIdObj = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.MASTERRATEID);
    	    	if (masterRateIdObj.isPresent()) {
    	    		masterRateId = Long.parseLong(masterRateIdObj.get().getParamValue());
    	    	}
    		}
    	}  
    	
        List<OverrideDetailsDto> latestFinalRates = finalRecommendationsRepository.getLatestFinalRateByClientId(clients.getId());
        RateUpdate updateRequest = new RateUpdate();
        RateUpdate.UpdateRequest request = new RateUpdate.UpdateRequest();
        List<RateUpdate.UpdateRequest.Room> roomList = new ArrayList<>();
        List<RateUpdate.UpdateRequest.Room.Rate> rateList = new ArrayList<>();
        RateUpdate.UpdateRequest.Room room = new RateUpdate.UpdateRequest.Room();
        RateUpdate.UpdateRequest.Room.Rate rate = new RateUpdate.UpdateRequest.Room.Rate();
        List<RateUpdate.UpdateRequest.Room.Rate.Dates> data = new ArrayList<>();
        latestFinalRates.forEach(row -> {
                    RateUpdate.UpdateRequest.Room.Rate.Dates dates = new RateUpdate.UpdateRequest.Room.Rate.Dates();
                    dates.setStart_date(DateUtil.formatDate(row.getCheckin_date(), "yyyy-MM-dd"));
                    dates.setEnd_date(DateUtil.formatDate(row.getCheckin_date(), "yyyy-MM-dd"));
                    dates.setPrice((int) row.getRate());
                    data.add(dates);
                }
        );
        rate.setDates(data);
        if (clients.getCmMasterRate() != null) {
        	if(masterRateId!=null) {
        		rate.setRate_id(masterRateId);
        	}else {
        		rate.setRate_id(clients.getCmMasterRate()); // client cm master rate
        	}
            rateList.add(rate);
            room.setRate(rateList);
        }
        if (clients.getCmMasterRoom() != null) {
            room.setRoom_id(clients.getCmMasterRoom()); // client cm master room
            roomList.add(room);
            request.setRoom(roomList);
        }
        updateRequest.setUpdaterequest(request);
        setData(updateRequest,clients);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RateUpdate> requestObj = new HttpEntity<>(updateRequest,headers);
        try {
	        RestTemplate restTemplate = new RestTemplate();
	        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
	        response1 = restTemplate.postForObject(getRmsUrl(), requestObj, String.class, new HashMap<>()).replaceAll("&", "&amp;");
	        alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
	        pullRate(clients, updateRequest);
	        resp.setMessage(response1);
        	resp.setStatus(Status.SUCCESS);
        }catch(Exception e) {
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,response1);
        	if(response1.equals("")){
        		resp.setMessage(e.getMessage()+" "+e.getCause());
        	}else {
        		resp.setMessage(response1);
        	}
        	resp.setStatus(Status.FAILED);
            e.printStackTrace();
        }
        return response;
    }

    private void pullRate(Clients clients, RateUpdate updateRequest) throws Exception {
        InventoryYearlyRequest inventoryYearlyRequest = new InventoryYearlyRequest();
        inventoryYearlyRequest.setPropertyid(clients.getCmHotel());
        inventoryYearlyRequest.setRate_id(updateRequest.getUpdaterequest().getRoom().get(0).getRate().get(0).getRate_id());
        inventoryYearlyRequest.setRoom_id(updateRequest.getUpdaterequest().getRoom().get(0).getRoom_id());
        inventoryYearlyRequest.setApikey(RMS_API_KEY);
        inventoryYearlyRequest.setAction(StaahAllRequestType.YEAR_INFO_ARR);
        viewInventoryRateByYear(inventoryYearlyRequest);
    }

    @RequestMapping(value = "/outbound/getBooking", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response getBooking(@RequestBody DailyBookingRequest dailyBookingRequest) throws Exception{
        logger.error("Fetching Booking for client started : " + dailyBookingRequest.getHotelCode());
        StaahMaxPopulationDto staahPopulationDto = new StaahMaxPopulationDto();
        Response resp = new Response();
        Clients clients = clientsRepository.findByCmHotel(dailyBookingRequest.getHotelCode());
        if (clients == null) {
            throw new RevseedException("Hotel/client not found with id :" + dailyBookingRequest.getHotelCode());
        }
        staahPopulationDto.setClient(clients);
        DailyBookingResponse dailyBookingResponse = new DailyBookingResponse();
        String response1 = "";
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("STAAH-AUTH-KEY", CRM_API_KEY);
            HttpEntity<DailyBookingRequest> requestObj = new HttpEntity<>(dailyBookingRequest,headers);
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            response1 = restTemplate.postForObject(getCrmUrl(), requestObj, String.class, new HashMap<>()).replaceAll("&", "&amp;");
            byte[] bytes = StringUtils.getBytesUtf8(response1);           
            String utf8EncodedString = StringUtils.newStringUtf8(bytes);
            try {
            	ObjectMapper om = new ObjectMapper();
            	dailyBookingResponse = om.readValue(utf8EncodedString, DailyBookingResponse.class);
	            staahPopulationDto.setDailyBookingResponse(dailyBookingResponse);
	            
	            GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
				genericIntegrationDto.setClient(clients);
				genericIntegrationDto.setDailyBookingResponse(dailyBookingResponse);
				genericIntegrationDto.setIntegerationType(IntegrationType.STAAHMAX.toString());
				populateGenericGateway.populateGenericBooking(genericIntegrationDto);
				
	            // populateStaahGateway.populateStaahMaxBooking(staahPopulationDto);
               	resp.setMessage(utf8EncodedString);
            	resp.setStatus(Status.SUCCESS);
            }catch(Exception ce) {
               	resp.setMessage(utf8EncodedString);
            	resp.setStatus(Status.FAILED);
            	ce.printStackTrace();
            }
        return resp;
    }

    @RequestMapping(value = "/changePropertyStatus", method = RequestMethod.POST)
    @ResponseBody
    public String check(@RequestParam("clientId") Integer clientId) {
        logger.info("Change Staah All Property Status for: "+clientId);
        Optional<Clients> clients = clientsRepository.findById(clientId);
        String response1 ="";
        try {
	        if (!clients.isPresent()){
	            throw new RevseedException("Client not found with id :" + clientId);
	        }else{
	            Clients client = clients.get();
	            HttpEntity<StaahMaxPropertStatus> statusEntity = setPropertyStatusHeader("Live",client.getCmHotel());
	            RestTemplate restTemplate = new RestTemplate();
	            restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
	            response1 = restTemplate.postForObject(getConformationUrl(), statusEntity, String.class, new HashMap<>()).replaceAll("&", "&amp;");
	            if(response1.contains("Successfully Live Property") || response1.contains("Property is already active")){
	                parametersRepository.findByClientIdAndParamName(clientId, "Staah_Dual_Integration").ifPresentOrElse(parameter -> {
	                    parameter.setParamValue("1");
	                    parametersRepository.save(parameter);
	                },()->{
	                    Parameter parameter = new Parameter();
	                    parameter.setParamType("Recommendation");
	                    parameter.setParamName("Staah_Dual_Integration");
	                    parameter.setParamValue("1");
	                    parameter.setUiTag("Staah Dual Integration");
	                    parameter.setClientId(clientId);
	                    parameter.setModDate(new Date());
	                    parametersRepository.save(parameter);
	                });
	                return "Property is Live Successfully!!";
	            }else {
	            	return "Property Failed to Make Live!!";
	            }
	        }
        }catch(Exception ce) {
        	ce.printStackTrace();
        	return  "Property Failed to Make Live!!";
        }		
    }
        
    
    @RequestMapping(value = "/changeProperty", method = RequestMethod.GET)
    @ResponseBody
    public boolean changeProperty(@RequestParam("clientId") Integer clientId) {
        if(parametersRepository.findByClientIdAndParamName(clientId, "Staah_Dual_Integration").isPresent()){
            Parameter parameter = parametersRepository.findByClientIdAndParamName(clientId, "Staah_Dual_Integration").get();
            return !parameter.getParamValue().equalsIgnoreCase("1");
        }
        return true;
    }
    
    @RequestMapping(value = "/outbound/pushAllOverrideRates", method = RequestMethod.PUT)
    @ResponseBody
    public Response updateRate(@RequestParam("clientId") Integer clientId) {
        Response response = createInitialResponse();
        Clients clients = clientsRepository.findById(clientId).get();
        String response1 = "";
        try {
	        if (clients == null) {
	            response.setMessage("Hotel/client not found with id :" + clientId);
	        }
	        Parameter par = new Parameter();
	        Long masterRateId = null;
	    	Optional<Parameter>  isMax = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.ISMAX);
	    	if (isMax.isPresent()) {
	    		par = isMax.get();
	    		if(par.getParamValue().equals("true")){
	    			Optional<Parameter>  masterRateIdObj = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.MASTERRATEID);
	    	    	if (masterRateIdObj.isPresent()) {
	    	    		masterRateId = Long.parseLong(masterRateIdObj.get().getParamValue());
	    	    	}
	    		}
	    	}  
	    	
	        Date startDate = DateUtil.localToDate(DateUtil.dateToLocalDate(clients.getSystemToday()).minusDays(1));
	        List<OverrideDetailsDto> overrides = overrideRepository.getLatestOverrideValue(clientId, startDate);
	        RateUpdate updateRequest = new RateUpdate();
	        RateUpdate.UpdateRequest request = new RateUpdate.UpdateRequest();
	        List<RateUpdate.UpdateRequest.Room> roomList = new ArrayList<>();
	        List<RateUpdate.UpdateRequest.Room.Rate> rateList = new ArrayList<>();
	        RateUpdate.UpdateRequest.Room room = new RateUpdate.UpdateRequest.Room();
	        RateUpdate.UpdateRequest.Room.Rate rate = new RateUpdate.UpdateRequest.Room.Rate();
	        List<RateUpdate.UpdateRequest.Room.Rate.Dates> data = new ArrayList<>();
	        overrides.forEach(row -> {
	                    RateUpdate.UpdateRequest.Room.Rate.Dates dates = new RateUpdate.UpdateRequest.Room.Rate.Dates();
	                    dates.setStart_date(DateUtil.formatDate(row.getCheckin_date(), "yyyy-MM-dd"));
	                    dates.setEnd_date(DateUtil.formatDate(row.getCheckin_date(), "yyyy-MM-dd"));
	                    dates.setPrice((int) row.getRate());
	                    data.add(dates);
	                }
	        );
	        rate.setDates(data);
	        if (clients.getCmMasterRate() != null) {
	        	if(masterRateId!=null) {
	        		rate.setRate_id(masterRateId);
	        	}else {
	        		rate.setRate_id(clients.getCmMasterRate()); // client cm master rate
	        	}
	            rateList.add(rate);
	            room.setRate(rateList);
	        }
	        if (clients.getCmMasterRoom() != null) {
	            room.setRoom_id(clients.getCmMasterRoom()); // client cm master room
	            roomList.add(room);
	            request.setRoom(roomList);
	        }
	        updateRequest.setUpdaterequest(request);
	        setData(updateRequest,clients);
	        ObjectMapper objmap = new ObjectMapper();
	        try {
	        	String s = objmap.writeValueAsString(updateRequest);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
	        HttpHeaders headers = new HttpHeaders();
	        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        HttpEntity<RateUpdate> requestObj = new HttpEntity<>(updateRequest,headers);
	        RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            response1 = restTemplate.postForObject(getRmsUrl(), requestObj, String.class, new HashMap<>()).replaceAll("&", "&amp;");
	        alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
	        pullRate(clients, updateRequest);
	        response.setMessage(response1);
	        response.setStatus(Status.SUCCESS);
        }catch (Exception e) {
        	response.setMessage(response1);
        	response.setStatus(Status.FAILED);
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,response1);
		}
        return response;
    }
    
    private Response createInitialResponse() {
        Response response = new Response();
        response.setStatus(com.revnomix.revseed.model.Status.COMPLETE);
        response.setMessage("Rate update has been process successfully");
        return response;
    }
    
    public String getGetUrl() {
        return getUrl;
    }

    public String getCrmUrl() {
        return crmUrl;
    }

    public String getRmsUrl() {
        return rmsUrl;
    }

    public String getConformationUrl() {
        return conformationUrl;
    }

    private HttpEntity setHeader(DailyBookingRequest dailyBookingRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("STAAH-AUTH-KEY", CRM_API_KEY);
        HttpEntity<DailyBookingRequest> entity = new HttpEntity<>(dailyBookingRequest, headers);
        return entity;
    }

    private HttpEntity<StaahMaxPropertStatus> setPropertyStatusHeader(String status, Integer hotelCode) {
        HttpHeaders headers = new HttpHeaders();
        StaahMaxPropertStatus staahMaxPropertStatus = new StaahMaxPropertStatus();
        staahMaxPropertStatus.setHotelCode(hotelCode);
        staahMaxPropertStatus.setConfirmation(status);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("STAAH-AUTH-KEY", CRM_API_KEY);
        HttpEntity<StaahMaxPropertStatus> entity = new HttpEntity<>(staahMaxPropertStatus, headers);
        return entity;
    }

    private RateUpdate setData(RateUpdate dailyBookingRequest, Clients clients) {
        RateUpdate.UpdateRequest result = dailyBookingRequest.getUpdaterequest();
        result.setHotel_id(clients.getCmHotel());
        result.setUsername(clients.getCmUsername()); 
        result.setPassword(clients.getCmPassword()); 
        result.setVersion("1.0"); 
        return dailyBookingRequest;
    }
    
    private ScheduledJob changeStatus(ScheduledJob job, Status status) {
        job.setStatus(status);
        return scheduledJobRepository.save(job);
    }
    
    private ScheduledJob createJob(Clients clients, JobType jobType) {
        ScheduledJob scheduledJob = new ScheduledJob();
        scheduledJob.setClientId(clients.getId());
        scheduledJob.setJobType(jobType);
        scheduledJob.setStartDateTime(new Date());
        scheduledJob.setResponse("");
        scheduledJob.setStatus(Status.CREATED);
        return scheduledJob;
    }

}

//staah.url:https://getyourweb.staah.net/common-cgi/Simulator/Services.pl
//staah.url:https://metachannel.staah.net/common-cgi/api/Services.pl
