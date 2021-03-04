package com.meowlomo.ci.ems.bundle.schemavalidator;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator;

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
		startMass(context, ISchemaValidator.class.getName(), new SchemaValidatorImpl(this));
		registerEvent("com/meowlomo/bundle/schemavalidator/*", new SchemaValidatorEventHandler());
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
