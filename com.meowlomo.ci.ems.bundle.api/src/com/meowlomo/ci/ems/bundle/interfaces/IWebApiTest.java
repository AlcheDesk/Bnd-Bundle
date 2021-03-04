package com.meowlomo.ci.ems.bundle.interfaces;

import java.util.List;

/**
 * @author 陈琪
 *
 */

public interface IWebApiTest extends IInstructionExecutable, IBundleStateClearable{
	//true;passed;
	//false;error;parameter xxx is not valid
	//false;exception;time out while find button 'btn-search'
	String doTestProcess(String jsonTask, List<String> infoOut);
	void notifyTimeout();
}
