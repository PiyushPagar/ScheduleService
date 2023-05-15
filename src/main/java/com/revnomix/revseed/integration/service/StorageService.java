package com.revnomix.revseed.integration.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.revnomix.revseed.model.FileLogs;
import com.revnomix.revseed.model.HistoryAndForecast;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.model.StorageException;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.FileLogRepository;
import com.revnomix.revseed.repository.HistoryAndForecastRepository;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.Util.StringUtil;
import com.revnomix.revseed.Util.SystemUtil;

@Service
public class StorageService {
    @Value("${file.upload-dir}")
    private String paths;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    final static Integer CELL_INDEX_DATE = 0;
    final static Integer CELL_INDEX_CAPACITY = 1;
    final static Integer CELL_INDEX_ROOMS = 2;
    final static Integer CELL_INDEX_AVAILABILITY = 3;
    final static Integer CELL_INDEX_REVENUE = 4;

    @Autowired
    private HistoryAndForecastRepository historyAndForecastRepository;
    @Autowired
    private ClientsRepository clientsRepository;   
	@Autowired
	private FileLogRepository fileLogRepository;
	@Autowired
	private SystemUtil sysutil;

    private Set<String> errorMessage = new HashSet<>();

    public String uploadFile(MultipartFile file, String path) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
        }
        try {
            String fileName = file.getOriginalFilename();
            String[] split = fileName.split("\\.csv");
            InputStream is = file.getInputStream();
            String fileNameChange = split[0] + "_" + new Date().getTime()+ ".csv";
            Files.copy(is, Paths.get(path + "/" + fileNameChange), StandardCopyOption.REPLACE_EXISTING);
            return fileNameChange;
        } catch (IOException e) {
        	e.printStackTrace();
            String msg = String.format("Failed to store file", file.getName());
            throw new StorageException(msg, e);
        }
    }

    public void uploadImageFile(MultipartFile file, String staahIncomingDirectoryPath) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
        }
    //    logger.info("Upload for file has been started " + file.getOriginalFilename());
        try {
            String fileName = file.getOriginalFilename();
            String[] split = fileName.split("\\.(?=[^\\.]+$)");
            InputStream is = file.getInputStream();
            Files.copy(is, Paths.get(paths + "/" + split[0] + "." + split[1]), StandardCopyOption.REPLACE_EXISTING);
   //         logger.info("Upload for file has been completed ", file.getOriginalFilename());

        } catch (IOException e) {
        	e.printStackTrace();
            String msg = String.format("Failed to store file", file.getName());
            throw new StorageException(msg, e);
        }
    }
    
    
    public Set<String> createHNFImport(MultipartFile file, String hnfValidationDirectoryPath, Integer clientId) throws IOException {
        List<HistoryAndForecast> historyAndForecasts = new ArrayList<>();
        errorMessage.clear();
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
        }
        String validFilePath = saveFiletoLocation(file, hnfValidationDirectoryPath,clientId);
        
        return errorMessage;

    }

    public Set<String> createHNFImportforexcel(MultipartFile file, String hnfValidationDirectoryPathforexcel, Integer clientId) throws IOException {
        List<HistoryAndForecast> historyAndForecasts = new ArrayList<>();
        errorMessage.clear();
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
        }
        String validFilePath = saveFiletoLocationforexcel(file, hnfValidationDirectoryPathforexcel);
        File validFile = new File(validFilePath);
        FileLogs fileLog= setFileLog(clientId,file.getOriginalFilename(),validFilePath); //Running
        XSSFWorkbook errorFile = null;	
        errorFile = checkFileSize(validFile);
        if(errorFile!=null) {
        	writeFileinLocation (errorFile, validFile.getParent(),fileLog.getId());
        	return null;
        }
        errorFile = checkFileHeader(validFile);
        if(errorFile!=null) {
        	writeFileinLocation (errorFile, validFile.getParent(),fileLog.getId());
        	return null;
        }
        XSSFWorkbook workbook = new XSSFWorkbook();
        workbook = checkFileValidation(validFile,fileLog.getId());

        try {
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                Iterator<Row> rowIterator = sheet.iterator();
                boolean isFirstRow = true;
                int rowIndex = 0;
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if (isFirstRow) {
                        isFirstRow = false;
                        continue;
                    }
                    HistoryAndForecast hnf = new HistoryAndForecast();
                    hnf = save(row, rowIndex++, clientId);
                    HistoryAndForecast existhnf = historyAndForecastRepository.findByClientIdAndDateAndAvailabilityAndCapacityAndRevenueAndRoomsSoldAndHotelId(
                            hnf.getClientId(), hnf.getDate(), hnf.getAvailability(), hnf.getCapacity(), hnf.getRevenue(), hnf.getRoomsSold(), hnf.getHotelId()).orElse(null);
                    if (existhnf!=null) {                    	
                    	existhnf.setUpdatedDate(new Date());
                        historyAndForecastRepository.save(existhnf);             
                    }else{
                        historyAndForecasts.add(hnf);
                    }
                }
            }
            if (errorMessage.isEmpty()) {
                historyAndForecastRepository.saveAll(historyAndForecasts);
            }
            fileLog.setStatus(Status.COMPLETE.toString());
            fileLogRepository.save(fileLog);
        } catch (RuntimeException e) {
        	e.printStackTrace();
            if (errorMessage.isEmpty()) {
                errorMessage.add(e.getMessage());
            }
            return errorMessage;
        }

        return errorMessage;

    }

    private XSSFWorkbook checkFileValidation(File validFile,Integer fileLogId) throws IOException {
    	FileInputStream fis = new FileInputStream(validFile);		
    	XSSFWorkbook workbook = new XSSFWorkbook(fis);
    	XSSFWorkbook errWorkbook = new XSSFWorkbook();
    	XSSFWorkbook sccWorkbook = new XSSFWorkbook();
    	Boolean isError=false;
    	Boolean isSuccess=false;
    	for(int i = 0; i < workbook.getNumberOfSheets(); i++){
    		Sheet sheet = workbook.getSheetAt(i);
    		Sheet errsheet = errWorkbook.createSheet(i+"");
    		Sheet sccsheet = sccWorkbook.createSheet(i+"");
		    Map<String, Object[]> errordata= new TreeMap<String, Object[]>();
		    errordata.put("1",new Object[] {"Date", "Capacity", "Rooms Sold","Availability","Revenue","Error"});
		    errsheet =writingDataintoSheet(errordata, errsheet);
		    Map<String, Object[]> sccdata= new TreeMap<String, Object[]>();
		    sccdata.put("1",new Object[] {"Date", "Capacity", "Rooms Sold","Availability","Revenue"});
		    sccsheet =writingDataintoSheet(sccdata, sccsheet);
    		Iterator<Row> rowIterator = sheet.iterator();
    		Iterator<Row> errorRowIterator = errsheet.iterator();
    		Iterator<Row> sccRowIterator = sccsheet.iterator();
    		int errrownum=1;
    		int sccrownum=1;
    		boolean isFirstRow = true;
    		while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
        
                String errorData = validateData(row);
                if(errorData!=null) {
                	Row errorRow = errsheet.createRow(errrownum++);
                	errorRow = copyRow(errorRow , row);
                	Cell errorCell = errorRow.createCell(row.getLastCellNum(), CellType.STRING);
                	errorCell.setCellValue(errorData);
                	isError = true;
                }else {
                	Row sccRow = sccsheet.createRow(sccrownum++);
                	sccRow = copyRow(sccRow , row);
                	isSuccess = true;
                }
    		}
    	}
    	if(isError) {
            writeFileinLocation (errWorkbook, validFile.getParent(),fileLogId);
    	}else {
    		writeSuccessFileinLocation(sccWorkbook, "C:\\revseed\\incomingDirectory\\hnf",fileLogId);
    	}
		return sccWorkbook;
	}
    
    public Row copyRow(Row emptyrow , Row rowtocopy) {
    	for (int i = 0; i < rowtocopy.getPhysicalNumberOfCells(); i++) {
    		Cell newCell = emptyrow.createCell(i);
    		if(i==0) {
    			try {
    				newCell.setCellValue(DateUtil.formatDate(rowtocopy.getCell(i).getDateCellValue(), "dd-MMM-yyyy"));
    			}catch(Exception ce) {
    				Cell sheetcell = rowtocopy.getCell(i);
    				if(newCell!=null&& sheetcell!=null) {
    					setCellTypeandValue(newCell,sheetcell);
    				}
    			}
    		}else {
    			try {     			
	        			if(rowtocopy.getCell(i).getCellType().equals(CellType.NUMERIC)) {
	        				newCell.setCellValue(rowtocopy.getCell(i).getNumericCellValue());
		        		}else if (rowtocopy.getCell(i).getCellType().equals(CellType.STRING)) {
		        			newCell.setCellValue(rowtocopy.getCell(i).getStringCellValue());
		        		}else {
		        			newCell.setCellValue(rowtocopy.getCell(i).getNumericCellValue());
		        		}
        			}catch(Exception ce) {
        				Cell firstSheetCell = rowtocopy.getCell(i);
        				if(newCell!=null&& firstSheetCell!=null) {
        				setCellTypeandValue(newCell,firstSheetCell);
        				}
        			}
    		}
    	}
    	return emptyrow;
    }
    
	public Cell setCellTypeandValue(Cell newCell,Cell oldCell) {
        // Set the cell data value
        switch (oldCell.getCellType()) {
        case BLANK:// Cell.CELL_TYPE_BLANK:
            newCell.setCellValue(oldCell.getStringCellValue());
            break;
        case BOOLEAN:
            newCell.setCellValue(oldCell.getBooleanCellValue());
            break;
        case FORMULA:
            newCell.setCellFormula(oldCell.getCellFormula());
            break;
        case NUMERIC:
            newCell.setCellValue(oldCell.getNumericCellValue());
            break;
        case STRING:
            newCell.setCellValue(oldCell.getRichStringCellValue());
            break;
        default:
            break;
        }
		return newCell;
    }
    
    private String validateData(Row row) {
    	String error = null;
    	if(row.getCell(CELL_INDEX_DATE)==null || checkDDMMMYYYY(row.getCell(CELL_INDEX_DATE))) {
			if(row.getCell(CELL_INDEX_DATE)==null) {
				error = "Date should be present and in dd-mmm-yyyy format";
			}else {
				error = "Date should be present and in dd-mmm-yyyy format "+row.getCell(CELL_INDEX_DATE).getStringCellValue();
			}
		}
    	if(row.getCell(CELL_INDEX_CAPACITY)==null || checkInteger(row.getCell(CELL_INDEX_CAPACITY))) {
			if(row.getCell(CELL_INDEX_CAPACITY)==null) {
				error = "Capacity should be present and in number format";
			}else {
				error = "Capacity should be present and in number format "+row.getCell(CELL_INDEX_CAPACITY).getStringCellValue();
			}
		}
    	if(row.getCell(CELL_INDEX_ROOMS)==null || checkInteger(row.getCell(CELL_INDEX_ROOMS))) {
			if(row.getCell(CELL_INDEX_ROOMS)==null) {
				error = "Rooms should be present and in number format";
			}else {
				error = "Rooms should be present and in number format "+row.getCell(CELL_INDEX_ROOMS).getStringCellValue();
			}
		}
    	if(row.getCell(CELL_INDEX_AVAILABILITY)==null || checkInteger(row.getCell(CELL_INDEX_AVAILABILITY))) {
			if(row.getCell(CELL_INDEX_AVAILABILITY)==null) {
				error = "Availability should be present and in number format";
			}else {
				error = "Availability should be present and in number format "+row.getCell(CELL_INDEX_AVAILABILITY).getStringCellValue();
			}
		}
    	if(row.getCell(CELL_INDEX_REVENUE)==null || checkInteger(row.getCell(CELL_INDEX_REVENUE))) {
			if(row.getCell(CELL_INDEX_REVENUE)==null) {
				error = "Revenue should be present and in number format";
			}else {
				error = "Revenue should be present and in number format "+row.getCell(CELL_INDEX_REVENUE).getStringCellValue();
			}
		}
    	
    	return error;
    }

	private XSSFWorkbook checkFileHeader(File validFile)  throws IOException {
    	FileInputStream fis = new FileInputStream(validFile);		
    	XSSFWorkbook workbook = new XSSFWorkbook(fis);
    	XSSFWorkbook errWorkbook = new XSSFWorkbook();
    	Boolean isError=false;
    	for(int i = 0; i < workbook.getNumberOfSheets(); i++){
    		Sheet sheet = workbook.getSheetAt(i);
    		Row headerRow = sheet.getRow(0);
    		List<String> headers = new ArrayList<String>();
            Iterator<Cell> cells = headerRow.cellIterator();
            while (cells.hasNext()) {
                Cell cell = (Cell) cells.next();
                RichTextString value = cell.getRichStringCellValue();
                headers.add(value.getString());
            }
            String errorString = validateHeader(headers);
            if(errorString!=null) {
            Sheet errsheet = errWorkbook.createSheet(i+"");
 	    	   Map<String, Object[]> data= new TreeMap<String, Object[]>();
 	    	   data.put("1",new Object[] { "Error" });
 	    	   data.put("2",new Object[] { errorString});
 	    	   sheet =writingDataintoSheet(data, errsheet);
 	    	   isError = true;
            }
    	}
    	if(isError) {
    		return errWorkbook;
    	}else {
    		return null;
    	}
    	 
	}
    
    private String validateHeader(List<String> headers) {
    	List<String> mandatoryHeaders = Arrays.asList("Date", "Capacity", "Rooms Sold","Availability","Revenue");
        List<String> allHeaders = new ArrayList<>(mandatoryHeaders);
        Map<String, Integer> headerIndexMap = IntStream.range(0, headers.size())
                                                    .boxed()
                                                    .collect(Collectors.toMap(i -> headers.get(i), i -> i));

        if (!headers.containsAll(mandatoryHeaders)) {
            return "Mandatory headers are not present";
        }
        // Check if the manadatory headers are in order
        Integer result = mandatoryHeaders.stream()
                        .map(headerIndexMap::get)
                        .reduce(-1, (x, hi) -> x < hi ? hi : headers.size());


        if (result == headers.size()) {
            return "Mandatory headers are not in order";
        }
        return null;
    }
    
    
	private XSSFWorkbook checkFileSize(File validFile) throws IOException {
    	FileInputStream fis = new FileInputStream(validFile);		
    	 XSSFWorkbook workbook = new XSSFWorkbook(fis);
    	 XSSFWorkbook errWorkbook = new XSSFWorkbook();
    	 Boolean isError=false;
    	 for(int i = 0; i < workbook.getNumberOfSheets(); i++){
    	       if(isSheetEmpty(workbook.getSheetAt(i))) {
    	    	   Sheet sheet = errWorkbook.createSheet(i+"");
    	    	   Map<String, Object[]> data= new TreeMap<String, Object[]>();
    	    	   data.put("1",new Object[] { "Error" });
    	    	   data.put("2",new Object[] { "Sheet " + i + " has no data: "});
    	    	   sheet =writingDataintoSheet(data, sheet);
    	    	   isError = true;
    	       }
    	}
    	if(isError) {
    		return errWorkbook;
    	}else {
    		return null;
    	}
    	 
	}
    
    public void writeFileinLocation (XSSFWorkbook workbook, String errorFilePath, Integer fileLogId) throws IOException {
    	errorFilePath = errorFilePath.replace("\\", "/");
    	String fileName = errorFilePath+"/"+getErrorFileName();
    	String csvFileName = errorFilePath+"/"+getCSVFileName();
    	setFileErrorLog(fileLogId,fileName);
    	FileOutputStream out = new FileOutputStream(new File(fileName));
            workbook.write(out);
            File inputFile = new File(fileName);
            File outputFile = new File(csvFileName);
            sysutil.XlsxToCsvConvertor(inputFile, outputFile);
            out.close();
    }
    
    public void writeSuccessFileinLocation (XSSFWorkbook workbook, String filePath, Integer fileLogId) throws IOException {
    	filePath = filePath.replace("\\",  "/");
    	String xlsxfileName = getSuccFileName();
    	String csvFileName = getCSVFileName();
    	String xlsxfilePath =  filePath+"/"+xlsxfileName;
    	String csvFilePath = filePath+"/"+csvFileName;
    	FileOutputStream out = new FileOutputStream(new File(xlsxfilePath));
        workbook.write(out);
        File inputFile = new File(xlsxfilePath);
        File outputFile = new File(csvFilePath);
        sysutil.XlsxToCsvConvertor(inputFile, outputFile);
        out.close();
    	FileLogs fileLogs = fileLogRepository.findById(fileLogId).orElse(null);
    	fileLogs.setFilePath(csvFilePath);
    	fileLogs.setFileName(csvFileName);
    	fileLogRepository.save(fileLogs);
    }
    
    public String getErrorFileName() {
    	DateTimeFormatter f = DateTimeFormatter.ofPattern( "'HnFErrorReport_'uuuuMMdd'T'HHmmss'.xlsx'" ) ;
    	String fileName = OffsetDateTime.now( ZoneOffset.UTC ).truncatedTo( ChronoUnit.SECONDS ).format( f );
    	return fileName;
    }
    
    public String getSuccFileName() {
    	DateTimeFormatter f = DateTimeFormatter.ofPattern( "'HnFReport_'uuuuMMdd'T'HHmmss'.xlsx'" ) ;
    	String fileName = OffsetDateTime.now( ZoneOffset.UTC ).truncatedTo( ChronoUnit.SECONDS ).format( f );
    	return fileName;
    }
    
    public String getCSVFileName() {
    	DateTimeFormatter f = DateTimeFormatter.ofPattern( "'HnFReport_'uuuuMMdd'T'HHmmss'.csv'" ) ;
    	String fileName = OffsetDateTime.now( ZoneOffset.UTC ).truncatedTo( ChronoUnit.SECONDS ).format( f );
    	return fileName;
    }
    
    public Sheet writingDataintoSheet(Map<String, Object[]> data,Sheet sheet) {
        Set<String> keyset = data.keySet();
        int rownum = 0;
        for (String key : keyset) {      	 
            // Creating a new row in the sheet
            Row row = sheet.createRow(rownum++);
            Object[] objArr = data.get(key);
            int cellnum = 0;
            for (Object obj : objArr) {
                // This line creates a cell in the next
                //  column of that row
                Cell cell = row.createCell(cellnum++);
                if (obj instanceof String)
                    cell.setCellValue((String)obj);
                else if (obj instanceof Integer)
                    cell.setCellValue((Integer)obj);
            }
        }
		return sheet;
    }
    
    boolean isSheetEmpty(Sheet sheet){
        Iterator rows = sheet.rowIterator();
        while (rows.hasNext()) {
        	Row row = (Row) rows.next();
            Iterator cells = row.cellIterator();
            while (cells.hasNext()) {
            	Cell cell = (Cell) cells.next();
                 if(!cell.getStringCellValue().isEmpty()){
                     return false;
                 }
            }
        }
        return true;
 }

	private HistoryAndForecast save(Row row, int i, Integer clientId) throws RuntimeException {
        HistoryAndForecast hnf = new HistoryAndForecast();
        hnf.setClientId(clientId);
        try {
	        if (isValidDate(row.getCell(CELL_INDEX_DATE))) {
	            hnf.setDate(null != row.getCell(CELL_INDEX_DATE) ? DateUtil.toDate(row.getCell(CELL_INDEX_DATE).getStringCellValue(), "dd-MMM-yyyy") : new Date());
	        }
	        if (isValidCapacity(row.getCell(CELL_INDEX_CAPACITY), hnf)) {
	            hnf.setCapacity(null != row.getCell(CELL_INDEX_CAPACITY) ? (int) row.getCell(CELL_INDEX_CAPACITY).getNumericCellValue() : 0);
	        }
	        if (isValidRoomsSold(row.getCell(CELL_INDEX_ROOMS), hnf)) {
	            hnf.setRoomsSold(null != row.getCell(CELL_INDEX_ROOMS) ? (int) row.getCell(CELL_INDEX_ROOMS).getNumericCellValue() : 0);
	        }
	        if (isValidAvailability(row.getCell(CELL_INDEX_AVAILABILITY), hnf)) {
	            if(row.getCell(CELL_INDEX_AVAILABILITY)!=null) {
	        		if(row.getCell(CELL_INDEX_AVAILABILITY).getCellType().equals(CellType.NUMERIC)) {
	        			hnf.setAvailability((int) Math.round(row.getCell(CELL_INDEX_AVAILABILITY).getNumericCellValue()));
	        		}else if (row.getCell(CELL_INDEX_REVENUE).getCellType().equals(CellType.STRING)) {
	        			hnf.setAvailability(Integer.parseInt(row.getCell(CELL_INDEX_AVAILABILITY).getStringCellValue().replaceAll(",", "")));
	        		}else {
	        			hnf.setAvailability( 0);
	        		}
	        	}else {
	        		hnf.setAvailability(0);
	        	}
	        }
	        if (isValidRevenue(row.getCell(CELL_INDEX_REVENUE), hnf)) {
	        	if(row.getCell(CELL_INDEX_REVENUE)!=null) {
	        		if(row.getCell(CELL_INDEX_REVENUE).getCellType().equals(CellType.NUMERIC)) {
	        			hnf.setRevenue(new BigDecimal(row.getCell(CELL_INDEX_REVENUE).getNumericCellValue()));
	        		}else if (row.getCell(CELL_INDEX_REVENUE).getCellType().equals(CellType.STRING)) {
	        			hnf.setRevenue(new BigDecimal(row.getCell(CELL_INDEX_REVENUE).getStringCellValue().replaceAll(",", "")));
	        		}else {
	        			hnf.setRevenue( BigDecimal.ZERO);
	        		}
	        	}else {
	        		hnf.setRevenue(BigDecimal.ZERO);
	        	}
	        }
        }catch(Exception ce) {
        	ce.printStackTrace();
        }
        clientsRepository.findById(hnf.getClientId()).ifPresent(cl -> {
            hnf.setHotelId(cl.getHotelId());
        });

        return hnf;

    }

    Boolean isValidDate(Cell cell) {
        Boolean result = Boolean.TRUE;
        if (cell == null) {
            errorMessage.add("Date Should not be empty");
        } else if (StringUtil.isEmpty(cell.toString())) {
            errorMessage.add("Date Should not be empty");
        }

        return result;
    }

    Boolean isValidCapacity(Cell cell, HistoryAndForecast hnf) {
        Boolean result = Boolean.FALSE;
        if (cell == null) {
            errorMessage.add(hnf.getDate() + " : " + "Capacity Should not be empty");
        } else if (StringUtil.isEmpty(cell.toString())) {
            errorMessage.add(hnf.getDate() + " : " + "Capacity Should not be empty");
        } else if (cell.getNumericCellValue() < 1) {
            errorMessage.add(hnf.getDate() + " : " + "Capacity Should not be less than 1 ");
        } else {
            result = Boolean.TRUE;
        }
        //System.out.println("Cell is : "+cell.toString());
        return result;
    }

    Boolean isValidRoomsSold(Cell cell, HistoryAndForecast hnf) {
        Boolean result = Boolean.FALSE;
        if (cell == null) {
            errorMessage.add(hnf.getDate() + " : " + "Rooms Sold Should not be empty");
        } else if (StringUtil.isEmpty(cell.toString())) {
            errorMessage.add(hnf.getDate() + " : " + "Rooms Sold Should not be empty");
        } else if (cell.getNumericCellValue() < -1) {
            errorMessage.add(hnf.getDate() + " : " + "Rooms Sold Should not be less than 1 ");
        } else {
            result = Boolean.TRUE;
        }
        //System.out.println("Cell is : "+cell.toString());
        return result;
    }

    Boolean isValidAvailability(Cell cell, HistoryAndForecast hnf) {
        Boolean result = Boolean.FALSE;
        if (cell == null) {
            errorMessage.add(hnf.getDate() + " : " + "Availability Should not be empty");
        } else if (StringUtil.isEmpty(cell.toString())) {
            errorMessage.add(hnf.getDate() + " : " + "Availability Should not be empty");
        }/* else if (cell.getNumericCellValue() < -1) {
            errorMessage.add(hnf.getDate() + " : " + "Availability Should not be less than 1 ");
        }*/ else {
            result = Boolean.TRUE;
        }
        //System.out.println("Cell is : "+cell.toString());
        return result;
    }

    Boolean isValidRevenue(Cell cell, HistoryAndForecast hnf) {
        Boolean result = Boolean.FALSE;
        if (cell == null) {
            errorMessage.add(hnf.getDate() + " : " + "Revenue Should not be empty");
//        } else if (Integer.parseInt(cell.getStringCellValue().replaceAll(",", ""))< 0) {
//            errorMessage.add(hnf.getDate() + " : " + "Revenue Should not be empty");
        } else {
            result = Boolean.TRUE;
        }
        //System.out.println("Cell is : "+cell.toString());
        return result;
    }


    public String checkUpdateHnfByClientId(Integer clientId) {
        if (historyAndForecastRepository.findAllByClientIdAndUpdatedDate(clientId, new java.sql.Date((new Date()).getTime())).isPresent()) {
            return "true";
        }
        return "false";
    }
    
    private FileLogs setFileLog(Integer getclientId, String originalFilename,String filePath) {
    	FileLogs fileLogs = new FileLogs();
		fileLogs.setClientId(getclientId);
		fileLogs.setStatus(Status.RUNNING.toString());
		fileLogs.setUploadDate(new Date());
		fileLogs.setFileName(originalFilename);
		fileLogs.setType(ConstantUtil.HNFDATA);
		fileLogs.setFilePath(filePath);
		fileLogRepository.save(fileLogs);
		return fileLogs;
	}

	public FileLogs setFileLog(Integer fileLogId){
	    	FileLogs fileLogs = fileLogRepository.findOneById(fileLogId);	  
    		if(!fileLogs.getStatus().equals(Status.FAILED.toString())) {
    			fileLogs.setStatus(Status.COMPLETE.toString());
    			fileLogs.setUploadDate(new Date());
				fileLogRepository.save(fileLogs);
    		}
			return fileLogs;
	 }
	
	 public void setFileErrorLog(Integer fileLogId,String filePath){
		FileLogs fileLog = fileLogRepository.findOneById(fileLogId);
		fileLog.setStatus(Status.FAILED.toString());
		fileLog.setErrFilePath(filePath);
		fileLogRepository.save(fileLog);
	 }
	 
	 public String saveFiletoLocation(MultipartFile file, String path,Integer clientid) {
			try {
	            String fileName = file.getOriginalFilename();
	            logger.info("filename +++++++++ "+fileName);
	            String[] split = fileName.split("\\.csv");
	            InputStream is = file.getInputStream();
	            String fileNameChange =  clientid+ "_HnF_data_" + new Date().getTime()+ ".csv";
	            Files.copy(is, Paths.get(path + "/" + fileNameChange), StandardCopyOption.REPLACE_EXISTING);
//	            String csvFileName = path+"/"+getCSVFileName();
//	            File inputFile = new File(fileName);
//	            File outputFile = new File(csvFileName);
//	            sysutil.XlsxToCsvConvertor(inputFile, outputFile);
//	            logger.info("Upload for file has been completed ", file.getOriginalFilename());
//	            logger.info("Upload for file has been completed path "+path+"filename  "+fileNameChange);
	            logger.info("filename -------- "+fileNameChange+"========path"+path);
	            return path + "/" + fileNameChange;
	        } catch (IOException e) {
	        	e.printStackTrace();
	        	String msg = String.format("Failed to store file", file.getName());
	            throw new StorageException(msg, e);
	        }
		}
	
	public String saveFiletoLocationforexcel(MultipartFile file, String path) {
		try {
            String fileName = file.getOriginalFilename();
            String[] split = fileName.split("\\.xlsx");
            InputStream is = file.getInputStream();
            String fileNameChange = split[0] + "_" + new Date().getTime()+ ".xlsx";
            Files.copy(is, Paths.get(path + "/" + fileNameChange), StandardCopyOption.REPLACE_EXISTING);
//            String csvFileName = path+"/"+getCSVFileName();
//            File inputFile = new File(fileName);
//            File outputFile = new File(csvFileName);
//            sysutil.XlsxToCsvConvertor(inputFile, outputFile);
            logger.info("Upload for file has been completed ", file.getOriginalFilename());
            return path + "/" + fileNameChange;
        } catch (IOException e) {
        	e.printStackTrace();
        	String msg = String.format("Failed to store file", file.getName());
            throw new StorageException(msg, e);
        }
	}
	
 private Boolean checkInteger (Cell cell) {
        boolean numeric = false;
        DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
        try {
        	String str = formatter.formatCellValue(cell);
        	if(str!=null && !str.equals("")) {
        		Integer.parseInt(str.toString());
        		numeric = false;
        	}else {
        		numeric = true;
        	}
        } catch (NumberFormatException e) {
        	e.printStackTrace();
        	numeric = true;
        	try {
            	String str = formatter.formatCellValue(cell);
            	if(str!=null && !str.equals("")) {
            		Double.parseDouble(str.toString());
            		numeric = false;
            	}else {
            		numeric = true;
            	}
            } catch (NumberFormatException ce) {
            	ce.printStackTrace();
                numeric = true;
            }
        }
		return numeric;
 }
 
 
 
 private Boolean checkDDMMMYYYY (Cell cell) {
        boolean date = false;
        DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
        try {
        	String str = formatter.formatCellValue(cell);
        	DateUtil.toDateByAllFormat(str, "dd-MMM-YYYY");
        } catch (NumberFormatException e) {
        	e.printStackTrace();
        	date = true;
        }
		return date;
 }
}
