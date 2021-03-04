package com.meowlomo.ci.ems.bundle.interfaces;

/**
 * @author 陈琪
 *
 */

public interface IWebDriver extends IInstructionExecutable, IBundleStateClearable{
	String doTestProcess(String jsonTask);
	void notifyTimeout();
}
