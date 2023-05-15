package com.revnomix.revseed.schedular.Service;

import java.util.Date;


import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.revnomix.revseed.model.ApplicationParameters;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.SchedulerTimingAlt;
import com.revnomix.revseed.repository.ApplicationParametersRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.SchedulerTimingAltRepository;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.integration.eglobe.EGlobeSchedulerService;
import com.revnomix.revseed.integration.eglobe.GenericSchedulerService;
import com.revnomix.revseed.integration.service.EzeeInboundSchedulerService;
import com.revnomix.revseed.integration.service.StaahInboundSchedulerService;
import com.revnomix.revseed.integration.service.StaahMaxInboundSchedulerService;

@Service
public class DataSyncScheduleService {



	@Autowired
	SchedulerTimingAltRepository schedulerTimingRepository;
	
	@Autowired
	ClientsRepository clientsRepository;
	
	@Autowired
	EzeeInboundSchedulerService ezeeInboundSchedulerService;
	
	@Autowired
	StaahInboundSchedulerService staahInboundSchedulerService;
	
	@Autowired
	StaahMaxInboundSchedulerService staahMaxInboundSchedulerService;
	
	@Autowired
	EGlobeSchedulerService eGlobeSchedulerService;
	
	@Autowired
	ApplicationParametersRepository applicationParameterRepository;
	
	@Autowired
	ParameterRepository parameterRepository;
	
	@Autowired
	GenericSchedulerService genericSchedulerService;
	
    @Value("${scheduler.flag:YES}")
    private String schedulerFlag;
    
	@Scheduled(cron = ConstantUtil.TEN_MIN_CRON)
	public void scheduleTimerDataSync() {
		if(schedulerFlag.equalsIgnoreCase(ConstantUtil.YES)) {
			ApplicationParameters parameterProp = applicationParameterRepository.findOneByCode(ConstantUtil.SCHEDULER_PROPERTY);
			String paramvalue=""; 
			if(parameterProp!=null) {
				paramvalue = parameterProp.getCodeDesc();
			}
			List<String> paramList = new ArrayList<String>(Arrays.asList(paramvalue.split(",")));
			if(paramList.size()>0) {
				Thread thread2 = new Thread() {
				    public void run() {
				    	ApplicationParameters isOldDataSync = applicationParameterRepository.findOneByCode(ConstantUtil.OLD_DATA_SYNC); 
						if(isOldDataSync!=null && parameterProp.getCodeDesc().equals("NO")) {
							runDataMultiProcessing (paramList);
						}else {
							runData(paramList);
						}
				    	
				    }
				};
				thread2.start();
			}
		}
	}
	
	void runData (List<String> paramList) {
		Calendar currentTimeNow = Calendar.getInstance();
		Date currentTime = currentTimeNow.getTime();
		currentTimeNow.add(Calendar.MINUTE, 10);	
		Date tenMinsFromNow = (Date) currentTimeNow.getTime();
		Time startTime = new Time(currentTime.getTime());
		Time endTime = new Time(tenMinsFromNow.getTime());
		List<SchedulerTimingAlt> scheduledTimingListRateSync = schedulerTimingRepository.findAllbyTimingRec("12:01:00", "12:11:00",ConstantUtil.ACTIVE,ConstantUtil.DATA);
		for(SchedulerTimingAlt scp : scheduledTimingListRateSync) {
			scp.setIsRunToday(true);
			schedulerTimingRepository.save(scp);
		}
		for(SchedulerTimingAlt sctrs : scheduledTimingListRateSync) {
				Clients clients = clientsRepository.findOneById(sctrs.getClientId().intValue());
				List<String> refreshCodeList = new ArrayList<String>();
				if(paramList.contains(ConstantUtil.REC) && clients.getRunRecommendation()!=null && clients.getRunRecommendation().equals(ConstantUtil.YES)) {
					String channelMan = clients.getChannelManager();
					if(channelMan.equals(ConstantUtil.EZEE)) {				
				        refreshCodeList.add(ConstantUtil.FETCH_BOOKING_DATA);
				        refreshCodeList.add(ConstantUtil.FETCH_INVENTORY);
				        refreshCodeList.add(ConstantUtil.FETCH_VIEW_RATE);
				        refreshCodeList.add(ConstantUtil.FETCH_ROOM_MAPPING);
						ezeeInboundSchedulerService.createScheduledJob(clients,refreshCodeList);
					} else if (channelMan.equals(ConstantUtil.STAAH_ALL)) {
				        refreshCodeList.add(ConstantUtil.FETCH_INVENTORY);
				        refreshCodeList.add(ConstantUtil.FETCH_VIEW_RATE);
				        refreshCodeList.add(ConstantUtil.FETCH_ROOM_MAPPING);
						staahMaxInboundSchedulerService.createScheduledJob(clients,refreshCodeList);
					} else if (channelMan.equals(ConstantUtil.STAAH)) {
				        refreshCodeList.add(ConstantUtil.FETCH_INVENTORY);
				        refreshCodeList.add(ConstantUtil.FETCH_VIEW_RATE);
				        refreshCodeList.add(ConstantUtil.FETCH_ROOM_MAPPING);
						staahInboundSchedulerService.createScheduledJob(clients,refreshCodeList);
					} else if (channelMan.equals(ConstantUtil.EGLOBE)) {
						refreshCodeList.add(ConstantUtil.FETCH_INVENTORY);
				        refreshCodeList.add(ConstantUtil.FETCH_BOOKING_DATA);
				        refreshCodeList.add(ConstantUtil.FETCH_ROOM_MAPPING);
				        eGlobeSchedulerService.createScheduledJob(clients,refreshCodeList);
					}
					schedulerTimingRepository.save(sctrs);
				}
		}
	}
	
	void runDataMultiProcessing (List<String> paramList) {
		Calendar currentTimeNow = Calendar.getInstance();
		Date currentTime = (java.sql.Date) currentTimeNow.getTime();
		List<Clients> clientsList = new ArrayList<Clients>();
		currentTimeNow.add(Calendar.MINUTE, 10);	
		Date tenMinsFromNow = (java.sql.Date) currentTimeNow.getTime();
		Time startTime = new Time(currentTime.getTime());
		Time endTime = new Time(tenMinsFromNow.getTime());
		List<SchedulerTimingAlt> scheduledTimingListRateSync = schedulerTimingRepository.findAllbyTimingRec(startTime.toString(), endTime.toString(),ConstantUtil.ACTIVE,ConstantUtil.DATA);
		for(SchedulerTimingAlt scp : scheduledTimingListRateSync) {
			Clients client = clientsRepository.findOneById(scp.getClientId().intValue());
			scp.setIsRunToday(true);
			schedulerTimingRepository.save(scp);
			clientsList.add(client);
		} 

		List<String> refreshCodeList = new ArrayList<String>();
		refreshCodeList.add(ConstantUtil.FETCH_ROOM_MAPPING);
		refreshCodeList.add(ConstantUtil.FETCH_INVENTORY);
		refreshCodeList.add(ConstantUtil.FETCH_VIEW_RATE);
		refreshCodeList.add(ConstantUtil.FETCH_BOOKING_DATA);		
		genericSchedulerService.createMultipleScheduledJob(clientsList, refreshCodeList,false);
	}
}
