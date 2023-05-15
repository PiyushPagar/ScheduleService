package com.revnomix.revseed.integration.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.list.TreeList;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
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
import com.revnomix.revseed.repository.AccountsRepository;
import com.revnomix.revseed.repository.AlertConditionTypeRepository;
import com.revnomix.revseed.repository.AlertConfigurationRepository;
import com.revnomix.revseed.repository.AlertTypeRepository;
import com.revnomix.revseed.repository.ApplicationParametersRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.ScheduledJobRepository;
import com.revnomix.revseed.schema.ezee.RESRequest;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.integration.eglobe.GenericSchedulerService;
import com.revnomix.revseed.schedular.Service.DataSyncScheduleService;
import com.revnomix.revseed.schedular.Service.ScheduledService;


@Controller
@RequestMapping("/ezee")
@CrossOrigin(origins = "*")
@PropertySource("classpath:application.properties")
public class EzeeInboundSchedulerService {
    public static final String ACTIVE = "Active";
    public static final String EZEE = "ezee";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private ScheduledJobRepository scheduledJobRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private EzeeInboundService ezeeInboundService;

    @Autowired
    private ClientOnDemandService clientOnDemandService;

    @Autowired
    private OnDemandStatusService onDemandStatusService;

    @Autowired
    private DataSyncScheduleService dataSyncScheduleService;
    
    @Autowired
    private ScheduledService scheduledService;

	@Autowired
	GenericSchedulerService genericSchedulerService;

    @Autowired
    private SystemUtil systemUtil;
    
    @Autowired
    private ParameterRepository parameterRepository;
    
	@Autowired
	ApplicationParametersRepository appParameterRepository;
	
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
        refreshCodeList.add(ConstantUtil.FETCH_VIEW_RATE);
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
    
    @RequestMapping(value = "/multi", method = RequestMethod.GET)
    @ResponseBody
    public boolean refreshDataMulti() {
    	dataSyncScheduleService.scheduleTimerDataSync();
        return true;
    }
    
    @RequestMapping(value = "/dataTimingAlert", method = RequestMethod.GET)
    @ResponseBody
    public boolean dataTimingAlertFunction() {
    	scheduledService.dataTimingAlertFunction();
        return true;
    }
    
    @RequestMapping(value = "/passreminder", method = RequestMethod.GET)
    @ResponseBody
    public boolean passreminder() {
    	scheduledService.passwordAlertFunction();
        return true;
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
        	genericSchedulerService.createMultipleScheduledJob(clientsList, refreshCodeList,true);
            return true;
        } else {
            return false;
        }
    }

    @RequestMapping(value = "/inbound/runRecommendation", method = RequestMethod.POST)
    @ResponseBody
    public boolean runRecommendation(@RequestParam("clientId") Integer clientId) {
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
    public boolean runCalibration(@RequestParam("clientId") Integer clientId) {
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
        processEzeeInputData();
        return "Fetch staah data for all the property completed";
    }
    
   // @Scheduled(cron = "0 0 8,13,17 * * ?")
   // @Profile("prod")
    public void scheduleEzeeInputData() {
        logger.info("Fetch Ezee Input Data Scheduled Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        processEzeeInputData();
    }

    /*@Scheduled(cron = "0 0 7,12,16 * * ?")
    @Profile("prod")
    public void fetchEzeeBookings() {
        logger.info("Fetch Ezee Bookings Data Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        List<Clients> clientsList = clientsRepository.findAllByStatusAndChannelManager(ACTIVE, EZEE);
        for (Clients clients : clientsList) {
            logger.info("Fetch Ezee Bookings Data for client {} :: Execution Time - {}", clients.getPropertyName(), dateTimeFormatter.format(LocalDateTime.now()));
            refreshBookings(clients);
        }
    }*/
    
    private void processEzeeInputData() {
    	List<Clients> clientsList = clientsRepository.findAllByStatusAndChannelManager(ACTIVE, EZEE);
        for (Clients clients : clientsList) {
            logger.info("FetchEzeeInputData for client {} :: Execution Time - {}", clients.getPropertyName(), dateTimeFormatter.format(LocalDateTime.now()));
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
        //String result = restClientConfig.post(getAirflowUrl() + "/api/experimental/dags/recommendation/dag_runs", "{\"conf\":{}}",false);
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
        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
        try {
	        Optional<Parameter> paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA);
	        if(paramOpt!=null && paramOpt.isPresent()) {
	        	Parameter param = paramOpt.get();
	        	isMonitoringData = param.getParamValue();
	        }
	        Response bookResponse = null;
	        bookResponse = ezeeInboundService.viewBookings(getBookingRequest(clients));       
	        if(bookResponse.getStatus().equals(Status.FAILED)) {
         	   runningJob.setResponse(bookResponse.getMessage().toString());
         	   runningJob.setEndDateTime(new Date());
                changeStatus(runningJob, Status.FAILED);
                alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,bookResponse.getMessage().toString());
            }else {
                if(isMonitoringData.equals(ConstantUtil.YES)) {
                	runningJob.setResponse(bookResponse.getMessage().toString());
                }else {
                	runningJob.setResponse("");
                }
                runningJob.setEndDateTime(new Date());
                changeStatus(runningJob, Status.COMPLETE);
                alertService.createAlertsforClientRefreshData(clients, ConstantUtil.BOOKFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
            }
	        return true;
        }
        catch(Exception ce) {
        	changeStatus(runningJob, Status.FAILED);
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,ce.getMessage()+" "+ce.getCause());
    		return false;
        }  
    }

    public void createScheduledJob(Clients clients,List<String> scheduledJobTaskList) {
    	
        Optional<Parameter> monitoringParam =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA);        
    	List<ScheduledJob> list = createScheduledJobs(clients,scheduledJobTaskList);  	
    		list.stream().forEach(job -> {
    			String isMonitoringData = "";
    			if(monitoringParam!=null && monitoringParam.isPresent()) {
    	        	Parameter params = monitoringParam.get();
    	        	isMonitoringData = params.getParamValue();
    	        }
            ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
            try {
            	Integer dateRange = 75;
                Optional<Parameter> paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), "date_range");
                if(paramOpt!=null && paramOpt.isPresent()) {
                	Parameter param = paramOpt.get();
                	dateRange = Integer.parseInt(param.getParamValue());
                }
            	
                if (runningJob.getJobType().equals(JobType.FETCH_ROOM_MAPPING)) {
                    logger.error("Started FETCH_ROOM_MAPPING Data");
                    try {
	                    Response roomResponse = ezeeInboundService.getRoomInfo(getRoomRequest(clients));
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
                if (runningJob.getJobType().equals(JobType.FETCH_VIEW_RATE)) {
                	try {
	                    logger.error("Started FETCH_VIEW_RATE Data");
	                    Date fromDate = new Date();
	                    String totalResponse ="";
	                    for(int i = 0; i < dateRange; i++){ 
	                        Date toDate = DateUtil.addDays(fromDate,5);
	                        Response viewRate = ezeeInboundService.viewRate(getRateRequestSegments(clients,fromDate,toDate));
	                        if(viewRate.getStatus().equals(Status.FAILED)) {
		                    	   totalResponse = totalResponse +""+viewRate.getMessage().toString();
		                       }else {
		                        if(isMonitoringData.equals(ConstantUtil.YES)) {
		                        	runningJob.setResponse(viewRate.getMessage().toString());
		                        }else {
		                        	runningJob.setResponse("");
		                        }
		                    }
	                        fromDate = DateUtil.addDays(toDate,1);
	                    }
	                    if(!totalResponse.equals("")) {
	                    	   runningJob.setResponse(totalResponse);
		                	   changeStatus(runningJob, Status.FAILED);
			   	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,totalResponse);
		                   }else {
		                	   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEINVFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		                	   changeStatus(runningJob, Status.COMPLETE);
		                   }
	                    logger.error("Ended FETCH_VIEW_RATE Data");
	                }catch(Exception ce) {
	                	changeStatus(runningJob, Status.FAILED);
	                	alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,ce.getMessage()+" "+ce.getCause());
	                }
                }
                if (runningJob.getJobType().equals(JobType.FETCH_INVENTORY)) {
                    logger.error("Started FETCH_INVENTORY Data");
                	try {
                		String totalResponse ="";
	                    Date fromDate = new Date();
	                    for(int i = 0; i < dateRange; i++){
	                        Date toDate = DateUtil.addDays(fromDate,5);
	                        Response viewInventory = ezeeInboundService.viewInventory(getAvailRequestSegments(clients,fromDate,toDate));
	                        if(viewInventory.getStatus().equals(Status.FAILED)) {
		                    	   totalResponse = totalResponse +""+viewInventory.getMessage().toString();
		                       }else {
		                        if(isMonitoringData.equals(ConstantUtil.YES)) {
		                        	runningJob.setResponse(viewInventory.getMessage().toString());
		                        }else {
		                        	runningJob.setResponse("");
		                        }
		                    }
	                        fromDate = DateUtil.addDays(toDate,1);
	                    }
	                    if(!totalResponse.equals("")) {
		                	   changeStatus(runningJob, Status.FAILED);
			   	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,totalResponse);
		                   }else {
		                	   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.INVENTFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		                	   changeStatus(runningJob, Status.COMPLETE);
		                   }
	                    logger.error("Ended FETCH_INVENTORY Data");
                	}catch(Exception ce) {
	                	changeStatus(runningJob, Status.FAILED);
	                	alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,ce.getMessage()+" "+ce.getCause());
	                }
                }
                if (runningJob.getJobType().equals(JobType.FETCH_BOOKING_DATA)) {
                	try {
	                    logger.error("Started FETCH_BOOKING_DATA");
	                    Response response = ezeeInboundService.viewBookings(getBookingRequest(clients));
	                    if(response.getStatus().equals(Status.FAILED)) {
	                    	   runningJob.setResponse(response.getMessage().toString());
		                	   runningJob.setEndDateTime(new Date());
			                   changeStatus(runningJob, Status.FAILED);
			                   alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,response.getMessage().toString());
	                       }else {
			                    if(isMonitoringData.equals(ConstantUtil.YES)) {
			                    	runningJob.setResponse(response.getMessage().toString());
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

    private RESRequest getAvailRequestSegments(Clients clients, Date fromDate, Date toDate) {
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

    private RESRequest getRateRequestSegments(Clients clients, Date fromDate, Date toDate) {
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

    private RESRequest getBookingRequest(Clients clients) {
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

    private RESRequest getRoomRequest(Clients clients) {
        RESRequest request = new RESRequest();
        RESRequest.Authentication roomRequest = new RESRequest.Authentication();
        roomRequest.setHotelCode(clients.getCmHotel());
        roomRequest.setAuthCode(clients.getCmPassword());
        request.setRequestType(EzeeRequestType.ROOM_INFO);
        request.setAuthentication(roomRequest);
        return request;
    }

    private ScheduledJob changeStatus(ScheduledJob job, Status status) {
        job.setStatus(status);
        job.setEndDateTime(new Date());
        return scheduledJobRepository.save(job);
    }

    private List<ScheduledJob> createScheduledJobs(Clients clients,List<String> scheduledJobTaskList) {
        List<ScheduledJob> jobList = new TreeList();
        if(scheduledJobTaskList!=null) {
        	for (String task : scheduledJobTaskList) {
        		if(task.equals(ConstantUtil.FETCH_ROOM_MAPPING)){
        			jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_ROOM_MAPPING)));
        		}else if (task.equals(ConstantUtil.FETCH_VIEW_RATE)) {
        			jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_VIEW_RATE)));
        		}else if (task.equals(ConstantUtil.FETCH_INVENTORY)) {
        			jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_INVENTORY)));
        		}else if (task.equals(ConstantUtil.FETCH_BOOKING_DATA)) {
        			jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_BOOKING_DATA)));
        		}
        	}
        }else {
	        jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_ROOM_MAPPING)));
	        jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_VIEW_RATE)));
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
