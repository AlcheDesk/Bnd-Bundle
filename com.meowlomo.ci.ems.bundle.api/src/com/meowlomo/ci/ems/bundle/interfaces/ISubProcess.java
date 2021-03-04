/**
 * 
 */
package com.meowlomo.ci.ems.bundle.interfaces;

import java.util.List;

/**
 * @author 陈琪
 *
 */
public interface ISubProcess {
	public String doProcess(String jsonTask, List<String> infoOut);
	void notifyTimeout();
}
