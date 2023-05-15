package com.revnomix.revseed.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.revnomix.revseed.model.Accounts;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.DowDefinitions;
import com.revnomix.revseed.model.EmailTemplate;
import com.revnomix.revseed.model.Hotels;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.RateHorizon;
import com.revnomix.revseed.model.RequestMessage;
import com.revnomix.revseed.model.RowStatus;
import com.revnomix.revseed.model.SeasonalityDefinitions;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.AccountsRepository;
import com.revnomix.revseed.repository.BookingPaceOccupancyByDateRepository;
import com.revnomix.revseed.repository.ClientCompetitorsRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.DowDefinitionsRepository;
import com.revnomix.revseed.repository.EmailTemplateRepository;
import com.revnomix.revseed.repository.EventRepository;
import com.revnomix.revseed.repository.HotelsRepository;
import com.revnomix.revseed.repository.OverrideRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.RateHorizonRepository;
import com.revnomix.revseed.repository.RmRatesRepository;
import com.revnomix.revseed.repository.SeasonalityDefinitionsRepository;
import com.revnomix.revseed.repository.StaahRatesRepository;
import com.revnomix.revseed.wrapper.CompetitorPricingDto;
import com.revnomix.revseed.wrapper.ISellReportDto;
import com.revnomix.revseed.wrapper.OtaPerformanceDto;
import com.revnomix.revseed.wrapper.OverrideStrageyDto;
import com.revnomix.revseed.wrapper.RateDto;
import com.revnomix.revseed.wrapper.RateHorizonDto;
import com.revnomix.revseed.wrapper.dashboard.PickupDto;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.SystemUtil;
import com.revnomix.revseed.integration.configuration.PropertiesConfig;
import com.revnomix.revseed.integration.service.Response;
import com.revnomix.revseed.integration.utility.DateTimeUtil;



@Service
public class EmailReportServiceImpl {
	
    @Autowired
    private SystemUtil sysutil;
    
    @Autowired
    private ClientsRepository clientsRepository;
    
    @Autowired
    private BookingPaceOccupancyByDateRepository paceOccupancyByDateRepository;
    
    @Autowired
    private OverrideRepository overrideRepository;
    
    @Autowired
    private RateHorizonRepository rateHorizonRepository;
    
    @Autowired
    private DowDefinitionsRepository dowDefinitionsRepository;
    
    @Autowired
    private ClientCompetitorsRepository clientCompetitorsRepository;
    
    @Autowired
    private HotelsRepository hotelsRepository;
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private StaahRatesRepository staahRatesRepository;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Qualifier("accountsRepository")
    @Autowired
    private AccountsRepository accountsRepository;
    
    @Autowired
    private SeasonalityDefinitionsRepository seasonalityDefinitionsRepository;
    
    @Autowired
    private EmailTemplateRepository emailTemplateRepository;
    
    @Autowired
    private EmailServiceImpl emailServiceImpl;
    
    @Autowired
    private PropertiesConfig config;
    
    @Autowired
    private RmRatesRepository rmRatesRepository;
    
    @Autowired
    private ParameterRepository parameterRepository;


    @Transactional
	public RequestMessage sendISellReportMail(Integer clientId) {
		RequestMessage message = new RequestMessage();
		Parameter isAccountManager = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.ISACCMANAGER).orElse(null);
		List<Accounts> accountsList = null;
		if(isAccountManager!=null) {
			if(isAccountManager.getParamValue()!=null && isAccountManager.getParamValue().equals("true")) {
				accountsList = accountsRepository.findByClientIdAndPosition(clientId,ConstantUtil.SUPER_ROLE);
			}else {
				accountsList = accountsRepository.findByClientIdAndPosition(clientId,ConstantUtil.CLIENT);
			}
			message = isellSetMail(clientId, accountsList);
		}
		return message;
	}
    
    public RequestMessage isellSetMail(Integer clientId,List<Accounts> accountsList) {
    	RequestMessage message = new RequestMessage();
		File file = createIsellReport(clientId);
		if(accountsList!=null) {
			for(Accounts acc: accountsList) {
				EmailTemplate emailtemplate = emailTemplateRepository.findByTemplateCodeAndStatus(ConstantUtil.ISELL_REPORT, true);
				try {
					HashMap<String,String> tags = new HashMap<String,String> ();
					tags.put("<ACCOUNTNAME>", acc.getAccountFirstName());
					tags.put("<DATETIME>", DateUtil.formatDate(new Date(), ConstantUtil.DDMMYYYY_SLASH));
					String textBody = emailServiceImpl.replaceNameTags(emailtemplate.getBody(), tags);					
					emailServiceImpl.sendMessageWithAttachment(acc.getEmail(),emailtemplate.getFrom(), emailtemplate.getSubject(), textBody, file);
					message.setErrorMessage("The report mail has been sent");
				} catch (Exception e) {
					message.setErrorMessage("mail not sent");
					e.printStackTrace();
				}
			}
		}
		return message;
    }
	
	public Map<String,ISellReportDto> createISellReportData (Integer clientId) {
		
		Map<String,ISellReportDto> iSellReportMapDto = new LinkedHashMap<String,ISellReportDto>();
		Clients clients = clientsRepository.findOneById(clientId);
        DateTime systemToday = new DateTime(sysutil.getSystemToday(clientId));
        Date startDate = DateUtil.minusDay(systemToday, 0);
     	DateUtil.setTimeToZero(startDate);        
     	Date endDate = DateUtil.addDay(systemToday,365);
        DateUtil.setTimeToZero(endDate);
        
        //performance
        Map<String, OtaPerformanceDto> result = paceOccupancyByDateRepository
                .findAllOtaPerformanceByClientIdAndOccupancyDate(clientId,startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        p -> DateTimeUtil.formatDate(p.getOccupancyDate(), ConstantUtil.YYYYMMDD), p -> p, (e1, e2) -> e1,
                        LinkedHashMap::new));
        
        result.forEach((key, value) -> {
        	ISellReportDto iSellReportDto = new ISellReportDto();
        	iSellReportDto.setDate(DateTimeUtil.formatDate(value.getOccupancyDate(), ConstantUtil.DDMMYYYY));
        	if(value.getSold()!=null) {
        		iSellReportDto.setSold(value.getSold().intValue());
        	}else {
        		iSellReportDto.setSold(0);
        	}
        	if(value.getAdr()!=null) {
        		iSellReportDto.setAdr(value.getAdr().intValue());
        	}else {
        		iSellReportDto.setAdr(0);
        	}
        	if(value.getRevenue()!=null) {
        		iSellReportDto.setRevenue(value.getRevenue().intValue());
        	}else {
        		iSellReportDto.setRevenue(0);
        	}
        	if(value.getCapacity()!=null) {
        		iSellReportDto.setHotelCap(clients.getCapacity().intValue());
        	}else {
        		iSellReportDto.setHotelCap(0);
        	}
        	if(value.getAvailableCapacity()!=null) {
        		iSellReportDto.setAvailCap(value.getAvailableCapacity().intValue()); 
        	}else {
        		iSellReportDto.setAvailCap(0);
        	}
        	iSellReportDto.setCompAvg(0);
        	iSellReportDto.setCompMax(0);
        	iSellReportDto.setCompMin(0);
        	iSellReportDto.setPickup(0);
        	iSellReportDto.setSysRate(0);
        	iSellReportDto.setRecommend(0);
        	LocalDate date = DateUtil.dateToLocalDate(value.getOccupancyDate());
            int weekOfYear = DateUtil.getWeekofYear(date);
            iSellReportDto.setDow(findDay(date.getDayOfWeek().name()));
      		List<SeasonalityDefinitions> def = seasonalityDefinitionsRepository.findByClientIdAndStartWeekLessThanEqualAndEndWeekGreaterThanEqualAndStartWeekNot(clientId, weekOfYear, weekOfYear, 0);
    		def.stream().forEach(as->{
    			iSellReportDto.setSeason(as.getSeasonName());
    		});
    		if(iSellReportDto.getSeason()==null) {
    			iSellReportDto.setSeason("Season 1");
    		}
        	iSellReportMapDto.put(DateTimeUtil.formatDate(value.getOccupancyDate(), ConstantUtil.YYYYMMDD), iSellReportDto);
        });

        Map<String, Integer> allPickup = getAllPickup(clientId, startDate, endDate, clients.getSystemToday());
        allPickup.forEach((key, value) -> {
        	ISellReportDto iSellReportDto = iSellReportMapDto.get(DateTimeUtil.formatDate(DateUtil.toDate(key, ConstantUtil.YYYYMMDD), ConstantUtil.YYYYMMDD));
            if (iSellReportDto != null) {
            	if(value!=null) {
            		iSellReportDto.setPickup(value.intValue());
            	}else {
            		iSellReportDto.setPickup(0);
            	}
            	iSellReportMapDto.replace(DateTimeUtil.formatDate(DateUtil.toDate(iSellReportDto.getDate(),ConstantUtil.DDMMYYYY), ConstantUtil.YYYYMMDD), iSellReportDto);
            }
        });
        
        // override strategy
        List<OverrideStrageyDto> dto = new ArrayList<>(); 
		List<OverrideStrageyDto> over = overrideRepository.getOverrideAlgoByDate(clientId,startDate, endDate); 
	    List<RateHorizon> horizon = rateHorizonRepository.findAllByClientIdAndStatus(clientId,ConstantUtil.ACTIVE);
		List<RateHorizonDto> horizonDates = formateHorizonDate(horizon);
        long noOfDays = DateUtil.daysBetween(startDate, endDate);  	  
  	  	for (int i = 0; i <= noOfDays; i++) { 	  
  		  Date date = DateUtil.addDays(startDate, i); OverrideStrageyDto
  		  overrideStrageyDto = new OverrideStrageyDto();
  		  overrideStrageyDto.setDate(date);		  
  		  over.stream().filter(a ->
  		  	  date.equals(a.getDate())).findFirst().ifPresentOrElse(o -> {
  			  overrideStrageyDto.setOverrideAlgo(o.getOverrideAlgo());
  			  overrideStrageyDto.setOverrideValue(o.getOverrideValue());
  			  overrideStrageyDto.setOverridden(true); overrideStrageyDto.setHorizon(false);
  			  }, () -> { 
  				  Optional<RateHorizonDto> hor = horizonDates.stream().filter(a ->a.getDate().equals(date)).findFirst(); 
  				  if (hor.isPresent()){
  					  overrideStrageyDto.setOverrideAlgo(hor.get().getAlgo());
  					  overrideStrageyDto.setOverridden(false); 
  					  overrideStrageyDto.setHorizon(true);
  				  }else{ 
  					  overrideStrageyDto.setOverrideAlgo(clients.getAlgo());
  					  overrideStrageyDto.setOverridden(false); 
  				  } 
  			  }); 
  		  dto.add(overrideStrageyDto); 
  	  }
  	  	
  	  dto.forEach((overdto)->{
  		ISellReportDto iSellReportDto = iSellReportMapDto.get(DateTimeUtil.formatDate(overdto.getDate(), ConstantUtil.YYYYMMDD));
        if (iSellReportDto != null) {
        	iSellReportDto.setStrategy(findStrategy(overdto.getOverrideAlgo()));
        	iSellReportMapDto.replace(DateTimeUtil.formatDate(DateUtil.toDate(iSellReportDto.getDate(),ConstantUtil.DDMMYYYY), ConstantUtil.YYYYMMDD), iSellReportDto);
        }
  	  });
		  // dow
	 List<List<DowDefinitions>> dowList = new ArrayList<>(7);
      String [] day = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
      for(Integer seasonNo=0;seasonNo<=6;seasonNo++) {
          List<DowDefinitions> dow = new ArrayList<>();
          for (int i=0; i<=6 ;i++){
              dow.add(dowDefinitionsRepository.findByClientIdAndSeasonNoAndDay(clientId,seasonNo,day[i]));
          }
          dowList.add(dow);
      }
      
      dowList.forEach((dowdtoList)->{
        	dowdtoList.forEach((dowdto)->{
		  		ISellReportDto iSellReportDto = iSellReportMapDto.get(DateTimeUtil.formatDate(dowdto.getModDate(), ConstantUtil.YYYYMMDD));
		        if (iSellReportDto != null) {
		        	iSellReportDto.setDow(findDay(dowdto.getDay()));
		        	iSellReportMapDto.replace(DateTimeUtil.formatDate(DateUtil.toDate(iSellReportDto.getDate(),ConstantUtil.DDMMYYYY), ConstantUtil.YYYYMMDD), iSellReportDto);
		        }
        	});
  	  });
      
      
      
      
      // competitor pricing
      List<String> rmCodes =  getHotelIdbyClientId (clientId);
      Map<String, CompetitorPricingDto> compPriceList = dashboardService.getPricing(clientId, rmCodes, startDate, endDate);
      
      compPriceList.forEach((key,compPrice)->{
		  		ISellReportDto iSellReportDto = iSellReportMapDto.get(DateTimeUtil.formatDate(DateUtil.toDate(key, ConstantUtil.YYYYMMDDHHMMSS), ConstantUtil.YYYYMMDD));;
		        if (iSellReportDto != null) {
		        	if(compPrice.getAvg()!=null) {
		        		iSellReportDto.setCompAvg(compPrice.getAvg().intValue());
		        	}else {
	            		iSellReportDto.setCompAvg(0);
	            	}
		        	if(compPrice.getMax()!=null) {
		        		iSellReportDto.setCompMax(compPrice.getMax().intValue());
		        	}else {
	            		iSellReportDto.setCompMax(0);
	            	}
		        	if(compPrice.getMin()!=null) {
		        		iSellReportDto.setCompMin(compPrice.getMin().intValue());
		        	}else {
	            		iSellReportDto.setCompMin(0);
	            	}
		        	iSellReportMapDto.replace(DateTimeUtil.formatDate(DateUtil.toDate(iSellReportDto.getDate(),ConstantUtil.DDMMYYYY), ConstantUtil.YYYYMMDD), iSellReportDto);
		        }
	  });
      
      Map<String,List<CompetitorPricingDto>> comprMapList = new  HashMap<String,List<CompetitorPricingDto>>();
      List<CompetitorPricingDto> competerMinRateDtoList = rmRatesRepository.findMinAllCompetitorPricingByClientIdAndOccupancyDate(clientId, startDate, endDate, clients.getPropertyName());
      List<String> compHotel = getCompetitorsbyClientId (clientId);
      compHotel.forEach(x->{
    	 List<CompetitorPricingDto> hotelComp = competerMinRateDtoList.stream().filter(dtoa->dtoa.getName().equals(x)).collect(Collectors.toList());
    	 comprMapList.put(x, hotelComp);
      });
      
      comprMapList.forEach((key,compList)->{
      	
    	  Map<String, CompetitorPricingDto> cmpDtoList = compList.stream().collect(Collectors.toMap(p -> p.getOccupancyDate().toString(), p -> p));
    	  cmpDtoList.forEach((k,cmDto)->{
    		  Map<String,Integer> compMinRate = new HashMap<String,Integer>();
    	  ISellReportDto iSellReportDto = iSellReportMapDto.get(DateTimeUtil.formatDate(DateUtil.toDate(k, ConstantUtil.YYYYMMDD), ConstantUtil.YYYYMMDD));
            if (iSellReportDto != null) {
            	if(cmDto.getMin()!=null) {
            		compMinRate.put(key,cmDto.getMin().intValue());
            	}else {
            		compMinRate.put(key,0);
            	}
            	if(iSellReportDto.getCompMinRate()!=null) {
            		iSellReportDto.getCompMinRate().putAll(compMinRate);
            		iSellReportDto.setCompMinRate(iSellReportDto.getCompMinRate());
            	}else {
            		iSellReportDto.setCompMinRate(compMinRate);
            	}
            	iSellReportMapDto.replace(DateTimeUtil.formatDate(DateUtil.toDate(iSellReportDto.getDate(),ConstantUtil.DDMMYYYY), ConstantUtil.YYYYMMDD), iSellReportDto);
            }	
    	  });
      });
            
  /*    competerMinRateDtoList.forEach((minrateDto)->{
    	  ISellReportDto iSellReportDto = iSellReportMapDto.get(DateTimeUtil.formatDate(DateUtil.toDate(key, ConstantUtil.YYYYMMDDHHMMSS), ConstantUtil.YYYYMMDD));;
	        if (iSellReportDto != null) {
	        	iSellReportDto.setCompMinRate(allPickup);
	        	iSellReportMapDto.put(iSellReportDto.getDate(), iSellReportDto);
	        }

          competitorNames.add(minrateDto.getName());
      });*/
      
      // rateshop
      Map<String, RateDto> rateDtoList = staahRatesRepository.findAllRatesByClientIdAndRateDate(clientId, startDate,endDate ).stream().collect(Collectors.toMap(p -> p.getRateDate().toString(), p -> p));
      rateDtoList.forEach((key,rateDto)->{
	  		ISellReportDto iSellReportDto = iSellReportMapDto.get(DateTimeUtil.formatDate(DateUtil.toDate(key, ConstantUtil.YYYYMMDDHHMMSS), ConstantUtil.YYYYMMDD));;
	        if (iSellReportDto != null) {
	        	if(rateDto.getRateOnCm()!=null) {
	        		iSellReportDto.setRecommend(rateDto.getRateOnCm().intValue());
	        	}else {
	        		iSellReportDto.setRecommend(0);
	        	}
	        	if(rateDto.getRecommended()!=null) {
	        		iSellReportDto.setSysRate(rateDto.getRecommended().intValue());
	        	}else {
	        		iSellReportDto.setSysRate(0);
	        	}
	        	iSellReportMapDto.replace(DateTimeUtil.formatDate(DateUtil.toDate(iSellReportDto.getDate(),ConstantUtil.DDMMYYYY), ConstantUtil.YYYYMMDD), iSellReportDto);
	        }
	  });

      // events
      eventRepository.findByClientIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualAndStatus(clientId, startDate, endDate, RowStatus.ACTIVE)
      .stream().forEach(event -> {
      if (event.getStartDate() != null && event.getEndDate() != null) {
      DateUtil.dateToLocalDate(event.getStartDate()).datesUntil(
      DateUtil.dateToLocalDate(event.getEndDate()).plusDays(1)).forEach(date -> 
      {
    	  ISellReportDto iSellReportDto = iSellReportMapDto.get(DateTimeUtil.formatDate(DateUtil.localToDate(date), ConstantUtil.YYYYMMDD));
    	  if (iSellReportDto != null) {
	    	  iSellReportDto.setEventName(event.getName());
	    	  iSellReportDto.setEventType(event.getType());
	    	  iSellReportMapDto.replace(DateTimeUtil.formatDate(DateUtil.toDate(iSellReportDto.getDate(),ConstantUtil.DDMMYYYY), ConstantUtil.YYYYMMDD), iSellReportDto);
    	  }
      });
      }
      });
  
		return iSellReportMapDto;		
	}
	
    public List<String> getHotelIdbyClientId (Integer clientId){
    List<Integer> hotelIds = clientCompetitorsRepository.findByClientId(clientId).stream().map(cc ->{
    	return cc.getHotelId();
    }).collect(Collectors.toList());
    List<String> rmCodeList = hotelsRepository.findByIdIn(hotelIds).stream().map(cc ->{
      	return cc.getRmCode().toString();
      }).collect(Collectors.toList());
    Hotels hotel = hotelsRepository.findRmCodeByClientId(clientId);
    if(hotel!=null && hotel.getRmCode()!=null) {
  	  rmCodeList.add(hotel.getRmCode().toString());
    }
    return rmCodeList;
    }
    
    public List<String> getCompetitorsbyClientId (Integer clientId){
        List<Integer> hotelIds = clientCompetitorsRepository.findByClientId(clientId).stream().map(cc ->{
        	return cc.getHotelId();
        }).collect(Collectors.toList());
        List<String> nameList = hotelsRepository.findByIdIn(hotelIds).stream().map(cc ->{
          	return cc.getName();
          }).collect(Collectors.toList());
        Hotels hotel = hotelsRepository.findRmCodeByClientId(clientId);
        if(hotel!=null && hotel.getRmCode()!=null) {
        	nameList.add(hotel.getName());
        }
        return nameList;
    }
	
	public Map<String, Integer> getAllPickup(Integer clientId, Date startDate, Date endDate, Date systemToday) {
        DateUtil.setTimeToZero(systemToday);
        Map<String, Integer> pickup = new HashMap<>();
        List<PickupDto> confirmedPickup = paceOccupancyByDateRepository.findNewPickupByClientIdAndOccupancyDateAndSystemTodayAndStatus(clientId, startDate, endDate, systemToday);
        List<PickupDto> cancelledPickup = paceOccupancyByDateRepository.findCancelPickupByClientIdAndOccupancyDateAndSystemTodayAndStatus(clientId, startDate, endDate, systemToday, ConstantUtil.CANCELLED_STATUS);

        List<PickupDto> collect = cancelledPickup.stream().map(e -> new PickupDto(e.getOccupancyDate(), -1 * e.getPickup(),e.getOta_name())).collect(Collectors.toList());
        Stream.concat(confirmedPickup.stream(), collect.stream()).forEach(value -> {
            if (pickup.get(DateUtil.formatDate(value.getOccupancyDate(), ConstantUtil.YYYYMMDD)) == null) {
                pickup.put(DateUtil.formatDate(value.getOccupancyDate(),ConstantUtil.YYYYMMDD), value.getPickup());
            } else {
                pickup.put(DateUtil.formatDate(value.getOccupancyDate(),ConstantUtil.YYYYMMDD), pickup.get(DateUtil.formatDate(value.getOccupancyDate(), ConstantUtil.YYYYMMDD)) + value.getPickup());
            }
        });
        return pickup;
    }
	
    private List<RateHorizonDto> formateHorizonDate(List<RateHorizon> horizon) {
        List<RateHorizonDto> horizonDates = new ArrayList<RateHorizonDto>();
        
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date today = new Date();
    	horizon.forEach(ho ->{
    		Date todayWithZeroTime = new Date();;
    		try {
    			todayWithZeroTime = formatter.parse(formatter.format(today));
    		} catch (ParseException e) {
    			e.printStackTrace();
    		}
    		if(DateUtil.addDays(DateUtil.setTimeToZero(ho.getDate()), -(ho.getPacePoint())).equals(DateUtil.addDays(DateUtil.setTimeToZero(todayWithZeroTime),0))) {
	    		RateHorizonDto dto = new RateHorizonDto();
	    		dto.setAlgo(ho.getAlgo());
	    		dto.setDate(ho.getDate());
	    		dto.setClientId(ho.getClientId());
	    		dto.setPacePoint(ho.getPacePoint());
	    		dto.setHorizonDate(DateUtil.addDays(DateUtil.setTimeToZero(ho.getDate()), -(ho.getPacePoint())));
	    		horizonDates.add(dto);
    		}
        });
    	return horizonDates;
    }
    
    
    public File createIsellReport(Integer clientId) {
    	Map<String,ISellReportDto> report = createISellReportData (clientId);
    	List<String> compHotel = getCompetitorsbyClientId (clientId);
    	File file = null;
    	 Workbook wb = new XSSFWorkbook();
    	 Sheet sheet = wb.createSheet("ISell");
    	 setISellReportHeader(sheet,wb,compHotel);
    	 AtomicInteger counter = new AtomicInteger(sheet.getLastRowNum());
    	 report.forEach((key,value)->{   		 
    		 Row row = sheet.createRow(counter.incrementAndGet()+1);
    		 int columnCount = 0;
    		 Cell cell = row.createCell(columnCount);
	    	 //Create style
    		 CellStyle style1 = wb.createCellStyle();
    		 CellStyle style2 = wb.createCellStyle();
        	 cell.setCellValue(value.getDate());
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getDow());
        	 if(value.getDow()!=null && (value.getDow().equals("Sun")|| value.getDow().equals("Sat"))) {
        		 style1.setFillForegroundColor(IndexedColors.PINK.index);
        		 style1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    	         cell.setCellStyle(style1);
        	 }
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getSeason());
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getEventName());
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getEventType());
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getHotelCap());
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getAvailCap());
        	 if (value.getAvailCap()!=null && value.getAvailCap()<=0){
        		 style2.setFillForegroundColor(IndexedColors.RED.index);
        		 style2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    	         cell.setCellStyle(style2);
        	 }
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getSold());
        	 style1.setFillForegroundColor(IndexedColors.PINK.index);
        	 style1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	         cell.setCellStyle(style1);
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getPickup());
        	 if(value.getPickup()!=null && value.getPickup()>0) {
        		 CellStyle style3 = wb.createCellStyle();
        		 style3.setFillForegroundColor(IndexedColors.GREEN.index);
        		 style3.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    	         cell.setCellStyle(style3);
        	 }else if (value.getPickup()!=null && value.getPickup()<0){
        		 style2.setFillForegroundColor(IndexedColors.RED.index);
        		 style2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    	         cell.setCellStyle(style2);
        	 }

        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getRevenue());
        	 CellStyle style4 = wb.createCellStyle();
        	 style4.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.index);
        	 style4.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	         cell.setCellStyle(style4);
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getAdr());
        	 style4.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.index);
	    	 style4.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	         cell.setCellStyle(style4);
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getStrategy());
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getSysRate());
        	 style4.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.index);
        	 style4.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	         cell.setCellStyle(style4);
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getRecommend());
        	 style4.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.index);
        	 style4.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	         cell.setCellStyle(style4);
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getCompMin());
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getCompAvg());
        	 cell = row.createCell(++columnCount);
        	 cell.setCellValue(value.getCompMax());
        	 if(value.getCompMinRate()!=null) {
	        	 for (Map.Entry<String,Integer> entry : value.getCompMinRate().entrySet()) {
	            	 cell = row.createCell(++columnCount);
	            	 cell.setCellValue(entry.getValue());
	        	 }
        	 }
    	 });
    	 for(int i=0;i<30;i++) {
     		sheet.autoSizeColumn(i, true);
     	 }
    	 try {
    		 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss") ;
    		 file = new File(config.getiSellReportDirectoryPath() +"ISellReport"+dateFormat.format(new Date()) + ".xlsx");
    		 OutputStream fileOut = new FileOutputStream(file); 
		     wb.write(fileOut);
		     fileOut.close();    		
    	 }
    	 catch(Exception ce) {
    		 ce.printStackTrace();
    	 }
    	return file;
    }
    
    Integer addCounter(int i){
    	return i=i+1;
    }
    
    void setISellReportHeader(Sheet sheet, Workbook wb,List<String> compHotel){
    	Row row = sheet.createRow((short) 1);
    	Cell cell = row.createCell((short) 0);
    	CellStyle style = wb.createCellStyle();//Create style
    	style.setAlignment(HorizontalAlignment.CENTER);
        Font font = wb.createFont();//Create font
        font.setBold(true);//Make font bold
        style.setFont(font);
        cell.setCellStyle(style);
    	int columnCount = 0;
    	cell.setCellValue("Date and Day");
    	sheet.addMergedRegion(new CellRangeAddress(1,1,0,1));
    	cell = row.createCell(2);
    	cell.setCellStyle(style);
    	cell.setCellValue("Season No");
    	cell = row.createCell(3);
    	cell.setCellStyle(style);
    	cell.setCellValue("Events");
    	sheet.addMergedRegion(new CellRangeAddress(1,1,3,4));
    	cell = row.createCell(5);
    	cell.setCellStyle(style);
    	cell.setCellValue("Capacity");
    	sheet.addMergedRegion(new CellRangeAddress(1,1,5,6));
    	cell = row.createCell(7);
    	cell.setCellStyle(style);
    	cell.setCellValue("Performance");
    	sheet.addMergedRegion(new CellRangeAddress(1,1,7,11));
    	cell = row.createCell(12);
    	cell.setCellStyle(style);
    	cell.setCellValue("Rate");
    	sheet.addMergedRegion(new CellRangeAddress(1,1,12,13));
    	cell = row.createCell(14);
    	cell.setCellStyle(style);
    	cell.setCellValue("Competitor Pricing");
    	sheet.addMergedRegion(new CellRangeAddress(1,1,14,16));
    	cell = row.createCell(17);
    	cell.setCellStyle(style);
    	Row row2 = sheet.createRow((short) 2);
    	Cell cell2 = row2.createCell((short) 0);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Date");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,0,0));
    	cell2 = row2.createCell(1);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Day of Week");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,1,1));
    	cell2 = row2.createCell(2);
    	cell2.setCellStyle(style);
    	cell2.setCellValue(" ");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,2,2));
    	cell2 = row2.createCell(3);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Name");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,3,3));
    	cell2 = row2.createCell(4);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Type");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,4,4));
    	cell2 = row2.createCell(5);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Hotel");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,5,5));
    	cell2 = row2.createCell(6);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Rooms Available Online");
    	cell2 = row2.createCell(7);
    	cell2.setCellStyle(style);
    	sheet.addMergedRegion(new CellRangeAddress(2,3,6,6));
    	cell2.setCellValue("Sold");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,7,7));
    	cell2 = row2.createCell(8);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Pickup");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,8,8));
    	cell2 = row2.createCell(9);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Revenue");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,9,9));
    	cell2 = row2.createCell(10);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("ADR");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,10,10));
    	cell2 = row2.createCell(11);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Strategy");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,11,11));
    	cell2 = row2.createCell(12);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("System Rate");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,12,12));
    	cell2 = row2.createCell(13);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Recommended");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,13,13));
    	cell2 = row2.createCell(14);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Min");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,14,14));
    	cell2 = row2.createCell(15);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Avg");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,15,15));
    	cell2 = row2.createCell(16);
    	cell2.setCellStyle(style);
    	cell2.setCellValue("Max");
    	sheet.addMergedRegion(new CellRangeAddress(2,3,16,16));
    	cell2 = row2.createCell(17);
    	cell2.setCellStyle(style);
    	int i = 17;
    	for(String s : compHotel) {
	    	cell2.setCellValue(s);
	    	sheet.addMergedRegion(new CellRangeAddress(2,3,i,i));
	    	cell2 = row2.createCell(++i);
	    	cell2.setCellStyle(style);
    	}
    	
    }
    
    public String findStrategy(String strat) {
    	switch (strat) {
	        case "MPI":
	            return "V";
	
	        case "M":
	            return "V";
	
	        case "ARI":
	            return "R";
	
	        case "A":
	            return "R";
	
	        case "PQM":
	            return "M";
	        case "P":
	            return "M";
	
	        case "RCP":
	            return "A";
	
	        case "R":
	            return "A";
	
	        default:
	            return "O";
    	}
    }
    
    public String findDay(String day) {
    	switch (day) {
	        case "SUNDAY":
	            return "Sun";
	
	        case "MONDAY":
	            return "Mon";
	
	        case "TUESDAY":
	            return "Tue";
	
	        case "WEDNESDAY":
	            return "Wed";
	
	        case "THURSDAY":
	            return "Thu";
	        case "FRIDAY":
	            return "Fri";
	
	        case "SATURDAY":
	            return "Sat";

	        default:
	            return " ";
    	}   	
    }
    
	public Response sendCMStructralErrorMail(String channelManager,String body, String clientName) {
		Response response = new Response();
		EmailTemplate emailtemplate = emailTemplateRepository.findByTemplateCodeAndStatus(ConstantUtil.INTEGRATION_STRUCTURE_ERROR, true);
		try {
			HashMap<String,String> tags = new HashMap<String,String> ();
			tags.put("<CHANNELMAN>", channelManager);
			tags.put("<CLIENT>", clientName);
			tags.put("<RESPONSEBODY>", body);
			String textBody = emailServiceImpl.replaceNameTags(emailtemplate.getBody(), tags);
			emailServiceImpl.sendHTMLMessageWithoutAttachment("vinavp1@gmail.com",emailtemplate.getFrom(), emailtemplate.getSubject(), textBody);
			response.setStatus(Status.SUCCESS);
			response.setMessage(Status.SUCCESS.toString());
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(Status.FAILED);
			response.setMessage(Status.FAILED.toString());
		}
		return response;
	}
}
