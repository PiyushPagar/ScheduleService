package com.revnomix.revseed.schedular.Service;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.revnomix.revseed.model.AccountPassword;
import com.revnomix.revseed.model.ApplicationParameters;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.Hotels;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.SchedulerTimingAlt;
import com.revnomix.revseed.repository.AccountPasswordRepository;
import com.revnomix.revseed.repository.AccountsRepository;
import com.revnomix.revseed.repository.ApplicationParametersRepository;
import com.revnomix.revseed.repository.BookingsRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.HotelsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.SchedulerTimingAltRepository;
import com.revnomix.revseed.wrapper.BookingsDto;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Service.EmailReportServiceImpl;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.service.EzeeInboundSchedulerService;



@Service
public class ScheduledService {

	@Autowired
	private SchedulerTimingAltRepository schedulerTimingRepository;
	
	@Autowired
	private ClientsRepository clientsRepository;
	
	@Autowired
	private EzeeInboundSchedulerService ezeeInboundSchedulerService;
	
	@Autowired
	private ApplicationParametersRepository applicationParameterRepository;
	
	@Autowired
	private ParameterRepository parameterRepository;
	
	@Autowired
	private EmailReportServiceImpl emailReportServiceImpl;
	
	@Autowired
	private AccountPasswordRepository accountPasswordRepository;
	
	@Autowired
	private AccountsRepository accountsRepository;
	
	@Autowired
	private AlertService alertService;
	
    
    @Autowired
    private BookingsRepository bookingsRepository;
    
    @Autowired
    private HotelsRepository hotelsRepository;
	
    @Value("${scheduler.flag:YES}")
    private String schedulerFlag;
    
    @Value("${scheduler.flagPassword:YES}")
    private String schedulerFlagPasswordAlert;
	
	@Scheduled(cron = ConstantUtil.TEN_MIN_CRON)
	public void scheduleTimerRecommendation () {
		if(schedulerFlag.equalsIgnoreCase(ConstantUtil.YES)) {
			ApplicationParameters parameterProp = applicationParameterRepository.findOneByCode(ConstantUtil.SCHEDULER_PROPERTY);
			String paramvalue=""; 
			if(parameterProp!=null) {
				paramvalue = parameterProp.getCodeDesc();
			}
			List<String> paramList = new ArrayList<String>(Arrays.asList(paramvalue.split(",")));
			if(paramList.size()>0) {
				Thread thread1 = new Thread() {
				    public void run() {
				    	runISellRepAndRec(paramList);
				    }
				};
				thread1.start();
			}
		}
	}
	@Scheduled(cron = ConstantUtil.EVERYDAY_AT_THREE_PM_CRON)
	public void dataTimingAlertFunction() {
		if(schedulerFlag.equalsIgnoreCase(ConstantUtil.YES)) {
			try {
				List<Clients> clients = clientsRepository.findByStatus(ConstantUtil.ACTIVE);
				clients.stream().forEach(cl->{
					 BookingsDto bookingsDto = bookingsRepository.findBookingDetails(cl.getId());
					 if(bookingsDto!=null && bookingsDto.getModDate()!=null){
						 Date bookingDate = bookingsDto.getModDate();		
						  long bookingDateRange = DateUtil.daysBetween(bookingDate, new Date());
						  if(bookingDateRange>1) {
							  alertService.createScheduledCheckAlertsforClient(cl,ConstantUtil.BOOKINGCHECKALRT,bookingDate);
						  }
					 }
					 List<Date> rateSyncedOnDateList = bookingsRepository.findByClientIdRateList(cl.getId());
						  if(rateSyncedOnDateList!=null && rateSyncedOnDateList.size() >0) {
						  Date rateSyncedOnDate = rateSyncedOnDateList.stream().max(Date::compareTo).get();
						  long rateSynchedOnRange = DateUtil.daysBetween(rateSyncedOnDate, new Date());
						  if(rateSynchedOnRange>1) {
							  alertService.createScheduledCheckAlertsforClient(cl,ConstantUtil.RATECHECKALRT,rateSyncedOnDate);
						  }
					 }
				     Hotels hotel = hotelsRepository.findOneById(cl.getHotelId());
				     if(hotel!=null && hotel.getRmCode()!=null) {
						  List<Date> ratesShopedOnDateList = bookingsRepository.findByClientIdRateShopDateList(hotel.getRmCode());					  
						  if(ratesShopedOnDateList!=null && ratesShopedOnDateList.size() >0) {
							  Date ratesShopedOnDate = ratesShopedOnDateList.stream().max(Date::compareTo).get();
							  long ratesShopedOnDateRange = DateUtil.daysBetween(ratesShopedOnDate, new Date());
							  if(ratesShopedOnDateRange>2) {
								  alertService.createScheduledCheckAlertsforClient(cl,ConstantUtil.RATESHOPCHECKALRT,ratesShopedOnDate);
							  }
						  }
				     }
				});
			}catch(Exception ce) {
				ce.printStackTrace();
			}
		}
	}
	
	@Scheduled(cron = ConstantUtil.EVERYDAY_AT_ELEVEN_CRON)
	public void passwordAlertFunction() {
		if(schedulerFlagPasswordAlert.equalsIgnoreCase(ConstantUtil.YES)) {
			try {
			accountsRepository.findAll().forEach(acc->{
				List<AccountPassword> accountPassList =  accountPasswordRepository.findByUserId(acc.getAccountId());
				if(accountPassList!=null && accountPassList.size()>0) {
			    	Comparator<AccountPassword> comparator = (c1, c2) -> { 
			            return Long.valueOf(c1.getUpdatedDate().getTime()).compareTo(c2.getUpdatedDate().getTime()); 
			    	};
			    	Collections.sort(accountPassList, comparator);
			    	AccountPassword accPass = accountPassList.get(0);
			    	ApplicationParameters passExpiryDays = applicationParameterRepository.findOneByCode(ConstantUtil.PASSWORD_EXPIRY_DAYS);
			    	ApplicationParameters passExpiryIntervals = applicationParameterRepository.findOneByCode(ConstantUtil.PASSWORD_EXPIRY_INTERVALS);
			    	ApplicationParameters passExpiryStartDaysBefore = applicationParameterRepository.findOneByCode(ConstantUtil.PASS_EXPIRY_NOT_START_DAYS_BEFORE);
			    	Integer passExpiryDaysValue=60; 
			    	Integer passExpiryIntervalsValue=2;
			    	Integer passExpiryStartDaysBeforeValue=54;
					if(passExpiryDays!=null) {
						passExpiryDaysValue = Integer.parseInt(passExpiryDays.getCodeDesc());
					}
					if(passExpiryIntervals!=null) {
						passExpiryIntervalsValue = Integer.parseInt(passExpiryIntervals.getCodeDesc());
					}
					if(passExpiryStartDaysBefore!=null) {
						passExpiryStartDaysBeforeValue = Integer.parseInt(passExpiryStartDaysBefore.getCodeDesc());
					}
			    	Long dayDiff = DateUtil.daysBetween(accPass.getUpdatedDate(), new Date());
			    	if(dayDiff>=passExpiryStartDaysBeforeValue && dayDiff<=passExpiryDaysValue) {
			    		Long num = passExpiryDaysValue%dayDiff;
			    		if (num % passExpiryIntervalsValue == 0) {
			    			alertService.createAlertsforUser(acc,ConstantUtil.PASSWORD_EXPIRY_ALERT, dayDiff.intValue(),passExpiryDaysValue);
			    		}
			    	}
				}
			});
			}catch(Exception ce) {
				ce.printStackTrace();
			}
		}
	}
	
	@Scheduled(cron = ConstantUtil.TEN_MIN_CRON)
	public void scheduleTimerCaliberation() {
		if(schedulerFlag.equalsIgnoreCase(ConstantUtil.YES)) {
			ApplicationParameters parameterProp = applicationParameterRepository.findOneByCode(ConstantUtil.SCHEDULER_PROPERTY);
			String paramvalue=""; 
			if(parameterProp!=null) {
				paramvalue = parameterProp.getCodeDesc();
			}
			List<String> paramList = new ArrayList<String>(Arrays.asList(paramvalue.split(",")));
			if(paramList.size()>0) {
				Thread thread3 = new Thread() {
				    public void run() {
				    	runCal(paramList);
				    }
				};
				thread3.start();
			}
		}
	}

	
	void runCal (List<String> paramList) {
		Calendar currentTimeNow = Calendar.getInstance();
		Date currentTime = currentTimeNow.getTime();
		currentTimeNow.add(Calendar.MINUTE, 10);	
		Date tenMinsFromNow = currentTimeNow.getTime();
		Time startTime = new Time(currentTime.getTime());
		Time endTime = new Time(tenMinsFromNow.getTime());
		if(paramList.contains(ConstantUtil.CAL)) {
			List<SchedulerTimingAlt> scheduledCaliberationList = schedulerTimingRepository.findAllbyTimingRec(startTime.toString(), endTime.toString(),ConstantUtil.ACTIVE,ConstantUtil.CAL);
			for(SchedulerTimingAlt scp : scheduledCaliberationList) {
				scp.setIsRunToday(true);
				schedulerTimingRepository.save(scp);
			}
			for(SchedulerTimingAlt sc :scheduledCaliberationList) {
				Clients clients = clientsRepository.findOneById(sc.getClientId().intValue());
				if(clients.getRunCalibration()!=null && clients.getRunCalibration().equals(ConstantUtil.YES)) {
					ezeeInboundSchedulerService.runCalibration(sc.getClientId().intValue());
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	void runISellRepAndRec (List<String> paramList){
		Calendar currentTimeNow = Calendar.getInstance();
		Date currentTime = currentTimeNow.getTime();
		currentTimeNow.add(Calendar.MINUTE, 10);	
		Date tenMinsFromNow = currentTimeNow.getTime();
		Time startTime = new Time(currentTime.getTime());
		Time endTime = new Time(tenMinsFromNow.getTime());
		List<SchedulerTimingAlt> scheduledTimingsList = schedulerTimingRepository.findAllbyTimingRec(startTime.toString(), endTime.toString(),ConstantUtil.ACTIVE,ConstantUtil.REC);
		for(SchedulerTimingAlt scr : scheduledTimingsList) {
			scr.setIsRunToday(true);
			schedulerTimingRepository.save(scr);
		}
		for(SchedulerTimingAlt sct : scheduledTimingsList) {
			Clients clients = clientsRepository.findOneById(sct.getClientId().intValue());
			if(paramList.contains(ConstantUtil.REC) && clients.getRunRecommendation()!=null && clients.getRunRecommendation().equals(ConstantUtil.YES)) {
				ezeeInboundSchedulerService.runRecommendation(sct.getClientId().intValue());
			}
			Parameter isellrepParam= parameterRepository.findByClientIdAndParamName(sct.getClientId().intValue(), ConstantUtil.AUTOISELLREPORT).orElse(null);
			if(paramList.contains(ConstantUtil.ISELLREP) && clients.getRunRecommendation()!=null && clients.getRunRecommendation().equals(ConstantUtil.YES)) {
		    	if(isellrepParam!=null && isellrepParam.getParamValue().equals(ConstantUtil.YES)) {
		    		emailReportServiceImpl.sendISellReportMail(sct.getClientId().intValue());
		    	}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}	
	
}