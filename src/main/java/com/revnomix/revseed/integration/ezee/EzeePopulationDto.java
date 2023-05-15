package com.revnomix.revseed.integration.ezee;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.schema.ezee.RESRequest;
import com.revnomix.revseed.schema.ezee.RESResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EzeePopulationDto {

    private Clients clients;
    private RESResponse resResponse;
    private RESRequest resRequest;
}
