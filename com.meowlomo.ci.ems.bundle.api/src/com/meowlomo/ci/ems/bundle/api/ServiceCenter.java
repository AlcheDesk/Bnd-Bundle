/**
 * 
 */
package com.meowlomo.ci.ems.bundle.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.meowlomo.ci.ems.bundle.interfaces.IServiceCenter;

/**
 * @author tester
 *
 */
public class ServiceCenter implements IServiceCenter {

	static Set<String> _serviceNames = null;
	Map<String, ServiceReference> _serviceMap = new HashMap<String, ServiceReference>();
	Map<String, ServiceTracker> _serviceTrackMap = new HashMap<String, ServiceTracker>();
	BundleContext _context = null;
	
	static {
		_serviceNames = new HashSet<String>();
		
		//todo. need init by binary text or sth like db
		_serviceNames.add("com.meowlomo.ci.beaver.bundle.interfaces.IExample");
		_serviceNames.add("com.meowlomo.ci.beaver.bundle.interfaces.IServiceCenter");
	}
	
	public ServiceCenter(BundleContext apiBundleContext){
		_context = apiBundleContext;
	}
	
	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IServiceCenter#getService(java.lang.String)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getService(String serviceName) {
		// TODO Auto-generated method stub
		if (null != _context && _serviceNames.contains(serviceName)){
			Object service = getServiceIfExists(serviceName);
			if (null == service && !_serviceTrackMap.containsKey(serviceName)){
				//add service tracker
				addServiceTracker(serviceName);
				service = getServiceIfExists(serviceName);
			}
			return service;
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object getServiceIfExists(String serviceName){
		if (_serviceTrackMap.containsKey(serviceName)){
			if (_serviceMap.containsKey(serviceName)){
				ServiceReference srReference = _serviceMap.get(serviceName);
				if (null != srReference){
					return _context.getService(srReference);						
				}
			}
		}
		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void addServiceTracker(String serviceName){
		//add service tracker
		_serviceTrackMap.put(serviceName,  new ServiceTracker(_context, serviceName, null){
			public Object addingService(ServiceReference reference) {
				_serviceMap.put(serviceName, reference);
				
				Object serviceTmp = reference.getBundle().getBundleContext().getService(reference);
	        	System.out.println("adding service: " + serviceTmp.getClass().getName());
	        	
		        return super.addingService(reference);
			}
			
			@Override
		    public void removedService(ServiceReference reference, Object service) {
//				String serviceName = service.getClass().getName();
				_serviceMap.remove(service.getClass().getName());
				
		        System.out.println("removed service: " + service.getClass().getName());
		        super.removedService(reference, service);
		    }
		});
	}
}
