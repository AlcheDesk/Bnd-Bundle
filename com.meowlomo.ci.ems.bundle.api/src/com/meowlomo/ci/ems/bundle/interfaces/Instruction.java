package com.meowlomo.ci.ems.bundle.interfaces;

import org.json.JSONObject;

public class Instruction {
	
	public String getId() {
		return id;
	}

	public String getInput() {
		return input;
	}

	public String getInstructionType() {
		return instructionType;
	}

	public String getDriverType() {
		return driverType;
	}

	public boolean isDriver() {
		return isDriver;
	}

	public String getAction() {
		return action;
	}

	public String getIndex() {
		return index;
	}

	public String getOptions() {
		return options;
	}

	public String getTestCaseId() {
		return testCaseId;
	}

	public String getElementId() {
		return elementId;
	}

	public JSONObject getElementJson() {
		return elementJson;
	}
	
	public String getTarget() {
		return target;
	}

	public long getRunId() {
		return runId;
	}

	public String getUuid() {
		return uuid;
	}

	public long getInstructionRunId() {
		return instructionRunId;
	}

	public String getExtraData() {
		return extraData;
	}
	
	protected String id;
	protected String input;
	protected String instructionType; 			// webbrowser  ,webfunction, SQL
	protected String driverType; 				// webfunction, JDBC
	protected boolean isDriver;					//element 是否driver
	protected String action;
	protected String index; 					// logicalOrderIndex
	protected String options;

	protected String testCaseId;
	protected String elementId;
	protected JSONObject elementJson;
	protected String target;
	protected long runId;
	protected String uuid;
	protected long instructionRunId;
	
	protected String extraData;
	
	public static Instruction generate(String instruction) {
		Instruction ins = new Instruction();
		MOJSONObject insObj = new MOJSONObject(instruction);
		
		ins.id = 				insObj.getString("id");
		ins.input = 			insObj.getString("input");
		ins.instructionType = 	insObj.getString("instructionType");
		ins.driverType = 		insObj.getString("driverType");
		ins.isDriver = 			insObj.getBoolean("isDriver");
		ins.action = 			insObj.getString("action");
		ins.index = 			insObj.getString("index");
		ins.options = 			insObj.getString("options");
		ins.testCaseId = 		insObj.getString("testCaseId");
		ins.elementId = 		insObj.getString("elementId");
		ins.elementJson = 		insObj.optJSONObject("element");
		ins.target = 			insObj.getString("target");
		ins.runId = 			insObj.getLong("runId");
		ins.uuid = 				insObj.getString("uuid");
		ins.instructionRunId = 	insObj.getLong("instructionRunId");
		
		return ins;
	}
}
