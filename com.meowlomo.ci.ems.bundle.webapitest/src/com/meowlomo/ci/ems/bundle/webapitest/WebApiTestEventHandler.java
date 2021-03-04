package com.meowlomo.ci.ems.bundle.webapitest;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IWebApiTest;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;

public class WebApiTestEventHandler implements EventHandler {

	@Override
	public void handleEvent(Event event) {
		String topic = event.getTopic();
		if ("com/meowlomo/bundle/webapitest/dotest" == topic){
			String params = (String)event.getProperty("params");
			
			IWebApiTest ds = BaseBundleActivator.getTheServiceObject("webapitest", IWebApiTest.class);
			if (null != ds) {
				List<String> infoOut = new ArrayList<String>();
				String bTestResult = ds.doTestProcess(params, infoOut);
				System.out.println(bTestResult);
			}
		}else if ("com/meowlomo/bundle/webapitest/timeout" == topic){
			SGLogger.timeoutError();
		}
	}

}
