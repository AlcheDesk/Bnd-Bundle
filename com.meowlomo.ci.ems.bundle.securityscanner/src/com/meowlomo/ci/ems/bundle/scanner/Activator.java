package com.meowlomo.ci.ems.bundle.scanner;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IWebApiTest;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends BaseBundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	public Activator() {
		super(logger);
	}

	/**
	 * 
	 */
	public void start(BundleContext context) throws Exception {
		startMass(context, IWebApiTest.class.getName(), new NMapScanner());
//		registerEvent("com/meowlomo/bundle/webapitest/*", new WebApiTestEventHandler());
	}
	
	/**
	 * 
	 */
	public void stop(BundleContext context) throws Exception {
		stopMass(context);
		unRegisterEvent();
	}
}
