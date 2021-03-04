/**
 * 
 */
package com.meowlomo.ci.ems.bundle.interfaces;

import org.osgi.framework.BundleException;

/**
 * @author tester
 *
 */
public class MeowlomoBundleBaseException extends BundleException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4669561058657722503L;

	public MeowlomoBundleBaseException(String msg, Throwable cause) {
		super(msg, 0, cause);
	}

	public MeowlomoBundleBaseException(String msg) {
		super(msg, 0);
	}

	public MeowlomoBundleBaseException(String msg, int type, Throwable cause) {
		super(msg, cause);
	}

	public MeowlomoBundleBaseException(String msg, int type) {
		super(msg);
	}

	
}
