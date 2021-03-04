package com.meowlomo.ci.ems.bundle.utils;

import org.json.JSONObject;

public class FileUtilConstant {
	public static int screenShootIndex = 0;
	public static int remoteScreenShootIndex = 0;
	public static String TEST_CASE_NAME = "";
	public static String TEST_CASE_RESULT_FOLDER = "";
	public static String REMOTE_TEST_CASE_RESULT_FOLDER = "";
	public static String LOG_FOLDER = "";
	public static String UUID = "";
	public static String REMOTE_INSTRUCTION_RESULT_FOLDER = "";
	
	public static String remoteScreenShootFileName = "";
	public static String INSTRUCTION = "";
	public static long EXCEL_ROW_NUMBER = 0l;
	public static String EXCEL_ROW_NUMBER_STRING = "";
	
	public static void reset() {
		screenShootIndex = 0;
		remoteScreenShootIndex = 0;
//		TEST_CASE_NAME = "";
//		TEST_CASE_RESULT_FOLDER = "";
//		REMOTE_TEST_CASE_RESULT_FOLDER = "";
//		LOG_FOLDER = "";
//		UUID = "";
//		REMOTE_INSTRUCTION_RESULT_FOLDER = "";
//		remoteScreenShootFileName = "";
//		INSTRUCTION = "";
//		EXCEL_ROW_NUMBER = 0l;
//		EXCEL_ROW_NUMBER_STRING = "";
	}
	
	public static String genTestcaseResultFolder(JSONObject testCase) {
		TEST_CASE_NAME = testCase.getString("name");
		UUID = testCase.getString("uuid");
		return "/log/" + TEST_CASE_NAME + "_" + UUID + "/";
	}
	
	public static void updateInstructionIndexString(String excelRowId) {
		excelRowId = excelRowId == null ? "" : excelRowId;
		EXCEL_ROW_NUMBER = Integer.parseInt(excelRowId);

		if (0 == EXCEL_ROW_NUMBER)
			EXCEL_ROW_NUMBER_STRING = excelRowId;
		else {
			// format digit
			String format = "%1$04d";
			excelRowId = String.format(format, FileUtilConstant.EXCEL_ROW_NUMBER);
			EXCEL_ROW_NUMBER_STRING = excelRowId;
		}
	}
    
    public static String normalIndexName() {
    	return String.format("[%1$s][%2$s]", EXCEL_ROW_NUMBER_STRING, INSTRUCTION);
    }
    
    public static String tailIndexName(String tail) {
    	return String.format("[%1$s][%2$s]%3$s", EXCEL_ROW_NUMBER_STRING, INSTRUCTION, tail);
    }
}
