package com.revnomix.revseed.integration.ezee.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.StaahInventory;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.StaahInventoryRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.ezee.EzeePopulationDto;

@Component
public class EzeeRoomInventoryMappingTransformer implements GenericTransformer<EzeePopulationDto, List<StaahInventory>> {

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private StaahInventoryRepository staahInventoryRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Transactional
    @Override
    public List<StaahInventory> transform(EzeePopulationDto ezeePopulationDto) {
        List<StaahInventory> staahInventories = new ArrayList<>();
        Date today = new Date();
        ezeePopulationDto.getResResponse().getRoomInfo().getSource().forEach(rs -> {
            rs.getRoomTypes().getRoomType().forEach(r -> {
                long noOfDays = DateUtil.daysBetween(r.getFromDate().toGregorianCalendar().getTime(),r.getToDate().toGregorianCalendar().getTime());
                Long id = r.getRoomTypeID();
                StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(id, ezeePopulationDto.getClients().getId());
                if (staahRoomTypes == null) {
                    staahRoomTypes = new StaahRoomTypes();
                    staahRoomTypes.setClientId(ezeePopulationDto.getClients().getId());
                    staahRoomTypes.setName(rs.getName());
                    staahRoomTypes.setStaahId(id);
                    staahRoomTypes.setRegDate(new Date());
                    staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
                }
                for (int i = 0; i <= noOfDays;i++){
                    StaahInventory staahInventory = staahInventoryRepository.findByClientIdAndInvDateAndRoomTypeId(ezeePopulationDto.getClients().getId(),DateUtil.addDays(r.getFromDate().toGregorianCalendar().getTime(), i), staahRoomTypes.getId());
                    if (staahInventory == null) {
                        staahInventory = new StaahInventory();
                    }
                    staahInventory.setClientId(ezeePopulationDto.getClients().getId());
                    staahInventory.setRoomTypeId(staahRoomTypes.getId());
                    staahInventory.setAvailability(r.getAvailability());
                    staahInventory.setInvDate(DateUtil.addDays(r.getFromDate().toGregorianCalendar().getTime(), i));
                    staahInventory.setRegDate(today);
                    staahInventoryRepository.save(staahInventory);
                    staahInventories.add(staahInventory);
                }
            });
        });
        Clients client = ezeePopulationDto.getClients();
        client.setSystemToday(new Date());
        clientsRepository.save(client);
        return staahInventories;
    }
}
