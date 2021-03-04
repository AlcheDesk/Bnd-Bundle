package com.meowlomo.ci.ems.bundle.jmeter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;
import com.meowlomo.ci.ems.bundle.interfaces.IFileService;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IStressTest;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.utils.StringUtil;

public class StressTestImpl implements IStressTest {

	static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss  :");//设置日期格式
	IFileService fileService = null;
	IHttpUtil httpUtil = null;
	
	public void bundleGetter() {
		if (null == fileService)
			fileService = BaseBundleActivator.getTheServiceObject("file", IFileService.class);
		
		if (null == httpUtil)
			httpUtil = BaseBundleActivator.getTheServiceObject("httpclient", IHttpUtil.class);	
	}
	
	@Override
	public String doTestProcess(String jsonTask, List<String> paramsInout) {
		// TODO Auto-generated method stub
		if (StringUtil.nullOrEmpty(jsonTask))
			return new ExecutionResult("压测任务为空或不是有效的,直接返回").toString();
		
		JSONObject task = null;
		try {
			System.err.println("StressTestImpl.doTestProcess:");
			System.err.println(jsonTask);
			task = new JSONObject(jsonTask);
		}
		catch (Exception e) {
			return new ExecutionResult("压测任务不是有效的json格式,直接返回").toString();
		}

		//1.拿到各条api信息		instructions:url,method,
		//2.拿到压测任务本身属性	threads,rampUp,timer,
		//TODO 
		
		String fileName = "jmeter-test-";
		long index = System.currentTimeMillis();
		fileName += (index + "-" + task.getString("name"));
		Object jmxFileResult = JmxWrite.testWrite(fileName, task);
		
		System.err.println("updateRun:" + httpUtil.updateRunWIPStatus());
		System.err.println("StressTestImpl.doTestProcess:" + jmxFileResult.toString());
		
		if (jmxFileResult instanceof ExecutionResult) {
			System.err.println("StressTestImpl.doTestProcess.executionResult.ExecutionResult");
			return jmxFileResult.toString();
		}
		else if (jmxFileResult instanceof String) {
			System.err.println("StressTestImpl.doTestProcess.executionResult.String");
			Object jmeterResult = doJMeterExecution((String)jmxFileResult, index);
			System.err.println("StressTestImpl.doTestProcess.jmeter.result:" + jmeterResult.toString());
			if (jmeterResult instanceof ExecutionResult) {
				ExecutionResult er = (ExecutionResult) jmeterResult;
				System.err.println("StressTestImpl:" + er.toString());
				if (null != er && er.bOK()) {
					httpUtil.finishRun("{'status':'PASS'}");
					JSONObject pressTestStaticsResult = new JSONObject(er.msg());
					JSONArray overall = pressTestStaticsResult.getJSONObject("overall").getJSONArray("data");
					JSONArray datas = pressTestStaticsResult.getJSONArray("items");
					System.err.println("overall:" + overall.toString());
					System.err.println("titles:" + pressTestStaticsResult.getJSONArray("titles"));
					int itemLength = datas.length();
					for(int i = 0; i < itemLength; ++i) {
						JSONObject item = datas.getJSONObject(i);
						JSONArray itemData = item.getJSONArray("data");
						System.err.println("item." + i + ":" + itemData);
					}
				} else {
					httpUtil.finishRun("{'status':'ERROR'}");
				}
				System.err.println("jmeter.StreeTestImpl.doTestProcess:" + ((ExecutionResult) jmeterResult).msg());
				
				return jmeterResult.toString();
			}
			else if (jmeterResult instanceof String) {
				httpUtil.finishRun("{'status':'ERROR'}");
				return new ExecutionResult("压测任务执行失败").toString();
			}
		} else {
			httpUtil.finishRun("{'status':'ERROR'}");
			System.err.println("StressTestImpl.doTestProcess.executionResult.Else");
			return new ExecutionResult("压测创建任务文件失败").toString();
		}
		return new ExecutionResult(true, "OK").toString();
	}

	@Override
	public void notifyTimeout() {
		// TODO Auto-generated method stub

	}
	
	static void pressPrintMsg(String s) {
		System.err.println(df.format(new Date()) + "StressTestImpl.doJMeterExecution." + s);
	}
	
	/**
	 * @author qi.chen
	 * @param jmxFile
	 * @param index
	 * @return ExecutionResult 根据bOK判断执行结果;String 
	 */
	public Object doJMeterExecution(String jmxFile, long index) {
		
		String jmeterDir = "D:\\apache-jmeter-5.0\\bin\\";
		File dir = new File(jmeterDir);
		
		String resultCSV = "", reportDir = "";
		
		ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "D:\\apache-jmeter-5.0\\bin\\jmeter", "-n", "-t",
				jmxFile, "-r", "-l",
				resultCSV = String.format("D:\\apache-jmeter-5.0\\bin\\result%d.csv", index), "-e", "-o",
				reportDir = String.format("D:\\apache-jmeter-5.0\\bin\\tmp\\report%d", index));
		
		String zipFile = "report";
//		String zipFile = "D:\\apache-jmeter-5.0\\bin\\tmp\\report.zip";
		
		pressPrintMsg("1");
		pressPrintMsg(" csv:" + resultCSV);
		pressPrintMsg(" report:" + reportDir);
		
		pb.directory(dir);
		try {
			pressPrintMsg("2");
			Process process = pb.start();
			pressPrintMsg("3");
			BufferedReader cmdStreamReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String cmdOutput;
			pressPrintMsg("4");
			while ((cmdOutput = cmdStreamReader.readLine()) != null) {
				System.out.println(cmdOutput);
			}
			pressPrintMsg("5");
			process.waitFor();
			pressPrintMsg("6");
			
			//假定成功生成了report
			File reportFile = new File(reportDir);
			pressPrintMsg("7:" + reportDir);
			if (reportFile.exists() && reportFile.isDirectory()) {
				String dashJS = reportDir + "\\content\\js\\dashboard.js";
				File jsFile = new File(dashJS);
				if (jsFile.exists() && jsFile.isFile()) {
					pressPrintMsg("7.1");
					//找到相应的行与内容
					int totalLineCount = getTotalLines(dashJS);
					pressPrintMsg("7.2:" + totalLineCount);
					if (totalLineCount < 184) {
						pressPrintMsg("7.3");
						return new ExecutionResult("压测任务归纳数据生成有误");
					} else {
						pressPrintMsg("7.4");
						String targetContent = readTargetFileContent(dashJS, 184);
						pressPrintMsg("7.5:" + targetContent);
						if (null == targetContent) {
							return new ExecutionResult("压测任务归纳数据读取有误");
						} else {
							String[] splits = targetContent.split("\\),");
							pressPrintMsg("7.5.1:" + splits.length);
							if (2 == splits.length) {
								targetContent = splits[1];
								pressPrintMsg("7.5.2:" + targetContent);
								String[] subSplits = targetContent.split(", function");
								pressPrintMsg("7.5.3:" + subSplits.length);
								if (2 == subSplits.length) {
									String jmeterResult = subSplits[0].trim();
									pressPrintMsg("7.5.4:" + jmeterResult);
									JSONObject jmeterRe = new JSONObject(jmeterResult);
									
									System.err.println("reportDir:" + reportDir);
									System.err.println("runId:" + String.valueOf(runId()));
									
									SGLogger.copyFileToFileServer(fileService.compress(reportDir, zipFile));
//									SGLogger.copyLogFileDirToFileServer(reportDir, String.valueOf(runId()), false);
									return new ExecutionResult(true, jmeterRe.toString());
								}
							}
							System.err.println("压测任务归纳数据获取失败.");
							return new ExecutionResult("压测任务归纳数据获取失败.");
						}
					}
				} else {
					pressPrintMsg("7.6.no data file");
				}
			} else {
				pressPrintMsg("8");
			}
		} catch (IOException | InterruptedException e) {
			pressPrintMsg("9");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		pressPrintMsg("End");
		return new ExecutionResult(false, -1, reportDir);
	}
	
	@Override
	public JSONObject attachData(String data) {
		bundleGetter();
		return IStressTest.super.attachData(data);
	}
	
	static int getTotalLines(String fileName) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		LineNumberReader reader = new LineNumberReader(in);
		String s = reader.readLine();
		int lines = 0;
		while (s != null) {
			lines++;
			s = reader.readLine();
		}
		reader.close();
		in.close();
		return lines;
	}
	
	static String readTargetFileContent(String fileName, int lineNumber) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		String line = reader.readLine();
		int num = 0;
		while (line != null) {
			if (lineNumber == ++num) {
				reader.close();
				return line;
			}
			line = reader.readLine();
		}
		reader.close();
		return null;
	}

}
