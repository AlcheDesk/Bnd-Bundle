package com.meowlomo.ci.ems.bundle.webdriver;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IDataSource;
import com.meowlomo.ci.ems.bundle.interfaces.IWebDriver;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;

public class WebDriverEventHandler implements EventHandler {

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		String topic = event.getTopic();
		if ("com/meowlomo/bundle/webdriver/dotest" == topic){
			String params = (String)event.getProperty("params");
			IWebDriver wd = BaseBundleActivator.getTheServiceObject("webdriver", IWebDriver.class);
			if (null != wd) {
				//TODO
//				int testResult = wd.doTestProcess(params);
				String testResult = wd.doTestProcess(params);
				System.out.println("WebDriver doTest process result:" + testResult);
			} else {
				System.err.println("Bundle web driver not ready.");
			}
		}else if ("com/meowlomo/bundle/webdriver/timeout" == topic){
			SGLogger.timeoutError();
		}
	}
}
