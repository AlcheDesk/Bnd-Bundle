package com.meowlomo.ci.ems.bundle.webdriver;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IWebDriver;

public class Activator extends BaseBundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	
	public Activator() {
		super(logger);
	}

	public void start(BundleContext context) throws Exception {
		
		startMass(context, IWebDriver.class.getName(), new WebDriverImplNew(this));
		registerEvent("com/meowlomo/bundle/webdriver/*", new WebDriverEventHandler());
	}
	
	public void stop(BundleContext context) throws Exception {
		stopMass(context);
		unRegisterEvent();
	}

}
