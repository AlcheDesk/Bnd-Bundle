package com.meowlomo.ci.ems.bundle.webdriver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.meowlomo.ci.ems.bundle.webdriver.LocatorUtils;
import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.InstructionUtils;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.webdriver.WebElementExecutor;
import com.meowlomo.ci.ems.bundle.webdriver.abandoned.FixInstructions;
import com.meowlomo.ci.ems.bundle.webdriver.ModuleExecutorSelector;

public class MainInstrctionExecutor {

	private WebElementExecutor elementExecutor = null;
	private ModuleExecutorSelector moduleExecutorSelector = null;

	public MainInstrctionExecutor(WebDriver driver, JSONObject elementMap, JSONObject instructionsArrayObj) {
		this.elementExecutor = new WebElementExecutor(driver);
		this.moduleExecutorSelector = new ModuleExecutorSelector(driver, elementMap, instructionsArrayObj);
	}

	/**
	 * 
	 * @param repoMap
	 * @param instructionObject
	 * @param action
	 * @param input
	 * @param options
	 * @param target
	 * @return 1: true,OK,pass;		0: false, failed;		-1: action not match method
	 */
//	public int executeInstrcution(JSONObject repoMap, String instructionObject, String action,
//			String input, InstructionOptions options, String target,WebDriver driver) {
//		int result = 0;
//		if (instructionObject.isEmpty()) {
//			SGLogger.info("      Found an empty instruction and it will be ingored.");
//			return 0;
//		} else {
//			Pattern re = Pattern.compile("^\\d+$");
//			Matcher m = re.matcher(instructionObject.toLowerCase());
//			if (!m.find()) {
//				SGLogger.info("      The TargetElementId is not a Valid Integer.");
//				return 0;
//			}
//		}
//
//		String sectionKey = instructionObject;
//		// check if repo contain that section
//		SGLogger.instructionStart("开始执行指令 [" + target + "]");
//		if (repoMap.has(sectionKey)) {
//			JSONObject elementContentMap = (JSONObject) repoMap.get(sectionKey);
//			System.out.println(elementContentMap);
//			
//			String type = elementContentMap.getString(sectionKey + ".type");
//			// check the type to see if this is a default type
//			int executionResult = 0;
//			String locatorType = elementContentMap.getString(sectionKey + ".locator-type");
//			String locatorValue = elementContentMap.getString(sectionKey + ".locator-value");
//			if (null != locatorValue) {
//				locatorValue = options.replaceLocatorValue(locatorValue);
//				if (null == locatorValue) return 0;			
//			}
//			
//			if (type != null && WebElementExecutor.isWebElementType(type)) {
//				String methodName = getMethodName(sectionKey, elementContentMap);			
//				By locator = LocatorUtils.getLocator(locatorType, locatorValue);
//				executionResult = elementExecutor.executeDefaultElementAction(type, methodName, locator, action, input, options);
//				if (-1 == executionResult)
//					SGLogger.error(" 方法不存在,ElementType: [" + type + "], Action:" + action);
//				else if (-2 == executionResult)
//					SGLogger.error(" 方法名称不存在,请注意组合,ElementType: [" + type + "], Action:" + action);
//			} else {
//				/*
//				 * what we need for module executor 1 : locator 2 : the type 3 :
//				 * the instruction object string 4 : the action string 5 : the
//				 * input string
//				 */
//				if (type.equalsIgnoreCase("sql")) {
//					executionResult = ActionCommon.sqlScriptExecute(locatorValue, input) ? 1 : 0;
//				} else if (type.equalsIgnoreCase("javascript")) {
//					executionResult = ActionCommon.jScriptExecute(driver, input, options) ? 1 : 0;
//				} else if (type.equalsIgnoreCase("webframe")) {
//					if (action.equalsIgnoreCase("switchto"))
//						executionResult = ActionCommon.switchToFrame(driver, locatorValue, options) ? 1 : 0;
//				} else {
//					By locator = LocatorUtils.getLocator(locatorType, locatorValue);
//					executionResult = moduleExecutorSelector.selectModuleToExecute(locator, type, instructionObject,
//							action, input, options) ? 1 : 0;
//				}
//			}
//			SGLogger.instructionEnd(String.format("完成执行指令[%s] 结果 [%s]", target, executionResult));
////			SGLogger.instructionEnd(String.format("完成执行指令[%s]%s 结果 [%s]", instructionObject, target, executionResult));
//			result = executionResult;
//		} else {
//			SGLogger.error(" 目标元素不存在， Target Id: [" + sectionKey + "]" + target);
//			result = 0;
//		}
//
////		SGLogger.instructionEnd("=============== 第 [" + ContextConstant.EXCEL_ROW_NUMBER + "]指令 [" + instructionObject
////				+ "]" + target + " 处理完毕===============");
//		return result;
//	}

	private String getMethodName(String sectionKey, JSONObject elementContentMap) {
		String methodName = null;
		if (!elementContentMap.isNull(sectionKey + ".method"))
			methodName = elementContentMap.getString(sectionKey + ".method");
		
		if (null == methodName)
			methodName = "default";
		return methodName;
	}
}
