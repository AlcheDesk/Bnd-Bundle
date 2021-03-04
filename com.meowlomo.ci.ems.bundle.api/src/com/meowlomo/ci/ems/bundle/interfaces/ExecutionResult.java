package com.meowlomo.ci.ems.bundle.interfaces;

import org.json.JSONObject;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExecutionResult{
	public ExecutionResult(boolean result, int code, String info) {
		bOK = result;
		exitCode = code;
		msg = info;
	}
	
	public ExecutionResult(boolean result, String trueInfo, String falseInfo) {
		bOK = result;
		if (bOK)
			msg = trueInfo;
		else
			msg = falseInfo;
	}
	
	public ExecutionResult(boolean result, String info) {
		bOK = result;
		msg = info;
	}
	
	public ExecutionResult(String info) {
		bOK = false;
		msg = info;
	}
	
	public ExecutionResult() {
		bOK = true;
		msg = "";
	}
	
	public ExecutionResult setOK(boolean b) {
		bOK = b;
		return this;
	}
	
	@Override
	public String toString() {
		JSONObject exeResult = new JSONObject();
		
		exeResult.put("bOK", bOK);
		exeResult.put("exitCode", exitCode);
		exeResult.put("msg", msg);
		
		return exeResult.toString();
	}
	
	public static ExecutionResult fromString(String str) {
		JsonParser parser = new JsonParser();
		
		try {
			JsonObject instructionRun = parser.parse(str).getAsJsonObject();
			ExecutionResult er = new ExecutionResult();
			if (instructionRun.has("bOK")) {
				er.bOK = instructionRun.get("bOK").getAsBoolean();
			}
			if (instructionRun.has("msg")) {
				er.msg = instructionRun.get("msg").toString();
			}
			if (instructionRun.has("exitCode")) {
				er.exitCode = instructionRun.get("exitCode").getAsInt();
			}
			return er;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean bOK() {
		return bOK;
	}
	
	public String msg() {
		return msg;
	}
	
	public Integer exitCode() {
		return exitCode;
	}
	
	private boolean bOK;
	private String msg;
	private Integer exitCode = 0;	// 辅助字段，一般场景不需要   0:ok	1:exception	2:error
}