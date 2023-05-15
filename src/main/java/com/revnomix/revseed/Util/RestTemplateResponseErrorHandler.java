package com.revnomix.revseed.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revnomix.revseed.integration.staahMax.dto.StaahMaxErrorDto;

@Component
public class RestTemplateResponseErrorHandler 
  implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse httpResponse) 
      throws IOException {
        return (httpResponse.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR || httpResponse.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR);
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
      if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody()))) {
          String httpBodyResponse = reader.lines().collect(Collectors.joining(""));
          StaahMaxErrorDto staahMaxErrorDto = new StaahMaxErrorDto();
          String errorMessage = httpBodyResponse;
          try {
        	  ObjectMapper objectMapper = new ObjectMapper();
        	  objectMapper.readValue(httpBodyResponse.replaceAll("&", "&amp;"), StaahMaxErrorDto.class);
          }catch(Exception ce) {
        	  ce.printStackTrace();
          }
          if(staahMaxErrorDto.getError_desc()!=null) {
        	  errorMessage = staahMaxErrorDto.getError_desc();
          }else {
        	  errorMessage = httpBodyResponse;
          }         
          throw new RestTemplateException(response.getStatusCode(), errorMessage);
        }
      }
    }
}