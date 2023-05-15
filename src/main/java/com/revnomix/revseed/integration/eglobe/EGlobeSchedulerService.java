package com.revnomix.revseed.integration.eglobe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.list.TreeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.revnomix.revseed.model.ApplicationParameters;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.JobType;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.ProcessingStatus;
import com.revnomix.revseed.model.ScheduledJob;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.ApplicationParametersRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.ScheduledJobRepository;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.integration.ezee.RequestDto;
import com.revnomix.revseed.integration.service.ClientOnDemandService;
import com.revnomix.revseed.integration.service.OnDemandStatusService;
import com.revnomix.revseed.integration.service.Response;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingRequest;
import com.revnomix.revseed.integration.staahMax.dto.InventoryRequest;
import com.revnomix.revseed.integration.staahMax.dto.RoomRateRequest;


@Controller
@RequestMapping("/eglobe")
@CrossOrigin(origins = "*")
public class EGlobeSchedulerService {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());   

	@Autowired
	EGlobeInboundService eGlobeInboundService;
	
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
    private ClientOnDemandService clientOnDemandService;

    @Autowired
    private OnDemandStatusService onDemandStatusService;
    
	@Autowired
	private GenericSchedulerService genericSchedulerService;
    
    @RequestMapping(value = "/inbound/runRefreshOtaData", method = RequestMethod.POST)
    @ResponseBody
    public boolean refreshData(@RequestParam("clientId") Integer clientId) {
        Clients clients = clientsRepository.findById(clientId).get();
        List<Clients> clientsList = new ArrayList<Clients>();
        clientsList.add(clients);
        List<String> refreshCodeList = new ArrayList<String>();
        refreshCodeList.add(ConstantUtil.FETCH_ROOM_MAPPING);
        refreshCodeList.add(ConstantUtil.FETCH_INVENTORY);
        if (clients != null) {
            logger.error("Started Refresh Data");
           // createScheduledJob(clients,refreshCodeList);
        	genericSchedulerService.createMultipleScheduledJob(clientsList, refreshCodeList,false);
            return true;
        } else {
            return false;
        }
    }
    
    @RequestMapping(value = "/inbound/runRefreshBookings", method = RequestMethod.POST)
    @ResponseBody
    public boolean runRefreshBookings(@RequestParam("clientId") Integer clientId) {
        Clients clients = clientsRepository.findById(clientId).get();
        if (clients != null) {
        	List<Clients> clientsList = new ArrayList<Clients>();
            clientsList.add(clients);
            List<String> refreshCodeList = new ArrayList<String>();
            refreshCodeList.add(ConstantUtil.FETCH_BOOKING_DATA);
        	genericSchedulerService.createMultipleScheduledJob(clientsList, refreshCodeList,false);
            return true;
        } else {
            return false;
        }
    }
    
    @RequestMapping(value = "/inbound/importBookings", method = RequestMethod.POST)
    @ResponseBody
    public Boolean importBookings(@RequestBody RequestDto requestDto) {
    	Clients clients = clientsRepository.findById(requestDto.getClientId()).get();
    	Boolean response = false;
    	try {
	        if (clients != null) {
	        	refreshBookingsImports(clients,requestDto);
	            response = true;
	        } else {
	        	response = false;
	        }
    	} catch (Exception e) {
    		alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,e.getMessage()+""+e.getCause());
        }
		return response;
    }
    
    private boolean refreshBookingsImports(Clients clients, RequestDto requestDto) {
        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.FETCH_BOOKING_DATA));
        String isMonitoringData = "";
        Parameter paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA).orElse(null);
        if(paramOpt!=null) {
        	isMonitoringData = paramOpt.getParamValue();
        }
        String totalResponse ="";
        Date fromDate = DateUtil.toDate(requestDto.getFromDate(), ConstantUtil.YYYYMMDD);
        Date toDate = DateUtil.toDate(requestDto.getToDate(), ConstantUtil.YYYYMMDD);
        Long dateRange = DateUtil.daysBetween(fromDate, toDate);
        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
        Response result = new Response();
        try {
	        for(int i = 0; i < dateRange; i+=20){
	     	   Date toDate20 = DateUtil.addDays(fromDate,20);
	            result = processBooking(clients, runningJob,fromDate,toDate20);
	            if(result.getStatus().equals(Status.FAILED)) {
             	   totalResponse = totalResponse +""+result.getMessage().toString();
                }else {
		            if(isMonitoringData.equals(ConstantUtil.YES)) {
		            	runningJob.setResponse(result.getMessage().toString());
		            }else {
		            	runningJob.setResponse("");
		            }
		            runningJob.setEndDateTime(new Date());
                }
	            fromDate = toDate20;
	        }
	        if(!totalResponse.equals("")) {
         	   changeStatus(runningJob, Status.FAILED);
	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,totalResponse);
            }else {
         	   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.BOOKFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
         	   changeStatus(runningJob, Status.COMPLETE);
            }
            logger.error("Ended Fetch Booking Data"+clients.getPropertyName());
            return true;
        }catch(Exception ce) {
        	changeStatus(runningJob, Status.FAILED);
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage()+" "+ce.getCause());
        	return false;
        }
    }
    
    private boolean refreshBookings(Clients clients) {
        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.FETCH_BOOKING_DATA));
        String isMonitoringData = "";
        String totalResponse ="";
        Parameter paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA).orElse(null);
        if(paramOpt!=null) {
        	isMonitoringData = paramOpt.getParamValue();
        }
        Integer dateRange = 364;
        Parameter paramOptDate =parameterRepository.findByClientIdAndParamName(clients.getId(), "date_range").orElse(null);
        if(paramOptDate!=null) {
        	dateRange = Integer.parseInt(paramOptDate.getParamValue());
        }	
        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
        Date fromDate = clients.getSystemToday();
        Response result = new Response();
        try {
	        for(int i = 0; i < dateRange; i+=20){
	     	   Date toDate = DateUtil.addDays(fromDate,20);
		       result = processBooking(clients, runningJob,fromDate,toDate);
			     	   if(result.getStatus().equals(Status.FAILED)) {
		            	   totalResponse = totalResponse +""+result.getMessage().toString();
		               }else {
			            if(isMonitoringData.equals(ConstantUtil.YES)) {
			            	runningJob.setResponse(result.getMessage().toString());
			            }else {
			            	runningJob.setResponse("");
			            }
			            runningJob.setEndDateTime(new Date());
		               }
	            fromDate = toDate;
	        }
	        if(!totalResponse.equals("")) {
         	   changeStatus(runningJob, Status.FAILED);
	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,totalResponse);
            }else {
         	   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.BOOKFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
         	   changeStatus(runningJob, Status.COMPLETE);
            }
            changeStatus(runningJob, Status.COMPLETE);
            logger.error("Ended Fetch Booking Data"+clients.getPropertyName());
            return true;
        }catch(Exception ce) {
        	changeStatus(runningJob, Status.FAILED);
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage()+" "+ce.getCause());
        	return false;
        }
    }
    
    @RequestMapping(value = "/inbound/runRecommendation", method = RequestMethod.POST)
    @ResponseBody
    private boolean runRecommendation(@RequestParam("clientId") Integer clientId) {
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
        clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.IN_PROGRESS);
        onDemandStatusService.changeStatus(clientId,"Run Recommendation");
        try {
        	HttpHeaders headers = new HttpHeaders();
         	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
         	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
         	RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            result = restTemplate.exchange(paramvalue+clientId.toString(), HttpMethod.POST, requestEntity, String.class);
            //result = restClientConfig.post(paramvalue+clientId.toString(), null,false);
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
            clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.COMPLETED);
            alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RECFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
            return true;
        } else {
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RECFAIL,response);
            changeStatus(runningJob, Status.FAILED);
            clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.FAILED);
            return false;
        }
    }

    @RequestMapping(value = "/inbound/runCalibration", method = RequestMethod.POST)
    @ResponseBody
    private boolean runCalibration(@RequestParam("clientId") Integer clientId) {
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
        clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.IN_PROGRESS);
        onDemandStatusService.changeStatus(clientId,"Run Calibration");
        try {
       	    HttpHeaders headers = new HttpHeaders();
        	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        	RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
            result = restTemplate.exchange(paramvalue+clientId.toString(), HttpMethod.POST, requestEntity, String.class);
            //result = restClientConfig.post(paramvalue+clientId.toString(), null,false);
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
            clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.COMPLETED);
            alertService.createAlertsforClientRefreshData(clients, ConstantUtil.CALFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
            return true;
        } else {
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.CALFAIL,response);
            changeStatus(runningJob, Status.FAILED);
            clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.FAILED);
            return false;
        }
    }
    

    public void createScheduledJob(Clients clients, List<String> scheduledJobTaskList) {
   	Optional<Parameter> monitoringParam =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA);        
   	List<ScheduledJob> list = createScheduledJobs(clients,scheduledJobTaskList);  	
		list.stream().forEach(job -> {
			String isMonitoringData = "";
			if(monitoringParam!=null && monitoringParam.isPresent()) {
	        	Parameter params = monitoringParam.get();
	        	isMonitoringData = params.getParamValue();
	        }
           ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
           Integer dateRange = 364;
           Optional<Parameter> paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), "date_range");
           if(paramOpt!=null && paramOpt.isPresent()) {
           	Parameter param = paramOpt.get();
           	dateRange = Integer.parseInt(param.getParamValue());
           }	
           try {
               if (runningJob.getJobType().equals(JobType.FETCH_ROOM_MAPPING)) {
            	   try {
	                   logger.error("Started FETCH_ROOM_MAPPING Data");
	                   Response roomResponse = eGlobeInboundService.roomMapping(getRoomRequest(clients));
	                   if(roomResponse.getStatus().equals(Status.FAILED)) {
	                	   runningJob.setResponse(roomResponse.getMessage().toString());
	                	   runningJob.setEndDateTime(new Date());
		                   changeStatus(runningJob, Status.FAILED);
		                   alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,roomResponse.getMessage().toString());
	                   }else {
		                   if(isMonitoringData.equals(ConstantUtil.YES)) {
		                   	runningJob.setResponse(roomResponse.getMessage().toString());
		                   }else {
		                   	runningJob.setResponse("");
		                   }
		                   runningJob.setEndDateTime(new Date());
		                   changeStatus(runningJob, Status.COMPLETE);
		                   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.ROOMINVFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
	                   }
	                   logger.error("Ended FETCH_ROOM_MAPPING Data");
            	   }catch(Exception ce) {
            		   logger.error(ce.getMessage());
            		   ce.printStackTrace();
            		   runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
            		   logger.error("EGLOBE - ROOMMAPPING");
            		   changeStatus(runningJob, Status.FAILED);
       	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,ce.getMessage()+" "+ce.getCause());
            	   }
               }
               if (runningJob.getJobType().equals(JobType.FETCH_INVENTORY)) {
            	   try {
	                   logger.error("Started FETCH_RATE_INVENTORY Data");
	                   String totalResponse ="";
	                   Date fromDate = clients.getSystemToday();
	                   for(int i = 0; i < dateRange; i+=20){
	                       Date toDate = DateUtil.addDays(fromDate,20);
	                       Response viewRate = eGlobeInboundService.getInventory(getRateRequestSegments(clients,fromDate,toDate));
	                       if(viewRate.getStatus().equals(Status.FAILED)) {
	                    	   totalResponse = totalResponse +""+viewRate.getMessage().toString();
	                       }else {
		                       if(isMonitoringData.equals(ConstantUtil.YES)) {
			                   	runningJob.setResponse(viewRate.getMessage().toString());
			                   }else {
			                   	runningJob.setResponse("");
			                   }
			                   runningJob.setEndDateTime(new Date());
	                       }
		                   fromDate = toDate;
	                   }
	                   if(!totalResponse.equals("")) {
	                	   changeStatus(runningJob, Status.FAILED);
		   	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,totalResponse);
	                   }else {
	                	   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.INVENTFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
	                	   changeStatus(runningJob, Status.COMPLETE);
	                   }
	                   logger.error("Ended FETCH_RATE_INVENTORY Data");
	               }catch(Exception ce) {
	            	   logger.error(ce.getMessage());
            		   ce.printStackTrace();
            		   runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
            		   logger.error("EGLOBE - INVENTORY");
	        		   changeStatus(runningJob, Status.FAILED);
	   	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,ce.getMessage()+" "+ce.getCause());
	        	   }
               }
               if (runningJob.getJobType().equals(JobType.FETCH_BOOKING_DATA)) {
            	   try {
	                   logger.error("Started FETCH_BOOKING_DATA");
	                   String totalResponse ="";
	                   Date fromDate = clients.getSystemToday();
	                   for(int i = 0; i < dateRange; i+=20){
	                	   Date toDate = DateUtil.addDays(fromDate,20);
	                	   Response result = processBooking(clients, runningJob,fromDate,toDate);
	                	   if(result.getStatus().equals(Status.FAILED)) {
	                    	   totalResponse = totalResponse +""+result.getMessage().toString();
	                       }else {
			                   if(isMonitoringData.equals(ConstantUtil.YES)) {
			                   	runningJob.setResponse(result.getMessage().toString());
			                   }else {
			                   	runningJob.setResponse("");
			                   }
			                   runningJob.setEndDateTime(new Date());
	                       }
		                   fromDate = toDate;
	                   }
	                   if(!totalResponse.equals("")) {
	                	   changeStatus(runningJob, Status.FAILED);
		   	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,totalResponse);
	                   }else {
	                	   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.BOOKFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
	                	   changeStatus(runningJob, Status.COMPLETE);
	                   }
	                   logger.error("Ended FETCH_BOOKING_DATA");
	               }catch(Exception ce) {
	            	   logger.error(ce.getMessage());
            		   ce.printStackTrace();
            		   runningJob.setResponse(ce.getMessage() + "" + ce.getCause());
            		   logger.error("EGLOBE - BOOKING");
	        		   changeStatus(runningJob, Status.FAILED);
	   	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage()+" "+ce.getCause());
	        	   }
               }
           } catch (Exception e) {
        	   logger.error(e.getMessage());
    		   e.printStackTrace();
    		   logger.error("EGLOBE-");
               runningJob.setResponse(e.getMessage() + "_" + e.getCause());
               changeStatus(runningJob, Status.FAILED);
           }
       });
   }
    
    
    
    private ScheduledJob changeStatus(ScheduledJob job, Status status) {
        job.setStatus(status);
        return scheduledJobRepository.save(job);
    }
    
    private Response processBooking(Clients clients, ScheduledJob runningJob, Date fromDate, Date toDate) throws Exception {
    	Response result = new Response();
	    DailyBookingRequest dailyBookingRequest = getBookingRequest(clients);
        DailyBookingRequest.CheckIn checkIn = new DailyBookingRequest.CheckIn();
        checkIn.setStart(DateUtil.formatDate(fromDate,"dd-MMM-yyyy"));
        checkIn.setEnd(DateUtil.formatDate(toDate,"dd-MMM-yyyy"));
        dailyBookingRequest.setCheckIn_At(checkIn);
        result = eGlobeInboundService.getBookings(dailyBookingRequest);
        runningJob.setResponse(result.getMessage().toString());       
    return result;
    }

    @SuppressWarnings("unchecked")
	private List<ScheduledJob> createScheduledJobs(Clients clients,List<String> scheduledJobTaskList) {
        List<ScheduledJob> jobList = new TreeList();
        if(scheduledJobTaskList!=null) {
        	for (String task : scheduledJobTaskList) {
        		if(task.equals(ConstantUtil.FETCH_ROOM_MAPPING)){
        			jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_ROOM_MAPPING)));
        		}else if (task.equals(ConstantUtil.FETCH_INVENTORY)) {
        			jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_INVENTORY)));
        		}else if (task.equals(ConstantUtil.FETCH_BOOKING_DATA)) {
        			jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_BOOKING_DATA)));
        		}
        	}
        }else {
	        jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_ROOM_MAPPING)));
	        jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_INVENTORY)));
	        jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_BOOKING_DATA)));
        }
        return jobList;
    }
    
    private RoomRateRequest getRoomRequest(Clients clients) {
        RoomRateRequest roomrequest = new RoomRateRequest();
        roomrequest.setPropertyid(clients.getCmHotel());
        return roomrequest;
    }
    
    private DailyBookingRequest getBookingRequest(Clients clients) {
        DailyBookingRequest dailyBookingRequest = new DailyBookingRequest();
        dailyBookingRequest.setHotelCode(clients.getId());
        return dailyBookingRequest;
    }
    
    private InventoryRequest getRateRequestSegments(Clients clients, Date fromDate, Date toDate) {
    	InventoryRequest inventoryRequest = new InventoryRequest();
        inventoryRequest.setPropertyid(clients.getId());
        inventoryRequest.setFrom_date(DateUtil.formatDate(fromDate,"dd-MMM-yyyy"));
        inventoryRequest.setTo_date(DateUtil.formatDate(toDate,"dd-MMM-yyyy"));
        return inventoryRequest;
    }
    
    private ScheduledJob createJob(Clients clients, JobType jobType) {
        ScheduledJob scheduledJob = new ScheduledJob();
        scheduledJob.setClientId(clients.getId());
        scheduledJob.setJobType(jobType);
        scheduledJob.setStartDateTime(new Date());
        scheduledJob.setStatus(Status.CREATED);
        return scheduledJob;
    }

}
