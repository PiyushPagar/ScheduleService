package com.revnomix.revseed.integration.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;


@Component
public class XmlMessageConverter extends AbstractHttpMessageConverter<String>{

	public XmlMessageConverter() {
		super(MediaType.APPLICATION_XML,
			      MediaType.TEXT_XML,
			      new MediaType("application", "*+xml")); 
	}
	
    @Override
    protected String readInternal(Class<? extends String> arg, HttpInputMessage inputMsg) throws IOException,
            HttpMessageNotReadableException {
        // TODO Auto-generated method stub
        String paramMap = getPostParameter(inputMsg);
        BufferedReader file =  new BufferedReader(new StringReader(paramMap));
        String str = null;
        JAXBContext jaxbContext;
        System.out.println(str);
        return paramMap;
    }

    @Override
    protected boolean supports(Class<?> type) {
        if(type.getSimpleName().equalsIgnoreCase("String")){
            return true;
        }
        return false;
    }


    private String getPostParameter(HttpInputMessage input) throws IOException{
        String payload = null;
        //String[] params = null;
        BufferedReader buf = new BufferedReader(new InputStreamReader(input.getBody()));
        //Map<String,String> paramMap = new HashMap<String,String>();

        String line="";
        while((line = buf.readLine())!=null){
            payload = payload+line;
        }

        if(payload.contains("null")){
        	payload = payload.replace("null","");
        }

        return payload;
    }

	@Override
	protected void writeInternal(String t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		try {
	          OutputStream outputStream = outputMessage.getBody();
	          String body = t;
	          outputStream.write(body.getBytes());
	          outputStream.close();
	      } catch (Exception e) {
	      }
	}


}