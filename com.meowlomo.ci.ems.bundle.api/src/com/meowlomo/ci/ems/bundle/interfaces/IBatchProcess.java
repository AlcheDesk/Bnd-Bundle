package com.meowlomo.ci.ems.bundle.interfaces;

import java.util.List;

/**
 * interface for test base's instructions batch process in bundle
 * @author qi.chen
 *
 */
public interface IBatchProcess {
	/**
	 * 
	 * @param jsonTask
	 * @return String ,json format of ExecutionResult,  that means it can deserialized to ExecutionResult
	 */
	String doTestProcess(String jsonTask, List<String> paramsInOut);
	
	/**
	 * tell the executor, the task has been time out.
	 */
	void notifyTimeout();
}
