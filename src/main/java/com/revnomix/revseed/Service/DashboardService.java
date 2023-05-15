package com.revnomix.revseed.Service;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.revnomix.revseed.model.Accounts;
import com.revnomix.revseed.model.BookingPaceOccupancyByDate;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.HnfType;
import com.revnomix.revseed.model.Hotels;
import com.revnomix.revseed.model.SeasonalityDefinitions;
import com.revnomix.revseed.repository.AccountsRepository;
import com.revnomix.revseed.repository.BookingPaceOccupancyByDateRepository;
import com.revnomix.revseed.repository.BookingsRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.CustomRepository;
import com.revnomix.revseed.repository.HotelsRepository;
import com.revnomix.revseed.repository.OverrideRepository;
import com.revnomix.revseed.repository.RecommendationCaliberationLogRepository;
import com.revnomix.revseed.repository.RecommendationToShowRepository;
import com.revnomix.revseed.repository.RmRatesRepository;
import com.revnomix.revseed.repository.SeasonalityDefinitionsRepository;
import com.revnomix.revseed.repository.StaahRatesRepository;
import com.revnomix.revseed.wrapper.BookingsDto;
import com.revnomix.revseed.wrapper.CompetitorPricingDto;
import com.revnomix.revseed.wrapper.OtaPerformanceDto;
import com.revnomix.revseed.wrapper.OverrideStrageyDto;
import com.revnomix.revseed.wrapper.analysis.PercentileDto;
import com.revnomix.revseed.wrapper.dashboard.DashboardMonitoringDto;
import com.revnomix.revseed.wrapper.dashboard.DashboardOtaPerformanceDto;
import com.revnomix.revseed.wrapper.dashboard.JQGridDTO;
import com.revnomix.revseed.wrapper.dashboard.PickupTrendDto;
import com.revnomix.revseed.wrapper.dashboard.PickupValueDto;
import com.revnomix.revseed.wrapper.dashboard.TrendByOtaDto;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.SystemUtil;

@Service
public class DashboardService {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BookingPaceOccupancyByDateRepository paceOccupancyByDateRepository;
    
    @Autowired
    private BookingsRepository bookingsRepository;
    
    @Autowired
    private SystemUtil systemUtil;
    
    @Autowired
    private RmRatesRepository rmRatesRepository;
    
    @Autowired
    private ClientsRepository clientsRepository;
    
    @Autowired
    private HotelsRepository hotelsRepository;
    
    @Autowired
    private SeasonalityDefinitionsRepository seasonalityDefinitionsRepository;
    
    @Autowired
    private OverrideRepository overrideRepository;
    
    @Autowired
    private RecommendationCaliberationLogRepository recommendationCaliberationLogRepository;
    
    @Autowired
    private RecommendationToShowRepository recommendationToShowRepository;
    
    @Autowired
    private AccountsRepository accountsRepository;
    
    @Autowired
    private CustomRepository clientsCustomRepository;
    
 
    
    @Autowired
    private StaahRatesRepository staahRatesRepository;
    
	 public DashboardOtaPerformanceDto getCardNumbersFromOta(Integer clientId) {
	    	 DashboardOtaPerformanceDto dashboardOtaPerformanceDto = new DashboardOtaPerformanceDto();
	    	 try {
	    	 	 DateTime systemToday = new DateTime(systemUtil.getSystemToday(clientId));
		         Date yesterday = DateUtil.minusDay(systemToday, 1);
		         Date toDate = DateUtil.convertDateTimeToDate(systemToday);
		         Date firstDateofMonth = DateUtil.getFirstDateOfMonth(systemToday);
		         Date lastDateofMonth = DateUtil.getLastDateOfMonth(systemToday);
		         Date lastMonthFirstDate =  DateUtil.getLastMonthFirstDate(systemToday);
		         Date lastMonthlastDate =  DateUtil.getLastMonthLastDate(systemToday);
		         PickupValueDto yesterdays  = bookingsRepository.findPickupDuration(clientId, yesterday, yesterday,ConstantUtil.CANCELLED_STATUS);
	             OtaPerformanceDto yesterdaysOtaPerformance = paceOccupancyByDateRepository.getSumOfOtaPerformance(clientId, yesterday,yesterday);
	        	 yesterdaysOtaPerformance.setPickup(yesterdays.getPickup());
	        	 yesterdaysOtaPerformance.setGrossPickup(bookingsRepository.findGrossPickupDuration(clientId, yesterday, yesterday).getPickup());
	             dashboardOtaPerformanceDto.setYesterdaysOta(yesterdaysOtaPerformance);
	             PickupValueDto todaysPickup = bookingsRepository.findPickupDuration(clientId, toDate, toDate,ConstantUtil.CANCELLED_STATUS);
	             OtaPerformanceDto todaysOtaPerformance =paceOccupancyByDateRepository.getSumOfOtaPerformance(clientId, toDate,toDate);
	             todaysOtaPerformance.setPickup(todaysPickup.getPickup());
	             todaysOtaPerformance.setGrossPickup(bookingsRepository.findGrossPickupDuration(clientId, toDate, toDate).getPickup());
	             dashboardOtaPerformanceDto.setTodaysOta(todaysOtaPerformance);
	             PickupValueDto currentMonthSumOfPickup =bookingsRepository.findPickupDuration(clientId,firstDateofMonth,lastDateofMonth,ConstantUtil.CANCELLED_STATUS);
	             PickupValueDto lastMonthSumOfPickup = bookingsRepository.findPickupDuration(clientId,lastMonthFirstDate,lastMonthlastDate ,ConstantUtil.CANCELLED_STATUS);
	             OtaPerformanceDto currentMonthSumOfOtaPerformance = paceOccupancyByDateRepository.getSumOfOtaPerformance(clientId,firstDateofMonth,lastDateofMonth);
	             currentMonthSumOfOtaPerformance.setPickup(currentMonthSumOfPickup.getPickup());
	             currentMonthSumOfOtaPerformance.setGrossPickup(bookingsRepository.findGrossPickupDuration(clientId, firstDateofMonth, lastDateofMonth).getPickup());
	             OtaPerformanceDto lastMonthSumOfOtaPerformance = paceOccupancyByDateRepository.getSumOfOtaPerformance(clientId, lastMonthFirstDate, lastMonthlastDate);
	             lastMonthSumOfOtaPerformance.setPickup(lastMonthSumOfPickup.getPickup());
	             lastMonthSumOfOtaPerformance.setGrossPickup(bookingsRepository.findGrossPickupDuration(clientId, lastMonthFirstDate, lastMonthlastDate).getPickup());
	             dashboardOtaPerformanceDto.setThisMonthOta(currentMonthSumOfOtaPerformance);
	             dashboardOtaPerformanceDto.setLastMontOta(lastMonthSumOfOtaPerformance);	 	             
	    	 }
	         catch (Exception e) {
	        	 System.out.println(e);
				// TODO: handle exception
			}
	    	return dashboardOtaPerformanceDto;
	    }
	 
	  @Cacheable(cacheNames = "getCompetitorPricing", key="{ #clientId, #fromDate, #toDate }")
      public Map<String, CompetitorPricingDto> getPricing(Integer clientId, List<String> rmCodes, Date fromDate, Date toDate) {
    	  List<CompetitorPricingDto>  competitorPriceList = new ArrayList<CompetitorPricingDto>();
      competitorPriceList = rmRatesRepository.findCompetitorPricingDtos(fromDate, toDate, rmCodes,clientId);
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
      Map<Date, Map<String, List<CompetitorPricingDto>>> abv = competitorPriceList.stream().collect(Collectors.groupingBy(CompetitorPricingDto::getOccupancyDate,
      		Collectors.groupingBy(x -> x.getHotelCode().toString())));
      List<CompetitorPricingDto> lvl1 = new ArrayList <CompetitorPricingDto>();
       List<CompetitorPricingDto> lvl2 = new ArrayList <CompetitorPricingDto>();
      abv.entrySet().stream()
      .forEach(e ->{
      	TreeMap<String, List<CompetitorPricingDto>> ddmsp = new TreeMap<String, List<CompetitorPricingDto>>();
      	ddmsp.putAll( e.getValue());
      	ddmsp.entrySet().stream().forEach(r -> {	        		
      		CompetitorPricingDto minByAge = r.getValue()
      			      .stream()
      			      .min(Comparator.comparing(CompetitorPricingDto::getOnsiteRate)).get();
      		lvl1.add(minByAge);
      	});
      });
      Map<Date, List<CompetitorPricingDto>> mp= lvl1.stream().collect(Collectors.groupingBy(CompetitorPricingDto::getOccupancyDate));
      mp.entrySet().stream()
      .forEach(e ->{
      	DoubleSummaryStatistics d = e.getValue().stream()
          .mapToDouble((x) -> x.getOnsiteRate())
          .summaryStatistics();
      	CompetitorPricingDto dmp = new CompetitorPricingDto();
      	dmp.setAvg(d.getAverage());
      	dmp.setMax(d.getMax());
      	dmp.setMin(d.getMin());
      	dmp.setOccupancyDate(e.getValue().get(0).getOccupancyDate());
      	dmp.setHotelCode(e.getValue().get(0).getHotelCode());
      	//dmp.setOccupancyDateStr(DateUtil.formatDate(e.getValue().get(0).getOccupancyDate(), ConstantUtil.YYYYMMDD));
      	dmp.setOccupancyDateStr(df.format(e.getValue().get(0).getOccupancyDate()));
      	lvl2.add(dmp);
      });
      Map<String, CompetitorPricingDto> masp = lvl2.stream().collect(
  	        		Collectors.toMap(CompetitorPricingDto::getOccupancyDateStr, Function.identity()));
      TreeMap<String, CompetitorPricingDto> treemap = new TreeMap<String, CompetitorPricingDto>();
      	treemap.putAll(masp);
      	return treemap;
		}
	  
	 
}
