package com.meowlomo.ci.ems.bundle.interfaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;

/**
 * @author 陈琪
 *
 */
public class BaseBundleActivator implements BundleActivator {
	public static String MOInterfacePackageName = "com.meowlomo.ci.ems.bundle.interfaces.";
	public static String MOInterServiceName = "com.meowlomo.ci.ems.bundle.";
	protected BundleContext _context = null;
	protected Logger _logger = null;
	
	static Map<String, BundleContext> _bundleDict = new HashMap<String, BundleContext>();
	static Map<String, BaseBundleActivator> _bundleActivatorDict = new HashMap<String, BaseBundleActivator>();	//symboliName
	
	@SuppressWarnings("rawtypes")
	List<ServiceRegistration> _srs = new ArrayList<ServiceRegistration>();
	ServiceRegistration _eventSr = null;
	
	protected BaseBundleActivator(Logger logger){
		_logger = logger;
	}

	private BaseBundleActivator(){
		
	}
	
	public static BundleContext getBundleContext(String bundleName){
		return _bundleDict.get(bundleName);
	}
	
	public static BaseBundleActivator getBundleActivator(String bundleName){
		return _bundleActivatorDict.get(bundleName);
	}
	
	public static Object getTheServiceObject(String serviceInterfaceName) {
		String apiBundleName = "com.meowlomo.ci.ems.bundle.api";
		BaseBundleActivator apiActivator = getBundleActivator(apiBundleName);
		
		if (!serviceInterfaceName.contains(".")) {
			serviceInterfaceName = MOInterfacePackageName + serviceInterfaceName;			
		}
		try {
			return apiActivator.getServiceObject(serviceInterfaceName);
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	public static <T> T getTheServiceObject(String serviceBundleName, Class<T> serviceClass) {
		if (!serviceBundleName.contains(".")) {
			serviceBundleName = MOInterServiceName + serviceBundleName;			
		}
		
		BaseBundleActivator activator = getBundleActivator(serviceBundleName);
		
		if (null == activator) {
			System.err.println("Bundle name:[" + serviceBundleName + "] is null.");
			return null;
		} else {
	        try {
				return activator.getServiceObject(serviceClass);
			} catch (InstantiationException | IllegalAccessException e) {
				return null;
			}
		}
    }
	
	public Object getServiceObject(String clazz) throws InstantiationException, IllegalAccessException{
		ServiceReference<?> sr = _context.getServiceReference(clazz);  
		if (null != sr){
			Object serviceObj = _context.getService(sr);
			
			if (null != serviceObj){
				_logger.info("Get Service {} OK !!!!", clazz);
				return serviceObj;
			}
		}
		_logger.info("Get Service {} FAILED !!!!", clazz);
		return null;
	}
	
	public <T> T getServiceObject(Class<T> serviceClass) throws InstantiationException, IllegalAccessException{
		ServiceReference<?> sr = _context.getServiceReference(serviceClass.getName());
		if (null != sr){
			T serviceObj = (T)_context.getService(sr);
			
			if (null != serviceObj){
				_logger.info("Get Service {} OK !!!!", serviceClass.toString());
				return serviceObj;
			}
		}
		_logger.info("Get Service {} FAILED !!!!", serviceClass.toString());
		return null;
	}
	
	public EventAdmin getEventAdmin(){
		if (null == _context)
			return null;
		
		ServiceReference<EventAdmin> ref = _context.getServiceReference(EventAdmin.class);
		if (null == ref)
			return null;
		
		EventAdmin eventAdmin = _context.getService(ref);
		return eventAdmin;
	}
	
	protected void setContext(BundleContext context){
		if (null == context && null == _context)
			return;
		if (null == context && null != _context){
			_bundleDict.remove(_context.getBundle().getSymbolicName());
			_bundleActivatorDict.remove(_context.getBundle().getSymbolicName());
			_context = null;
		}
		else if(null != context && null == _context){
			_bundleDict.put(context.getBundle().getSymbolicName(), context);
			_bundleActivatorDict.put(context.getBundle().getSymbolicName(), this);
			_context = context;
		}else{//null != context && null != _context
			if (context != _context){
				_bundleDict.remove(_context.getBundle().getSymbolicName());
				_bundleActivatorDict.remove(_context.getBundle().getSymbolicName());
				_bundleDict.put(context.getBundle().getSymbolicName(), context);
				_bundleActivatorDict.put(context.getBundle().getSymbolicName(), this);
				_context = context;
			}
		}
	}
	
	public BundleContext getContext(){
		return _context;
	}
	
	protected void startMass(BundleContext context) throws Exception {
		setContext(context);
		printBundleInfo();
	}
	
	protected void startMass(BundleContext context, String serviceName, Object serviceImplObj) throws Exception {
		setContext(context);
		//TODO check if exist
		_srs.add(context.registerService(serviceName, serviceImplObj, null));
		printBundleInfo();
	}
	
	protected void startMass(BundleContext context, String[] serviceNames, Object[] serviceImplObjs) throws Exception {
		setContext(context);
		if (_srs.isEmpty()){
			if (serviceNames.length == serviceImplObjs.length){
				for(int i = 0; i < serviceNames.length; ++i)
					if (null != serviceNames[i] && null != serviceImplObjs[i] )
						_srs.add(context.registerService(serviceNames[i], serviceImplObjs[i], null));
			}
		}
		printBundleInfo();
	}

	private void printBundleInfo() {
		_logger.info(String.format("Bundle %s start.", this.getClass().getPackage().getName()));
		if (null != _context)
			_logger.info(String.format("    Bundle symbolic name: %s", _context.getBundle().getSymbolicName()));
	}
	
	protected void stopMass(BundleContext context) throws Exception {
		setContext(null);
		for (ServiceRegistration _sr :_srs){
			_sr.unregister();
		}
		_srs.clear();
		_logger.info(String.format("Bundle %s stop.", this.getClass().getPackage().getName()));
	}
	
	protected void registerEvent(String topic, EventHandler eh) {
		String[] topics = new String[] {topic}; 
	    Hashtable<String,String[]> ht = new Hashtable<String,String[]>(); 
	    ht.put(EventConstants.EVENT_TOPIC, topics);
	    if (null != _eventSr) _eventSr.unregister();
	    _eventSr = _context.registerService( 
	        EventHandler.class.getName(), 
	        eh, 
	        ht);
	}
	
	protected void unRegisterEvent() {
		if (null != _eventSr) _eventSr.unregister();
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
