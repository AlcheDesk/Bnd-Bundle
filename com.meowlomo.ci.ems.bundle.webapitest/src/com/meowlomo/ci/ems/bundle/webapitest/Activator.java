package com.meowlomo.ci.ems.bundle.webapitest;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.IWebApiTest;
import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;

public class Activator extends BaseBundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	public Activator() {
		super(logger);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		startMass(context, IWebApiTest.class.getName(), ContextConstant.refactor ? new WebApiTestImplNew(this) : new WebApiTestImpl(this));
		registerEvent("com/meowlomo/bundle/webapitest/*", new WebApiTestEventHandler());
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		stopMass(context);
		unRegisterEvent();
	}
}
