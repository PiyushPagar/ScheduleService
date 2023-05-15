package com.revnomix.revseed.integration.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.list.TreeList;
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
import com.revnomix.revseed.schema.staah.AvailRequestSegment;
import com.revnomix.revseed.schema.staah.AvailRequestSegments;
import com.revnomix.revseed.schema.staah.RateRequestSegment;
import com.revnomix.revseed.schema.staah.RateRequestSegments;
import com.revnomix.revseed.schema.staah.Roomrequest;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Service.EventLogService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.Util.service.XMLUtilService;
import com.revnomix.revseed.integration.eglobe.GenericSchedulerService;

@Controller
public class StaahInboundSchedulerService {
    public static final String ACTIVE = "Active";
    public static final String STAAH = "staah";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @Autowired
    private ScheduledJobRepository scheduledJobRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private StaahInboundService staahInboundService;

    @Autowired
    private ClientOnDemandService clientOnDemandService;

    @Autowired
    private OnDemandStatusService onDemandStatusService;

	@Autowired
	GenericSchedulerService genericSchedulerService;

    @Autowired
    private XMLUtilService xmlUtilService;

    @Autowired
    private SystemUtil systemUtil;

    @Autowired
    private EventLogService eventLogService;
    
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

    @RequestMapping(value = "/staah/inbound/runRefreshOtaData", method = RequestMethod.POST)
    @ResponseBody
    public boolean refreshData(@RequestParam("clientId") Integer clientId, HttpServletRequest request) {
        Clients clients = clientsRepository.findById(clientId).get();
        List<Clients> clientsList = new ArrayList<Clients>();
        clientsList.add(clients);
        List<String> refreshCodeList = new ArrayList<String>();
        refreshCodeList.add(ConstantUtil.FETCH_ROOM_MAPPING);
        refreshCodeList.add(ConstantUtil.FETCH_VIEW_RATE);
        refreshCodeList.add(ConstantUtil.FETCH_INVENTORY);
        if (clients != null) {
        	// createScheduledJob(clients,refreshCodeList);
        	genericSchedulerService.createMultipleScheduledJob(clientsList, refreshCodeList,false);
            eventLogService.saveLog("Refresh Data", "Update", clients, "/staah/inbound/runRefreshOtaData", request, clients.getId());
            return true;
        } else {
            return false;
        }
    }

    @RequestMapping(value = "/staah/inbound/runRefreshBookings", method = RequestMethod.POST)
    @ResponseBody
    public boolean runRefreshBookings(@RequestParam("clientId") Integer clientId, HttpServletRequest request) {
        Clients clients = clientsRepository.findById(clientId).get();
        if (clients != null) {
        	List<Clients> clientsList = new ArrayList<Clients>();
            clientsList.add(clients);
            List<String> refreshCodeList = new ArrayList<String>();
            refreshCodeList.add(ConstantUtil.FETCH_BOOKING_DATA);
        	genericSchedulerService.createMultipleScheduledJob(clientsList, refreshCodeList,false);
            eventLogService.saveLog("Refresh Data", "Update", clients, "/staah/inbound/runRefreshBookings", request, clients.getId());
            return true;
        } else {
            return false;
        }
    }

    @RequestMapping(value = "/staah/inbound/runRecommendation", method = RequestMethod.POST)
    @ResponseBody
    private boolean runRecommendation(@RequestParam("clientId") Integer clientId, HttpServletRequest request) {
        Clients clients = clientsRepository.findById(clientId).get();
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
        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.RUN_RECOMMENDATION));
        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
        clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.IN_PROGRESS);
        onDemandStatusService.changeStatus(clientId,"Run Recommendation");
        eventLogService.saveLog("Refresh Data", "Update", clients, "/staah/inbound/runRecommendation", request, clientId);
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
            return true;
        } else {
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.RECFAIL,response);
            changeStatus(runningJob, Status.FAILED);
            clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.FAILED);
            return false;
        }
    }

    @RequestMapping(value = "/staah/inbound/runCalibration", method = RequestMethod.POST)
    @ResponseBody
    private boolean runCalibration(@RequestParam("clientId") Integer clientId, HttpServletRequest request) {
        Clients clients = clientsRepository.findById(clientId).get();
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
        ScheduledJob job = scheduledJobRepository.save(createJob(clients, JobType.RUN_CALIBRATION));
        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
        clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.IN_PROGRESS);
        onDemandStatusService.changeStatus(clientId,"Run Calibration");
        eventLogService.saveLog("Refresh Data", "Update", clients, "/staah/inbound/runCalibration", request, clientId);
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
            return true;
        } else {
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.CALFAIL,response);
            changeStatus(runningJob, Status.FAILED);
            clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.FAILED);
            return false;
        }
    }

    @RequestMapping(value = "/staah/inbound/triggerRecommendationOnDemand", method = RequestMethod.POST)
    @ResponseBody
    public String recommendationOnDemand() {
        logger.error("Recommendation Process Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        recommendationTrigger();
        return "Recommendation Scheduled Task for all the property completed";
    }
    
    //@Scheduled(cron = "0 0 12,15,18 * * ?")
   // @Profile("prod")
    public void scheduleRecommendationOnDemand() {
        logger.error("Recommendation Scheduled Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        recommendationTrigger();
    }

    @RequestMapping(value = "/staah/inbound/triggerStaahInbound/client", method = RequestMethod.POST)
    @ResponseBody
    public String fetchStaahInputData(@RequestParam("clientId") Integer clientId) {
        logger.info("FetchStaahInputData Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        processStaahInputData(clientId);
        return "Fetch staah data for all the property completed";
    }
    
    @RequestMapping(value = "/staah/inbound/triggerStaahInbound", method = RequestMethod.POST)
    @ResponseBody
    public String fetchStaahInputData() {
        logger.info("FetchStaahInputData Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        processStaahInputData();
        return "Fetch staah data for all the property completed";
    }
    
    //@Scheduled(cron = "0 0 8,11,14,17 * * ?")
    //@Profile("prod")
    public void scheduleStaahInputData() {
        logger.info("FetchStaahInputData Scheduled Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
        processStaahInputData();
    }
    
    private void processStaahInputData(Integer clientId) {
    	clientsRepository.findById(clientId).ifPresent(c->{
    		createScheduledJob(c,null);
    	});
    	
    }

    
    private void processStaahInputData() {
    	List<Clients> clientsList = clientsRepository.findAllByStatusAndChannelManager(ACTIVE,STAAH);
        clientsList.forEach(clients -> {
            logger.info("FetchStaahInputData for client {} :: Execution Time - {}", clients.getPropertyName(), dateTimeFormatter.format(LocalDateTime.now()));
            createScheduledJob(clients,null);
        });
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
	        ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
	        String isMonitoringData = "";
	    	ResponseEntity<String> result = null;
	    	String response="";
	        try {
	        Optional<Parameter> paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA);
	        if(paramOpt!=null && paramOpt.isPresent()) {
	        	Parameter param = paramOpt.get();
	        	isMonitoringData = param.getParamValue();
	        }
	        clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.IN_PROGRESS);
	        onDemandStatusService.changeStatus(clients.getId(),"Refresh Bookings");
	    	HttpHeaders headers = new HttpHeaders();
	     	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	     	HttpEntity<?> requestEntity = new HttpEntity<>(headers);
	    	RestTemplate restTemplate = new RestTemplate();
	        restTemplate.setMessageConverters(systemUtil.setJSONMessageConvertor());
	        result = restTemplate.exchange(getAirflowUrl() + "/api/experimental/dags/fetch_booking_data_on_demand/dag_runs", HttpMethod.POST, requestEntity, String.class);
	        response = result.getBody();

	        //String result = restClientConfig.post(getAirflowUrl() + "/api/experimental/dags/fetch_booking_data_on_demand/dag_runs", "{\"conf\":{\"clientId\":\"" + clients.getId() + "\"}}",false);
	        if(isMonitoringData.equals(ConstantUtil.YES)) {
	        	runningJob.setResponse(response);
	        }else {
	        	runningJob.setResponse("");
	        }
	        if (response.contains("Created")) {
	            changeStatus(runningJob, Status.COMPLETE);
	            clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.COMPLETED);
	            return true;
	        } else {
	            changeStatus(runningJob, Status.FAILED);
	            clientOnDemandService.changeStatus(clients.getId(), ProcessingStatus.FAILED);
	            return false;
	        }
        }
        catch (Exception e) {
        	alertService.createFailureAlertsforClient(clients, ConstantUtil.BOOKFAIL,e.getMessage()+" "+e.getCause());
        	return false;
		}
    }

    public void createScheduledJob(Clients clients,List<String> scheduledJobTaskList) {
    	Optional<Parameter> monitoringParam =parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.MONITORING_DATA);        
    	List<ScheduledJob> list = createScheduledJobs(clients,scheduledJobTaskList);  	
		list.stream().forEach(job -> {
            ScheduledJob runningJob = changeStatus(job, Status.RUNNING);
            try {
            	String isMonitoringData = "";
    			if(monitoringParam!=null && monitoringParam.isPresent()) {
    	        	Parameter params = monitoringParam.get();
    	        	isMonitoringData = params.getParamValue();
    	        }
            	Integer dateRange = 364;
                Optional<Parameter> paramOpt =parameterRepository.findByClientIdAndParamName(clients.getId(), "date_range");
                if(paramOpt!=null && paramOpt.isPresent()) {
                	Parameter param = paramOpt.get();
                	dateRange = Integer.parseInt(param.getParamValue());
                }
                if (runningJob.getJobType().equals(JobType.FETCH_ROOM_MAPPING)) {
                	try {
	                    Response roomresponse = staahInboundService.roomMapping(getRoomrequest(clients));
	                    if(roomresponse.getStatus().equals(Status.FAILED)) {
	                    	   runningJob.setResponse(roomresponse.getMessage().toString());
		                	   runningJob.setEndDateTime(new Date());
			                   changeStatus(runningJob, Status.FAILED);
			                   alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,roomresponse.getMessage().toString());
                       }else {
		                    if(isMonitoringData.equals(ConstantUtil.YES)) {
		                    	runningJob.setResponse(roomresponse.getMessage().toString());
		                    }else {
		                    	runningJob.setResponse("");
		                    }
		                    runningJob.setEndDateTime(new Date());
		                    changeStatus(runningJob, Status.COMPLETE);
		                    alertService.createAlertsforClientRefreshData(clients, ConstantUtil.ROOMINVFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
                       }
	                }catch(Exception ce) {
                		changeStatus(runningJob, Status.FAILED);
                		alertService.createFailureAlertsforClient(clients, ConstantUtil.ROOMINVFAIL,ce.getMessage()+" "+ce.getCause());
                	}
                }
                if (runningJob.getJobType().equals(JobType.FETCH_VIEW_RATE)) {
                	try {
	                    Date fromDate = new Date();
	                    fromDate = DateUtil.addDays(fromDate,-1);
	                    String viewRate = "";
	                    for(int i = 0; i < dateRange; i++){ 
	                        Date toDate = DateUtil.addDays(fromDate,27);
	                        Response newviewRate = staahInboundService.viewRate(getRateRequestSegments(clients,fromDate,toDate));
	                        if(newviewRate.getStatus().equals(Status.FAILED)) {
	                        	viewRate = viewRate +""+newviewRate.getMessage().toString();
		                       }else {
		                        if(isMonitoringData.equals(ConstantUtil.YES)) {
		                        	runningJob.setResponse(newviewRate.getMessage().toString());
		                        }else {
		                        	runningJob.setResponse("");
		                        }
		                    }
	                        fromDate = DateUtil.addDays(toDate,1);
	                        i = i + 27;
	                    }
	                    if(!viewRate.equals("")) {
		                	   changeStatus(runningJob, Status.FAILED);
			   	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,viewRate);
		                   }else {
		                	   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.RATEINVFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		                	   changeStatus(runningJob, Status.COMPLETE);
		                   }              	
	                    }catch(Exception ce) {
                		changeStatus(runningJob, Status.FAILED);
                		alertService.createFailureAlertsforClient(clients, ConstantUtil.RATEINVFAIL,ce.getMessage() +" "+ce.getCause());
                	}
                }
                if (runningJob.getJobType().equals(JobType.FETCH_INVENTORY)) {
                	try {
	                    Date fromDate = new Date();
	                    fromDate = DateUtil.addDays(fromDate,-1);
	                    String viewResult = "";
	                    for(int i = 0; i < dateRange; i++){ 
	                        Date toDate = DateUtil.addDays(fromDate,27);
	                        Response result = staahInboundService.viewInventory(getAvailRequestSegments(clients,fromDate,toDate));
	                        if(result.getStatus().equals(Status.FAILED)) {
	                        	viewResult = viewResult +""+result.getMessage().toString();
		                       }else {
		                        if(isMonitoringData.equals(ConstantUtil.YES)) {
		                        	runningJob.setResponse(result.getMessage().toString());
		                        }else {
		                        	runningJob.setResponse("");
		                        }
		                    }
		                    fromDate = DateUtil.addDays(toDate,1);
		                    i = i + 27;
	                    }
	                    if(!viewResult.equals("")) {
		                	   changeStatus(runningJob, Status.FAILED);
			   	        	   alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,viewResult);
		                   }else {
		                	   alertService.createAlertsforClientRefreshData(clients, ConstantUtil.INVENTFAIL,ConstantUtil.DATA_REFRESH_SUCCESS);
		                	   changeStatus(runningJob, Status.COMPLETE);
		                   }	                
	                    }catch(Exception ce) {
	            		changeStatus(runningJob, Status.FAILED);
	            		alertService.createFailureAlertsforClient(clients, ConstantUtil.INVENTFAIL,ce.getMessage()+" "+ce.getCause());
	            	}
                }
            } catch (Exception e) {
                runningJob.setResponse(e.getMessage() + "_" + e.getCause());
                changeStatus(runningJob, Status.FAILED);
            }
        });
    }

    private AvailRequestSegments getAvailRequestSegments(Clients clients, Date fromDate, Date toDate) {
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

    private RateRequestSegments getRateRequestSegments(Clients clients, Date fromDate, Date toDate) {
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

    private Roomrequest getRoomrequest(Clients clients) {
        Roomrequest roomrequest = new Roomrequest();
        roomrequest.setHotelId(clients.getCmHotel());
        roomrequest.setPassword(clients.getCmPassword());
        roomrequest.setUsername(clients.getCmUsername());
        return roomrequest;
    }

    private ScheduledJob changeStatus(ScheduledJob job, Status status) {
        job.setStatus(status);
        job.setEndDateTime(new Date());
        return scheduledJobRepository.save(job);
    }
    
    @SuppressWarnings("unchecked")
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
        		}
        	}
        }else {
	        jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_ROOM_MAPPING)));
	        jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_VIEW_RATE)));
	        jobList.add(scheduledJobRepository.save(createJob(clients, JobType.FETCH_INVENTORY)));
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
