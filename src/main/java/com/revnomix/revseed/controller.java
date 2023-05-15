package com.revnomix.revseed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.revnomix.revseed.schedular.Service.DataSyncScheduleService;

@RestController
@CrossOrigin(origins = "*")
public class controller {
	@Autowired
	DataSyncScheduleService dataSyncScheduleService;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ResponseBody
	public String runDemoHotelScheduler() {
		dataSyncScheduleService.scheduleTimerDataSync();
		return "Success";
	}

}
