package com.revnomix.revseed.integration.service;


import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.AccountsRepository;
import com.revnomix.revseed.repository.AlertConditionTypeRepository;
import com.revnomix.revseed.repository.AlertConfigurationRepository;
import com.revnomix.revseed.repository.AlertTypeRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.FinalRecommendationsRepository;
import com.revnomix.revseed.repository.OverrideRepository;
import com.revnomix.revseed.schema.staah.AvailRequestSegments;
import com.revnomix.revseed.schema.staah.RateRequestSegments;
import com.revnomix.revseed.schema.staah.Reservations;
import com.revnomix.revseed.schema.staah.Roomrequest;
import com.revnomix.revseed.schema.staah.Roomresponse;
import com.revnomix.revseed.schema.staah.UpdateRate;
import com.revnomix.revseed.schema.staah.UpdateRoom;
import com.revnomix.revseed.schema.staah.Updaterequest;
import com.revnomix.revseed.schema.staah.Updateresponse;
import com.revnomix.revseed.wrapper.OverrideDetailsDto;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.StaahXmlUtil;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.Util.service.XMLUtilService;
import com.revnomix.revseed.integration.exception.RevseedException;
import com.revnomix.revseed.integration.staah.gateway.PopulateStaahGateway;
import com.revnomix.revseed.integration.staah.transformer.StaahPopulationDto;

@Controller
@CrossOrigin(origins = "*")
public class StaahInboundService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ClientsRepository clientsRepository;
    @Autowired
    private PopulateStaahGateway populateStaahGateway;
    @Autowired
    private SystemUtil systemUtil;
    @Autowired
    private Jaxb2Marshaller jaxb2Marshaller;

    @Autowired
    private XMLUtilService xmlUtilService;

    @Autowired
    
    private OverrideRepository overrideRepository;

    @Autowired
    private FinalRecommendationsRepository finalRecommendationsRepository;
    
	@Autowired
	AlertConfigurationRepository alertConfigurationRepository;
	
	@Autowired
	AccountsRepository accountsRepository;
	
	@Autowired
	AlertService alertService;
	
	@Autowired
	AlertTypeRepository alertTypeRepository;
	
	@Autowired
	AlertConditionTypeRepository alertConditionTypeRepository;

    @Value("${staah.url:https://revnomix.staah.net/common-cgi/Services.pl}")
    private String staahUrl;

    @Value("${ezee.url:https://live.ipms247.com/pmsinterface/reservation.php}")
    private String ezeeUrl;


    @RequestMapping(value = "/staah/inbound/reservation", method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public Response inboundReservationsTest(@RequestBody String reservationString) {
        logger.error("StaahInboundService inbound Reservations call : ");
        logger.error("Reservations  >>>>>>>>>>>>> : \n" + reservationString.toString());
        Response resp = new Response();
        Reservations reservations = (Reservations) xmlUtilService.convertFromXMLToObject(jaxb2Marshaller,StaahXmlUtil.removeAmpercent(StaahXmlUtil.removeRemarks(reservationString)));
        Clients clients = clientsRepository.findOneByCmHotel(reservations.getReservation().get(0).getHotelId());
        try{
	        String str = reservationString.substring(reservationString.indexOf("<hotel_name>"), reservationString.indexOf("</hotel_name>") + 13);
	        reservationString = reservationString.replace(str, "");
	        StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
	        staahPopulationDto.setReservations(reservations);
	        logger.error(xmlUtilService.convertFromObjectToXMLString(jaxb2Marshaller, reservations));
	        if (clients == null) {
	            resp.setMessage(reservationString);
            	resp.setStatus(Status.FAILED);
	            return resp;
	        }
	        staahPopulationDto.setClient(clients);
	        populateStaahGateway.populateStaahReservation(staahPopulationDto);
	        resp.setMessage(reservationString);
        	resp.setStatus(Status.SUCCESS);
        }catch(Exception ce) {
        	resp.setMessage(reservationString);
        	resp.setStatus(Status.FAILED);
        }
        return resp;
    }

    @RequestMapping(value = "/staah/outbound/roomMapping", method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public Response roomMapping(@RequestBody Roomrequest roomrequest) throws Exception {
        logger.error("Fetching roomMapping for client started : " + roomrequest.getHotelId());
        StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
        Clients clients = clientsRepository.findOneByCmHotel(roomrequest.getHotelId());
        String xml = "";
        ResponseEntity<String> result = null;
        Response resp = new Response();
        if (clients == null) {
            throw new RevseedException("Hotel/client not found with id :" + roomrequest.getHotelId());
        }
        staahPopulationDto.setClient(clients);
        Roomresponse response = null;
        HttpHeaders headers = new HttpHeaders();
     	headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
     	HttpEntity<?> requestEntity = new HttpEntity<>(roomrequest,headers);
     	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
        result = restTemplate.exchange(getStaahUrl(), HttpMethod.POST, requestEntity, String.class);
        xml = result.getBody();
        try {
        	if(xml!=null) {
		        response = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml.replaceAll("&", "&amp;"));
		        staahPopulationDto.setRoomresponse(response);
		        populateStaahGateway.populateStaahRoomMapping(staahPopulationDto);
		        resp.setMessage(xml);
            	resp.setStatus(Status.SUCCESS);
        	}
        }catch(Exception ce) {
        	ce.printStackTrace();
        	resp.setMessage(xml);
        	resp.setStatus(Status.FAILED);
        }
        logger.error("Fetching room mapping for client completed : " + roomrequest.getHotelId());
        return resp;
    }


    @RequestMapping(value = "/staah/outbound/viewInventory", method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public Response viewInventory(@RequestBody AvailRequestSegments availRequestSegments) throws Exception {
        logger.error("Fetching inventory for client started :" + availRequestSegments.getHotelId());
        StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
        String xml = "";
        ResponseEntity<String> result = null;
        Response resp = new Response();
        Clients clients = clientsRepository.findOneByCmHotel(availRequestSegments.getHotelId());
        if (clients == null) {
            throw new RevseedException("Hotel/client not found with id :" + availRequestSegments.getHotelId());
        }
        staahPopulationDto.setClient(clients);
        AvailRequestSegments response = null;
        HttpHeaders headers = new HttpHeaders();
     	headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
     	HttpEntity<?> requestEntity = new HttpEntity<>(availRequestSegments,headers);
     	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
        result = restTemplate.exchange(getStaahUrl(), HttpMethod.POST, requestEntity, String.class);
        xml = result.getBody();
        try {
        	if(xml!=null) {
	        	response = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml.replaceAll("&", "&amp;"));    
	            staahPopulationDto.setAvailRequestSegments(response);
	            populateStaahGateway.populateStahInventory(staahPopulationDto);
	            resp.setMessage(xml);
            	resp.setStatus(Status.SUCCESS);
        	}
        }catch(Exception ce) {
        	ce.printStackTrace();
        	resp.setMessage(xml);
        	resp.setStatus(Status.FAILED);
        }
    	return resp;
    }


    @RequestMapping(value = "/staah/outbound/viewRate", method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public Response viewRate(@RequestBody RateRequestSegments rateRequestSegments) throws Exception{
        logger.debug("Fetching viewRate for client started : " + rateRequestSegments.getHotelId());
        Response resp = new Response();
        Clients clients = clientsRepository.findOneByCmHotel(rateRequestSegments.getHotelId());
        String xml = "";
        ResponseEntity<String> result = null;
        if (clients == null) {
            throw new RevseedException("Hotel/client not found with id :" + rateRequestSegments.getHotelId());
        }
        StaahPopulationDto staahPopulationDto = new StaahPopulationDto();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            HttpEntity<RateRequestSegments> request = new HttpEntity<RateRequestSegments>(rateRequestSegments, headers);
           	RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
            result = restTemplate.exchange(getStaahUrl(), HttpMethod.POST, request, String.class);
            xml = result.getBody();
            //String xml = restClientConfig.post(getStaahUrl(), request,true);
            logger.debug("xml : \n" + xml);
            try {
            	if(xml!=null) {
		            RateRequestSegments response = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml.replaceAll("&", "&amp;"));
		            staahPopulationDto.setRateRequestSegmentsResponse(response);
		            staahPopulationDto.setClient(clients);
		            populateStaahGateway.populateStahRate(staahPopulationDto);
		            resp.setMessage(xml);
	            	resp.setStatus(Status.SUCCESS);
            	}
            }catch(Exception ce) {
            	ce.printStackTrace();
            	resp.setMessage(xml);
            	resp.setStatus(Status.FAILED);
            }
            logger.debug("Fetching viewRate for client completed : " + rateRequestSegments.getHotelId());
        	return resp;
    }

    @RequestMapping(value = "/staah/outbound/updateRateForTodaysRun", method = RequestMethod.PUT)
    @ResponseBody
    public Response updateAllRates(@RequestParam("clientId") Integer clientId) {
        return upateRate(clientId);
    }

    @RequestMapping(value = "/staah/outbound/pushAllOverrideRates", method = RequestMethod.PUT)
    @ResponseBody
    public Response updateRate(@RequestParam("clientId") Integer clientId, HttpServletRequest request) {
        Response response = createInitialResponse();
        Clients clients = clientsRepository.findById(clientId).get();

        try {
	        if (clients == null) {
	            response.setMessage("Hotel/client not found with id :" + clientId);
	        }
	        Date startDate = DateUtil.localToDate(DateUtil.dateToLocalDate(clients.getSystemToday()).minusDays(1));
	        List<OverrideDetailsDto> overrides = overrideRepository.getLatestOverrideValue(clientId, startDate);
	        overrides.stream().forEach(override -> {
	            String xml = "";
	            ResponseEntity<String> result = null;
	            Updaterequest updaterequest = createUpdaterequest(clients, override);
	            HttpHeaders headers = new HttpHeaders();
	            headers.setContentType(MediaType.APPLICATION_XML);
	            HttpEntity<Updaterequest> updateRequest = new HttpEntity<Updaterequest>(updaterequest, headers);
	           	RestTemplate restTemplate = new RestTemplate();
	            restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
	            result = restTemplate.exchange(getStaahUrl(), HttpMethod.POST, updateRequest, String.class);
	            xml = result.getBody();
	            try {
	            	if(xml!=null) {
	            		Updateresponse updateresponse =  xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml.replaceAll("&", "&amp;"));
	            		response.setMessage(xml);
	            		response.setStatus(Status.SUCCESS);
	            		alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
	            	}
	            }catch(Exception ce) {
	            	ce.printStackTrace();
	                response.setStatus(Status.FAILED);
	                response.setMessage(xml);
	                alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,xml);
	            }
	            });
        }catch(Exception ce) {
        	ce.printStackTrace();
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,ce.getMessage()+" "+ce.getCause());
        }
        return response;
    }

    private Updaterequest createUpdaterequest(Clients clients, OverrideDetailsDto override) {
        Updaterequest updaterequest = new Updaterequest();
        updaterequest.setHotelId(clients.getCmHotel());
        updaterequest.setUsername(clients.getCmUsername());
        updaterequest.setPassword(clients.getCmPassword());
        updaterequest.setVersion(1.0f);
        UpdateRoom updateRoom = new UpdateRoom();
        updateRoom.setRoomId(clients.getCmMasterRoom().intValue());
        UpdateRate updateRate = new UpdateRate();
        updateRate.setRateId(clients.getCmMasterRate().intValue());
        updateRate.setStartDate(DateUtil.formatDate(override.getCheckin_date(), "yyyy-MM-dd"));
        updateRate.setEndDate(DateUtil.formatDate(override.getCheckin_date(), "yyyy-MM-dd"));
        updateRate.setPrice(Double.valueOf(override.getRate()));
        updateRoom.setRate(updateRate);
        updaterequest.getRoom().add(updateRoom);
        return updaterequest;
    }

    private Response createInitialResponse() {
        Response response = new Response();
        response.setStatus(com.revnomix.revseed.model.Status.COMPLETE);
        response.setMessage("Rate update has been process successfully");
        return response;
    }

    @RequestMapping(value = "/staah/outbound/updateRate", method = RequestMethod.PUT)
    @ResponseBody
    public Response upateRate(@RequestParam("clientId") Integer clientId) {
        Response response = createInitialResponse();
        Clients clients = clientsRepository.findById(clientId).get();
        try {
	        if (clients == null) {
	            response.setMessage("Hotel/client not found with id :" + clientId);
	            response.setStatus(com.revnomix.revseed.model.Status.FAILED);
	        }
	        List<OverrideDetailsDto> latestFinalRates = finalRecommendationsRepository.getLatestFinalRateByClientId(clients.getId());
	        latestFinalRates.stream().forEach(row -> {
		            String xml = "";
		            ResponseEntity<String> result = null;
                    Updaterequest updaterequest = new Updaterequest();
                    updaterequest.setHotelId(clients.getCmHotel());
                    updaterequest.setUsername(clients.getCmUsername());
                    updaterequest.setPassword(clients.getCmPassword());
                    updaterequest.setVersion(1.0f);
                    UpdateRoom updateRoom = new UpdateRoom();
                    if (clients.getCmMasterRoom() != null) {
                        updateRoom.setRoomId(clients.getCmMasterRoom().intValue());
                    }
                    UpdateRate updateRate = new UpdateRate();
                    if (clients.getCmMasterRoom() != null) {
                        updateRate.setRateId(clients.getCmMasterRate().intValue());
                    }
                    updateRate.setStartDate(DateUtil.formatDate(row.getCheckin_date(), "yyyy-MM-dd"));
                    updateRate.setEndDate(DateUtil.formatDate(row.getCheckin_date(), "yyyy-MM-dd"));
                    updateRate.setPrice((double) row.getRate());
                    updateRoom.setRate(updateRate);
                    updaterequest.getRoom().add(updateRoom);
                    HttpHeaders headers = new HttpHeaders();
                 	headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
                 	HttpEntity<Updaterequest> requestEntity = new HttpEntity<>(updaterequest,headers);
                 	RestTemplate restTemplate = new RestTemplate();
                    restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
                    result = restTemplate.exchange(getStaahUrl(), HttpMethod.POST, requestEntity, String.class);
                    xml = result.getBody();
                    try {
                    	if(xml!=null) {
	                    Updateresponse updateresponse = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml.replaceAll("&", "&amp;"));
	                    response.setMessage(xml);
	            		response.setStatus(Status.SUCCESS);
	            		alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);

                    	}
                    }catch(Exception ce) {
    	            	ce.printStackTrace();
    	                response.setStatus(Status.FAILED);
    	                response.setMessage(xml);
    	                alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,xml);
                    }
                }
	        );
	        alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
        }catch(Exception ce) {
        	ce.printStackTrace();
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,ce.getMessage()+" "+ce.getCause());
        }
        return response;
    }

    public String getStaahUrl() {
        return staahUrl;
    }

    public String getEzeeUrl() {
        return ezeeUrl;
    }

    public void setStaahUrl(String staahUrl) {
        this.staahUrl = staahUrl;
    }
}
