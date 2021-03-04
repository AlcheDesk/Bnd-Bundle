package com.meowlomo.ci.ems.bundle.interfaces;

import java.io.Serializable;
import java.util.List;

public interface IInstructionExecutable {
	
	public String step(String instructionJson, List<String> paramsInOrOut);
	public String getExecutionEnvironmentInfo(String info);
	
	public class CompositeStepResult implements Serializable{
		private static final long serialVersionUID = 1749248410745556541L;
		
		public CompositeStepResult(String str) {
			
		}
	}
}
