package com.revnomix.revseed.Service;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.HnfType;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.SchedulerTimingAlt;
import com.revnomix.revseed.model.Status;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.SchedulerTimingAltRepository;
import com.revnomix.revseed.wrapper.SchedulerTimingDto;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.service.Response;

@Service
public class ParameterService {
    @Autowired
    private ParameterRepository parameterRepository;
    @Autowired
    private ClientsRepository clientsRepository;
    @Autowired
    private SchedulerTimingAltRepository schedulerTimingRepository;

    public List<Parameter> findAll() {
        return parameterRepository.findAll();
    }

    public List<Parameter> findAllByClientId(Integer clientId ) {
        return parameterRepository.findAllByClientId(clientId);
    }

    public Parameter findById(Integer id) {
        return parameterRepository.findById(id).orElse(new Parameter());

    }
    public Integer create(Parameter parameter) {
    	parameter.setModDate( new Date());
        return parameterRepository.save(parameter).getId();
    }

    public void update(Integer id, Parameter parameter) {
        parameter.setClientId(parameter.getClientId()).setDescription(parameter.getDescription()).setModDate(parameter.getModDate()).setParamName(parameter.getParamName())
                .setParamType(parameter.getParamType()).setUiOperator(parameter.getUiOperator()).setUiTag(parameter.getUiTag()).setAuthor(parameter.getAuthor());
        parameterRepository.save(parameter);
        if(parameter.getParamName().equals(ConstantUtil.USE_MIN_MAX_CALIB)) {
        	if(parameter.getParamValue().equalsIgnoreCase(ConstantUtil.TRUE)) {
        		Clients client = clientsRepository.findById(parameter.getClientId()).orElse(null);
        		if(client!=null) {
        			client.setAlgo(ConstantUtil.RCP);
        		}
        	}
        }
        updateClient(parameter);

    }

    private void updateClient(Parameter parameter) {
        if (parameter.getParamName().equalsIgnoreCase("hnf")) {
            clientsRepository.findById(parameter.getClientId()).ifPresent(c->{
                if (parameter.getParamValue().equals("0")) {
                    c.setHnf(HnfType.NONE);
                }
                if (parameter.getParamValue().equals("1")) {
                    c.setHnf(HnfType.NORMAL);
                }
                if (parameter.getParamValue().equals("2")) {
                    c.setHnf(HnfType.HYBRID);
                }
                clientsRepository.save(c);

            });
        }
    }

    public void createParametersForClients(Integer clientId){

        List<Parameter> pr = findAllByClientId(0).stream().map((p)->{
            Parameter par = new Parameter();
            par.setAuthor(p.getAuthor());
            par.setParamName(p.getParamName());
            par.setDescription(p.getDescription());
            par.setClientId(clientId);
            par.setParamType(p.getParamType());
            par.setModDate( new Date());
            par.setUiOperator(p.getUiOperator());
            par.setUiTag(p.getUiTag());
            par.setParamValue(p.getParamValue());
            return par;
        }).collect(Collectors.toList());

        parameterRepository.saveAll(pr);
    }
    
    public void createisMaxParametersForClients(Integer clientId,String paramValue,String paramName){
    		
    	Parameter par = new Parameter();
    	Optional<Parameter>  parameter = parameterRepository.findByClientIdAndParamName(clientId, paramName);
    	if (parameter.isPresent()) {
    		par = parameter.get();
    	}    
        par.setAuthor("ADMIN");
        par.setParamName(paramName);
        par.setDescription(paramName);
        par.setClientId(clientId);
        par.setParamType("Recommendation");
        par.setModDate( new Date());
        par.setUiTag("Max Hotel");
        par.setParamValue(paramValue);
        parameterRepository.save(par);
    }
    
    public void createUpdateParametersForClients(Integer clientId,Object paramValue,String paramName,String uiTag){
    	Parameter  parameter = parameterRepository.findByClientIdAndParamName(clientId, paramName).orElse(null);
    	if (parameter!=null) {
    		parameter.setAuthor("ADMIN");
    		parameter.setParamName(paramName);
    		parameter.setDescription(paramName);
    		parameter.setClientId(clientId);
    		parameter.setParamType("Recommendation");
    		parameter.setModDate( new Date());
    		parameter.setUiTag(uiTag);
    		parameter.setParamValue(paramValue.toString());
            parameterRepository.save(parameter);
    	}else {
    		parameter = new Parameter();
    		parameter.setAuthor("ADMIN");
    		parameter.setParamName(paramName);
    		parameter.setDescription(paramName);
    		parameter.setClientId(clientId);
    		parameter.setParamType("Recommendation");
    		parameter.setModDate( new Date());
    		parameter.setUiTag(uiTag);
    		parameter.setParamValue(paramValue.toString());
    	}
        
    }
    
    public void createMasterRateIdParametersForClients(Integer clientId,Long paramValue,String paramName){		
    	Parameter par = new Parameter();
    	Optional<Parameter>  parameter = parameterRepository.findByClientIdAndParamName(clientId, paramName);
    	if (parameter.isPresent()) {
    		par = parameter.get();
    	}    
    	if(par.getParamValue()==null || (par.getParamValue()!=null && !par.getParamValue().equals(paramValue))) {
    		par.setAuthor("ADMIN");
            par.setParamName(paramName);
            par.setDescription(paramName);
            par.setClientId(clientId);
            par.setParamType("Recommendation");
            par.setModDate( new Date());
            par.setUiTag("Master Rate Id");
            par.setParamValue(paramValue.toString());
            parameterRepository.save(par);
    	}
        
    }

    public void deleteById(Integer id) {
        parameterRepository.deleteById(id);
    }

	public SchedulerTimingDto findSchedulerTimings(Long clientId) {
		List<SchedulerTimingAlt> schedulerTimingList = schedulerTimingRepository.findAllByClientIdAndSchedulerName(clientId,ConstantUtil.REC);
		SchedulerTimingDto stdto = new SchedulerTimingDto();
			if(schedulerTimingList.size()>0) {
				SchedulerTimingAlt schedulerTiming = schedulerTimingList.get(0);	
				stdto.setId(schedulerTiming.getId());
				stdto.setClientId(schedulerTiming.getClientId());
				ArrayList <String> timingList = new ArrayList<String>();
				for(SchedulerTimingAlt alt : schedulerTimingList) {
					if(schedulerTiming.getTiming()!=null) {
						timingList.add(alt.getTiming().toString().substring(0, 5));
					}
				}
				stdto.setTimings(timingList);
				Parameter is12HrParam = parameterRepository.findByClientIdAndParamName(clientId.intValue(), ConstantUtil.IS12HR).orElse(null);
				if(is12HrParam!=null) {
					if(is12HrParam.getParamValue().equals(ConstantUtil.TRUE)) {
						stdto.setToggleflag(true);
					}else {
						stdto.setToggleflag(false);
					}
				}else {
					stdto.setToggleflag(false);
				}
			}else {			
				stdto.setClientId(clientId);
				stdto.setToggleflag(false);
			}
		return stdto;
	}

	public Response saveUpdateSchedulerTimings(SchedulerTimingDto schedulerTimingDto) {
		Response res = new Response();
		try {
			if(schedulerTimingDto.getId()!=null) {
				List<SchedulerTimingAlt> schedulerTimingList = schedulerTimingRepository.findAllByClientIdAndSchedulerName(schedulerTimingDto.getClientId(),ConstantUtil.REC);
				List<SchedulerTimingAlt> schedulerTimingDataList = schedulerTimingRepository.findAllByClientIdAndSchedulerName(schedulerTimingDto.getClientId(),ConstantUtil.DATA);
				schedulerTimingList.addAll(schedulerTimingDataList);
				if(schedulerTimingList!=null) {	
					schedulerTimingRepository.deleteAll(schedulerTimingList);
				}
			}
			
			ArrayList<String> timings = schedulerTimingDto.getTimings();
			for(String t:timings) {
				SchedulerTimingAlt schedulerTiming = new SchedulerTimingAlt();
				schedulerTiming.setClientId(schedulerTimingDto.getClientId());
				schedulerTiming.setSchedulerName(ConstantUtil.REC);
				schedulerTiming.setTiming(Time.valueOf(t+":00"));
				schedulerTiming.setIsRunToday(false);
				schedulerTimingRepository.save(schedulerTiming);
				SchedulerTimingAlt schedulerTimingData = new SchedulerTimingAlt();
				schedulerTimingData.setClientId(schedulerTimingDto.getClientId());
				schedulerTimingData.setSchedulerName(ConstantUtil.DATA);
				schedulerTimingData.setTiming(DateUtil.addTime(Time.valueOf(t+":00"), -1, 0));
				schedulerTimingData.setIsRunToday(false);
				schedulerTimingRepository.save(schedulerTimingData);
			}
			createUpdateParametersForClients(schedulerTimingDto.getClientId().intValue(),schedulerTimingDto.getToggleflag(),ConstantUtil.IS12HR,ConstantUtil.IS12HRUITAG);
			res.setMessage("Updated Successfully");
			res.setStatus(Status.SUCCESS);
		}
		catch (Exception e) {
			e.printStackTrace();
			res.setMessage("Update Failed");
			res.setStatus(Status.FAILED);
		}
		return res;
	}
}

