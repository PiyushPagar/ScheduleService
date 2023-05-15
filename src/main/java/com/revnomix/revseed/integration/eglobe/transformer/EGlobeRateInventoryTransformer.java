package com.revnomix.revseed.integration.eglobe.transformer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.eglobe.dto.DayWiseInventory;
import com.revnomix.revseed.integration.eglobe.dto.EGlobePopulationDto;
import com.revnomix.revseed.integration.eglobe.dto.RoomWiseInventory;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.RoomTypeMappings;
import com.revnomix.revseed.model.StaahInventory;
import com.revnomix.revseed.model.StaahRates;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.RoomTypeMappingsRepository;
import com.revnomix.revseed.repository.StaahInventoryRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;


@Component
public class EGlobeRateInventoryTransformer implements GenericTransformer<EGlobePopulationDto, List<StaahRates>> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private RoomTypeMappingsRepository roomTypeMappingsRepository;

    @Autowired
    private StaahInventoryRepository staahInventoryRepository;
    
    @Autowired
    private ClientsRepository clientsRepository;

    @Override
    public List<StaahRates> transform(EGlobePopulationDto eGlobePopulationDto) {
        final List<StaahRates> staahRatesList = new ArrayList<>();
//        Date today = new Date();
//        List<RoomWiseInventory> roomwiseInvetoryList = eGlobePopulationDto.getRoot().getRoomWiseInventory();
//        for(RoomWiseInventory roomWiseInventory: roomwiseInvetoryList) {
//        	Long roomId = Long.parseLong(roomWiseInventory.getRoomId());
//        	Integer clientId = eGlobePopulationDto.getClient().getId();
//        	StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId,clientId);
//            if (staahRoomTypes == null) {
//                staahRoomTypes = new StaahRoomTypes();
//                staahRoomTypes.setRegDate(today);
//                staahRoomTypes.setStaahId(roomId);
//                staahRoomTypes.setClientId(clientId);
//                staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
//            }            
//            RoomTypeMappings roomTypeMappings = roomTypeMappingsRepository.findByClientRoomTypeAndClientId(roomId, clientId);
//            if (roomTypeMappings == null) {
//                roomTypeMappings = new RoomTypeMappings();
//                roomTypeMappings.setClientId(eGlobePopulationDto.getClient().getId());
//                roomTypeMappings.setClientRoomType(staahRoomTypes.getStaahId());
//                roomTypeMappings.setRoomTypeId(1);
//                roomTypeMappings.setType(IntegrationType.EGLOBE.name());
//                roomTypeMappingsRepository.save(roomTypeMappings);
//            }
//            List<DayWiseInventory>  daywiseInventoryList = roomWiseInventory.getDayWiseInventory();
//            for(DayWiseInventory dayWiseInventory : daywiseInventoryList) {
//            	Date invDate = DateUtil.toDate(dayWiseInventory.getAsOnDate(),ConstantUtil.DASHDDMMMYYYY) ;
//	            StaahInventory staahInventory = staahInventoryRepository.findByClientIdAndInvDateAndRoomTypeId(clientId,invDate, staahRoomTypes.getId());
//	            if (staahInventory == null) {
//	                staahInventory = new StaahInventory();
//	            }
//	            staahInventory.setClientId(clientId);	
//	            staahInventory.setRoomTypeId(staahRoomTypes.getId());
//	            staahInventory.setAvailability(dayWiseInventory.getDayAvailability());
//	            staahInventory.setInvDate(invDate);
//	            staahInventory.setRegDate(today);
//	            staahInventory.setDescription("From RateInventoryMapping Update for :" + dayWiseInventory.getDayAvailability());
//	            staahInventoryRepository.save(staahInventory);
//            }
//        }       
//        Clients client = eGlobePopulationDto.getClient();
//        client.setSystemToday(DateUtil.setTimeToZero(new Date()));
//        clientsRepository.save(client);
        logger.info("data save successfully : ");
        return staahRatesList;
    }
}
