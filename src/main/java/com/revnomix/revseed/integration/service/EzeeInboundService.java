package com.revnomix.revseed.integration.service;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
import com.revnomix.revseed.model.StaahRateTypes;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.AccountsRepository;
import com.revnomix.revseed.repository.AlertConfigurationRepository;
import com.revnomix.revseed.repository.AlertTypeRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.FinalRecommendationsRepository;
import com.revnomix.revseed.repository.OverrideRepository;
import com.revnomix.revseed.repository.StaahRateTypesRepository;
import com.revnomix.revseed.schema.ezee.RESRequest;
import com.revnomix.revseed.schema.ezee.RESResponse;
import com.revnomix.revseed.schema.ezee.RESResponse.Reservations;
import com.revnomix.revseed.schema.ezee.RESResponse.Reservations.CancelReservation;
import com.revnomix.revseed.schema.ezee.RESResponse.Reservations.Reservation;
import com.revnomix.revseed.schema.ezee.RESResponse.Reservations.Reservation.BookByInfo;
import com.revnomix.revseed.schema.ezee.RESResponse.Reservations.Reservation.BookByInfo.BookingTran;
import com.revnomix.revseed.wrapper.OverrideDetailsDto;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.StaahXmlUtil;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.Util.service.XMLUtilService;
import com.revnomix.revseed.integration.exception.RevseedException;
import com.revnomix.revseed.integration.ezee.EzeePopulationDto;
import com.revnomix.revseed.integration.ezee.gateway.PopulateEseeGateway;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.staah.transformer.PopulateGenericGateway;
import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;

@Controller
@RequestMapping("/ezee")
@CrossOrigin(origins = "*")
public class EzeeInboundService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PopulateEseeGateway populateEseeGateway;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private Jaxb2Marshaller jaxb2Marshaller;

    @Autowired
    private SystemUtil systemUtil;

    @Autowired
    private XMLUtilService xmlUtilService;

    @Autowired
    private FinalRecommendationsRepository finalRecommendationsRepository;
    
	@Autowired
	EzeeMultiProcessingService ezeeMultiProcessingService;

    @Autowired
    private OverrideRepository overrideRepository;

    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;
    
	@Autowired
	private AlertConfigurationRepository alertConfigurationRepository;
	
	@Autowired
	private AccountsRepository accountsRepository;
	
	@Autowired
	private AlertService alertService;
	
	@Autowired
	private AlertTypeRepository alertTypeRepository;	
	
    @Autowired
    private PopulateGenericGateway populateGenericGateway;

    @Value("${ezee.url:https://live.ipms247.com/pmsinterface/reservation.php}")
    private String endPointUrl;

    @Value("${ezee.url:https://live.ipms247.com/pmsinterface/getdataAPI.php}")
    private String endPointUrlForGetData;


    @RequestMapping(value = "/outbound/roominfo", method = RequestMethod.POST)
    @ResponseBody
    public Response getRoomInfo(@RequestBody RESRequest request) throws Exception{
        logger.info("Fetching Room Mapping for client started : " + request.getAuthentication().getHotelCode());
        RESResponse response = new RESResponse();
        String xml = "";
        Response resp = new Response();
        ResponseEntity<String> result = null;
        Clients clients = clientsRepository.findOneByCmHotel(request.getAuthentication().getHotelCode());
        EzeePopulationDto ezeePopulationDto = new EzeePopulationDto();
        ezeePopulationDto.setClients(clients);
        HttpHeaders headers = new HttpHeaders();
     	headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
     	HttpEntity<?> requestEntity = new HttpEntity<>(request,headers);
     	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
        result = restTemplate.exchange(getEndPointUrl(), HttpMethod.POST, requestEntity, String.class);
        xml = result.getBody();
        try {
        	if(xml!=null) {
		        response = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml);
		        ezeePopulationDto.setResResponse(response);
		        populateEseeGateway.populateEzeeRoomInfo(ezeePopulationDto);
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

    @RequestMapping(value = "/outbound/viewInventory", method = RequestMethod.POST)
    @ResponseBody
    public Response viewInventory(@RequestBody RESRequest request) throws Exception {
        logger.info("Fetching Inventory for client started : " + request.getAuthentication().getHotelCode());
        String xml = "";
        Response resp = new Response();
        ResponseEntity<String> result = null;
        Clients clients = clientsRepository.findOneByCmHotel(request.getAuthentication().getHotelCode());
        RESResponse response = new RESResponse();
        EzeePopulationDto ezeePopulationDto = new EzeePopulationDto();
        ezeePopulationDto.setClients(clients);
        HttpHeaders headers = new HttpHeaders();
     	headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
     	HttpEntity<?> requestEntity = new HttpEntity<>(request,headers);
     	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
        result = restTemplate.exchange(getEndPointUrlForGetData(), HttpMethod.POST, requestEntity, String.class);
        xml = result.getBody();
        try {
        	if(xml!=null) {
		        xml = xml.replace("\t<?xml", "<?xml");
		        response = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml);
		        ezeePopulationDto.setResResponse(response);
		        populateEseeGateway.populateEzeeInventory(ezeePopulationDto);
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

//    @RequestMapping(value = "/outbound/updateInventory", method = RequestMethod.POST)
//    @ResponseBody
//    public Response updateInventory(@RequestBody RESRequest request) {
//        logger.info("Update Inventory for client started : " + request.getAuthentication().getHotelCode());
//        Response resp = new Response();
//        RESResponse response = new RESResponse();
//        RESResponse.Errors error = new RESResponse.Errors();
//        RESResponse.Success success = new RESResponse.Success();
//        EzeePopulationDto ezeePopulationDto = new EzeePopulationDto();
//        try {
//            if (clientsRepository.findByCmHotel(request.getAuthentication().getHotelCode()) != null) {
//                ezeePopulationDto.setClients(clientsRepository.findByCmHotel(request.getAuthentication().getHotelCode()));
//            } else {
//                throw new RevseedException("Client is not available for hotel code :" + request.getAuthentication().getHotelCode());
//            }
//            ezeePopulationDto.setResRequest(request);
//            populateEseeGateway.populateEzeeInventoryUpdate(ezeePopulationDto);
//            error.setErrorCode((short) 0);
//            error.setErrorMessage("Success");
//            success.setSuccessMsg("Room Inventory Successfully Updated");
//            response.setErrors(error);
//            response.setSuccess(success);
//        } catch (Exception e) {
//	        resp.setMessage(xml);
//        	resp.setStatus(Status.SUCCESS);
//        }
//        return response;
//    }

//    @RequestMapping(value = "/outbound/updateRate", method = RequestMethod.POST)
//    @ResponseBody
//    public Response updateRate(@RequestBody RESRequest request) {
//        logger.info("Update Rate for client started : " + request.getAuthentication().getHotelCode());
//        Clients client = clientsRepository.findByCmHotel(request.getAuthentication().getHotelCode());
//        Response resp = new Response();
//        RESResponse response = new RESResponse();
//        RESResponse.Errors error = new RESResponse.Errors();
//        RESResponse.Success success = new RESResponse.Success();
//        EzeePopulationDto ezeePopulationDto = new EzeePopulationDto();
//        logger.error("Rate Update from Channel Manager started for clientId : "+request.getAuthentication().getHotelCode());
//        try {
//            if (client != null) {
//                ezeePopulationDto.setClients(client);
//            } else {
//                throw new RevseedException("Client is not available for hotel code :" + request.getAuthentication().getHotelCode());
//            }
//            ezeePopulationDto.setResRequest(request);
//            populateEseeGateway.populateEzeeRateUpdate(ezeePopulationDto);
//            resp.setMessage(xml);
//        	resp.setStatus(Status.SUCCESS);
//            alertService.createAlertsforClientRefreshData(client, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
//        } catch (Exception e) {
//        	alertService.createFailureAlertsforClient(client, ConstantUtil.RATEPUSHFAIL);
//            error.setErrorCode((short) 104);
//            error.setErrorMessage("Rate type is missing");
//            response.setErrors(error);
//            logger.error("Failed while parsing response for update rate for hotel {} {} ", request.getRequestType(), e);
//        }
//
//        return response;
//    }

    @RequestMapping(value = "/inbound/updateRate", method = RequestMethod.POST)
    @ResponseBody
    public Response updateRoomRate(@RequestParam("clientId") Integer clientId) {
        logger.info("Inbound Update Rate for client started : " + clientId);
        Response resp = new Response();
        Clients clients = clientsRepository.findById(clientId).get();
        RESRequest request = getDefaultRESRequest(clients, EzeeRequestType.UPDATE_ROOM_RATE);
        List<OverrideDetailsDto> latestFinalRates = finalRecommendationsRepository.getLatestFinalRateByClientId(clientId);
        StaahRateTypes rateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(clientId, clients.getCmMasterRate(), clients.getCmMasterRoom());
        try {
            latestFinalRates.forEach(row -> {
            	String xml = "";
                ResponseEntity<String> result = null;
                RESRequest.RateType type = new RESRequest.RateType();
                RESRequest.RateType.RoomRate rate = new RESRequest.RateType.RoomRate();
                if (clients.getCmMasterRoom() != null) {
                    type.setRoomTypeID(clients.getCmMasterRoom());
                }
                if (rateTypes != null) {
                    type.setRateTypeID(rateTypes.getRateId());
                }else{
                    throw new RevseedException("Rate Id is not available for client :"+clientId);
                }
                type.setFromDate(DateUtil.dateToXMLGregorianCalendarByFormatter(row.getCheckin_date()));
                type.setToDate(DateUtil.dateToXMLGregorianCalendarByFormatter(row.getCheckin_date()));
                rate.setBase((int) row.getRate());
                type.setRoomRate(rate);
                request.setRateType(type);
                HttpHeaders headers = new HttpHeaders();
             	headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
             	HttpEntity<?> requestEntity = new HttpEntity<>(request,headers);
             	RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
                result = restTemplate.exchange(getEndPointUrl(), HttpMethod.POST, requestEntity, String.class);
                xml = result.getBody();
                try {
                	if(xml!=null) {
		                RESResponse response1 = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml);
				        resp.setMessage(xml);
			        	resp.setStatus(Status.SUCCESS);
                	}
                }catch(Exception ce) {
                	ce.printStackTrace();
                    resp.setMessage(xml);
                	resp.setStatus(Status.FAILED);
                }
            });
            alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
        } catch (Exception e) {
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,e.getMessage()+" "+e.getCause());
            resp.setMessage(e.getCause());
        	resp.setStatus(Status.FAILED);
        }
        return resp;
    }

    @RequestMapping(value = "/outbound/viewBookings", method = RequestMethod.POST)
    @ResponseBody
    public Response viewBookings(@RequestBody RESRequest request) throws Exception{
    	String xml = "";
    	Response resp = new Response();
        ResponseEntity<String> result = null;
        logger.info("Fetching Bookings for client started : " + request.getAuthentication().getHotelCode());
        RESResponse response = new RESResponse();
        EzeePopulationDto ezeePopulationDto = new EzeePopulationDto();
        Clients clients = clientsRepository.findOneByCmHotel(request.getAuthentication().getHotelCode());
        ezeePopulationDto.setClients(clients);
        HttpHeaders headers = new HttpHeaders();
     	headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
     	HttpEntity<?> requestEntity = new HttpEntity<>(request,headers);
     	RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
        result = restTemplate.exchange(getEndPointUrlForGetData(), HttpMethod.POST, requestEntity, String.class);
        xml = result.getBody();
        logger.error("xml : \n" + xml);
        if(xml!=null) {
        	try {
		        response = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml);
		        Reservations res = response.getReservations();
		        List<CancelReservation> clr= res.getCancelReservation();
		        if(clr.size()>0) {
		        	System.out.print(clr.get(0));
		        }
				  List<Reservation> lr= new ArrayList<Reservation>(); 
				  res.getReservation().stream().forEach(r ->{
					  BookByInfo bbi = r.getBookByInfo(); 
					  BookingTran bt = bbi.getBookingTran().get(0); 
					  bbi.getBookingTran().stream().forEach(btd ->{
						  if(btd.getStatus().equalsIgnoreCase("Cancel")) {
							  System.out.println("Status : "+btd.getStatus());
						  }else {
							  System.out.println("Status : "+btd.getStatus());
						  }
					  });
					  if(bt.getIsConfirmed()==1) { 
						  Reservation reserv = new Reservation (); 
						  reserv.setBookByInfo(bbi); 
						  lr.add(reserv); 
					  }else {
							System.out.println(""+bt.toString());
					  }
				  });
				 res.getReservation().clear();
				 res.getReservation().addAll(lr);
				 if(clr!=null) {
					 res.getCancelReservation().addAll(clr);
				 }
		        response.setReservations(res);
		        ezeePopulationDto.setResResponse(response);
		        GenericIntegrationDto genericIntegrationDto = new GenericIntegrationDto();
				genericIntegrationDto.setResResponse(response);
				genericIntegrationDto.setClient(clients);
				genericIntegrationDto.setIntegerationType(IntegrationType.EZEE.toString());
				populateGenericGateway.populateGenericBooking(genericIntegrationDto);
		       // populateEseeGateway.populateEzeeBookings(ezeePopulationDto);
		        resp.setMessage(xml);
	        	resp.setStatus(Status.SUCCESS);
        	}catch(Exception ce) {
        		ce.printStackTrace();
                resp.setMessage(xml);
            	resp.setStatus(Status.FAILED);
        	}
        }
     return resp;
    }

    @RequestMapping(value = "/inbound/bookings", method = RequestMethod.POST, consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
    @ResponseBody
    public RESResponse inboundBookings(@RequestBody String reservationString) throws Exception {
        logger.info("Inbound Bookings for client started");
        Response resp = new Response();
        RESResponse reservations = (RESResponse) xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, StaahXmlUtil.removeComment(reservationString));
        RESResponse response = new RESResponse();
        RESResponse.Errors error = new RESResponse.Errors();
        RESResponse.Success success = new RESResponse.Success();
        Clients clients = null;
        if (reservations.getReservations().getReservation() == null || (reservations.getReservations().getReservation().size() == 0 && reservations.getReservations().getCancelReservation().size() == 0)) {
            error.setErrorCode((short) 0);
            error.setErrorMessage("Success");
            success.setSuccessMsg("Booking Successfully Updated");
            response.setErrors(error);
            response.setSuccess(success);
        } else {
    		response = reservations;
        	if(reservations.getReservations().getReservation().size()>0) {
        		clients = clientsRepository.findByCmHotel(reservations.getReservations().getReservation().get(0).getBookByInfo().getLocationId());
        	}else {
        		clients = clientsRepository.findByCmHotel(reservations.getReservations().getCancelReservation().get(0).getLocationId());
        	}
            EzeePopulationDto ezeePopulationDto = new EzeePopulationDto();
            logger.error(xmlUtilService.convertFromObjectToXMLString(jaxb2Marshaller, reservations));

            if (clients != null) {
                ezeePopulationDto.setClients(clients);
            } else {
                throw new RevseedException("Client is not available for hotel code :" + reservations.getReservations().getReservation().get(0).getBookByInfo().getLocationId());
            }
	                Reservations res = response.getReservations();
	                List<CancelReservation> clr= res.getCancelReservation();
	                if(clr.size()>0) {
	                	System.out.print(clr.get(0));
	                }
	  			  	List<Reservation> lr= new ArrayList<Reservation>(); 
	  			  	res.getReservation().stream().forEach(r ->{
	  				  BookByInfo bbi = r.getBookByInfo(); 
					  BookingTran bt = bbi.getBookingTran().get(0); 
					  bbi.getBookingTran().stream().forEach(btd ->{
						  if(btd.getStatus().equalsIgnoreCase("Cancel")) {
							  System.out.println("Status : "+btd.getStatus());
						  }else {
							  System.out.println("Status : "+btd.getStatus());
						  }
					  });
					  if(bt.getIsConfirmed()==1) { 
						  Reservation reserv = new Reservation (); 
						  reserv.setBookByInfo(bbi); 
						  lr.add(reserv); 
					  }else {
							System.out.println(""+bt.toString());
					  }
	  				});
	  			  	res.getReservation().clear();
	  			  	res.getReservation().addAll(lr);
	  			  	if(clr!=null) {
	 				 res.getCancelReservation().addAll(clr);
	  			  	}
		  			
	  			  	response.setReservations(res);
	  			  	ezeePopulationDto.setResResponse(response);
	                populateEseeGateway.populateEzeeBookings(ezeePopulationDto);
		  			  
	                error.setErrorCode((short) 0);
	                error.setErrorMessage("Success");
	                success.setSuccessMsg("Booking Successfully Updated");
	                response.setErrors(error);
	                response.setSuccess(success);
        }
        return response;
    }

    @RequestMapping(value = "/outbound/viewRate", method = RequestMethod.POST)
    @ResponseBody
    public Response viewRate(@RequestBody RESRequest request) {
        logger.info("Fetching Rate for client started : " + request.getAuthentication().getHotelCode());
    	String xml = "";
    	Response resp = new Response();
        ResponseEntity<String> result = null;
        RESResponse response = new RESResponse();
        EzeePopulationDto ezeePopulationDto = new EzeePopulationDto();
        Clients clients = clientsRepository.findOneByCmHotel(request.getAuthentication().getHotelCode());
        ezeePopulationDto.setClients(clients);
        try {
            HttpHeaders headers = new HttpHeaders();
         	headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
         	HttpEntity<?> requestEntity = new HttpEntity<>(request,headers);
         	RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
            result = restTemplate.exchange(getEndPointUrlForGetData(), HttpMethod.POST, requestEntity, String.class);
            xml = result.getBody();
	            if(xml!=null) {
		            xml = xml.replace("\t<?xml", "<?xml");
		            logger.error("xml : \n" + xml);
		            response = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml);
		            if(response.getRoomInfo()!=null) {
			            ezeePopulationDto.setResResponse(response);
			            populateEseeGateway.populateEzeeRate(ezeePopulationDto);
			            resp.setMessage(xml);
			        	resp.setStatus(Status.SUCCESS);
		            }else {
		            	resp.setMessage(xml);
			        	resp.setStatus(Status.FAILED);
		            }
		            alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEINVFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
	            }
            } catch (Exception e) {
            	e.printStackTrace();
	        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,e.getMessage()+" "+e.getCause());
		        resp.setMessage(xml);
	        	resp.setStatus(Status.FAILED);
        }
        return resp;
    }

    @RequestMapping(value = "/outbound/pushAllOverrideRates", method = RequestMethod.PUT)
    @ResponseBody
    public Response updateRate(@RequestParam("clientId") Integer clientId) {
        RESResponse response = new RESResponse();
        Response resp = new Response();
        String xmlResult = "";
        RESResponse.Errors error = new RESResponse.Errors();
        RESResponse.Success success = new RESResponse.Success();
        Clients clients = clientsRepository.findById(clientId).get();
        RESRequest request = getDefaultRESRequest(clients, EzeeRequestType.UPDATE_ROOM_RATE);
        Date startDate = DateUtil.localToDate(DateUtil.dateToLocalDate(clients.getSystemToday()).minusDays(1));
        List<OverrideDetailsDto> overrides = overrideRepository.getLatestOverrideValue(clientId, startDate);
        StaahRateTypes rateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(clientId, clients.getCmMasterRate(), clients.getCmMasterRoom());
        try {
            for(OverrideDetailsDto override: overrides){
            	String xml = "";
                ResponseEntity<String> result = null;
                RESRequest.RateType type = new RESRequest.RateType();
                RESRequest.RateType.RoomRate rate = new RESRequest.RateType.RoomRate();
                if (clients.getCmMasterRoom() != null) {
                    type.setRoomTypeID(clients.getCmMasterRoom());
                }
                if (rateTypes != null) {
                    type.setRateTypeID(rateTypes.getRateId());
                }else{
                    throw new RevseedException("Rate Id is not available for client :"+clientId);
                }
                type.setFromDate(DateUtil.dateToXMLGregorianCalendarByFormatter(override.getCheckin_date()));
                type.setToDate(DateUtil.dateToXMLGregorianCalendarByFormatter(override.getCheckin_date()));
                rate.setBase((int) override.getRate());
                type.setRoomRate(rate);
                request.setRateType(type);
                HttpHeaders headers = new HttpHeaders();
             	headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
             	HttpEntity<?> requestEntity = new HttpEntity<>(request,headers);
             	RestTemplate restTemplate = new RestTemplate();
                restTemplate.setMessageConverters(systemUtil.setXMLMessageConvertor());
                result = restTemplate.exchange(getEndPointUrl(), HttpMethod.POST, requestEntity, String.class);
                xml = result.getBody();
                xmlResult = xmlResult + xml;
                if(xml!=null) {
	                RESResponse response1 = xmlUtilService.convertFromXMLToObject(jaxb2Marshaller, xml);
	                resp.setMessage(xml);
		        	resp.setStatus(Status.SUCCESS);
                }
            };
            alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEPUSHFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
        } catch (Exception e) {
        	e.printStackTrace();
	        resp.setMessage(xmlResult);
        	resp.setStatus(Status.FAILED);
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEPUSHFAIL,e.getMessage()+" "+e.getCause());
        }
        return resp;
    }

    private RESRequest getDefaultRESRequest(Clients clients, String type) {
        RESRequest request = new RESRequest();
        RESRequest.Authentication authentication = new RESRequest.Authentication();
        request.setRequestType(type);
        authentication.setAuthCode(clients.getCmPassword());
        authentication.setHotelCode(clients.getCmHotel());
        request.setAuthentication(authentication);
        return request;
    }

    public String getEndPointUrl() {
        return endPointUrl;
    }

    public String getEndPointUrlForGetData() {
        return endPointUrlForGetData;
    }
    
	@RequestMapping(value = "/inbound/pushbooking", method = RequestMethod.POST)
	public @ResponseBody Object pushBooking(@RequestBody RESResponse ezeeBooking) throws Exception {
		Object response  =  ezeeMultiProcessingService.savePushBooking(ezeeBooking);
		return response;
	}

}
