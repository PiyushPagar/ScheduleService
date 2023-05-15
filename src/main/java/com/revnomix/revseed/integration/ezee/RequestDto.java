package com.revnomix.revseed.integration.ezee;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class RequestDto {

    private Integer clientId;
    private String fromDate;
    private String toDate;
    private String message;
}
