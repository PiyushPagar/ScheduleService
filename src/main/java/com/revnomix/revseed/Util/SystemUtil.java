package com.revnomix.revseed.Util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.stereotype.Service;

import com.revnomix.revseed.integration.configuration.XmlMessageConverter;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.JobType;
import com.revnomix.revseed.model.ScheduledJob;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.AccountsRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.ScheduledJobRepository;
import com.revnomix.revseed.wrapper.SystemDateDto;




@Service
public class SystemUtil {


    @Autowired
    private ClientsRepository clientsRepository;
    
    @Autowired
    private AccountsRepository accountsRepository;
    
    @Autowired
    private ScheduledJobRepository scheduledJobRepository;
    
    public Date getSystemToday(Integer clientId) {
        return Optional.ofNullable(DateUtil.getDate(clientsRepository.getOne(clientId).getSystemToday())).orElse(DateUtil.getDate(new Date()));
    }
    
  
    
    public SystemDateDto getCombiDate(Integer clientId) {
    	SystemDateDto systemDateDto = new SystemDateDto();
		DateTime systemToday = new DateTime(getSystemToday(clientId));
		systemDateDto.setYesterday(DateUtil.minusDay(systemToday, 1));
		systemDateDto.setSystemToday(DateUtil.convertDateTimeToDate(systemToday));
		systemDateDto.setFirstDateofMonth( DateUtil.getFirstDateOfMonth(systemToday));
		systemDateDto.setLastDateofMonth(DateUtil.getLastDateOfMonth(systemToday));
		systemDateDto.setLastMonthFirstDate(DateUtil.getLastMonthFirstDate(systemToday));
		systemDateDto.setLastMonthlastDate( DateUtil.getLastMonthLastDate(systemToday));
		return systemDateDto;
    }
    
    public List<HttpMessageConverter<?>> setXMLMessageConvertor() {
    	List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
    	Jaxb2RootElementHttpMessageConverter jaxbMessageConverter = new Jaxb2RootElementHttpMessageConverter();
    	XmlMessageConverter xmlMessageConverter = new XmlMessageConverter();
    	List<MediaType> mediaTypes = new ArrayList<MediaType>();
    	mediaTypes.add(MediaType.APPLICATION_XML);
    	XmlMessageConverter converter = new XmlMessageConverter();
		converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_XML));
    	jaxbMessageConverter.setSupportedMediaTypes(mediaTypes);
    	List<MediaType> mediaTypes2 = new ArrayList<MediaType>();
    	mediaTypes2.add(MediaType.TEXT_XML);
    	xmlMessageConverter.setSupportedMediaTypes(mediaTypes2);
    	messageConverters.add(xmlMessageConverter);
    	messageConverters.add(converter);
    	messageConverters.add(jaxbMessageConverter);
    	return messageConverters;
    }
    
    public List<HttpMessageConverter<?>> setJSONMessageConvertor() {
    	List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
    	GsonHttpMessageConverter jsonmessageConverter = new GsonHttpMessageConverter();
    	List<MediaType> mediaTypes = new ArrayList<MediaType>();
    	mediaTypes.add(MediaType.APPLICATION_JSON);
    	jsonmessageConverter.setSupportedMediaTypes(mediaTypes);
    	messageConverters.add(new ByteArrayHttpMessageConverter());
    	messageConverters.add(new StringHttpMessageConverter());
    	messageConverters.add(new ResourceHttpMessageConverter());
    	messageConverters.add(new SourceHttpMessageConverter<>());
    	messageConverters.add(new AllEncompassingFormHttpMessageConverter());
    	messageConverters.add(new MappingJackson2XmlHttpMessageConverter());
    	messageConverters.add(new MappingJackson2HttpMessageConverter());
    	messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
    	return messageConverters;
    }
    
    public void XlsxToCsvConvertor(File inputFile, File outputFile) {
        // For storing data into CSV files
        StringBuffer data = new StringBuffer();

        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            // Get the workbook object for XLSX file
            FileInputStream fis = new FileInputStream(inputFile);
            Workbook workbook = null;

            String ext = FilenameUtils.getExtension(inputFile.toString());

            if (ext.equalsIgnoreCase("xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (ext.equalsIgnoreCase("xls")) {
                workbook = new HSSFWorkbook(fis);
            }

            // Get first sheet from the workbook

            int numberOfSheets = workbook.getNumberOfSheets();
            Row row;
            Cell cell;
            // Iterate through each rows from first sheet

            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();

                while (rowIterator.hasNext()) {
                    row = rowIterator.next();
                    // For each row, iterate through each columns
                    Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {

                        cell = cellIterator.next();

                        switch (cell.getCellType()) {
                        case BOOLEAN:
                            data.append(cell.getBooleanCellValue() + ",");

                            break;
                        case NUMERIC:
                            data.append(cell.getNumericCellValue() + ",");

                            break;
                        case STRING:
                            data.append(cell.getStringCellValue() + ",");
                            break;

                        case BLANK:
                            data.append("" + ",");
                            break;
                        default:
                            data.append(cell + ",");

                        }
                    }
                    data.append('\n'); // appending new line after each row
                }

            }
            fos.write(data.toString().getBytes());
            fos.close();

        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }
    
    public ScheduledJob createJob(Clients clients, JobType jobType) {
        ScheduledJob scheduledJob = new ScheduledJob();
        scheduledJob.setClientId(clients.getId());
        scheduledJob.setJobType(jobType);
        scheduledJob.setStartDateTime(new Date());
        scheduledJob.setStatus(Status.CREATED);
        return scheduledJob;
    }
    
    public ScheduledJob changeStatus(ScheduledJob job, Status status) {
        job.setStatus(status);
        return scheduledJobRepository.save(job);
    }
}

