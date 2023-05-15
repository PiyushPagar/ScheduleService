package com.revnomix.revseed.integration.staah.transformer;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.RoomTypeMappings;
import com.revnomix.revseed.model.StaahInventory;
import com.revnomix.revseed.model.StaahRateTypes;
import com.revnomix.revseed.model.StaahRates;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.RoomTypeMappingsRepository;
import com.revnomix.revseed.repository.StaahInventoryRepository;
import com.revnomix.revseed.repository.StaahRateTypesRepository;
import com.revnomix.revseed.repository.StaahRatesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.schema.staah.AvailRequestSegments;
import com.revnomix.revseed.schema.staah.Room;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.eglobe.dto.DayWiseInventory;
import com.revnomix.revseed.integration.eglobe.dto.RoomWiseInventory;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;
import com.revnomix.revseed.integration.staahMax.dto.RateInventoryMapping;

@Component
public class GenericInventoryMappingTransformer implements GenericTransformer<GenericIntegrationDto, List<StaahInventory>> {

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private RoomTypeMappingsRepository roomTypeMappingsRepository;

    @Autowired
    private StaahInventoryRepository staahInventoryRepository;
    
    @Autowired
    private ClientsRepository clientsRepository;
    
    @Autowired
    private StaahRatesRepository staahRatesRepository;
    
    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;

	@Override
	public List<StaahInventory> transform(GenericIntegrationDto source) {
		List<StaahInventory> staahInventories = new ArrayList<>();
        Date today = new Date();
        if(source.getIntegerationType().equals(IntegrationType.STAAH.toString())) {
        	for(AvailRequestSegments.Rooms rs : source.getStaahPopulationDto().getAvailRequestSegments().getRooms()){
                for(Room r : rs.getRoom()){
                	StaahRoomTypes  staahRoomTypes  = staahRoomTypesRepository.findByStaahIdAndClientId(r.getRoomTypeCode().longValue(), source.getClient().getId()) ;
                	if(staahRoomTypes!=null) {
	                	for(com.revnomix.revseed.schema.staah.Date date : r.getDates().getDate()) {
	                        StaahInventory staahInventory = saveStaahInventory(source.getClient().getId(),staahRoomTypes.getId(),date.getCount(),date.getEffectiveDate().toGregorianCalendar().getTime(),today);
	                        staahInventories.add(staahInventory);
	                	}
                	}
                }
        	}
        }
        
        if(source.getIntegerationType().equals(IntegrationType.STAAHMAX.toString())) {
        	HashMap<Date, HashMap<Double,List<Integer>>> gens = new HashMap<Date,HashMap<Double,List<Integer>>>();  
        		RateInventoryMapping rateinv = source.getRateInventoryMapping();
        		for (RateInventoryMapping.Data data : rateinv.getData()) {
        			Date date = DateUtil.toDateByAllFormat(data.getDate(), "yyyy-MM-dd");
        			if(gens.containsKey(date)) {
        				HashMap<Double,List<Integer>> li = gens.get(date);
        				if(li.containsKey(rateinv.getRoom_id())){
        					List<Integer> pi = li.get(rateinv.getRoom_id());
        					pi.add(data.getInventory());
        					li.put(rateinv.getRoom_id(), pi);
        				}else {
        					List<Integer> pi = new ArrayList<Integer>();
        					pi.add(data.getInventory());
        					li.put(rateinv.getRoom_id(), pi);
        				}
        				gens.put(date, li);  				
        			}else {
        				HashMap<Double,List<Integer>> li = new HashMap<Double,List<Integer>> ();
        				List<Integer> pi = new ArrayList<Integer>();
    					pi.add(data.getInventory());
    					li.put(rateinv.getRoom_id(), pi);
        				gens.put(date, li);  
        			}
        		}
        	
        	gens.forEach((dates,v)->{
        		v.forEach((rId,invList)->{
        			Integer maxInv = Collections.max(invList);
        			StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(rId.longValue(), source.getClient().getId());
        			if(staahRoomTypes!=null) {
	        			StaahInventory staahInventory = saveStaahInventory(source.getClient().getId(),staahRoomTypes.getId(),maxInv,dates,today);
	        			staahInventories.add(staahInventory);
        			}
        		});
        	});        
        		RateInventoryMapping rate = source.getRateInventoryMapping();
        		StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(rate.getRoom_id().longValue(), source.getClient().getId());
        		if(staahRoomTypes!=null) {
	        		saveStaahRoomTypeMapping(source.getClient().getId(),rate.getRoom_id().longValue(),staahRoomTypes.getStaahId(),1,IntegrationType.STAAH.name());
	        		StaahRateTypes staahRateTypes = saveRateTypes(source.getClient().getId(),rate.getRate_id().longValue(), rate.getRoom_id().longValue(),today);
	            	for (RateInventoryMapping.Data data : rate.getData()) {
	            		Date date = DateUtil.toDateByAllFormat(data.getDate(), "yyyy-MM-dd");
	            		saveStaahRates(source.getClient().getId(),today, staahRoomTypes.getId(), staahRateTypes.getId(), data.getAmountBeforeTax().getRate(), date);
	            	}
        		}
        }
        
        if(source.getIntegerationType().equals(IntegrationType.EZEE.toString())) {
        	source.getResResponse().getRoomInfo().getSource().forEach(rs -> {
                rs.getRoomTypes().getRoomType().forEach(r -> {
                	long noOfDays = DateUtil.daysBetween(r.getFromDate().toGregorianCalendar().getTime(),r.getToDate().toGregorianCalendar().getTime());
                    Long id = r.getRoomTypeID();
                	StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(id, source.getClient().getId());
                	if(staahRoomTypes!=null) {
	                	for (int i = 0; i <= noOfDays;i++){
	                        StaahInventory staahInventory = saveStaahInventory(source.getClient().getId(),staahRoomTypes.getId(),r.getAvailability(),DateUtil.addDays(r.getFromDate().toGregorianCalendar().getTime(), i),today);
	                        staahInventories.add(staahInventory);
	                    }
                	}
                });
        	});
        }
        
        if(source.getIntegerationType().equals(IntegrationType.EGLOBE.toString())) {
            List<RoomWiseInventory> roomwiseInvetoryList = source.getRoot().getRoomWiseInventory();
            for(RoomWiseInventory roomWiseInventory: roomwiseInvetoryList) {
            	Long roomId = Long.parseLong(roomWiseInventory.getRoomId());
            	StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId, source.getClient().getId());
            	if(staahRoomTypes!=null) {
	            	saveStaahRoomTypeMapping(source.getClient().getId(),roomId,staahRoomTypes.getStaahId(),1,IntegrationType.EGLOBE.name());
	                List<DayWiseInventory>  daywiseInventoryList = roomWiseInventory.getDayWiseInventory();
	                for(DayWiseInventory dayWiseInventory : daywiseInventoryList) {
	                	Date invDate = DateUtil.toDate(dayWiseInventory.getAsOnDate(),ConstantUtil.DASHDDMMMYYYY);
	                	StaahInventory staahInventory = saveStaahInventory(source.getClient().getId(),staahRoomTypes.getId(),dayWiseInventory.getDayAvailability(),invDate,today);
	                	staahInventories.add(staahInventory);
	                }
            	}
            }
        }
        
        Clients client = source.getClient();
        client.setSystemToday(new Date());
        clientsRepository.save(client);
		return staahInventories;
	}
	
//    private StaahRoomTypes getStaahRoomTypes(Integer clientId,Date regDate,String roomTypeName,Long staahId){
//        StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(staahId, clientId);
//        if (staahRoomTypes == null) {
//            staahRoomTypes = new StaahRoomTypes();
//            staahRoomTypes.setClientId(clientId);
//            staahRoomTypes.setStaahId(staahId);
//            staahRoomTypes.setName(roomTypeName);
//            staahRoomTypes.setRegDate(regDate);
//            staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
//        }
//        return staahRoomTypes;
//    }
//    
    private StaahInventory saveStaahInventory(Integer clientId,Integer roomTypeId,Integer availability,Date invDate,Date regDate) {
    	StaahInventory staahInventory = null;
    	List<StaahInventory> inv = staahInventoryRepository.findAllByClientIdAndRoomTypeIdAndInvDate(clientId,roomTypeId ,invDate);
        if(inv.size() > 1){
            staahInventoryRepository.deleteAll(inv);
        }else if (inv.size() == 1) {
        	staahInventory = inv.get(0);
        }
        
    	if (staahInventory == null) {
            staahInventory = new StaahInventory();
        }
        staahInventory.setClientId(clientId);	
        staahInventory.setRoomTypeId(roomTypeId);
        staahInventory.setAvailability(availability);
        staahInventory.setInvDate(invDate);
        staahInventory.setRegDate(regDate);
        staahInventory.setDescription("From RateInventoryMapping Update for :" + availability);
        staahInventoryRepository.save(staahInventory);
        return staahInventory;
    }
    
    private RoomTypeMappings saveStaahRoomTypeMapping(Integer clientId,Long roomId,Long clientRoomTypeId,Integer roomTypeId,String type) {
    	RoomTypeMappings roomTypeMappings = roomTypeMappingsRepository.findByClientRoomTypeAndClientId(roomId, clientId);
        if (roomTypeMappings == null) {
            roomTypeMappings = new RoomTypeMappings();
            roomTypeMappings.setClientId(clientId);
            roomTypeMappings.setClientRoomType(clientRoomTypeId);
            roomTypeMappings.setRoomTypeId(roomTypeId);
            roomTypeMappings.setType(type);
            roomTypeMappingsRepository.save(roomTypeMappings);
        }
        return roomTypeMappings;
    }
    
    private StaahRateTypes saveRateTypes(Integer clientId,Long staahRateId, Long staahRoomId,Date regDate) {
    	StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(clientId, staahRateId, staahRoomId);
        if (staahRateTypes == null) {
            staahRateTypes = new StaahRateTypes();
            staahRateTypes.setClientId(clientId);
            staahRateTypes.setStaahRateId(staahRateId);
            staahRateTypes.setStaahRoomId(staahRoomId);
            staahRateTypes.setRegDate(regDate);
            staahRateTypes = staahRateTypesRepository.save(staahRateTypes);
        }
        return staahRateTypes;
    }
    
    private StaahRates saveStaahRates(Integer clientId,Date regDate, Integer roomTypeId, Integer rateTypeId, Double rate, Date rateDate) {
        StaahRates staahRates = staahRatesRepository.findOneByClientIdAndRateDateAndRoomTypeIdAndRateTypeId(clientId, rateDate, roomTypeId, rateTypeId);
        if (staahRates == null) {
            staahRates = new StaahRates();
        }
        staahRates.setClientId(clientId);
        staahRates.setRegDate(regDate);
        staahRates.setRoomTypeId(roomTypeId);
        staahRates.setRateTypeId(rateTypeId);
        staahRates.setRate(rate);
        staahRates.setRateDate(rateDate);
        staahRatesRepository.save(staahRates);
        return staahRates;
    }
}
