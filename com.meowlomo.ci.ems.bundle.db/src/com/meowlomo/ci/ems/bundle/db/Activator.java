package com.meowlomo.ci.ems.bundle.db;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IDataSource;

public class Activator extends BaseBundleActivator {
	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	
	public Activator(){
		super(logger);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		startMass(context, IDataSource.class.getName(), new DataBaseImpl());
		registerEvent("com/meowlomo/bundle/db/*", new DBEventHandler());
		
//		Dictionary<String, String> msg = new Hashtable<String, String>();
//		msg.put("path", "C:\\workspace\\com.meowlomo.ci.beavor.bundle.db\\dbconfig.xml");
//		
//		EventAdmin eventAdmin = getEventAdmin();
//		if (null != eventAdmin) {
//			Event reportGeneratedEvent = new Event("com/meowlomo/bundle/db/init", msg);
//			eventAdmin.postEvent(reportGeneratedEvent);
//			return;
//		}
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
