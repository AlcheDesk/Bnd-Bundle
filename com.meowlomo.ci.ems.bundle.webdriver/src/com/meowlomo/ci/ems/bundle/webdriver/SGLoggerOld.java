package com.meowlomo.ci.ems.bundle.webdriver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IDataSource;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil.MethodType;
import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.JSONUtil;

public class SGLoggerOld {
	private static LinkedList<String> results = new LinkedList<String>();
	private static DateFormat  dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private static PrintStream ps = System.out;
	private static IHttpUtil http = null;
	private static String executionLogUrl = "";
	private static String stepLogUrl = "";	//TODO some like dynamic url for params mixed with it
	private static boolean stepInfoOne = false;
	private static boolean stepEndOne = false;
	private static IHttpUtil.MethodType executionLogUrlType = MethodType.POST;
	private static IHttpUtil.MethodType stepLogUrlType = MethodType.POST;

	private static JSONObject stepParams = new JSONObject();
	private static JSONObject executionParams = new JSONObject();

	private static String[] endStrings = new String[]{"INSTRUCTION_END","FAIL","ERROR","TIME_OUT","PASS","ELEMENT_NOT_FOUND"};
	private static List<String> endStringList = Arrays.asList(endStrings);

	private static String[] infoStrings = new String[]{"INSTRUCTION"};
	private static List<String> infoStringList = Arrays.asList(infoStrings);

	public static boolean bUseNewLog = true;

//	logLevel
//	1	N/A
//	2	PASS
//	3	FAIL
//	4	WIP
//	5	ERROR
//	6	INFO
//	7	WARNING
//	8	DEBUG
//	9	TRACE

	private static void formatLogMsg(String tip, String message, int level){
		String line = tip + message;
		doublePrintOut(line, level);
	}

	private static void formatLogMsg(String info, String message, String logLevel, String stepLogType){
		String line = info + message;
		doublePrintOut(line, logLevel, stepLogType);
	}

	private static void doublePrintOut(String info, String logLevel, String stepLogType){
		SGLoggerOld.results.add(dateFormat.format(new Date()) + info);
		if (null != http){
			//过去的step log,	业务级别的 step log type
			do{
				if (!stepLogUrl.isEmpty() && !stepLogType.isEmpty()){

					//TODO 控制相关语句最多一句,此处作多写控制,error时可能报告的信息没有落在关键点上
					if (infoStringList.contains(stepLogType)){
						if (!stepInfoOne)
							stepInfoOne = true;
						else
							break;
					}
					else if (endStringList.contains(stepLogType)){
						if (!stepEndOne)
							stepEndOne = true;
						else
							break;
					}

					stepParams.put("message", info);
					stepParams.put("type", stepLogType);
					if (ContextConstant.refactor)
						http.addStepLog(stepParams.toString());
					else {
						JSONArray array = new JSONArray();
						array.put(stepParams);
						http.request(stepLogUrl, array.toString(), stepLogUrlType);
					}
				}
			}while(false);

			JSONObject msg = new JSONObject();
			msg.put("message", info);
			msg.put("logLevel", logLevel);
			
			if (ContextConstant.refactor)
				http.addExecutionStepLog(msg.toString());
			//Execution 新增的执行级别的log
			else if (!executionLogUrl.isEmpty() && !logLevel.isEmpty()){
				JSONArray array = new JSONArray();
				array.put(JSONUtil.merge(executionParams, msg));
				http.request(executionLogUrl, array.toString(), executionLogUrlType);
			}
		}
		else
			ps.println(dateFormat.format(new Date()) + info);
	}

	private static void doublePrintOut(String line, int level){
		SGLoggerOld.results.add(dateFormat.format(new Date()) + line);
		if (null != http){
			if (!stepLogUrl.isEmpty()){
				stepParams.put("message", line);
				stepParams.put("levelId", level);
				JSONArray array = new JSONArray();
				array.put(stepParams);

				http.request(stepLogUrl, array.toString(), stepLogUrlType);
			}
		}
		else
			ps.println(dateFormat.format(new Date()) + line);
	}

	public static void clearLogBackRemoteApi(){
		if (!stepLogUrl.isEmpty()){
			stepLogUrl = "";
			stepLogUrlType = MethodType.POST;
			stepInfoOne = false;
			stepEndOne = false;
		}

		if (!executionLogUrl.isEmpty()){
			executionLogUrl = "";
			executionLogUrlType = MethodType.POST;
		}
		stepParams = new JSONObject();
		executionParams = new JSONObject();
	}

	public static void attachLogBackRemoteApi(String url, IHttpUtil.MethodType type, String humanUrl
			, IHttpUtil.MethodType humanType, long runId, long instructionRunId){
		if (null == http){
			try {
				URL u = new URL(url);
				http = BaseBundleActivator.getTheServiceObject("httpclient", IHttpUtil.class);
				if (null == http)
					http = BaseBundleActivator.getTheServiceObject("curl", IHttpUtil.class);
				
			} catch (MalformedURLException e) {
			}
		}
		stepLogUrl = url;
		stepLogUrlType = type;
		stepInfoOne = false;
		stepEndOne = false;

		executionLogUrl = humanUrl;
		executionLogUrlType = humanType;
		executionParams.put("runId", runId);
		executionParams.put("instructionResultId", instructionRunId);
	}

	public static void outputLogFile(String logFileName)  {
		File file = new File(logFileName);
		if (!file.exists()) {
			try {
				// Try creating the file
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		try {
//			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(logFileName), StandardCharsets.UTF_8);
//			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			BufferedWriter bw =  new BufferedWriter( new OutputStreamWriter(new FileOutputStream(logFileName),"UTF-8"));
			for (int i = 0; i < SGLoggerOld.results.size(); i++) {

				bw.write(SGLoggerOld.results.get(i).toString());
				bw.newLine();
//				writer.write(SGLogger.results.get(i).toString()+System.getProperty("line.separator"));
			}
			bw.close();
		}catch (IOException e){
			e.printStackTrace();
		}
	}

	public static void timeoutError(){
		try{
			if (bUseNewLog)
				formatLogMsg(" [  任务执行超时  ] ", "task time out.", "ERROR", "TIME_OUT");
			else
				formatLogMsg(" [  任务执行超时  ] ", "task time out.", 5);

		}catch(Exception e){
			System.out.println("exception while time out notify to ATM");
		}
	}

	public static void error(String message){
		if (bUseNewLog)
			formatLogMsg(" [      错误      ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [      错误      ] ", message, 5);
	}

	public static void info(String message, boolean bCallRemoteApi){
		if (bCallRemoteApi) {
			if (bUseNewLog)
				formatLogMsg(" [      信息      ] ", message, "INFO", "INSTRUCTION");
			else
				formatLogMsg(" [      信息      ] ", message, 6);
		} else {
			SGLoggerOld.results.add(dateFormat.format(new Date()) + message);
			ps.println(dateFormat.format(new Date()) + message);
		}
	}

	public static void info (String message){
		if (bUseNewLog)
			formatLogMsg(" [      信息      ] ", message, "INFO", "INSTRUCTION");
		else
			formatLogMsg(" [      信息      ] ", message, 6);
	}

//	public static void fatal (String message){
//		formatLogMsg(" [      致命      ] ", message, 3);
//	}

//	public static void page (String message){
//		formatLogMsg(" [      模组       ] ", message, 6);
//	}

//	public static void step (String message){
//		formatLogMsg(" [      步骤       ] ", message, 6);
//	}

	public static void actionComplete (String message){
		if (bUseNewLog)
			formatLogMsg(" [  动作执行完毕  ] ", message, "INFO", "INSTRUCTION");//"INSTRUCTION_END");
		else
			formatLogMsg(" [  动作执行完毕  ] ", message, 6);
	}

	public static void verifySuccess (String message){
		if (bUseNewLog)
			formatLogMsg(" [    验证成功    ] ", message, "INFO", "INSTRUCTION");//"INSTRUCTION_END");
		else
			formatLogMsg(" [    验证成功    ] ", message, 6);
	}

	public static void verifyError (String message){
		if (bUseNewLog)
			formatLogMsg(" [    验证失败    ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [    验证失败    ] ", message, 3);
	}

	public static void warn (String message){
		if (bUseNewLog)
			formatLogMsg(" [      警告      ] ", message, "WARN", "ERROR");
		else
			formatLogMsg(" [      警告      ] ", message, 7);
	}

	public static void done (String message){
		if (bUseNewLog)
			formatLogMsg(" [      完成      ] ", message, "INFO", "");
		else
			formatLogMsg(" [      完成      ] ", message, 6);
	}

	public static void testStopped (String message){
		if (bUseNewLog)
			formatLogMsg(" [    测试结束    ] ", message, "INFO", "PASS");
		else
			formatLogMsg(" [    测试结束    ] ", message, 6);
	}

//	public static void tryTo(String message){
//		formatLogMsg(" [      尝试      ] ", message, 6);
//	}

	public static void elementTimeOut(String message){
		if (bUseNewLog)
			formatLogMsg(" [  未能发现元素  ] ", message, "ERROR", "ELEMENT_NOT_FOUND");
		else
			formatLogMsg(" [  未能发现元素  ] ", message, 3);
	}

	public static void wrongValue(String message){
		if (bUseNewLog)
			formatLogMsg(" [    错误数值    ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [    错误数值    ] ", message, 3);
	}

	public static void notValidSelection(String message){
		if (bUseNewLog)
			formatLogMsg(" [    无效选项    ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [    无效选项    ] ", message, 3);
	}

	public static void invalidInput(String message){
		if (bUseNewLog)
			formatLogMsg(" [    无效输入    ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [    无效输入    ] ", message, 3);
	}

	public static void invalidAction(String message){
		if (bUseNewLog)
			formatLogMsg(" [    无效动作    ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [    无效动作    ] ", message, 3);
	}

//	public static void saveData(String message){
//		formatLogMsg(" [    保存数据    ] ", message, 6);
//	}

	public static void alert(String message){
		if (bUseNewLog)
			formatLogMsg(" [      弹窗      ] ", message, "INFO", "");
		else
			formatLogMsg(" [      弹窗      ] ", message, 6);
	}

//	public static void clearLog (){
//		SGLogger.results.clear();
//	}

//	public static void instruction(String message) {
//		if (bUseNewLog)
//			formatLogMsg(" [      指令      ] ", message, "INFO", "INSTRUCTION");
//		else
//			formatLogMsg(" [      指令      ] ", message, 6);
//	}

	public static void instructionStart(String message) {
		if (bUseNewLog)
			formatLogMsg(" [    指令开始    ] ", message, "INFO", "INSTRUCTION_BEGIN");
		else
			formatLogMsg(" [    指令开始    ] ", message, 6);
	}

	public static void instructionEnd(String message) {
		if (bUseNewLog)
			formatLogMsg(" [    指令结束    ] ", message, "INFO", "INSTRUCTION");//"INSTRUCTION_END");
		else
			formatLogMsg(" [    指令结束    ] ", message, 6);
	}

	public static void errorMessage(String message) {
		if (bUseNewLog)
			formatLogMsg(" [    错误信息    ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [    错误信息    ] ", message, 3);//TODO
	}

	public static void pass(String message) {
		if (bUseNewLog)
			formatLogMsg(" [      通过      ] ", message, "INFO", "PASS");
		else
			formatLogMsg(" [      通过      ] ", message, 2);
	}

	public static void faild(String message) {
		if (bUseNewLog)
			formatLogMsg(" [      错误      ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [      错误      ] ", message, 3);
	}

	public static void invalidOptions(String message) {
		if (bUseNewLog)
			formatLogMsg(" [    错误选项    ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [    错误选项    ] ", message, 3);
	}

	public static void codeError(String message) {
		if (bUseNewLog)
			formatLogMsg(" [    代码错误    ] ", message, "ERROR", "FAIL");
		else
			formatLogMsg(" [    代码错误    ] ", message, 5);
	}

//	public static void row(String message) {
//		formatLogMsg(" [       行       ] ", message, 6);
//	}
//
//	public static void header(String message) {
//		formatLogMsg(" [      表头      ] ", message, 6);
//	}
//
//	public static void footer(String message) {
//		formatLogMsg(" [      表尾      ] ", message, 6);
//	}
//
//	public static void printPage(String message) {
//		String startline = SGLogger.dateFormat.format(new Date()) + " [==========PAGE CONTENT START==========] ";
//		String endline = SGLogger.dateFormat.format(new Date()) + " [==========PAGE CONTENT END==========] ";
//		doublePrintOut(startline, 6);
//		doublePrintOut(message, 6);
//		doublePrintOut(endline, 6);
//	}
}