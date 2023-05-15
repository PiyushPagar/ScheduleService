package com.revnomix.revseed.integration.staah.transformer;

import java.text.DecimalFormat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.StaahInventory;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.RoomTypeMappingsRepository;
import com.revnomix.revseed.repository.StaahInventoryRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.schema.staah.AvailRequestSegments.Rooms;
import com.revnomix.revseed.schema.staah.Room;
import com.revnomix.revseed.Util.DateUtil;

@Component
public class ViewInventoryTransformer implements GenericTransformer<StaahPopulationDto, List<StaahInventory>> {

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
    public List<StaahInventory> transform(StaahPopulationDto staahPopulationDto) {
        long start = System.currentTimeMillis();
        List<StaahInventory> staahInventories = new ArrayList<>();
        Date today = new Date();
        if(staahPopulationDto.getAvailRequestSegments()!=null) {
	        for(Rooms rs : staahPopulationDto.getAvailRequestSegments().getRooms()){
	            for(Room r : rs.getRoom()){
	                StaahRoomTypes staahRoomTypes = getStaahRoomTypes(staahPopulationDto.getClient().getId(),r);
	                logger.info("Room: Date >"+r.getRoomTypeCode() + " no of records "+ r.getDates().getDate().size());
	                for(com.revnomix.revseed.schema.staah.Date date : r.getDates().getDate()) {
	                    List<StaahInventory> inv = staahInventoryRepository.findAllByClientIdAndRoomTypeIdAndInvDate(staahPopulationDto.getClient().getId(), staahRoomTypes.getId() ,date.getEffectiveDate().toGregorianCalendar().getTime());
	                    if(inv!=null && inv.size() > 1){
	                        staahInventoryRepository.deleteAll(inv);
	                    }else if (inv!=null) {
		                    StaahInventory staahInventory = inv.get(0);
		                    getAndUpdateStaahInventory(staahPopulationDto, today, date, staahRoomTypes, staahInventory);
	                    }
	                }
	            }
	        }
        }
        //updateStaahInventory(staahInventories);
        Clients client = staahPopulationDto.getClient();
        client.setSystemToday(DateUtil.setTimeToZero(new Date()));
        clientsRepository.save(client);
        long end = System.currentTimeMillis();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        logger.error("Execution time is " + formatter.format((end - start) / 1000d) + " seconds");
        return staahInventories;
    }

    private StaahRoomTypes getStaahRoomTypes(Integer clientId,Room r){
        StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(r.getRoomTypeCode().longValue(), clientId);
        if (staahRoomTypes == null) {
            staahRoomTypes = new StaahRoomTypes();
            staahRoomTypes.setClientId(clientId);
            staahRoomTypes.setName(r.getRoomName());
            staahRoomTypes.setStaahId(r.getRoomTypeCode().longValue());
            staahRoomTypes.setName(r.getRoomTypeName());
            staahRoomTypes.setRegDate(new Date());
            staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
        }
        return staahRoomTypes;
    }

    @Transactional
    public StaahInventory getAndUpdateStaahInventory(StaahPopulationDto staahPopulationDto, Date today, com.revnomix.revseed.schema.staah.Date date, StaahRoomTypes staahRoomTypes, StaahInventory staahInventory) {
        staahInventory.setClientId(staahPopulationDto.getClient().getId());
        staahInventory.setRoomTypeId(staahRoomTypes.getId());
        staahInventory.setAvailability(date.getCount());
        staahInventory.setInvDate(date.getEffectiveDate().toGregorianCalendar().getTime());
        staahInventory.setRegDate(today);
        staahInventory.setDescription("From AvailResponseType Update for :" + date.getCount());
        //logger.info("staahInventory: Date >"+staahInventory.getInvDate()+"  Id : "+staahInventory.getRoomTypeId()+" : "+ staahInventory.getDescription());
        staahInventory = staahInventoryRepository.save(staahInventory);
        return staahInventory;
    }

    @Transactional
    public void updateStaahInventory(List<StaahInventory> staahInventories) {
        staahInventoryRepository.saveAll(staahInventories);
    }


}
