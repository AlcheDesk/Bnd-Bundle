package com.meowlomo.ci.ems.bundle.webdriver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
//import com.meowlomo.ci.ems.bundle.webdriver.CustomMethod;

import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;

/**
 * The Class DefaultElementExecutor.
 * this class is used to execute default HTML web elements
 * button, text box, radio, link, check box, drop box, file.
 */
public class WebElementExecutor {

    public final static Set<String> WEB_ELEMENT_TYPES = new HashSet<>(Arrays.asList("webbrowser"
																					,"webbutton"
																					,"webtextbox"
																					,"webradio"
																					,"weblink"
																					,"webcheckbox"
																					,"webdropdown"
																					,"webfile"
																					,"webframe"
																					));

	private WebDriver driver = null;
	private WebElement webElement = null;

	public WebElementExecutor(WebDriver driver){
		this.driver = driver;
	}

	public WebElementExecutor(WebElement webElement) {
		this.webElement = webElement;
	}

	public int executeDefaultElementAction(String elementType, String methodName, By locator, String action, String input, InstructionOptions options){
		String actionCommonName = "com.meowlomo.ci.ems.bundle.webdriver.ActionCommon";
		String className = null;
		String executeMethodName = null;

		if(methodName.equalsIgnoreCase("default")){
			executeMethodName = getDefaultMethodNameFromTheType(elementType, action);
		}
		else{
			className = methodName;
			if(action.equalsIgnoreCase("verify")){
				executeMethodName = "verify";
			}
			else{
				executeMethodName = "doAction";
			}
		}

		if (null == executeMethodName){
			System.out.println("[WebDriver Error] Method name is null.");
			System.out.println("methodname:" + methodName + " ,elementType:" + elementType + " ,action:" + action);
			return -2;
		}

		Class[] cArg = new Class[4];
		if(this.driver == null){
			cArg[0] = WebElement.class;
		}
		else{
			cArg[0] = WebDriver.class;
		}

		cArg[1] = By.class;
		cArg[2] = String.class;
		cArg[3] = InstructionOptions.class;
		try {
			if(className == null){
				Class<?> actionClass = Class.forName(actionCommonName);
				System.out.println("call method " + executeMethodName + " locator "+(null == locator ? " empty " : locator.toString())+" input "+input);
				Method method = actionClass.getDeclaredMethod(executeMethodName, cArg);
				if (null == method)
					return -1;
				Boolean result;
				if(this.driver == null){
					result = (boolean) method.invoke(null,webElement,locator,input,options);
				}
				else{
					result = (boolean) method.invoke(null,this.driver,locator,input,options);
				}
				System.out.println("action method result "+result);
				return result ? 1 : 0;
			}
			else{
				System.out.println("call method "+executeMethodName+" locator "+locator.toString()+" input "+input);
				System.out.println("["+"com.meowlomo.ci.ems.bundle.webdriver.custom.method."+className+"]["+executeMethodName+"]["+cArg+"]");
				System.out.println("Class not loaded");
				return 0;
			}
		} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private final static Map<String, Map<String, String>> functionDictionary = new HashMap<String, Map<String, String>>(){
		{
//			put("sql", new HashMap<String, String>(){
//				{
//					put("ALL", 			"sqlScriptExecute");
//				}
//			});
			put("webbrowser", new HashMap<String, String>(){
				{
					put("Navigate", 			"browserNavigate");
					put("Wait", 				"browserSleep");
					put("Back", 				"browserBack");
					put("Close", 				"browserClose");
					put("Forward", 				"browserForward");
					put("Refresh", 				"browserRefresh");
					put("SwitchToNewtab", 		"browserSwitchToNewtab");
				}
			});
			put("webbutton", new HashMap<String, String>(){
				{
					put("Click", 				"clickButton");
					put("DoubleClick", 			"doubleClick");
					put("IsEnable", 			"isEnableVerify");
					put("IsDisable", 			"isDisableVerify");
					put("Exist", 				"ExistVerify");
					put("NonExist", 			"NonExistVerify");
					put("Count", 				"getItemsCount");
					put("clickOnHideButton",	"clickOnHideButton");
					put("moveToElement",		"moveToElement");
					put("dragAndDrop",			"dragAndDrop");
					put("jsExcuteForElement",	"jsExcuteForElement");
				}
			});
			put("webtextbox", new HashMap<String, String>(){
				{
					put("Enter", 				"enterText");
					put("Match", 				"verifyValue");
					put("InputContainsPageText","verifyContains");
					put("InputInPageText", 		"verifyIn");
					put("EnterReadonly", 		"enterTextReadonly");
					put("Clear", 				"clearText");
					put("IsEnable", 			"isEnableVerify");
					put("IsDisable", 			"isDisableVerify");
					put("Exist", 				"ExistVerify");
					put("NonExist", 			"NonExistVerify");
					put("moveToElement", 		"moveToElement");
					put("Count", 				"getItemsCount");
					put("jsExcuteForElement",	"jsExcuteForElement");
				}
			});
			put("webradio", new HashMap<String, String>(){
				{
					put("Select", 				"selectRadio");
					put("IsEnable", 			"isEnableVerify");
					put("IsDisable", 			"isDisableVerify");
					put("Exist", 				"ExistVerify");
					put("NonExist", 			"NonExistVerify");
					put("moveToElement",		"moveToElement");
					put("Count", 				"getItemsCount");
					put("jsExcuteForElement",	"jsExcuteForElement");
				}
			});
			put("weblink", new HashMap<String, String>(){
				{
					put("Click", 				"clickLink");
					put("DoubleClick", 			"doubleClick");
					put("IsEnable", 			"isEnableVerify");
					put("IsDisable", 			"isDisableVerify");
					put("Exist", 				"ExistVerify");
					put("NonExist", 			"NonExistVerify");
					put("moveToElement", 		"moveToElement");
					put("Count", 				"getItemsCount");
					put("jsExcuteForElement",	"jsExcuteForElement");
				}
			});
			put("webcheckbox", new HashMap<String, String>(){
				{
					put("Check", 				"checkbox");
					put("IsEnable", 			"isEnableVerify");
					put("IsDisable", 			"isDisableVerify");
					put("Exist", 				"ExistVerify");
					put("NonExist", 			"NonExistVerify");
					put("moveToElement", 		"moveToElement");
					put("Count", 				"getItemsCount");
					put("jsExcuteForElement",	"jsExcuteForElement");
				}
			});
			put("webdropdown", new HashMap<String, String>(){
				{
					put("Select", 				"selectDropdown");
					put("IsEnable", 			"isEnableVerify");
					put("IsDisable", 			"isDisableVerify");
					put("Exist", 				"ExistVerify");
					put("NonExist", 			"NonExistVerify");
					put("moveToElement", 		"moveToElement");
					put("Count", 				"getItemsCount");
					put("jsExcuteForElement",	"jsExcuteForElement");
				}
			});
			put("webfile", new HashMap<String, String>(){
				{
					put("FileUpload", 			"fileUp");
					put("FileUploadByWindow", 	"fileUpWithWindow");
					put("FileDownload", 		"fileDown");
					put("moveToElement", 		"moveToElement");
					put("jsExcuteForElement",	"jsExcuteForElement");
				}
			});
			put("webframe", new HashMap<String, String>(){
				{
					put("switchTo", 			"switchToFrame");
					put("moveToElement", 		"moveToElement");
					put("jsExcuteForElement", 	"jsExcuteForElement");
				}
			});
			put("webtable", new HashMap<String, String>(){
				{
					put("ALL", 					"TableInstructionCenter");
				}
			});
		}
	};
	
	public static String getDefaultMethodNameFromTheType(String elementType, String action){
//		if (!functionDictionary.isEmpty()) {
		String eType = elementType.toLowerCase();
		if (functionDictionary.containsKey(eType)) {
			Map<String, String> cps = functionDictionary.get(eType);
			for(Entry<String, String> entry : cps.entrySet()) {
				if (action.equalsIgnoreCase(entry.getKey()) || entry.getKey().equals("ALL")){
					return entry.getValue();
				}
			}
		}
		return null;
//		}
		
//		switch(elementType.toLowerCase()){
////		case "sql":
////			return "sqlScriptExecute";
//		case "webbrowser" :
//			if (action.equalsIgnoreCase("Navigate")) {
//				return "browserNavigate";
//			}
//			//暂时关闭wait动作在页面上的展示
//			/*if (action.equalsIgnoreCase("wait"))
//				return "browserWait";*/
//			if (action.equalsIgnoreCase("Wait")) {
//				return "browserSleep";
//			}
//			if (action.equalsIgnoreCase("Back")) {
//				return "browserBack";
//			}
//			if (action.equalsIgnoreCase("Close")) {
//				return "browserClose";
//			}
//			if(action.equalsIgnoreCase("Forward")){
//				return "browserForward";
//			}
//			if(action.equalsIgnoreCase("Refresh")){
//				return "browserRefresh";
//			}
//			return null;
//		case "webbutton" :
//			if(action.equalsIgnoreCase("Click")){
//				return "clickButton";
//			}
//			if(action.equalsIgnoreCase("DoubleClick")){
//				return "doubleClick";
//			}
//			else if(action.equalsIgnoreCase("IsEnable")){
//				return "isEnableVerify";
//			}
//			else if(action.equalsIgnoreCase("IsDisable")){
//				return "isDisableVerify";
//			}
//			else if(action.equalsIgnoreCase("Exist")){
//				return "existVerify";
//			}
//			else if(action.equalsIgnoreCase("NonExist")){
//				return "nonExistVerify";
//			}
//			else if (action.equalsIgnoreCase("Count")) {
//				return "getItemsCount";
//			}
//			else if (action.equalsIgnoreCase("clickOnHideButton")) {
//				return "clickOnHideButton";
//			}
//			else if (action.equalsIgnoreCase("moveToElement")) {
//				return "moveToElement";
//			}
//			else if (action.equalsIgnoreCase("dragAndDrop")) {
//				return "dragAndDrop";
//			}
//			else if (action.equalsIgnoreCase("jsExcuteForElement")) {
//				return "jsExcuteForElement";
//			}
//			return null;
//		case "webtextbox" :
//			if(action.equalsIgnoreCase("Enter")){
//				return "enterText";
//			}
//			else if(action.equalsIgnoreCase("Match")){
//				return "verifyValue";
//			}
//			else if(action.equalsIgnoreCase("InputContainsPageText")){
//				return "verifyContains";
//			}
//			else if(action.equalsIgnoreCase("InputInPageText")){
//				return "verifyIn";
//			}
//			else if (action.equalsIgnoreCase("EnterReadonly")) {
//				return "enterTextReadonly";
//			}
//			else if (action.equalsIgnoreCase("Clear")) {
//				return "clearText";
//			}
//			else if(action.equalsIgnoreCase("IsEnable")){
//				return "isEnableVerify";
//			}
//			else if(action.equalsIgnoreCase("IsDisable")){
//				return "isDisableVerify";
//			}
//			else if(action.equalsIgnoreCase("Exist")){
//				return "existVerify";
//			}
//			else if(action.equalsIgnoreCase("NonExist")){
//				return "nonExistVerify";
//			}
//			else if (action.equalsIgnoreCase("moveToElement")) {
//				return "moveToElement";
//			}
//			else if (action.equalsIgnoreCase("Count")) {
//				return "getItemsCount";
//			}
//			else if (action.equalsIgnoreCase("jsExcuteForElement")) {
//				return "jsExcuteForElement";
//			}
//
//			return null;
//
//		case "webradio" :
//			if(action.equalsIgnoreCase("Select")){
//				return "selectRadio";
//			}
//			else if(action.equalsIgnoreCase("IsEnable")){
//				return "isEnableVerify";
//			}
//			else if(action.equalsIgnoreCase("IsDisable")){
//				return "isDisableVerify";
//			}
//			else if(action.equalsIgnoreCase("Exist")){
//				return "existVerify";
//			}
//			else if(action.equalsIgnoreCase("NonExist")){
//				return "nonExistVerify";
//			}
//			else if (action.equalsIgnoreCase("moveToElement")) {
//				return "moveToElement";
//			}
//			else if (action.equalsIgnoreCase("Count")) {
//				return "getItemsCount";
//			}
//			else if (action.equalsIgnoreCase("jsExcuteForElement")) {
//				return "jsExcuteForElement";
//			}
//
//			return null;
//		case "weblink" :
//			if(action.equalsIgnoreCase("Click")){
//				return "clickLink";
//			}
//			else if(action.equalsIgnoreCase("DoubleClick")){
//				return "doubleClick";
//			}
//			else if(action.equalsIgnoreCase("IsEnable")){
//				return "isEnableVerify";
//			}
//			else if(action.equalsIgnoreCase("IsDisable")){
//				return "isDisableVerify";
//			}
//			else if(action.equalsIgnoreCase("Exist")){
//				return "existVerify";
//			}
//			else if(action.equalsIgnoreCase("NonExist")){
//				return "nonExistVerify";
//			}
//			else if (action.equalsIgnoreCase("moveToElement")) {
//				return "moveToElement";
//			}
//			else if (action.equalsIgnoreCase("Count")) {
//				return "getItemsCount";
//			}
//			else if (action.equalsIgnoreCase("jsExcuteForElement")) {
//				return "jsExcuteForElement";
//			}
//
//			return null;
//		case "webcheckbox" :
//			if(action.equalsIgnoreCase("Check")){
//				return "checkbox";
//			}
//			else if(action.equalsIgnoreCase("IsEnable")){
//				return "isEnableVerify";
//			}
//			else if(action.equalsIgnoreCase("IsDisable")){
//				return "isDisableVerify";
//			}
//			else if(action.equalsIgnoreCase("Exist")){
//				return "existVerify";
//			}
//			else if(action.equalsIgnoreCase("NonExist")){
//				return "nonExistVerify";
//			}
//			else if (action.equalsIgnoreCase("moveToElement")) {
//				return "moveToElement";
//			}
//			else if (action.equalsIgnoreCase("Count")) {
//				return "getItemsCount";
//			}
//			else if (action.equalsIgnoreCase("jsExcuteForElement")) {
//				return "jsExcuteForElement";
//			}
//
//			return null;
//		case "webdropdown" :
//			if(action.equalsIgnoreCase("Select")){
//				return "selectDropdown";
//			}
//			else if(action.equalsIgnoreCase("IsEnable")){
//				return "isEnableVerify";
//			}
//			else if(action.equalsIgnoreCase("IsDisable")){
//				return "isDisableVerify";
//			}
//			else if(action.equalsIgnoreCase("Exist")){
//				return "existVerify";
//			}
//			else if(action.equalsIgnoreCase("NonExist")){
//				return "nonExistVerify";
//			}
//			else if (action.equalsIgnoreCase("moveToElement")) {
//				return "moveToElement";
//			}
//			else if (action.equalsIgnoreCase("Count")) {
//				return "getItemsCount";
//			}
//			else if (action.equalsIgnoreCase("jsExcuteForElement")) {
//				return "jsExcuteForElement";
//			}
//
//			return null;
//		case "webfile":
//			if(action.equalsIgnoreCase("FileUpload")){
//				return "fileUp";
//			}
//			else if (action.equalsIgnoreCase("FileUploadByWindow")) {
//				return "fileUpWithWindow";
//			}
//			else if (action.equalsIgnoreCase("FileDownload")) {
//				return "fileDown";
//			}
//			else if (action.equalsIgnoreCase("moveToElement")) {
//				return "moveToElement";
//			}
//			else if (action.equalsIgnoreCase("jsExcuteForElement")) {
//				return "jsExcuteForElement";
//			}
//
//			return null;
//		case "webframe" :
//			if(action.equalsIgnoreCase("switchToFrame")){
//				return "switchToFrame";
//			}
//			else if (action.equalsIgnoreCase("moveToElement")) {
//				return "moveToElement";
//			}
//			else if (action.equalsIgnoreCase("jsExcuteForElement")) {
//				return "jsExcuteForElement";
//			}
//			return null;
//		case "webtable" :
//			//pass all input to table executor
//			return "TableInstructionCenter";
//		default :
//			return null;
//		}
	}
	
	public static boolean isWebElementType(String type) {
		return WebElementExecutor.WEB_ELEMENT_TYPES.contains(type.toLowerCase());
	}
	
	public static String getLocatorValue(InstructionOptions options, String sectionKey, JSONObject elementContentMap) {
		String locatorValue = elementContentMap.getString(sectionKey + ".locator-value");
		if (options.existOption(ContextConstant.ELEMENT_REPLACE_LOCATOR_VALUE)){
			String value = options.getValue(ContextConstant.ELEMENT_REPLACE_LOCATOR_VALUE);
			if (null != value){
				//TODO 非常重要，隐式顺序
				value = options.doWithAllTransfer(value);
				String[] replaceValues = value.split(",");
				if (replaceValues.length > 20){
					SGLogger.error("      [ELE_REPLACE_LOCATOR_VALUE] element more than 20 not support NOW.");
					return null;
				}
				for(int i = 0; i < replaceValues.length; ++i){
					String var = replaceValues[i];
					String matchVar = String.format("{%d}", i);
					locatorValue = locatorValue.replace(matchVar, var);
				}
			}
		}
		return locatorValue;
	}
}
