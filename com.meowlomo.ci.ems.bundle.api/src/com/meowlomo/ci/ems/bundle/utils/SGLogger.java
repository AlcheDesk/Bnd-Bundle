package com.meowlomo.ci.ems.bundle.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;
import com.meowlomo.ci.ems.bundle.interfaces.IFileService;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil.MethodType;

public class SGLogger {
	private static LinkedList<String> testCaseExecutionTmpLog = new LinkedList<String>();
	private static DateFormat  dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private static PrintStream ps = System.out;
	private static IHttpUtil http = null;
	private static String executionLogUrl = "";
	private static String stepLogUrl = "";	//TODO some like dynamic url for params mixed with it
	public static int stepInfoLimit = 0;
	public static boolean stepEndOne = false;
	private static IHttpUtil.MethodType executionLogUrlType = MethodType.POST;
	private static IHttpUtil.MethodType stepLogUrlType = MethodType.POST;

	private static JSONObject stepParams = new JSONObject();
	private static JSONObject executionParams = new JSONObject();

	private static String[] endStrings = new String[]{"INSTRUCTION_END","FAIL","ERROR","TIME_OUT","PASS","ELEMENT_NOT_FOUND"};
	private static List<String> endStringList = Arrays.asList(endStrings);

	private static String[] infoStrings = new String[]{"INSTRUCTION"};
	private static List<String> infoStringList = Arrays.asList(infoStrings);
	
	public static final String errTitle = " [    错误信息    ] ";

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
	private static String formatLogMsg(String info, String message, String logLevel, String stepLogType){
		String line = info + message;
		if (0 == logLevel.compareTo("ERROR") && InstructionOptions.instance().existOption(ContextConstant.RESULT_IGNORE)) {
			logLevel = "INFO";
			stepLogType = "INSTRUCTION";
			line += " [结果忽略选项] 结果忽略,继续执行";
		}
		doublePrintOut(line, logLevel, stepLogType);
		return line;
	}

	private static void doublePrintOut(String line, String logLevel, String stepLogType){
		if (null != http){
			JSONObject tmp = new JSONObject();
			tmp.put("line", line);
			tmp.put("logLevel", logLevel);
			tmp.put("stepLogType", stepLogType);
			
			http.addLogicalStepLog(tmp.toString());
		}
		else
			ps.println(dateFormat.format(new Date()) + line);
	}
	
	public static void attachHttp(IHttpUtil httpUtil) {
		http = httpUtil;
	}

	public static void outputLogFile(String logFileName) {
		String AbsLogFileName = FileUtilConstant.LOG_FOLDER + logFileName;
		if (null != testCaseExecutionTmpLog && testCaseExecutionTmpLog.size() > 0) {
			
			// TODO 使用IFile bundle，以使File的使用与实现 无关
			File file = new File(AbsLogFileName);
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
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(AbsLogFileName), "UTF-8"));
				for (int i = 0; i < testCaseExecutionTmpLog.size(); i++) {
					bw.write(testCaseExecutionTmpLog.get(i).toString());
					bw.newLine();
				}
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			copyFileToFileServer(logFileName);
		}
	}

	/**
	 * copy the files in dirName to remote file server
	 * @author qi.chen
	 * @param dirName 目录名
	 * @param withDir 远端是否创建同名目录存放子目录/子文件
	 * @return
	 */
//	public static boolean copyLogFileDirToFileServer(String dirName, String remoteDir, boolean withDir) {
//		try {
//			String localDir = FileUtilConstant.LOG_FOLDER + dirName;
//			if (dirName.contains(File.separator) && new File(dirName).isDirectory())
//				localDir = dirName;
//			
//			File localDirFile = new File(localDir);
//			
//			IFileService fileService = BaseBundleActivator.getTheServiceObject("file", IFileService.class);
//			String remotePath = fileService.remotePath(false);
//			//TODO
//			remotePath = removeLastSubPath(remotePath);
//			
//			Path remotePathName = Paths.get(remotePath + remoteDir);
//			if (fileService.exist(remotePathName)) {
//				System.err.println("remoteLogFilePath:" + remotePathName.toString() + " exists.");
//				return true;
//			} else {
//				byte[] fileContent = Files.readAllBytes(localDirFile.toPath());
//				System.err.println("logFileContent:" + fileContent);
//				System.err.println("localLogFilePath:" + localDir.toString());
//				System.err.println("remoteLogFilePath:" + remotePathName.toString());
//				
//				return fileService.create(remotePathName, fileContent, false);
//			}
//		} catch (IOException e) {
//			System.err.println("copy execution log to remote has exception occured. Eat it and type is:" + e.getClass());
//			return false;
//		}
//	}
	
	/**
	 * copy the fileName, for example, execution.log file to remote file server
	 * @author qi.chen
	 * @param fileName
	 * @return
	 */
	public static boolean copyFileToFileServer(String fileName) {
		try {
			File f = new File(fileName);
			String localFileName = FileUtilConstant.LOG_FOLDER + fileName;
			if (fileName.contains(File.separator) && f.isAbsolute()) {
				fileName = f.getName();
				localFileName = f.getAbsolutePath();
			}
			
			IFileService fileService = BaseBundleActivator.getTheServiceObject("file", IFileService.class);
			
			String remotePath = fileService.remotePath(false);
			//runId/instructionRunid
			if (!StringUtil.nullOrEmpty(FileUtilConstant.REMOTE_INSTRUCTION_RESULT_FOLDER))
				remotePath = removeLastSubPath(remotePath);
			
			Path remotePathName = Paths.get(remotePath + fileName);
			if (fileService.exist(remotePathName)) {
				System.err.println("remoteLogFilePath:" + remotePathName.toString() + " exists.");
				return true;
			} else {
				byte[] fileContent = Files.readAllBytes(new File(localFileName).toPath());
				System.err.println("logFileContent:" + fileContent);
				System.err.println("localLogFilePath:" + localFileName.toString());
				System.err.println("remoteLogFilePath:" + remotePathName.toString());
				
				return fileService.create(remotePathName, fileContent, false);
			}
		} catch (IOException e) {
			System.err.println("copy File To FileServer to remote has exception occured. Eat it and type is:" + e.getClass());
			return false;
		}
	}
	
	public static void timeoutError(){
		try{
			formatLogMsg(" [  任务执行超时  ] ", "task time out.", "ERROR", "TIME_OUT");
		}catch(Exception e){
			System.out.println("exception while time out notify to ATM");
		}
	}

	public static ExecutionResult error(String message) {
		return errorTitle(" [      错误      ] ", message);
	}

	public static void info(String message, boolean bCallRemoteApi) {
		if (bCallRemoteApi) {
			formatLogMsg(" [      信息      ] ", message, "INFO", "INSTRUCTION");
		} else {
			testCaseExecutionTmpLog.add(dateFormat.format(new Date()) + message);
			ps.println(dateFormat.format(new Date()) + message);
		}
	}

	public static ExecutionResult infoER(String message) {
		return new ExecutionResult(true, formatLogMsg(" [      信息      ] ", message, "INFO", "INSTRUCTION"));
	}
	
	public static void info(String message) {
		formatLogMsg(" [      信息      ] ", message, "INFO", "INSTRUCTION");
	}

	public static ExecutionResult actionComplete(String message) {
		return new ExecutionResult(true, formatLogMsg(" [  动作执行完毕  ] ", message, "INFO", "INSTRUCTION"));// "INSTRUCTION_END");
	}

	public static ExecutionResult verifySuccessER(String message) {
		return new ExecutionResult(true, formatLogMsg(" [    验证成功    ] ", message, "INFO", "INSTRUCTION"));// "INSTRUCTION_END");
	}
	
	public static void verifySuccess(String message) {
		formatLogMsg(" [    验证成功    ] ", message, "INFO", "INSTRUCTION");// "INSTRUCTION_END");
	}
	
	public static ExecutionResult verifyErrorER(String message) {
		return errorTitle(" [    验证失败    ] ", message);
	}

	public static void verifyError(String message) {
		errorTitle(" [    验证失败    ] ", message);
	}

	public static void warn(String message) {
		formatLogMsg(" [      警告      ] ", message, "WARN", "ERROR");
	}

	public static void done(String message) {
		formatLogMsg(" [      完成      ] ", message, "INFO", "");
	}

	public static void testStopped(String message) {
		formatLogMsg(" [    测试结束    ] ", message, "INFO", "PASS");
	}
	
	public static ExecutionResult elementTimeOut(String message){
		return new ExecutionResult(formatLogMsg(" [  未能发现元素  ] ", message, "ERROR", "ELEMENT_NOT_FOUND"));
	}

	public static ExecutionResult wrongValue(String message){
		return errorTitle(" [    错误数值    ] ", message);
	}

	public static ExecutionResult notValidSelection(String message) {
		return errorTitle(" [    无效选项    ] ", message);
	}

	public static ExecutionResult invalidInput(String message) {
		return errorTitle(" [    无效输入    ] ", message);
	}

//	public static void invalidAction(String message) {
//		formatLogMsg(" [    无效动作    ] ", message, "ERROR", "FAIL");
//	}

	public static void alert(String message){
		formatLogMsg(" [      弹窗      ] ", message, "INFO", "");
	}

	public static void clearLog (){
		if (null != testCaseExecutionTmpLog) testCaseExecutionTmpLog.clear();
	}

//	public static void instructionStart(String message) {
//		formatLogMsg(" [    指令开始    ] ", message, "INFO", "INSTRUCTION_BEGIN");
//	}

//	public static void instructionEnd(String message) {
//		formatLogMsg(" [    指令结束    ] ", message, "INFO", "INSTRUCTION");//"INSTRUCTION_END");
//	}

//	public static void errorMessage(String message) {
//		formatLogMsg(" [    错误信息    ] ", message, "ERROR", "FAIL");
//	}

//	public static void pass(String message) {
//		formatLogMsg(" [      通过      ] ", message, "INFO", "PASS");
//	}

//	public static ExecutionResult faild(String message) {
//		return new ExecutionResult(formatLogMsg(" [      错误      ] ", message, "ERROR", "FAIL"));
//	}

//	public static ExecutionResult invalidOptions(String message) {
//		return new ExecutionResult(formatLogMsg(" [    错误选项    ] ", message, "ERROR", "FAIL"));
//	}

	public static ExecutionResult errorTitle(String errorTitle, String message) {
		return new ExecutionResult(formatLogMsg(errorTitle, message, "ERROR", "FAIL"));
	}
	
	public static ExecutionResult codeError(String message) {
		return errorTitle(" [    代码错误    ] ", message);
	}
	
	public static void cacheMsg(String info) {
		if (null != testCaseExecutionTmpLog)
			testCaseExecutionTmpLog.add(info);
	}

	private static String removeLastSubPath(String remotePath) {
		if (remotePath.contains(File.separator)) {
			
			String[] paths = remotePath.split(File.separator);
			if (paths.length > 1) {
				String newRemotePath = "";
				for (int i = 0; i < paths.length - 1; ++i) {
					newRemotePath += paths[i] + "/";
				}
				remotePath = newRemotePath;
			}
		}
		return remotePath;
	}
}