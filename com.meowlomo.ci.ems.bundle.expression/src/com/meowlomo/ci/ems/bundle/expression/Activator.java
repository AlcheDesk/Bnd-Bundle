package com.meowlomo.ci.ems.bundle.expression;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IExpression;

public class Activator  extends BaseBundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	
	public Activator(){
		super(logger);
	}
	
	public void start(BundleContext context) throws Exception {
		startMass(context, IExpression.class.getName(), new ExpressionImpl());
		registerEvent("com/meowlomo/bundle/expression/*", new ExpressionEventHandler());
		logger.info("start expression bundle");
	}

	public void stop(BundleContext context) throws Exception {
		stopMass(context);
		unRegisterEvent();
		logger.info("stop expression bundle");
	}
}
