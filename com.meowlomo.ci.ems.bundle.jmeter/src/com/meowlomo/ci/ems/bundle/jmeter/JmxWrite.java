package com.meowlomo.ci.ems.bundle.jmeter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.json.JSONArray;
import org.json.JSONObject;

import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;

public class JmxWrite {
	void writeJmx(String path) {
//		try {
//			File = new 
//			XMLWriter writer = new XMLWriter();
//			Document document = new Document
//			writer.write(document);
//			
//			OutputFormat format = OutputFormat.createPrettyPrint();
//			format.setEncoding("utf-8");
//			
//		}
	}
	
	private final static Map<String, Map<String, String>> eleAttrMap = new HashMap<String, Map<String, String>>(){
		{
			put("TestPlan", new HashMap<String, String>(){
				{
					put("guiclass", 	"TestPlanGui");
					put("testclass", 	"TestPlan");
					put("testname", 	"VMC测试计划");
					put("enabled", 		"true");
				}
			});
			put("TestPlan.elementProp", new HashMap<String, String>(){
				{
					put("name", 		"TestPlan.user_defined_variables");
					put("elementType",	"Arguments");
					put("guiclass", 	"ArgumentsPanel");
					put("testclass", 	"Arguments");
					put("testname", 	"用户定义的变量");
					put("enabled", 		"true");
				}
			});
			put("ThreadGroup", new HashMap<String, String>(){
				{
					put("guiclass", 	"ThreadGroupGui");
					put("testclass", 	"ThreadGroup");
					put("testname", 	"VMC线程组1");
					put("enabled", 		"true");
				}
			});
			put("ThreadGroup.elementProp", new HashMap<String, String>(){
				{
					put("name", 		"ThreadGroup.main_controller");
					put("elementType", 	"LoopController");
					put("guiclass", 	"LoopControlPanel");
					put("testclass", 	"LoopController");
					put("testname", 	"循环控制器");
					put("enabled", 		"true");
				}
			});
			put("HTTPSamplerProxy", new HashMap<String, String>(){
				{
					put("guiclass", 	"HttpTestSampleGui");
					put("testclass", 	"HTTPSamplerProxy");
					put("testname", 	"HTTP请求Demo");
					put("enabled", 		"true");
				}
			});
			put("HTTPSamplerProxy.elementProp", new HashMap<String, String>(){
				{
					put("name", 		"HTTPsampler.Arguments");
					put("elementType", 	"Arguments");
					put("guiclass", 	"HTTPArgumentsPanel");
					put("testclass", 	"Arguments");
					put("testname", 	"用户定义的变量");
					put("enabled", 		"true");
				}
			});
			put("ConstantTimer", new HashMap<String, String>(){
				{
					put("guiclass", 	"ConstantTimerGui");
					put("testclass", 	"ConstantTimer");
					put("testname", 	"Constant Timer");
					put("enabled", 		"true");
				}
			});
		}
	};
	
	public static Object testWrite(String fileName, JSONObject task) {
		Document document = DocumentHelper.createDocument();
		JSONObject jsonTestCase = task.getJSONObject("testCase");
		JSONObject parameter = jsonTestCase.getJSONObject("parameter");
		
		Element testCase = document.addElement("jmeterTestPlan");
		testCase.addAttribute("version", "1.2");
		testCase.addAttribute("properties", "5.0");
		testCase.addAttribute("jmeter", "5.0 r1840935");//5.1.1 r1855137
		
		Element hashTreeRoot = testCase.addElement("hashTree");
		Element testPlan = addSimpleChild(hashTreeRoot, "TestPlan", true);
		
		addSimpleChild(testPlan, "stringProp", "name", "TestPlan.comments");
		
		addSimpleChild(testPlan, "boolProp", "name", "TestPlan.functional_mode", "false");
		addSimpleChild(testPlan, "boolProp", "name", "TestPlan.tearDown_on_shutdown", "true");
		addSimpleChild(testPlan, "boolProp", "name", "TestPlan.serialize_threadgroups", "false");
		
		Element testPlanElementProp = addSimpleChild(testPlan, "TestPlan.elementProp", true);
		//TODO 适合把属性放到三维数组中，根据name进行遍历
		addSimpleChild(testPlanElementProp, "collectionProp", "name", "Arguments.arguments");
		addSimpleChild(testPlan, "stringProp", "name", "TestPlan.user_define_classpath");

		//**************************TestPlan 基本属性结束****************************
		//**********************线程属性开始***********************
		
		Element hashTreeRootLevel1 = hashTreeRoot.addElement("hashTree");
		Element threadGroup = addSimpleChild(hashTreeRootLevel1, "ThreadGroup", true);
		addSimpleChild(threadGroup, "stringProp", "name", "ThreadGroup.on_sample_error", "continue");
		
		//循环控制器,即线程组执行多少次结束的控制
		Element threadGroupElementProp = addSimpleChild(threadGroup, "ThreadGroup.elementProp", true);
		//永久循环
		addSimpleChild(threadGroupElementProp, "boolProp", "name", "LoopController.continue_forever", "false");
		if (parameter.getInt("loopCount") == -1)
			addSimpleChild(threadGroupElementProp, "intProp", "name", "LoopController.loops", "-1");//未选择循环时取值-1
		else
			addSimpleChild(threadGroupElementProp, "stringProp", "name", "LoopController.loops", parameter.get("loopCount").toString());					//循环次数,需要封装
		
		
		addSimpleChild(threadGroup, "stringProp", "name", "ThreadGroup.num_threads", parameter.get("numberOfThreads").toString());
		addSimpleChild(threadGroup, "stringProp", "name", "ThreadGroup.ramp_time", parameter.get("rampUpPeriod").toString());
		
		addSimpleChild(threadGroup, "stringProp", "name", "ThreadGroup.scheduler", "false");
		addSimpleChild(threadGroup, "stringProp", "name", "ThreadGroup.duration", "");//schedulerDuration
		addSimpleChild(threadGroup, "stringProp", "name", "ThreadGroup.delay", "");//schedulerStartupDelay

		//是否延迟启动，个人认为不要延迟，因为必备的资源最好一开始就从系统完全申请，无法完成的测试不要开始
		//addSimpleChild(threadGroup, "boolProp", "name", "ThreadGroup.delayedStart", "true");
		
		addSimpleChild(hashTreeRootLevel1, "hashTree", false);
		Element constantTimerElement = addSimpleChild(hashTreeRootLevel1, "ConstantTimer", true);
		addSimpleChild(constantTimerElement, "stringProp", "name", "ConstantTimer.delay", "100");
		
		JSONArray instructions = jsonTestCase.getJSONArray("instructions");
		int count = instructions.length();
		
		for(int index = 0; index < count; ++index) {
			addSimpleChild(hashTreeRootLevel1, "hashTree", false);
			
			JSONObject instruction = instructions.getJSONObject(index);
			JSONObject insData = instruction.getJSONObject("data");
			String apiName = insData.getString("apiName");
			String apiMethod = insData.getString("method");
			String apiUrl = insData.getString("url");
			URL url = null;
			try {
				url = new URL(apiUrl);
			} catch (MalformedURLException e) {
				return new ExecutionResult("Url is not legal.For it is:" + apiUrl);
			}
			//添加http请求
			Element HTTPSamplerProxy = addSimpleChild(hashTreeRootLevel1, "HTTPSamplerProxy", true);
			HTTPSamplerProxy.addAttribute("testname", apiName);
			
			//http请求参数
			Element argumentsElementProp = addSimpleChild(HTTPSamplerProxy, "HTTPSamplerProxy.elementProp", true);
			//TODO
			//无参数时添加此prop即可
			Element argumentsCollectionProp = addSimpleChild(argumentsElementProp, "collectionProp", "name", "Arguments.arguments", null);
			
			//有参数时collectionProp继续添加
			if (insData.has("queryParameters") && !"[]".equals(insData.get("queryParameters").toString())) {
				JSONObject queryParameters = insData.getJSONObject("queryParameters");
				Iterator<String> keyIt = queryParameters.keys();
				while (keyIt.hasNext()) {
					String key = keyIt.next();
					String value = queryParameters.getString(key);
					addHttpArgument(argumentsCollectionProp, key, value, true);
				}
			}
			
			//为http请求设置配置
			int port = url.getPort();
			String portStr = -1 == port ? "" : Integer.valueOf(port).toString();
			addSimpleHttpChild(HTTPSamplerProxy, url.getProtocol(), url.getHost(), portStr, url.getPath(), apiMethod);
		}
	
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("UTF-8");
		format.setExpandEmptyElements(false);
		try {
			if (!fileName.endsWith(".jmx"))
				fileName += ".jmx";
			
			if (!fileName.contains("/") && !fileName.contains("\\"))
				fileName = "D:/JMeter/" + fileName;
			System.err.println("jmx file name:" + fileName);
			
		    File f = new File(fileName);
		    XMLWriter writer = new XMLWriter(new FileOutputStream(f), format);
		    //设置是否转义。默认true，代表转义
		    writer.setEscapeText(false);
		    writer.write(document);
		    writer.close();
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		return fileName;
	}

	private static Element addSimpleChild(Element subRoot, String elementName, String elementAttrName, String elementAttrValue, String textValue) {
		Element newEle = subRoot.addElement(elementName);
		newEle.addAttribute(elementAttrName, elementAttrValue);
		if (null != textValue)
			newEle.addText(textValue);
		return newEle;
	}
	
	private static Element addSimpleChild(Element subRoot, String elementName, String elementAttrName, String elementAttrValue) {
		Element newEle = subRoot.addElement(elementName);
		newEle.addAttribute(elementAttrName, elementAttrValue);
		return newEle;
	}
	
	private static Element addSimpleChild(Element root, String elementName, boolean useDefaultAttributesMap) {
		String prevElementName = elementName;
		if (elementName.contains(".")) {
			String[] names = elementName.split("\\.");
			elementName = names[names.length - 1];
		}
		
		Element newEle = root.addElement(elementName);
		if (useDefaultAttributesMap && eleAttrMap.containsKey(prevElementName)) {
			Map<String, String> attributes = eleAttrMap.get(prevElementName);
			if (null != attributes && !attributes.isEmpty()) {
				for (Map.Entry<String, String> entry : attributes.entrySet()) {
					newEle.addAttribute(entry.getKey(), entry.getValue());
				}
			}
		}
		return newEle;
	}
	
	private static Element addHttpArgument(Element argumentElement, String key, String value, boolean encode) {
		Element elementProp = addSimpleChild(argumentElement, "elementProp", "name", key);
		elementProp.addAttribute("elementType", "HTTPArgument");
		
		//TODO 是否需要加密默认设置为永久加密
		addSimpleChild(elementProp, "boolProp", "name", "HTTPArgument.always_encode", encode ? "true" : "false");
		addSimpleChild(elementProp, "stringProp", "name", "Argument.value", value);
		addSimpleChild(elementProp, "stringProp", "name", "Argument.metadata", "=");
		addSimpleChild(elementProp, "boolProp", "name", "HTTPArgument.use_equals", "true");
		addSimpleChild(elementProp, "stringProp", "name", "Argument.name", key);
		
		return elementProp;
	}
	
	private static void addSimpleHttpChild(Element httpSampleProxyElement, String protocol, String domain, String port, String path, String method) {
		addSimpleChild(httpSampleProxyElement, "stringProp", "name", "HTTPSampler.domain", domain);
		addSimpleChild(httpSampleProxyElement, "stringProp", "name", "HTTPSampler.port", port);
		addSimpleChild(httpSampleProxyElement, "stringProp", "name", "HTTPSampler.protocol", protocol);
		addSimpleChild(httpSampleProxyElement, "stringProp", "name", "HTTPSampler.contentEncoding", "UTF-8");
		addSimpleChild(httpSampleProxyElement, "stringProp", "name", "HTTPSampler.path", path);
		addSimpleChild(httpSampleProxyElement, "stringProp", "name", "HTTPSampler.method", method);
		
		
		addSimpleChild(httpSampleProxyElement, "boolProp", "name", "HTTPSampler.follow_redirects", "true");
		addSimpleChild(httpSampleProxyElement, "boolProp", "name", "HTTPSampler.auto_redirects", "false");
		addSimpleChild(httpSampleProxyElement, "boolProp", "name", "HTTPSampler.use_keepalive", "true");
		addSimpleChild(httpSampleProxyElement, "boolProp", "name", "HTTPSampler.DO_MULTIPART_POST", "false");
		
		
		addSimpleChild(httpSampleProxyElement, "stringProp", "name", "HTTPSampler.embedded_url_re", "");
		addSimpleChild(httpSampleProxyElement, "stringProp", "name", "HTTPSampler.connect_timeout", "");
		addSimpleChild(httpSampleProxyElement, "stringProp", "name", "HTTPSampler.response_timeout", "");
	}
}
