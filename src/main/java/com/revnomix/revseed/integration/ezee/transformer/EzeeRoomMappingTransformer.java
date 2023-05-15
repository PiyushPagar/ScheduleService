package com.revnomix.revseed.integration.ezee.transformer;

import java.util.Arrays;

import java.util.List;

import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.integration.ezee.EzeePopulationDto;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EzeeRoomMappingTransformer implements GenericTransformer<EzeePopulationDto, List<String>> {
    @Override
    public List<String> transform(EzeePopulationDto dto) {

        log.info("transform methos called...." + dto);

        return Arrays.asList("Test");
    }
}
