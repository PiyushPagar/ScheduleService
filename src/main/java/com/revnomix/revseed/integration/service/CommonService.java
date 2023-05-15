package com.revnomix.revseed.integration.service;

import com.revnomix.revseed.model.OtaMappings;
import com.revnomix.revseed.model.Otas;
import com.revnomix.revseed.repository.OtasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommonService {

    @Autowired
    private OtasRepository otasRepository;

    public List<Otas> getAllOtasByClientId(Integer clientId){
        return otasRepository.findByClientId(clientId);
    }
}
