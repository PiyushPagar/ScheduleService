package com.revnomix.revseed.integration.staahMax.transformer;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.staahMax.dto.RateInventoryMapping;
import com.revnomix.revseed.integration.staahMax.dto.StaahMaxPopulationDto;

@Component
public class StaahMaxRateInventoryTransformer implements GenericTransformer<List<StaahMaxPopulationDto>, List<StaahRates>> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private RoomTypeMappingsRepository roomTypeMappingsRepository;

    @Autowired
    private StaahRatesRepository staahRatesRepository;

    @Autowired
    private StaahInventoryRepository staahInventoryRepository;
    
    @Autowired
    private ClientsRepository clientsRepository;

    @Override
    public List<StaahRates> transform(List<StaahMaxPopulationDto> list) {
    	final List<StaahRates> staahRatesList = new ArrayList<>();
    	Integer clientId = list.get(0).getClient().getId();
    	Date today = new Date();
    	HashMap<Date, HashMap<Double,List<Integer>>> gens = new HashMap<Date,HashMap<Double,List<Integer>>>();  
		Collections.sort(list, new Comparator<StaahMaxPopulationDto>() {
	        @Override
	        public int compare(StaahMaxPopulationDto o1, StaahMaxPopulationDto o2) {
	            return Double.compare(o1.getRateInventoryMapping().getRoom_id(), o2.getRateInventoryMapping().getRoom_id());
	        }
	    });
    	for(StaahMaxPopulationDto staahPopulationDto : list) {
    		RateInventoryMapping rateinv = staahPopulationDto.getRateInventoryMapping();
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
    		
    	}
    	
    	gens.forEach((dates,v)->{
    		v.forEach((rId,invList)->{
    			Integer maxInv = Collections.max(invList);
    			 StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(rId.longValue(), clientId);
    			StaahInventory staahInventory = staahInventoryRepository.findByClientIdAndInvDateAndRoomTypeId(clientId, dates,staahRoomTypes.getId());
    			if (staahInventory == null) {
    				staahInventory = new StaahInventory();
    			}
    			staahInventory.setClientId(clientId);	
	            staahInventory.setRoomTypeId(staahRoomTypes.getId());
	            staahInventory.setAvailability(maxInv);
	            staahInventory.setInvDate(dates);
	            staahInventory.setRegDate(today);
	            staahInventory.setDescription("From RateInventoryMapping Update for :" + maxInv);
	            staahInventoryRepository.save(staahInventory);
    		});
    	});

    	for(StaahMaxPopulationDto staahPopulationDto : list) {	        
	        
	        RateInventoryMapping rate = staahPopulationDto.getRateInventoryMapping();
	        StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(rate.getRoom_id().longValue(), staahPopulationDto.getClient().getId());
	        if (staahRoomTypes == null) {
	            staahRoomTypes = new StaahRoomTypes();
	            staahRoomTypes.setRegDate(today);
	            staahRoomTypes.setStaahId(rate.getRoom_id().longValue());
	            staahRoomTypes.setClientId(staahPopulationDto.getClient().getId());
	            staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
	        }
	        RoomTypeMappings roomTypeMappings = roomTypeMappingsRepository.findByClientRoomTypeAndClientId(rate.getRoom_id().longValue(), staahPopulationDto.getClient().getId());
	        if (roomTypeMappings == null) {
	            roomTypeMappings = new RoomTypeMappings();
	            roomTypeMappings.setClientId(staahPopulationDto.getClient().getId());
	            roomTypeMappings.setClientRoomType(staahRoomTypes.getStaahId());
	            roomTypeMappings.setRoomTypeId(1);
	            roomTypeMappings.setType(IntegrationType.STAAH.name());
	            roomTypeMappingsRepository.save(roomTypeMappings);
	        }
	        StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(staahPopulationDto.getClient().getId(), rate.getRate_id().longValue(), rate.getRoom_id().longValue());
	        if (staahRateTypes == null) {
	            staahRateTypes = new StaahRateTypes();
	            staahRateTypes.setClientId(staahPopulationDto.getClient().getId());
	            staahRateTypes.setStaahRateId(rate.getRate_id().longValue());
	            staahRateTypes.setStaahRoomId(rate.getRoom_id().longValue());
	            staahRateTypes.setRegDate(new Date());
	            staahRateTypes = staahRateTypesRepository.save(staahRateTypes);
	        }
	
	        for (RateInventoryMapping.Data data : rate.getData()) {
	            Date date = DateUtil.toDateByAllFormat(data.getDate(), "yyyy-MM-dd");
	            StaahRates staahRates = staahRatesRepository.findOneByClientIdAndRateDateAndRoomTypeIdAndRateTypeId(staahPopulationDto.getClient().getId(), date, staahRoomTypes.getId(), staahRateTypes.getId());
	            if (staahRates == null) {
	                staahRates = new StaahRates();
	            }
	            staahRates.setClientId(staahPopulationDto.getClient().getId());
	            staahRates.setRegDate(today);
	            staahRates.setRoomTypeId(staahRoomTypes.getId());
	            staahRates.setRateTypeId(staahRateTypes.getId());
	            staahRates.setRate(data.getAmountBeforeTax().getRate());
	            staahRates.setRateDate(date);
	            staahRatesRepository.save(staahRates);
	            staahRatesList.add(staahRates);
	            
	        }
	        Clients client = staahPopulationDto.getClient();
	        client.setSystemToday(DateUtil.setTimeToZero(new Date()));
	        clientsRepository.save(client);
    	}
        logger.info("data save successfully : ");
        return staahRatesList;
    }
}
