package com.meowlomo.ci.ems.bundle.jmeter;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IStressTest;

public class Activator extends BaseBundleActivator {
	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	
	public Activator() {
		super(logger);
	}

	public void start(BundleContext context) throws Exception {
		startMass(context, IStressTest.class.getName(), new StressTestImpl());
	}
	
	public void stop(BundleContext context) throws Exception {
		stopMass(context);
	}
}
