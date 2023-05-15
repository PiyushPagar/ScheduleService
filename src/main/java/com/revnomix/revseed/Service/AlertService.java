package com.revnomix.revseed.Service;

import java.util.ArrayList;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.revnomix.revseed.model.Accounts;
import com.revnomix.revseed.model.AlertConditionType;
import com.revnomix.revseed.model.AlertConfiguration;
import com.revnomix.revseed.model.AlertLogs;
import com.revnomix.revseed.model.AlertType;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.AccountsRepository;
import com.revnomix.revseed.repository.AlertConditionTypeRepository;
import com.revnomix.revseed.repository.AlertConfigurationRepository;
import com.revnomix.revseed.repository.AlertTypeRepository;
import com.revnomix.revseed.repository.AlertsLogRepository;
import com.revnomix.revseed.repository.ApplicationParametersRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.CustomRepository;
import com.revnomix.revseed.wrapper.AlertConditionTypeDto;
import com.revnomix.revseed.wrapper.AlertConfigurationDto;
import com.revnomix.revseed.wrapper.AlertTypeDto;
import com.revnomix.revseed.wrapper.dashboard.JQGridDTO;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.service.Response;

@Service
public class AlertService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    private CustomRepository customRepository;
    
    @Autowired
    private AlertsLogRepository alertsLogRepository;
    
    @Autowired
    private AlertConfigurationRepository alertConfigurationRepository;
    
    @Autowired
    private AlertConditionTypeRepository alertConditionTypeRepository;
    
    @Autowired
    private AlertTypeRepository alertTypeRepository;
    
    @Autowired
    private ApplicationParametersRepository applicationParametersRepository;
    
    @Autowired
    private ClientsRepository clientsRepository;
    
    @Autowired
    private AccountsRepository accountsRepository;

    public JQGridDTO<AlertLogs> getLatestAlertsList(Integer pagenum, Integer pagesize,Integer accountId, Integer clientId, String body,
			String subject,String status,String readStatus, HttpServletRequest request) {
		List<AlertLogs> alertList = new ArrayList<AlertLogs>();
		JQGridDTO<AlertLogs> gridData = new JQGridDTO<AlertLogs>();
		try {
			Pageable pageable = PageRequest.of(pagenum, pagesize);
			Page<AlertLogs> alerts =  null;
			Page<AlertLogs> alertsUser =  null;
			List<String> alertConfigListAccount = alertConfigurationRepository.findByAccountId(accountId)
					.stream().filter(v->v.getStatus().equals(ConstantUtil.ACTIVE)).map(x->x.getAlertType())
					.collect(Collectors.toList());
			List<String> alertConfigListClient = alertConfigurationRepository.findByClientId(clientId)
					.stream().filter(v->v.getStatus().equals(ConstantUtil.ACTIVE)).map(x->x.getAlertType())
					.collect(Collectors.toList());
			alerts = customRepository.findAlertsByConditions(pagenum, pagesize, null,clientId, body, subject, status, readStatus,alertConfigListClient, pageable);
			alertsUser = customRepository.findAlertsByConditions(pagenum, pagesize, accountId,null, body, subject, status, readStatus,alertConfigListAccount, pageable);
			Long total = alerts.getTotalElements()+alertsUser.getTotalElements();
			gridData.setPage(Integer.valueOf(pagenum));
			gridData.setTotal(String.valueOf(total));
			gridData.setRows(String.valueOf(Math.ceil((double) total / pagesize)));
			alerts.stream().forEach(al->{
				alertList.add(al);
			});
			alertsUser.stream().forEach(alu->{
				alertList.add(alu);
			});
			gridData.setRecords(alertList);
		}
		  catch(Exception e) {
			  logger.error("AlertService - getAlertList",e.getMessage());
		  }
		  return gridData;
	}
    
    public Response updateAlertReadStatus(List<Integer> id) {
    	Response response = new Response();
    	List<AlertLogs> alertLogList = alertsLogRepository.findAllById(id);
    	for(AlertLogs a :alertLogList) {
    		a.setReadStatus(ConstantUtil.READ);
    	}
    	alertsLogRepository.saveAll(alertLogList);
    	response.setMessage(ConstantUtil.SUCCESS);
    	response.setStatus(Status.SUCCESS);
		return response;
    }	
    
    public Response updateAlertStatus(List<Integer> id) {
    	Response response = new Response();
    	List<AlertLogs> alertLogList = alertsLogRepository.findAllById(id);
    	for(AlertLogs a :alertLogList) {
    		a.setStatus(ConstantUtil.INACTIVE);
    	}
    	alertsLogRepository.saveAll(alertLogList);
    	response.setMessage(ConstantUtil.SUCCESS);
    	response.setStatus(Status.SUCCESS);
		return response;
    }
    
    public Long getAlertsCount(Integer pagenum, Integer pagesize, Integer accountId,Integer clientId, String status,String readStatus, HttpServletRequest request) {
		Long alertCount = 0L;
		try {
			Pageable pageable = PageRequest.of(pagenum, pagesize);
			Page<AlertLogs> alerts =  null;
			Page<AlertLogs> alertsUser =  null;
			List<String> alertConfigList = alertConfigurationRepository.findByAccountId(accountId)
					.stream().filter(v->v.getStatus().equals(ConstantUtil.ACTIVE)).map(x->x.getAlertType())
					.collect(Collectors.toList());
			alerts = customRepository.findAlertsByConditions(pagenum, pagesize, null,clientId, null, null, status, readStatus,alertConfigList, pageable);			
			alertsUser = customRepository.findAlertsByConditions(pagenum, pagesize, accountId,null, null, null, status, readStatus,alertConfigList, pageable);			
			alertCount = alerts.getTotalElements() + alertsUser.getTotalElements();
		}
		  catch(Exception e) {
			  logger.error("AlertService - getUnreadAlertsCount",e.getMessage());
		  }
		  return alertCount;
	}

	public List<AlertConfigurationDto> getAlertConfiguration(Integer accountId, Integer clientId, String status,
			HttpServletRequest request) {
		List<AlertConfigurationDto> alertConfigurationDtoList = new ArrayList<AlertConfigurationDto>();
		List<AlertConfiguration> alertConfigList = new ArrayList<AlertConfiguration>();
		if(accountId!=null) {
		    alertConfigList = alertConfigurationRepository.findByAccountId(accountId);	
		}
		else if (clientId!=null) {
			alertConfigList = alertConfigurationRepository.findByClientId(clientId);	
		}
		alertConfigList.forEach(config->{
			AlertConfigurationDto alertConfigurationDto = new AlertConfigurationDto();
			alertConfigurationDto.setAccountId(config.getAccountId());
			AlertType alertType = alertTypeRepository.findByCodeAndStatus(config.getAlertType(), ConstantUtil.ACTIVE);
			if(alertType!=null) {
				alertConfigurationDto.setAlertType(alertType.getName());
				alertConfigurationDto.setAlertTypeCode(alertType.getCode());
				alertConfigurationDto.setId(config.getId());
				alertConfigurationDto.setStatus(config.getStatus());
				alertConfigurationDtoList.add(alertConfigurationDto);
			}
		});
		return alertConfigurationDtoList;
	}

	public Response addUpdateAlertConfig(AlertConfiguration alertConfig) {
		Response response = new Response();
		AlertConfiguration alertConfiguration = null;
		try {
		if(alertConfig.getId()!=null) {
			Optional<AlertConfiguration> configOpt = alertConfigurationRepository.findById(alertConfig.getId());
			if(configOpt.isPresent()) {
				alertConfiguration = configOpt.get(); 
				alertConfiguration.setStatus(alertConfig.getStatus());
			}
			
		}else {
			alertConfiguration = new AlertConfiguration();
			alertConfiguration = alertConfig;
		}
		//alertConfiguration.setAlertConditionType(alertConditionTypeList);
		alertConfigurationRepository.save(alertConfiguration);
		response.setStatus(Status.SUCCESS);
		response.setMessage(Status.SUCCESS.toString());
		}catch(Exception ce) {			
			response.setStatus(Status.FAILED);
			response.setMessage(Status.FAILED.toString());
			ce.printStackTrace();
		}	
		return response;
	}
	
	public List<AlertConditionTypeDto> getAlertConditionType(Integer alertConfigId,
			HttpServletRequest request) {
		List<AlertConditionTypeDto> alertConditionDtoList = new ArrayList<AlertConditionTypeDto>();
		List<AlertConditionType> alertCondList = alertConditionTypeRepository.findByConfigIdAndStatus(alertConfigId,ConstantUtil.ACTIVE);
		alertCondList.forEach(cond->{
			AlertConditionTypeDto alertConditionTypeDto = new AlertConditionTypeDto();
			alertConditionTypeDto.setConfigId(cond.getConfigId());
			alertConditionTypeDto.setEndDate(DateUtil.formatDate(cond.getEndDate(), ConstantUtil.DDMMYYYY_SLASH));
			alertConditionTypeDto.setEndWindow(cond.getEndWindow());
			alertConditionTypeDto.setId(cond.getId());
			alertConditionTypeDto.setInterval(cond.getIntervals());
			alertConditionTypeDto.setIntervalType(cond.getIntervalType());
			alertConditionTypeDto.setStartDate(DateUtil.formatDate(cond.getStartDate(), ConstantUtil.DDMMYYYY_SLASH));
			alertConditionTypeDto.setStartWindow(cond.getStartWindow());
			alertConditionTypeDto.setStatus(cond.getStatus());
			alertConditionDtoList.add(alertConditionTypeDto);
		});
		return alertConditionDtoList;		
	}
	
	public Response addUpdateAlertConditionType(AlertConditionTypeDto alertConditionType) {
		Response response = new Response();
		AlertConditionType alertCondition = null;
		try {
			if(alertConditionType.getId()!=null) {
				Optional<AlertConditionType> condtType = alertConditionTypeRepository.findById(alertConditionType.getId());
				if(condtType.isPresent()) {
					alertCondition = condtType.get(); 
					alertCondition.setConfigId(alertConditionType.getConfigId());
					alertCondition.setEndDate(DateUtil.toDate(alertConditionType.getEndDate(), ConstantUtil.DDMMYYYY_SLASH));
					alertCondition.setEndWindow(alertConditionType.getEndWindow());
					alertCondition.setIntervals(alertConditionType.getInterval());
					alertCondition.setIntervalType(alertConditionType.getIntervalType());
					alertCondition.setStartDate(DateUtil.toDate(alertConditionType.getStartDate(), ConstantUtil.DDMMYYYY_SLASH));
					alertCondition.setStartWindow(alertConditionType.getStartWindow());
					alertCondition.setStatus(alertConditionType.getStatus());				
				}
			}else {
				alertCondition = new AlertConditionType();
				alertCondition.setConfigId(alertConditionType.getConfigId());
				alertCondition.setEndDate(DateUtil.toDate(alertConditionType.getEndDate(), ConstantUtil.DDMMYYYY_SLASH));
				alertCondition.setEndWindow(alertConditionType.getEndWindow());
				alertCondition.setIntervals(alertConditionType.getInterval());
				alertCondition.setIntervalType(alertConditionType.getIntervalType());
				alertCondition.setStartDate(DateUtil.toDate(alertConditionType.getStartDate(), ConstantUtil.DDMMYYYY_SLASH));
				alertCondition.setStartWindow(alertConditionType.getStartWindow());
				alertCondition.setStatus(alertConditionType.getStatus());	
			}			
			alertConditionTypeRepository.save(alertCondition);
			response.setStatus(Status.SUCCESS);
			response.setMessage(Status.SUCCESS.toString());
		}catch(Exception ce) {			
			response.setStatus(Status.FAILED);
			response.setMessage(Status.FAILED.toString());
			ce.printStackTrace();
		}	
		return response;	
	}
	
	public void createFailureAlertsforClient(Clients clients,String alertTypeCode, String errorResult) {
		AlertConfiguration  alertConfiguration  = alertConfigurationRepository.findByClientIdAndAlertType(clients.getId(),alertTypeCode);
		if(alertConfiguration!=null) {
			List<AlertConditionType> alertConditionTypeList = alertConditionTypeRepository.findByConfigIdAndStatus(alertConfiguration.getId(),ConstantUtil.ACTIVE);
			if(alertConditionTypeList!=null && alertConditionTypeList.size()>0) {
    			alertConditionTypeList.forEach(alcond->{
    				if(DateUtil.isDateBetweenTwoDates(alcond.getStartDate(), new Date(), alcond.getEndDate())) {
						HashMap<String,String> tags = new  HashMap<String,String>();
						tags.put("<CLIENT>", clients.getPropertyName());
						tags.put("<DATE>", DateUtil.currentformatDateTime(ConstantUtil.DDMMYYYYHHMMSS));
						if(errorResult!=null && !errorResult.equals("")) {
							tags.put("<CUSTOMMSG>", prepareErrorMsg(errorResult));
						}else {
							tags.put("<CUSTOMMSG>", "Not Available");
						}
						AlertType alertType = alertTypeRepository.findByCodeAndStatus(alertTypeCode, ConstantUtil.ACTIVE);
						createAlerts(alertType, tags, clients.getId(),null);
    				}
    			});
			}else {
				HashMap<String,String> tags = new  HashMap<String,String>();
   			 	tags.put("<CLIENT>", clients.getPropertyName());
   			 	tags.put("<DATE>", DateUtil.currentformatDateTime(ConstantUtil.DDMMYYYYHHMMSS));
   			 	if(errorResult!=null && !errorResult.equals("")) {
					tags.put("<CUSTOMMSG>", prepareErrorMsg(errorResult));
				}else {
					tags.put("<CUSTOMMSG>", "Not Available");
				}
   			 	AlertType alertType = alertTypeRepository.findByCodeAndStatus(alertTypeCode, ConstantUtil.ACTIVE);
   			 	createAlerts(alertType, tags,clients.getId(),null);
			}
		}
	}
	
	public void createScheduledCheckAlertsforClient(Clients clients,String alertTypeCode,Date lastUpdatedDate) {
		AlertConfiguration  alertConfiguration  = alertConfigurationRepository.findByClientIdAndAlertType(clients.getId(),alertTypeCode);
		if(alertConfiguration!=null) {
			List<AlertConditionType> alertConditionTypeList = alertConditionTypeRepository.findByConfigIdAndStatus(alertConfiguration.getId(),ConstantUtil.ACTIVE);
			if(alertConditionTypeList!=null && alertConditionTypeList.size()>0) {
    			alertConditionTypeList.forEach(alcond->{
    				Date startDate = alcond.getStartDate();
    				Date endDate = alcond.getEndDate();
    				if(alcond.getIntervals()!=null) {
    					Integer interval = alcond.getIntervals();
    					if(alcond.getStartWindow()!=null && alcond.getEndWindow()!=null) {
    						startDate = DateUtil.addDays(alcond.getStartDate(), alcond.getStartWindow());
    						endDate = DateUtil.addDays(alcond.getStartDate(), alcond.getEndWindow());
    					}
    					List<Date> dateList = DateUtil.getAllIntervalDates (interval, startDate, endDate);
    					if(dateList.contains(new Date())) {
    						HashMap<String,String> tags = new  HashMap<String,String>();
    		   			 	tags.put("<CLIENT>", clients.getPropertyName());
    		   			 	tags.put("<INTERVAL>",interval.toString());
    		   			 	AlertType alertType = alertTypeRepository.findByCodeAndStatus(alertTypeCode, ConstantUtil.ACTIVE);
    		   			 	createAlerts(alertType, tags,clients.getId(),null);
    					}
    				}
    				if(DateUtil.isDateBetweenTwoDates(startDate, new Date(),endDate)) {
    					Integer interval = alcond.getIntervals();
						HashMap<String,String> tags = new  HashMap<String,String>();
		   			 	tags.put("<CLIENT>", clients.getPropertyName());
		   			 	tags.put("<INTERVAL>",interval.toString());
						AlertType alertType = alertTypeRepository.findByCodeAndStatus(alertTypeCode, ConstantUtil.ACTIVE);
						createAlerts(alertType, tags, clients.getId(),null);
    				}
    			});
			}else {
				Long daysDiff = DateUtil.daysBetween(lastUpdatedDate, new Date());
				HashMap<String,String> tags = new  HashMap<String,String>();
   			 	tags.put("<CLIENT>", clients.getPropertyName());
   			 	tags.put("<INTERVAL>",daysDiff.toString());
   			 	AlertType alertType = alertTypeRepository.findByCodeAndStatus(alertTypeCode, ConstantUtil.ACTIVE);
   			 	createAlerts(alertType, tags,clients.getId(),null);
			}
		}
	}
	
	public void createAlertsforUser(Accounts accounts,String alertTypeCode, Integer dayDiff,Integer passExpiryDays) {
		AlertConfiguration  alertConfiguration  = alertConfigurationRepository.findByAccountIdAndStatusAndAlertType(accounts.getAccountId(), ConstantUtil.ACTIVE, alertTypeCode);
		if(alertConfiguration!=null) {
				HashMap<String,String> tags = new  HashMap<String,String>();
   			 	tags.put("<USER>", accounts.getAccountFirstName());
   			 	tags.put("<DAYDIFF>", dayDiff.toString());
   			    tags.put("<PASSEXPDAY>", passExpiryDays.toString());
   			    Integer daysRem = passExpiryDays - dayDiff;
   			    tags.put("<DAYSREMN>", daysRem.toString());
   			 	AlertType alertType = alertTypeRepository.findByCodeAndStatus(alertTypeCode, ConstantUtil.ACTIVE);
   			 	createAlerts(alertType, tags,null,accounts.getAccountId());
		}
	}
	
	public void createAlertsforClientRefreshData(Clients clients, String alertTypeDemoCode,String alertTypeCode) {
		AlertConfiguration  alertConfiguration  = alertConfigurationRepository.findByClientIdAndAlertType(clients.getId(),alertTypeCode);
		if(alertConfiguration!=null) {
			List<AlertConditionType> alertConditionTypeList = alertConditionTypeRepository.findByConfigIdAndStatus(alertConfiguration.getId(),ConstantUtil.ACTIVE);
			if(alertConditionTypeList!=null && alertConditionTypeList.size()>0) {
    			alertConditionTypeList.forEach(alcond->{
    				if(DateUtil.isDateBetweenTwoDates(alcond.getStartDate(), new Date(), alcond.getEndDate())) {
						HashMap<String,String> tags = new  HashMap<String,String>();
						tags.put("<CLIENT>", clients.getPropertyName());
						tags.put("<DATE>", DateUtil.currentformatDateTime(ConstantUtil.DDMMYYYYHHMMSS));
						tags.put("<ALTTYPE>", getAlertType(alertTypeDemoCode));
						AlertType alertType = alertTypeRepository.findByCodeAndStatus(alertTypeCode, ConstantUtil.ACTIVE);
						createAlerts(alertType, tags, clients.getId(),null);
    				}
    			});
			}else {
				HashMap<String,String> tags = new  HashMap<String,String>();
   			 	tags.put("<CLIENT>", clients.getPropertyName());
   			 	tags.put("<DATE>", DateUtil.currentformatDateTime(ConstantUtil.DDMMYYYYHHMMSS));
   			    tags.put("<ALTTYPE>", getAlertType(alertTypeDemoCode));
   			 	AlertType alertType = alertTypeRepository.findByCodeAndStatus(alertTypeCode, ConstantUtil.ACTIVE);
   			 	createAlerts(alertType, tags,clients.getId(),null);
			}
		}
	}
	
	public Response createAlerts(AlertType alertType, HashMap<String,String> tags,Integer clientId,Integer accountId) {
		Response response = new Response();
		try {
			if(clientId!=null) {
				List<AlertLogs> alertLogsList = alertsLogRepository.findByClientIdAndStatusAndType(clientId, ConstantUtil.ACTIVE,alertType.getCode());
				alertLogsList.forEach(x->{
					x.setReadStatus(ConstantUtil.READ);
					x.setStatus(ConstantUtil.INACTIVE);
					alertsLogRepository.save(x);
				});
				AlertLogs alertLogs = new AlertLogs();
				alertLogs.setType(alertType.getCode());
				String textBody = replaceNameTags(alertType.getBody(), tags);
				alertLogs.setBody(textBody);
				alertLogs.setReadStatus(ConstantUtil.UNREAD);
				alertLogs.setStatus(ConstantUtil.ACTIVE);
				alertLogs.setClientId(clientId);
				alertLogs.setSubject(alertType.getSubject());
				alertLogs.setCreatedBy("0");	
				alertsLogRepository.save(alertLogs);
				response.setMessage(Status.SUCCESS.toString());
				response.setStatus(Status.SUCCESS);
			}
			if(accountId!=null) {
				List<AlertLogs> alertLogsList = alertsLogRepository.findByAccountIdAndStatusAndType(accountId, ConstantUtil.ACTIVE,alertType.getCode());
				alertLogsList.forEach(x->{
					x.setReadStatus(ConstantUtil.READ);
					x.setStatus(ConstantUtil.INACTIVE);
					alertsLogRepository.save(x);
				});
				AlertLogs alertLogs = new AlertLogs();
				alertLogs.setType(alertType.getCode());
				String textBody = replaceNameTags(alertType.getBody(), tags);
				alertLogs.setBody(textBody);
				alertLogs.setReadStatus(ConstantUtil.UNREAD);
				alertLogs.setStatus(ConstantUtil.ACTIVE);
				alertLogs.setAccountId(accountId);
				alertLogs.setSubject(alertType.getSubject());
				alertLogs.setCreatedBy("0");	
				alertsLogRepository.save(alertLogs);
				response.setMessage(Status.SUCCESS.toString());
				response.setStatus(Status.SUCCESS);
			}
		}catch(Exception ce) {
			ce.printStackTrace();
			response.setMessage(Status.FAILED.toString());
			response.setStatus(Status.FAILED);
		}
		return response;	
	}
	
	public String replaceNameTags (String body, HashMap<String,String> tags) {	
    	for (Map.Entry<String, String> set : tags.entrySet()) {
    		String replaceString= body.replaceAll(set.getKey(), set.getValue());
    		body = replaceString;
		}
    	return body;
    }

	public JQGridDTO<AlertType> getAlertTypeList(Integer pagenum, Integer pagesize, String name, String status,HttpServletRequest request) {
		List<AlertType> alertList = new ArrayList<AlertType>();
		JQGridDTO<AlertType> gridData = new JQGridDTO<AlertType>();
		try {
			Pageable pageable = PageRequest.of(pagenum, pagesize);
			Page<AlertType> alerts =  null;
			alerts = customRepository.findAlertTypeByConditions(pagenum, pagesize, name, status, pageable);
			gridData.setPage(Integer.valueOf(pagenum));
			gridData.setTotal(String.valueOf(alerts.getTotalElements()));
			gridData.setRows(String.valueOf(Math.ceil((double) alerts.getTotalElements() / pagesize)));
			alerts.stream().forEach(al->{
				al.setStatus(applicationParametersRepository.findOneByCode(al.getStatus()).getShortDesc());
				alertList.add(al);
			});
			gridData.setRecords(alertList);
		}
		  catch(Exception e) {
			  logger.error("AlertService - getAlertTypeList",e.getMessage());
		  }
		  return gridData;
	}
	
	public String getAlertType(String type) {
		String alertType="";
		switch (type) {
		case ConstantUtil.BOOKFAIL:
			alertType = "Booking";
			break;
		case ConstantUtil.CALFAIL:
			alertType = "Calibration";		
			break;
		case ConstantUtil.INVENTFAIL:
			alertType = "Inventory";
			break;
		case ConstantUtil.RATEINVFAIL:
			alertType = "Rate Inventory";
			break;
		case ConstantUtil.RATEPUSHFAIL:
			alertType = "Rate Push";
			break;
		case ConstantUtil.RECFAIL:
			alertType = "Recommendation";
			break;
		case ConstantUtil.ROOMINVFAIL:
			alertType = "Room Inventory";
			break;

		default:
			break;
		}
		return alertType;
	}
	
	public String prepareErrorMsg(String errorMsg) {
		String s = "";
		if(errorMsg!=null) {
			if(errorMsg.length()>100) {
				s = errorMsg.substring(0, 100);
				s = s + "...";
			}else {
				s = errorMsg;
			}
		}else {
			s = "Currently no details available";
		}
		return s;
	}

	public Response addUpdateAlertType(AlertTypeDto alertTypeDto) {
		Response response = new Response();
		AlertType alertType = null;
		try {
			if(alertTypeDto.getId()!=null) {
				Optional<AlertType> alertTypeOpt = alertTypeRepository.findById(alertTypeDto.getId());
				if(alertTypeOpt.isPresent()) {
					alertType = alertTypeOpt.get(); 
					alertType.setBody(alertTypeDto.getBody());
					alertType.setCode(alertTypeDto.getCode());
					alertType.setName(alertTypeDto.getName());
					alertType.setSubject(alertTypeDto.getSubject());
					alertType.setStatus(alertTypeDto.getStatus());				
				}
			}else {
				alertType = new AlertType();
				alertType.setBody(alertTypeDto.getBody());
				alertType.setCode(alertTypeDto.getCode());
				alertType.setName(alertTypeDto.getName());
				alertType.setSubject(alertTypeDto.getSubject());
				alertType.setStatus(alertTypeDto.getStatus());		
			}			
			alertTypeRepository.save(alertType);
			response.setStatus(Status.SUCCESS);
			response.setMessage(Status.SUCCESS.toString());
		}catch(Exception ce) {			
			response.setStatus(Status.FAILED);
			response.setMessage(Status.FAILED.toString());
			ce.printStackTrace();
		}	
		return response;	
	}
	
	public Response createAlertConfig(Integer clientId,Integer accountId){
		Response response = new Response();
		try {
	        List<AlertConfiguration> pr = new ArrayList<AlertConfiguration>();
	        if(clientId!=null) {
		        pr.addAll(alertConfigurationRepository.findByClientIdAndStatus(0,ConstantUtil.ACTIVE).stream().map((p)->{
		        	AlertConfiguration par = new AlertConfiguration();
		        	par.setAccountId(null);
		        	par.setAlertType(p.getAlertType());
		        	par.setClientId(clientId);
		        	par.setStatus(ConstantUtil.ACTIVE);
		            return par;
		        }).collect(Collectors.toList()));
	        }
	        if(accountId!=null) {
		        pr.addAll(alertConfigurationRepository.findByAccountIdAndStatus(0,ConstantUtil.ACTIVE).stream().map((p)->{
		        	AlertConfiguration par = new AlertConfiguration();
		        	par.setAccountId(accountId);
		        	par.setAlertType(p.getAlertType());
		        	par.setClientId(null);
		        	par.setStatus(ConstantUtil.ACTIVE);
		            return par;
		        }).collect(Collectors.toList()));
	        }
	        alertConfigurationRepository.saveAll(pr);
	        response.setMessage(Status.SUCCESS.toString());
			response.setStatus(Status.SUCCESS);
		}catch(Exception ce) {
			ce.printStackTrace();
			response.setMessage(Status.FAILED.toString());
			response.setStatus(Status.FAILED);
		}
		return response;
	}
	
	public Response createAlertConfigforAllClientsAndAccounts(){
		Response response = new Response();
		try {
			List<AlertConfiguration> acList = new ArrayList<AlertConfiguration>();
			List<Clients> clientList = clientsRepository.findByStatus(ConstantUtil.ACTIVE);
			clientList.stream().forEach(cl->{
		        List<AlertConfiguration> pr = alertConfigurationRepository.findByClientIdAndStatus(0,ConstantUtil.ACTIVE);
		        for(AlertConfiguration p :pr) {
		        	AlertConfiguration ac = alertConfigurationRepository.findByClientIdAndAlertType(cl.getId(), p.getAlertType());
		        	if (ac==null) {
			        	AlertConfiguration par = new AlertConfiguration();
			        	par.setAccountId(null);
			        	par.setAlertType(p.getAlertType());
			        	par.setClientId(cl.getId());
			        	par.setStatus(ConstantUtil.ACTIVE);
			        	acList.add(par);
		        	}
		        }
		        
			});
	        
			accountsRepository.findByStatus(ConstantUtil.ACTIVE).stream().forEach(ac->{
				List<AlertConfiguration> pr = alertConfigurationRepository.findByAccountIdAndStatus(0,ConstantUtil.ACTIVE);
		        for(AlertConfiguration p :pr) {
		        	AlertConfiguration aconf = alertConfigurationRepository.findByAccountIdAndAlertType(ac.getAccountId(), p.getAlertType());
		        	if (aconf==null) {
			        	AlertConfiguration par = new AlertConfiguration();
			        	par.setAccountId(ac.getAccountId());
			        	par.setAlertType(p.getAlertType());
			        	par.setClientId(null);
			        	par.setStatus(ConstantUtil.ACTIVE);
			        	acList.add(par);
		        	}
		        }
			});
			alertConfigurationRepository.saveAll(acList);
			response.setMessage(Status.SUCCESS.toString());
			response.setStatus(Status.SUCCESS);
		}catch(Exception ce) {
			ce.printStackTrace();
			response.setMessage(Status.FAILED.toString());
			response.setStatus(Status.FAILED);
		}
		return response;
	}
	
	String prepareScheduledCheckMsg(long daysDiff,String type,String clientName){
		String result = "";
		switch(type) {
			case "booking" : result = "Booking is not updated for client"+clientName+"from last "+daysDiff+"days";
					break;
			case "rates" : result = "Rate is not updated for client"+clientName+"from last "+daysDiff+"days";
					break;
			case "rateshop": result = "RateShop is not updated for client"+clientName+"from last "+daysDiff+"days";
					break;
		}
		return result;
	}
}
