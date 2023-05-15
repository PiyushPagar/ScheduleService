package com.revnomix.revseed.integration.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.list.TreeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.revnomix.revseed.integration.eglobe.GenericSchedulerService;
import com.revnomix.revseed.integration.staahMax.StaahMaxInboundService;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingRequest;
import com.revnomix.revseed.integration.staahMax.dto.InventoryYearlyRequest;
import com.revnomix.revseed.integration.staahMax.dto.RoomRateRequest;

@Controller
@RequestMapping("/staahAll")
@CrossOrigin(origins = "*")
public class StaahMaxInboundSchedulerService {
    public static final String ACTIVE = "Active";
    public static final String STAAH_ALL = "staah All";
    public static final String CRM_API_KEY = "U2FsdGVkX1/6pIbvTr4DBIHu9cuU0DQ6smu0OmXTdLY=";
    public static final String RMS_API_KEY = "TlNQ9z7rYHR7sCQG4TNItVgfVidcYvAJ";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Value( "${recommendation.url:http://13.232.233.63:5000/api/recommendation/}" )
    private String RECOMMENDATION;

    @Value( "${caliberation.url:http://13.232.233.63:5000/api/calibration/}" )
    private String CALIBERATION;
    
    
    @Autowired
    private ScheduledJobRepository scheduledJobRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private StaahMaxInboundService staahMaxInboundService;

    @Autowired
    private ClientOnDemandService clientOnDemandService;

    @Autowired
    private OnDemandStatusService onDemandStatusService;

    @Autowired
    private SystemUtil systemUtil;

    @Autowired
    private ParameterRepository parameterRepository;
    
	@Autowired
	private ApplicationParametersRepository appParameterRepository;
	
	@Autowired
	private AlertService alertService;
	
	@Autowired
	GenericSchedulerService genericSchedulerService;


    @Value("${staah.url:http://13.235.212.209:8080}")
    private String airflowUrl;

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
            //createScheduledJob(clients,refreshCodeList);
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
            alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RECFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
            clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.COMPLETED);
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
            //result = restClientConfig.post(paramvalue+clientId.toString(), null,false);
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

    @RequestMapping(value = "/inbound/triggerRecommendationOnDemand", method = RequestMethod.POST)
    @ResponseBody
    public String recommendationOnDemand() {
        logger.info("Recommendation Process Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        recommendationTrigger();
        return "Recommendation Scheduled Task for all the property completed";
    }
    
    //@Scheduled(cron = "0 0 12,15,18 * * ?")
    @Profile("prod")
    public void scheduleRecommendationOnDemand() {
        logger.info("Recommendation Scheduled Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        recommendationTrigger();
    }



    @RequestMapping(value = "/inbound/triggerStaahInbound", method = RequestMethod.POST)
    @ResponseBody
    public String fetchStaahInputData() {
        logger.info("FetchStaahInputData Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        processStaahAllInputData();
        return "Fetch staah data for all the property completed";
    }

    //@Scheduled(cron = "0 0 8,11,14,17 * * ?")
    //@Profile("prod")
    public void scheduleStaahAllInputData() {
        logger.info("Fetch Staah All Input Data Scheduled Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        processStaahAllInputData();
    }

    /*@Scheduled(cron = "0 0 7,12,16 * * ?")
    @Profile("prod")
    public void fetchStaahAllBookings() {
        logger.info("Fetch Staah All Bookings Data Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        List<Clients> clientsList = clientsRepository.findAllByStatusAndChannelManager(ACTIVE, STAAH_ALL);
        for (Clients clients : clientsList) {
            logger.info("Fetch Staah All Bookings Data for client {} :: Execution Time - {}", clients.getPropertyName(), dateTimeFormatter.format(LocalDateTime.now()));
            refreshBookings(clients);
        }
    }*/
    
    private void processStaahAllInputData() {
    	List<Clients> clientsList = clientsRepository.findAllByStatusAndChannelManager(ACTIVE, STAAH_ALL);
        for (Clients clients : clientsList) {
            logger.info("Fetch Staah All Input Data for client {} :: Execution Time - {}", clients.getPropertyName(), dateTimeFormatter.format(LocalDateTime.now()));
            createScheduledJob(clients,null);
        }
    }

    private boolean recommendationTrigger() {
	    ResponseEntity<String> result = null;
		String response="";
		HttpHeaders headers = new HttpHeaders();
	 	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	 	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();
	    restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
	    result = restTemplate.exchange(getAirflowUrl() + "/api/experimental/dags/recommendation/dag_runs", HttpMethod.POST, requestEntity, String.class);
	    response = result.getBody();
        if (response.contains("Created")) {
            logger.info("Recommendation Scheduled Task for all the property Triggered");
            return true;
        } else {
            return false;
        }
    }

    private boolean refreshBookings(Clients clients) {
        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.FETCH_BOOKING_DATA));
        String isMonitoringData = "";
        Optional<Parameter> paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA);
        if(paramOpt!=null && paramOpt.isPresent()) {
        	Parameter param = paramOpt.get();
        	isMonitoringData = param.getParamValue();
        }
        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
        Response result = processBooking(clients, runningJob);
        runningJob.setEndDateTime(new Date());
        if(result.getStatus().equals(Status.FAILED)) {
     	   runningJob.setResponse(result.getMessage().toString());
     	   runningJob.setEndDateTime(new Date());
            changeStatus(runningJob, Status.FAILED);
            alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,result.getMessage().toString());
        }else {
        	if(isMonitoringData.equals(ConstantUtil.YES)) {
            	runningJob.setResponse(result.getMessage().toString());
            }else {
            	runningJob.setResponse("");
            }
            runningJob.setEndDateTime(new Date());
            changeStatus(runningJob, Status.COMPLETE);
            alertService.createAlertsforClientRefreshData(clients, ConstantUtil.BOOKFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
    	}
		return true;
    }

    private Response processBooking(Clients clients, ScheduledJob runningJob) {
    	Optional<Parameter> monitoringParam =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA);        
        Response result = new Response();
        String totalResponse ="";
        try {
        	String isMonitoringData = "";
			if(monitoringParam!=null && monitoringParam.isPresent()) {
	        	Parameter params = monitoringParam.get();
	        	isMonitoringData = params.getParamValue();
	        }
	        Date fromDate = DateUtil.setTimeToZero(new Date());
	        for(int i = 0; i < 5; i++) {
	            String date = DateUtil.formatDate(fromDate, "yyyy-MM-dd");
	            DailyBookingRequest dailyBookingRequest = getBookingRequest(clients);
	            DailyBookingRequest.CheckIn checkIn = new DailyBookingRequest.CheckIn();
	            checkIn.setStart(date);
	            checkIn.setEnd(date);
	            dailyBookingRequest.setCheckIn_At(checkIn);
	            result = staahMaxInboundService.getBooking(dailyBookingRequest);
	            if(result.getStatus().equals(Status.FAILED)) {
             	   totalResponse = totalResponse +""+result.getMessage().toString();
                }else {
                	if(isMonitoringData.equals(ConstantUtil.YES)) {
                    	runningJob.setResponse(result.getMessage().toString());
                    }else {
                    	runningJob.setResponse("");
                    }
                }
            
	            fromDate = DateUtil.addDays(fromDate, 1);
	        }
	        if(!totalResponse.equals("")) {
         	   changeStatus(runningJob, Status.FAILED);
	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,result.getMessage().toString());
            }else {
         	   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.BOOKFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
         	   changeStatus(runningJob, Status.COMPLETE);
            }        
	        }catch(Exception ce) {
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage()+" "+ce.getCause());
        }
        return result;
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
	                    Response roomResponse = staahMaxInboundService.roomMapping(getRoomRequest(clients));
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
                		changeStatus(runningJob, Status.FAILED);
                		alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,ce.getMessage()+" "+ce.getCause());
                	}
                }
                if (runningJob.getJobType().equals(JobType.FETCH_INVENTORY)) {
                	try {
	                    logger.error("Started FETCH_RATE_INVENTORY Data");
	                    Response viewRate = staahMaxInboundService.viewInventoryRateByYear(getRateRequestSegments(clients));
	                    if(viewRate.getStatus().equals(Status.FAILED)) {
		                	   runningJob.setResponse(viewRate.getMessage().toString());
		                	   runningJob.setEndDateTime(new Date());
			                   changeStatus(runningJob, Status.FAILED);
			                   alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,viewRate.getMessage().toString());
		                   }else {
			                   if(isMonitoringData.equals(ConstantUtil.YES)) {
			                   	runningJob.setResponse(viewRate.getMessage().toString());
			                   }else {
			                   	runningJob.setResponse("");
			                   }
			                   runningJob.setEndDateTime(new Date());
			                   changeStatus(runningJob, Status.COMPLETE);
			                   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.INVENTFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		                   }	                    
	                    logger.error("Ended FETCH_RATE_INVENTORY Data");
	                }catch(Exception ce) {
	            		changeStatus(runningJob, Status.FAILED);
	            		alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,ce.getMessage()+" "+ce.getCause());
	            	}
                }
                if (runningJob.getJobType().equals(JobType.FETCH_BOOKING_DATA)) {
                	try {
	                    logger.error("Started FETCH_BOOKING_DATA");
	                    Response result = processBooking(clients, runningJob);
	                    if(result.getStatus().equals(Status.FAILED)) {
		                	   runningJob.setResponse(result.getMessage().toString());
		                	   runningJob.setEndDateTime(new Date());
			                   changeStatus(runningJob, Status.FAILED);
			                   alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,result.getMessage().toString());
		                   }else {
			                   if(isMonitoringData.equals(ConstantUtil.YES)) {
			                   	runningJob.setResponse(result.getMessage().toString());
			                   }else {
			                   	runningJob.setResponse("");
			                   }
			                   runningJob.setEndDateTime(new Date());
			                   changeStatus(runningJob, Status.COMPLETE);
			                   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.BOOKFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		                   }	                    
	                    logger.error("Ended FETCH_BOOKING_DATA");
	                }catch(Exception ce) {
	            		changeStatus(runningJob, Status.FAILED);
	            		alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage()+" "+ce.getCause());
	            	}
                }
            } catch (Exception e) {
                runningJob.setResponse(e.getMessage() + "_" + e.getCause());
                changeStatus(runningJob, Status.FAILED);
            }
        });
    }

    private InventoryYearlyRequest getRateRequestSegments(Clients clients) {
        InventoryYearlyRequest inventoryRequest = new InventoryYearlyRequest();
        inventoryRequest.setAction(StaahAllRequestType.YEAR_INFO_ARR);
        inventoryRequest.setPropertyid(clients.getCmHotel());
        inventoryRequest.setApikey(RMS_API_KEY);
        return inventoryRequest;
    }
    
    private DailyBookingRequest getBookingRequest(Clients clients) {
        DailyBookingRequest dailyBookingRequest = new DailyBookingRequest();
        dailyBookingRequest.setHotelCode(clients.getCmHotel());
        return dailyBookingRequest;
    }

    private RoomRateRequest getRoomRequest(Clients clients) {
        RoomRateRequest roomrequest = new RoomRateRequest();
        roomrequest.setPropertyid(clients.getCmHotel());
        roomrequest.setAction(StaahAllRequestType.ROOMRATE_INFO);
        roomrequest.setApikey(RMS_API_KEY);
        return roomrequest;
    }

    private ScheduledJob changeStatus(ScheduledJob job, Status status) {
        job.setStatus(status);
        return scheduledJobRepository.save(job);
    }
    
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

    private ScheduledJob createJob(Clients clients, JobType jobType) {
        ScheduledJob scheduledJob = new ScheduledJob();
        scheduledJob.setClientId(clients.getId());
        scheduledJob.setJobType(jobType);
        scheduledJob.setStartDateTime(new Date());
        scheduledJob.setStatus(Status.CREATED);
        return scheduledJob;
    }

    public String getAirflowUrl() {
        return airflowUrl;
    }

    public void setAirflowUrl(String airflowUrl) {
        this.airflowUrl = airflowUrl;
    }
}
