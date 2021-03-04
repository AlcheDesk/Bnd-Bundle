package com.meowlomo.ci.ems.bundle.webdriver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.FileUtilConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.utils.StringUtil;

public class TestCaseProccessorNew {
	WebDriver webDriver;
	IHttpUtil httpUtil;
	
	public TestCaseProccessorNew(WebDriver driver) {
		webDriver = driver;
		httpUtil = IHttpUtil.getHttpTool();
	}
	
	public ExecutionResult stepInstruction(JSONObject instruction, List<String> paramsInOrOut) {
		System.err.println("begin webdriver stepInstruction:" + instruction);
		FileUtilConstant.INSTRUCTION = instruction.getString("target");
		
		JSONObject element = instruction.getJSONObject("element");
		String elementType = element.getString("elementType");
		String action = instruction.getString("action");
		String input = instruction.getString("input");
		String instructionType = instruction.optString("instructionType");
		boolean webfunctionType = instructionType.equals("WebFunction");
		input = InstructionOptions.instance().doWithAllTransfer(input);

		JSONObject updateParams = new JSONObject();
		updateParams.put("inputData", input);
		httpUtil.updateInstructionResult(updateParams.toString());
		
		// generate the options
		ExecutionResult er = InstructionOptions.instance().genFromOptionStr(instruction.getString("options"));
		if (!er.bOK()) return er;
		
		boolean noOne = InstructionOptions.instance().notEmpty() && InstructionOptions.instance().doInputSaveOptionWork(input);

		String locatorValue = element.isNull("locatorValue") ? null : element.optString("locatorValue");
		if (null != locatorValue) {
			locatorValue = InstructionOptions.instance().replaceLocatorValue(locatorValue);
			if (null == locatorValue)
				return new ExecutionResult("locatorValue is null.");
		} else {
			locatorValue = "";
		}
	
		ExecutionResult executionResult = null;
		if (WebElementExecutor.isWebElementType(elementType)) {
			String locatorType = element.getString("locatorType");
			By locator = LocatorUtils.getLocator(locatorType, locatorValue);
			SGLogger.info(String.format("lt:%s, lv:%s", locatorType, locatorValue));
			
			String screenshootFileName = FileUtilConstant.normalIndexName();
			if (webfunctionType) {
				if(!InstructionOptions.instance().existOption(ContextConstant.NO_BEGIN_SCREENSHOTS)) {
					doHigthLightScreenshow(screenshootFileName, locator, " begin");
				}
			} else {
				ActionCommon.takeNormalScreenshotBegin(webDriver, screenshootFileName);
			}
			System.err.println("webdriver stepInstruction step:" + FileUtilConstant.INSTRUCTION);
			executionResult = this.executeDefaultElementAction(elementType, "default", locator, action, input);
			if (webfunctionType) {
				//如果因为链接已跳走等原因,则使用原有截图方式
				if (false == doHigthLightScreenshow(screenshootFileName, locator, " end"))
					ActionCommon.takeNormalScreenshotEnd(webDriver, screenshootFileName);				
			} else {
				ActionCommon.takeNormalScreenshotEnd(webDriver, screenshootFileName);
			}
			
			// TODO does this needed here ? (must have notified ATM before)
			if (-1 == executionResult.exitCode())
				SGLogger.error(" 方法不存在,ElementType: [" + elementType + "], Action:" + action);
			else if (-2 == executionResult.exitCode())
				SGLogger.error(" 方法名称不存在,请注意组合,ElementType: [" + elementType + "], Action:" + action);
		} else {
			if (elementType.equalsIgnoreCase("sql")) {
				SGLogger.error(" sql类型Instruction不应在此处");
			} else if (elementType.equalsIgnoreCase("javascript")) {
				executionResult = ActionCommon.jScriptExecute(webDriver, input, InstructionOptions.instance());
			} /*else if (elementType.equalsIgnoreCase("webframe")) {
				if (action.equalsIgnoreCase("switchto"))
					executionResult = ActionCommon.switchToFrame(webDriver, locatorValue, InstructionOptions.instance());
			}*/ else {
				executionResult = new ExecutionResult(false, 100000, "未被处理的类型:" + elementType);
			}
		}
		
		String elementText = InstructionOptions.instance().doTextSaveIfNeeded(ActionCommon.elementText());
		if (!StringUtil.nullOrEmpty(elementText)) {
			System.out.println("element txt saved:" + elementText);
		}
	
		if (null == executionResult) {
			String info = String.format("未被处理的语句类型:%s,操作:%s,元素类型:%s", instructionType, action, elementType);
			executionResult = new ExecutionResult(false, info);
		}
		return executionResult;
	}
	
	/**
	 * methodName为default,copy的同名方法
	 * @author Andrew Chen
	 */
	public ExecutionResult executeDefaultElementAction(String elementType, String methodName, By locator, String action, String input){
		String actionCommonName = "com.meowlomo.ci.ems.bundle.webdriver.ActionCommon";
		String className = null;
		String executeMethodName = null;

		if(methodName.equalsIgnoreCase("default")){
			executeMethodName = WebElementExecutor.getDefaultMethodNameFromTheType(elementType, action);
			System.err.println("executeDefaultElementAction.executeMethodName:" + executeMethodName);
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
			String msString, msStringReason;
			System.out.println(msString = "[WebDriver Error] Method name is null.");
			System.out.println(msStringReason = "methodname:" + methodName + " ,elementType:" + elementType + " ,action:" + action);
			return new ExecutionResult(false, -2, msString + msStringReason);
		}

		Class[] cArg = new Class[4];
		if(webDriver == null){
			cArg[0] = WebElement.class;
		}
		else{
			cArg[0] = WebDriver.class;
		}

		cArg[1] = By.class;
		cArg[2] = String.class;
		cArg[3] = InstructionOptions.class;
		try {
			if (className == null) {
				Class<?> actionClass = Class.forName(actionCommonName);
				System.out.println("call method:" + executeMethodName + " locator:"
						+ (null == locator ? " empty " : locator.toString()) + " input:" + input);
				Method method = actionClass.getDeclaredMethod(executeMethodName, cArg);
				if (null == method)
					return new ExecutionResult(false, -1,"method is null. and name is:" + executeMethodName);
				Object resultObj = method.invoke(null, webDriver, locator, input, InstructionOptions.instance());
				System.out.println("action method result " + resultObj);
				if (resultObj instanceof Boolean) {
					Boolean result = (boolean) resultObj;
					return new ExecutionResult(result, "OK", "fail");
				} else if (resultObj instanceof ExecutionResult) {
					ExecutionResult result = (ExecutionResult) resultObj;
					return result;
				}
			} else {
				String msString,msString2,msString3;				
				System.out.println(msString = 
						"call method " + executeMethodName + " locator " + locator.toString() + " input " + input);
				System.out.println(msString2 = "[" + "com.meowlomo.ci.ems.bundle.webdriver.custom.method." + className + "]["
						+ executeMethodName + "][" + cArg + "]");
				System.out.println(msString3 = "Class not loaded");
				return new ExecutionResult(false, msString + msString2 + msString3);
			}
		} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| ClassNotFoundException e) {
			e.printStackTrace();
			return new ExecutionResult("执行WebDriver时有异常发生:" + e.getClass().getName());
		}
		return new ExecutionResult(false, "执行WebDriver时发生未定义的错误");
	}

	private boolean doHigthLightScreenshow(String screenshootFileName, By by, String fileTail) {
		ActionCommon.tail = fileTail.trim();
		WebElement webElement = ActionCommon.retryFindElement(webDriver, by);
		if (ActionCommon.takeNormalScreenshotWithHighLight(webDriver, screenshootFileName + fileTail, webElement)) {
			ActionCommon.removeElementHighLightStyle(webDriver, webElement);
			return true;
		} else {
			return false;
		}
	}
}
