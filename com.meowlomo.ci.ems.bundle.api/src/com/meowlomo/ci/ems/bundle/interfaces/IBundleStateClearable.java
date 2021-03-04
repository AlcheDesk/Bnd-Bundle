package com.meowlomo.ci.ems.bundle.interfaces;

import org.json.JSONObject;

import com.meowlomo.ci.ems.bundle.utils.FileUtilConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;

public interface IBundleStateClearable {
	default JSONObject attachData(String data) {
		JSONObject param = new JSONObject(data);
		
		//TODO 放在此处,目前没有更好的初始化一定调用的环节
		InstructionOptions.resetInstance();

		setStandSingleton(param.optBoolean("standSingleton"));
		setLogbackToServer(param.optBoolean("logFileToServer"));
		setRunId(param.optLong("runId"));
		
		// TODO no multi call 
		SGLogger.clearLog();
		
		return param;
	}

	LocalState localState = new LocalState();
	
	default LocalState localtState() {
		return localState;
	}
	
	default boolean clearState(){
		SGLogger.outputLogFile("execution.log");
		setStandSingleton(false);
		setLogbackToServer(false);
		setRunId(0L);
		return true;
	}
	
	default boolean isStandSingleton() {
		return localState.standSingleton;
	}
	
	default boolean isLogbackToServer(){
		return localState.logbackToServer;
	}
	
	default long runId(){
		return localState.runId;
	}
	
	default void setStandSingleton(boolean stand){
		localState.standSingleton = stand;
	}
	
	default void setLogbackToServer(boolean logback){
		localState.logbackToServer = logback;
	}
	
	default void setRunId(long runId){
		localState.runId = runId;
	}
	
	class LocalState{
		public boolean standSingleton;
		public boolean logbackToServer;
		public long runId;
		public long instructionRunId;
		public long instructionId;
	};
}
