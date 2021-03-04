package com.meowlomo.ci.ems.bundle.webdriver;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;
import com.meowlomo.ci.ems.bundle.interfaces.IBundleStepStateClearable;
import com.meowlomo.ci.ems.bundle.interfaces.IFileService;
import com.meowlomo.ci.ems.bundle.interfaces.IFileUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IWebDriver;
import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.JSONUtil;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.utils.StringUtil;

public class WebDriverImplNew implements IWebDriver, IBundleStepStateClearable{

	private static final Logger logger = LoggerFactory.getLogger(WebDriverImplNew.class);
	private BaseBundleActivator activator;
	TestCaseProccessorNew instructionProcessor = null;
	private Capabilities capabilities;
	String resolution;
	
	Map<String, String> propertyRules = new HashMap<String, String>() {
		/**
		*
		*/
		private static final long serialVersionUID = 7698030356312686287L;

		{
			put("firefoxPath", "string");// string, int
			put("geckodriverPath", "string");
			put("elementMap", "object");
			put("logFolder", "string");
			put("instructionArray", "object");
			put("name", "string");
			put("uuid", "string");
			put("taskData", "object");
			put("parameters", "object");
			put("runId", "integer");
		}
	};

	HashSet<String> propertyLenRules = new HashSet<String>() {
		/**
		*
		*/
		private static final long serialVersionUID = 2248306406683570926L;

		{
			add("instruction");
			add("action");
			add("input");
			add("option");
		}
	};

	public WebDriverImplNew(BaseBundleActivator bba){
		activator = bba;
	}

	protected WebDriverImplNew(){

	}

	@Override
	public void notifyTimeout() {
		SGLogger.timeoutError();
	}

	@Override
	public String doTestProcess(String jsonTaskContext) {
//		if (StringUtil.nullOrEmpty(jsonTaskContext))
//			return 10001;
//		if (false == JSONUtil.isJSONValid(jsonTaskContext))
//			return 10002;
//
//		JSONObject taskContext = new JSONObject(jsonTaskContext);
//		if (isFormatParseOK(taskContext)){
//			return doExecute(taskContext);
//		}
		return new ExecutionResult(false, 10003, "should'not be called").toString();
	}

	protected boolean isFormatParseOK(JSONObject taskContext){
		if (isPropertyRulesMatch(taskContext)){// && isPropertyLenRulesMatch((JSONArray)taskContext.getJSONArray("instructionArray"), propertyLenRules)){
			return true;
		}
		return false;
	}

	private boolean isPropertyRulesMatch(JSONObject taskContext){
		if (null == propertyRules || 0 == propertyRules.size())
			return true;

		for (Map.Entry<String, String> entry : propertyRules.entrySet()) {
			Object type = taskContext.get(entry.getKey());

			if (null == type)
				continue;

			switch(entry.getValue()){
			case "array":
				if (!(type instanceof JSONArray))
					return false;
				break;
			case "string":
				if (!(type instanceof String))
					return false;
				break;
			case "int":
			case "integer":
				if (!(type instanceof Integer))
					return false;
				break;
			case "object":
				if (!(type instanceof JSONObject))
					return false;
				break;
			default:
				break;
			}
		}
		return true;
	}

	private void genWebDriverNew(JSONObject instruction) {
		String browser = instruction.getJSONObject("element").optString("driverVendorName");
		resolution = instruction.getJSONObject("element").optString("driverWindowSize");
		webDriver = null;
		
		if (StringUtil.nullOrEmpty(browser)) browser = "Firefox";
		if (StringUtil.nullOrEmpty(resolution)) resolution = "1366x768";
		if (browser.equalsIgnoreCase("Chrome")) {
			String chromedriverPath = genChromedriverPath();
			System.setProperty("webdriver.chrome.driver", chromedriverPath);

			String chromePath = genChromePath();
			ChromeOptions options = new ChromeOptions();
			options.addArguments(
					"user-agent=\"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.119 Safari/537.36\"");
			options.addArguments("--start-maximized");
			options.setBinary(chromePath);
			options.addArguments("--lang=zh-CN.UTF-8"); // 设置中文
			options.addArguments("--test-type", "--ignore-certificate-errors"); // 设置忽略浏览器证书报警提示
			options.addArguments("--incognito");
			ChromeDriver driver = new ChromeDriver(options);
			capabilities = driver.getCapabilities();
			webDriver = driver;
		} else if (browser.equalsIgnoreCase("Edge")) {
			String edgedriverPath =  genEdgedriverPath();
			System.setProperty("webdriver.edge.driver", edgedriverPath);
			capabilities = DesiredCapabilities.edge();
			EdgeDriver driver = new EdgeDriver(capabilities);
			capabilities = driver.getCapabilities();
			webDriver = driver;
		} else if (browser.equalsIgnoreCase("IE")) {
			String iedriverPath = genIEdriverPath();
			System.setProperty("webdriver.ie.driver", iedriverPath);
			InternetExplorerDriver driver = new InternetExplorerDriver();
			capabilities = driver.getCapabilities();
			webDriver = driver;
		} else {
			String geckodriverPath = genGeckodriverPath();
			System.setProperty("webdriver.gecko.driver", geckodriverPath);
			String firefoxPath = genFirefoxPath();
			capabilities = getDesiredFirefoxCapabilities(firefoxPath);
			FirefoxDriver driver = new FirefoxDriver(capabilities);
			capabilities = driver.getCapabilities();
			webDriver = driver;
		}
		if (!StringUtil.nullOrEmpty(resolution)) {
			if (resolution.equalsIgnoreCase("fullScreen")) {
				webDriver.manage().window().fullscreen();
			} else if (resolution.equalsIgnoreCase("maximize")) {
				webDriver.manage().window().maximize();
			} else {
				String[] sizes = resolution.split("x");
				if (null != sizes && 2 == sizes.length) {
					webDriver.manage().window().setPosition(new Point(0, 0));
					webDriver.manage().window()
							.setSize(new Dimension(Integer.valueOf(sizes[0]), Integer.valueOf(sizes[1])));
				}
			}
		}

		browserName = browser;
	}

	private DesiredCapabilities getDesiredFirefoxCapabilities(String firefoxPath) {
		DesiredCapabilities capabilities = new FirefoxOptions()
			      .setProfile(new FirefoxProfile())
			      .addTo(DesiredCapabilities.firefox());

		capabilities.setCapability("marionette", true);
		if(null != firefoxPath){
			logger.info("Firefox Path set to : " + firefoxPath);
			capabilities.setCapability("firefox_binary", firefoxPath);
		}
	  //capabilities.addPreference("browser.download.dir", text);// 配置响应参数：下载路径
	  //capabilities.addPreference("browser.download.folderList", 2);// 2为指定路径，0为默认路径
	    capabilities.setCapability("browser.download.manager.showWhenStarting", false);// 是否显示开始
	 // 禁止弹出保存框，value是文件格式
	    capabilities.setCapability("browser.helperApps.neverAsk.saveToDisk",
					"application/zip,application/msword,text/plain,application/vnd.ms-excel,text/csv,text/comma-separated-values,application/octet-stream,multipart/form-data;charset=utf-8"
				  + ",application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		capabilities.setCapability("browser.download.manager.showWhenStarting", false);
		capabilities.setCapability("browser.helperApps.alwaysAsk.force", false);
		capabilities.setCapability("dom.max_script_run_time", 0);
		     //不会弹出警告框
		capabilities.setCapability("browser.download.manager.alertOnEXEopen", false);
		return capabilities;
	}

	protected boolean checkContextValueAsValidFile(IFileUtil fs, JSONObject context, String key, String extension){
		Object filePath = context.get(key);
		if (null == filePath){
			logger.error("[异常] " + key + " 的值为空 ");
			return false;
		}else if (!(filePath instanceof String)){
			logger.error("[异常] " + key + " 的值 [" + filePath + "]为空 ");
			return false;
		}
		else{
			File file = fs.getFile((String)filePath);
			if (null == file || !file.exists()){
				logger.error("[异常] " + key + " 不存在.");
				return false;
			}
		}
		return true;
	}

	WebDriver webDriver;
	String browserName;
	
	@Override
	public String step(String instructionJson, List<String> paramsInOrOut) {
		// TODO Auto-generated method stub
		System.err.println("Web Test Execution Step with data:" + instructionJson);		
		if (StringUtil.nullOrEmpty(instructionJson))
			new ExecutionResult(false, 10001, "[WebDriver] 传入指令数据为空").toString();
		if (false == JSONUtil.isJSONValid(instructionJson))
			new ExecutionResult(false, 10002, "[WebDriver] 传入指令数据格式不正确").toString();

		JSONObject instruction = new JSONObject(instructionJson);
		if (null == webDriver) {
			genWebDriverNew(instruction);
			if (null != webDriver)
				instructionProcessor = new TestCaseProccessorNew(webDriver);
			else {
				//TODO 
				return new ExecutionResult(false, "WebDriver构建失败.测试无法完成!").toString();
			}
		}
		
		if (null == ContextConstant.UUID)
			ContextConstant.UUID = instruction.optString("uuid");
		
		ExecutionResult stepResult = instructionProcessor.stepInstruction(instruction, paramsInOrOut);
		paramsInOrOut.add(String.format("Time is %d", System.currentTimeMillis()));
		System.err.println("[stepInstruction result]:" + stepResult);
		
		// TODO 返回得宜的结果信息 
		return stepResult.toString();
	}

	private String genGeckodriverPath() {
		return System.getProperty("user.home") + "\\Desktop\\VMCDrivers\\geckodriver-windows-64.exe";
	}
	
	private String genChromedriverPath() {
		return System.getProperty("user.home") + "\\Desktop\\VMCDrivers\\chromedriver-windows-32.exe";
	}
	
	private String genEdgedriverPath() {
		return System.getProperty("user.home") + "\\Desktop\\VMCDrivers\\MicrosoftWebDriver.exe";
	}
	
	private String genIEdriverPath() {
		return System.getProperty("user.home") + "\\Desktop\\VMCDrivers\\iedriver-windows-64.exe";
	}

	private String genFirefoxPath(){
		//check if the 32bit firefox is installed
		Path firefoxPath = null;
		File x86Firefox = new File("E:/Program Files (x86)/Mozilla Firefox/firefox.exe");
		if(x86Firefox.exists()) {
			firefoxPath = x86Firefox.toPath();
		}else {
			x86Firefox = new File("D:/Program Files (x86)/Mozilla Firefox/firefox.exe");
			if(x86Firefox.exists()) {
				firefoxPath = x86Firefox.toPath();
			}else {
				x86Firefox = new File("C:/Program Files (x86)/Mozilla Firefox/firefox.exe");
				if(x86Firefox.exists()) {
					firefoxPath = x86Firefox.toPath();
				}
			}
		}

		//check if the 64bit firefox is installed
		File x64Firefox = new File("E:/Program Files/Mozilla Firefox/firefox.exe");
		if(x64Firefox.exists()) {
			firefoxPath = x64Firefox.toPath();
		}else {
			x64Firefox = new File("D:/Program Files/Mozilla Firefox/firefox.exe");
			if(x64Firefox.exists()) {
				firefoxPath = x64Firefox.toPath();
			}else {
				x64Firefox = new File("C:/Program Files/Mozilla Firefox/firefox.exe");
				if(x64Firefox.exists()) {
					firefoxPath = x64Firefox.toPath();
				}
			}
		}

		return null == firefoxPath ? "" : firefoxPath.toString();
	}
	
	private String genChromePath(){
		//check if the 32bit firefox is installed
		Path chromePath = null;
		File x86Chrome = new File("C:/Program Files (x86)/Google/Chrome/Application/chrome.exe");
		if(x86Chrome.exists()) {
			chromePath = x86Chrome.toPath();
		}

		//check if the 64bit firefox is installed
		File x64Chrome = new File("C:/Program Files/Google/Chrome/Application/chrome.exe");
		if(x64Chrome.exists()) {
			chromePath = x64Chrome.toPath();
		}

		return null == chromePath ? "" : chromePath.toString();
	}

	@Override
	public boolean clearState() {
		// TODO Auto-generated method stub
		System.err.println("begin browserCleanWork in WebDriverImpl.");
		browserCleanWork();
		instructionProcessor = null;
		webDriver = null;
		InstructionOptions.resetInstance();
		System.err.println("end browserCleanWork in WebDriverImpl.");
		return IWebDriver.super.clearState();
	}

	@Override
	public JSONObject attachData(String data) {
		// TODO Auto-generated method stub
		JSONObject supperObj = IWebDriver.super.attachData(data);
		logger.debug("[webdriver attachData]" + data);
		if (supperObj.optBoolean("logFileToServer"))
			optionFileServerNew(supperObj.getJSONObject("parameters"));
		return supperObj;
	}
	
	private void browserCleanWork() {
		if (!StringUtil.nullOrEmpty(browserName))
			try {
				System.err.println("[browserCleanWork] browser name:" + browserName);
				if (null != this.webDriver) {
					this.webDriver.close();
					System.err.println("[browserCleanWork] close");
					this.webDriver.quit();
					System.err.println("[browserCleanWork] quit");
				} else {
					System.err.println("[browserCleanWork] the webDriver is null already!!!");
				}
				BrowserUtils.endCurrentBrowserSession(this.browserName);
			} catch (Exception e) {
				System.err.println("[browserCleanWork] exception:" + e.getClass().getName());
			} finally {
				this.webDriver = null;
				System.err.println("[browserCleanWork] set null at finally.");
			}
		else {
			this.webDriver = null;
			System.err.println("[browserCleanWork] browser name is null. no need.");
		}
	}
	
	private void optionFileServerNew(JSONObject parameters) {
		JSONObject remoteParameters = parameters.optJSONObject("remoteFileServer");
		if (null != remoteParameters) {
			try {
				IFileService fileService = BaseBundleActivator.getTheServiceObject("file", IFileService.class);
				IHttpUtil httpUtil = BaseBundleActivator.getTheServiceObject("httpclient", IHttpUtil.class);
				ActionCommon.setFileService(fileService, httpUtil);
			}catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	@Override
	public void attachInstructionRunData(String info) {
		JSONObject infoObj = new JSONObject(info);
		logger.info("[attachInstructionRunData WebDriverImplNew]: {}", info);
		
		localState.instructionRunId = infoObj.optLong("instructionRunId");
		ActionCommon.setFileService(localState.instructionRunId);
		localState.instructionId = infoObj.optInt("instructionId");
	}

	@Override
	public String getExecutionEnvironmentInfo(String info) {
		if (null != capabilities) {
			try {
				JSONUtil.beginJSONObject("webdriver.getExecutionEnvironmentInfo");
				JSONObject content = new JSONObject(info);
				JSONUtil.addJSONField("webdriver.getExecutionEnvironmentInfo", "浏览器", capabilities.getBrowserName());
				JSONUtil.addJSONField("webdriver.getExecutionEnvironmentInfo", "浏览器版本", capabilities.getVersion().isEmpty() ? content.optString(capabilities.getBrowserName()) : capabilities.getVersion());
				if (!StringUtil.nullOrEmpty(resolution)) {
					JSONUtil.addJSONField("webdriver.getExecutionEnvironmentInfo", "浏览器分辨率", resolution);
				}
				return JSONUtil.endJSONObject("webdriver.getExecutionEnvironmentInfo", true);
			}catch(Exception e) {
				JSONUtil.endJSONObject("webdriver.getExecutionEnvironmentInfo", true);
			}
		}
		return "";
	}
}
