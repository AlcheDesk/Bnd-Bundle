package com.meowlomo.ci.ems.bundle.webdriver;

import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;
import com.meowlomo.ci.ems.bundle.interfaces.IDataSource;
import com.meowlomo.ci.ems.bundle.interfaces.IFileService;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil.MethodType;
import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.FileUtilConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.utils.StringUtil;

import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

public class ActionCommon {
	public static String errTxt = "ERROR";
	// sperate this for remote file logging
	public static String tail = "";

	static String remoteScreenShootFileName = "";

	/**
	 * for SAVE_TEXT option
	 */
	private static String elementText = "";
	private static String msString = "fail";
	private static IFileService fileService = null;
	private static String fileServicePrefix = "";
	private static JSONObject addStepFileLog = null;
	private static IHttpUtil httpUtil = null;
	private static long runId = 0L;
	private static long instructionResultId = 0L;

	private static final ExecutionResult noVerifyER = new ExecutionResult(true, "[OPTION]noVerify option!!!");
	private static final ExecutionResult inputIgnoreER = new ExecutionResult(true, "[OPTION conflict]input ignore!!!");
	private static final ExecutionResult timeoutER = new ExecutionResult("超时异常发生!!!");
	
	public static String elementText() {
		String tmp = elementText;
		elementText = "";
		return tmp;
	}

	private static void trySaveElementText(String text) {
		if (null != text)
			elementText = text;
	}

	private static void trySaveElementText(WebElement element) {
		if (null != element) {
			String text = element.getText();
			if (null != text && !text.isEmpty())
				elementText = text;
		}
	}

	private static void logElementText(WebElement element) {
		elementText = element.getText();
		if (null == elementText)
			elementText = "";
		System.err.println(elementText);
	}

	protected static boolean checkPage(WebDriver driver, String pageTitle) {
		ActionCommon.waitForPageLoaded(driver);
		if (pageTitle == null) {
			SGLogger.notValidSelection("      Check page tile match [" + pageTitle + "]");
		} else if (!ContextConstant.matchIgnored(pageTitle)) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 5, ContextConstant.WEBELEMENT_WAIT_SLEEP);
				WebElement elem = wait.until(ExpectedConditions.elementToBeClickable(By.className("screentitle")));
				if (elem == null) {
					SGLogger.error("      Page tile does not exist.");
					return false;
				} else {
					String currentTitle = elem.getText();
					elementText = currentTitle;
					if (currentTitle.matches(pageTitle)) {
						SGLogger.actionComplete(
								"      Page title match. Expected [" + pageTitle + "] : Actual [" + currentTitle + "]");
						return true;
					} else {
						SGLogger.wrongValue("      Page title not match. Expected [" + pageTitle + "] : Actual ["
								+ currentTitle + "]");
						return false;
					}
				}
			} catch (TimeoutException e) {
				SGLogger.elementTimeOut("     Page title doesn't not exist.");
				return false;
			} finally {
				SGLogger.codeError("      ");
			}
		}
		return false;
	}

	protected static boolean click(WebDriver driver, By selector, String input) {
		if (input == null) {
			SGLogger.error(
					"      The input data is NULL point, please check if the excel file contains the corresponding data column.");
			return false;
		}
		if (!ContextConstant.matchIgnored(input)) {
			return true;
		}
		if (BooleanStringSet.isTrue(input)) {
			ActionCommon.waitForPageLoaded(driver);
			ActionCommon.checkAlert(driver);
			WebElement element = null;
			WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
			try {
				wait.until(ExpectedConditions.presenceOfElementLocated(selector));
				element = ActionCommon.retryFindElement(driver, selector);
				if (element != null) {
					String text = ActionCommon.retryGetText(driver, selector);
					if (text.equals(errTxt)) {
						return SGLogger.codeError("      Get Text unsuccessfully.").bOK();
					}
					elementText = text;
					if (!ActionCommon.retryClick(driver, selector)) {
						return SGLogger.codeError("      Click unsuccessfully.").bOK();
					}
					SGLogger.actionComplete("      Click the WebElement [" + text + "]");
					ActionCommon.checkAlert(driver);
					if (ActionCommon.checkErrorMessage(driver)) {
						return true;
					} else {
						return false;
					}
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				SGLogger.elementTimeOut("      Try to click none existing WebElement.");
				return false;
			} catch (Exception e) {
				SGLogger.codeError("      [AC_CLIK_00] Exception:[" + e.getClass().getName() + "]");
				return false;
			}
		} else if (!BooleanStringSet.isTrue(input)) {
			return true;
		} else {
			SGLogger.invalidInput("      Input [" + input + "] for Clicking WebElement is invalid.");
			return false;
		}
		return true;
	}

	
	/**
	 * 对WebElement执行js脚本操作
	 * 
	 * @param driver
	 * @param selector
	 * @param input
	 *            要执行的js脚本
	 * @param options
	 * @return
	 */
	public static ExecutionResult jsExcuteForElement(WebDriver driver, By selector, String input, InstructionOptions options) {
		if (input == null) {
			return SGLogger.error(
					"      The input data is NULL point, please check if the input contains the corresponding data column.");
		}
		ActionCommon.waitForPageLoaded(driver);
		ActionCommon.checkAlert(driver);
		WebElement element = null;
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(selector));
			element = ActionCommon.retryFindElement(driver, selector);
			if (element != null) {
				String text = ActionCommon.retryGetText(driver, selector);
				if (text.equals("ERROR")) {
					return SGLogger.codeError("      Get Text unsuccessfully.");
				}
				elementText = text;
				((JavascriptExecutor) driver).executeScript("arguments[0]." + input, element);				
				SGLogger.actionComplete("      jsExcute the WebElement [" + text + "]");
				ActionCommon.checkAlert(driver);
				if (ActionCommon.checkErrorMessage(driver)) {
					return new ExecutionResult(true, "js脚本执行成功");
				} else {
					return new ExecutionResult(false, "js脚本执行失败");
				}
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
			return SGLogger.elementTimeOut("      Try to jsExcute none existing WebElement.");
		} catch (Exception e) {
			return SGLogger.codeError("      [AC_CLIK_00] Exception:[" + e.getClass().getName() + "]");
		}
		return new ExecutionResult(false, "js脚本执行时发生未知错误");
	}
	
	public static ExecutionResult clickOnHideButton(WebDriver driver, By selector, String input, InstructionOptions options) {
		if (input == null) {
			return SGLogger.error(
					"      The input data is NULL point, please check if the input contains the corresponding data column.");
		}
		ActionCommon.waitForPageLoaded(driver);
		ActionCommon.checkAlert(driver);
		WebElement element = null;
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(selector));
			element = ActionCommon.retryFindElement(driver, selector);
			if (element != null) {
				String text = ActionCommon.retryGetText(driver, selector);
				if (text.equals(errTxt)) {
					return SGLogger.codeError("      Get Text unsuccessfully.");
				}
				elementText = text;
				((JavascriptExecutor) driver).executeScript("arguments[0].click()", element);
				SGLogger.actionComplete("      clickCoverButton the WebElement [" + text + "]");
				ActionCommon.checkAlert(driver);
				if (ActionCommon.checkErrorMessage(driver)) {
					return new ExecutionResult(true, "clickOnHideButton执行成功");
				} else {
					return new ExecutionResult(false, "clickOnHideButton执行失败");
				}
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
			return SGLogger.elementTimeOut("      Try to doubleclick none existing WebElement.");
		} catch (Exception e) {
			return SGLogger.codeError("      [AC_CLIK_00] Exception:[" + e.getClass().getName() + "]");
		}
		return new ExecutionResult(false, "clickOnHideButton执行时发生未知错误");
	}

	public static ExecutionResult doubleClick(WebDriver driver, By selector, String input, InstructionOptions options) {
		if (input == null) {
			return SGLogger.error(
					"      The input data is NULL point, please check if the input contains the corresponding data column.");
		}
		ActionCommon.waitForPageLoaded(driver);
		ActionCommon.checkAlert(driver);
		WebElement element = null;
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(selector));
			element = ActionCommon.retryFindElement(driver, selector);
			if (element != null) {
				String text = ActionCommon.retryGetText(driver, selector);
				if (text.equals(errTxt)) {
					return SGLogger.codeError("      Get Text unsuccessfully.");
				}
				elementText = text;
				// just for chrome
				/*
				 * Actions actions = new Actions(driver);
				 * actions.moveToElement(element).doubleClick().build().perform( );
				 * Thread.sleep(3000);
				 */

				((JavascriptExecutor) driver).executeScript("var evt = document.createEvent('MouseEvents');"
						+ "evt.initMouseEvent('dblclick',true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0,null);"
						+ "arguments[0].dispatchEvent(evt);", element);

				SGLogger.actionComplete("      doubleClick the WebElement [" + text + "]");
				ActionCommon.checkAlert(driver);
				if (ActionCommon.checkErrorMessage(driver)) {
					return new ExecutionResult(true, "doubleClick执行成功");
				} else {
					return new ExecutionResult(false, "doubleClick执行失败");
				}
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
			return SGLogger.elementTimeOut("      Try to doubleclick none existing WebElement.");
		} catch (Exception e) {
			return SGLogger.codeError("      [AC_CLIK_00] Exception:[" + e.getClass().getName() + "]");
		}
		return new ExecutionResult(false, "doubleClick执行时发生未知错误");
	}

	public static ExecutionResult clickButton(WebDriver driver, By selector, String input, InstructionOptions options)
			throws InterruptedException {
		if (input == null) {
			return SGLogger.error(
					"      The input data is NULL point, please check if the excel file contains the corresponding data column.");
		}
		if (ContextConstant.matchIgnored(input)) {
			return inputIgnoreER;
		}
		// check the input for the button is valid.
		if (input.isEmpty() || BooleanStringSet.isTrue(input) || input.equals(ContextConstant.SKIP_BY_NOT_FOUND)) {
			ActionCommon.waitForPageLoaded(driver);
			ActionCommon.checkAlert(driver);
			WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
			String text = "";
			// no matter what we need the check the button is on the screen
			// first.
			try {
				List<WebElement> buttons = driver.findElements(By.tagName("button"));
				for (int i = 0; i < buttons.size(); i++) {
					WebElement button = buttons.get(i);
					System.out.println("Button text [" + button.getText() + "] " + button.getAttribute("id"));
				}
				wait.until(ExpectedConditions.presenceOfElementLocated(selector));
				text = ActionCommon.retryGetText(driver, selector);
				if (text.equals(errTxt)) {
					return SGLogger.codeError("      Get Text unsuccessfully.");
				}
				elementText = text;
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (input.equals(ContextConstant.SKIP_BY_NOT_FOUND)) {
					return new ExecutionResult(true, ContextConstant.SKIP_BY_NOT_FOUND);
				} else {
					return SGLogger.elementTimeOut("      Try to click non existing button.");
				}
			} catch (Exception e) {
				e.printStackTrace();
				return SGLogger.codeError("      [AC_BUTT_00] Exception:[" + e.getClass().getName() + "]");
			}

			// check the button is disabled or not. can't perform an action on a
			// disabled button.
			if (ActionCommon.isDisabled(driver, selector)) {
				// check if ignore the disabled button error.
				if (options.existOption(ContextConstant.INSTRUCTION_INGORE_DISABLED_BUTTON)) {
					return SGLogger.infoER(
							"      Tried to click a disabled button. This error is ignored by the instruction option ["
									+ ContextConstant.INSTRUCTION_INGORE_DISABLED_BUTTON + "]");
				} else {
					if (options.existOption(ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE)) {
						return SGLogger.infoER("      Button [" + text + "] is disabled. With option ["
								+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE
								+ "], click action will not be performed.");
					} else {
						return SGLogger.error("      Button [" + text + "] is disabled. Cant not be clicked.");
					}
				}
			}

			// check the click until options in the in list for buttons
			int optionIndex = 0;
			optionIndex = options.existOption(ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE) ? (optionIndex + 1)
					: optionIndex;
			optionIndex = options.existOption(ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS) ? (optionIndex + 2)
					: optionIndex;
			optionIndex = options.existOption(ContextConstant.INSTRUCTION_BUTTON_UNTIL_POPUP) ? (optionIndex + 4)
					: optionIndex;
			optionIndex = options.existOption(ContextConstant.INSTRUCTION_BUTTON_RANDOM_CLICK_ONE) ? (optionIndex + 8)
					: optionIndex;

			/**
			 * @author yanfang.chen 16:弹窗确定=====BUTTON_AlERT_OK 32：弹窗取消====BUTTON_AlERT_NO
			 *         64：弹窗上的文字比对====以BUTTON_AlERT_INFO:开头
			 *         128：页面文字验证====以BUTTON_FIND_TEXT:开头
			 */
			optionIndex = options.existOption(ContextConstant.INSTRUCTION_BUTTON_AlERT_OK) ? (optionIndex + 16)
					: optionIndex;
			optionIndex = options.existOption(ContextConstant.INSTRUCTION_BUTTON_AlERT_CANCEL) ? (optionIndex + 32)
					: optionIndex;
			// checkOptionContains
			optionIndex = options.existOption(ContextConstant.BTN_CLICK_UNTIL_VERIFY_ALERT_TEXT) ? (optionIndex + 64)
					: optionIndex;
			optionIndex = options.existOption(ContextConstant.INSTRUCTION_BUTTON_FIND_TEXT) ? (optionIndex + 128)
					: optionIndex;

			int actionCount = 0;
			switch (optionIndex) {
			case 0:
				// ======normal case, just click======
				optionIndex = 0;
				if (ActionCommon.retryClick(driver, selector)) {
					ActionCommon.checkAlert(driver);
					if (ActionCommon.checkErrorMessage(driver)) {
						ActionCommon.checkAlert(driver);
						SGLogger.actionComplete(msString = "#1      Click the button [" + text + "]");
						ActionCommon.checkAlert(driver);
						return new ExecutionResult(true, msString);
					}
				} else {
					return SGLogger.error("      Click the button [" + text + "]");
				}
				return new ExecutionResult("click btn case 0 未定义分支");
			case 1:
				// ======click until the button to be disabled======
				optionIndex = 0;
				do {
					SGLogger.info("      Starting clicking the button [" + text + "] with option ["
							+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE + "]");
					ActionCommon.retryClick(driver, selector);
					SGLogger.info("      Click the button [" + text + "]");
					ActionCommon.checkAlert(driver);
					ActionCommon.checkErrorMessage(driver);
					actionCount++;
				} while (!ActionCommon.isDisabled(driver, selector)
						&& actionCount < ContextConstant.WEBELEMENT_RETRY_MAX);

				if (ActionCommon.isDisabled(driver, selector)) {
					return SGLogger.infoER("      After [" + actionCount + "] times of click. The button is disabled.");
				} else {
					ExecutionResult er = SGLogger.error("      After [" + actionCount + "] times of click. The button is still active.");
					SGLogger.info(
							"      If the button is not deigned to be disabled after click. Please remove the option ["
									+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE + "] from the instruction.");
					return er;
				}
			case 2:
				// ======click until the button disappears======
				SGLogger.info("      Starting clicking the button [" + text + "] with option ["
						+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "]");
				optionIndex = 0;
				wait.until(ExpectedConditions.presenceOfElementLocated(selector));
				do {
					try {
						wait.until(ExpectedConditions.presenceOfElementLocated(selector));
						ActionCommon.retryClick(driver, selector);
						SGLogger.info("      Click the button [" + text + "]");
						ActionCommon.checkAlert(driver);
						ActionCommon.checkErrorMessage(driver);
					} catch (TimeoutException e) {
						SGLogger.actionComplete(msString = "      Click the button [" + text + "] with option ["
								+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "]");
						return new ExecutionResult(true, msString);
					} catch (Exception e) {
						e.printStackTrace();
						return SGLogger.codeError("      [AC_BUTT_08] Exception:[" + e.getClass().getName() + "]");
					}
				} while (actionCount < ContextConstant.WEBELEMENT_RETRY_MAX);
				return SGLogger.error("      Click the button [" + text + "] with option ["
						+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "], tried [5] times");
			case 3:
				// ======click until the button disappears and disabled======
				optionIndex = 0;
				return SGLogger.errorTitle(" [    错误选项    ] ", "      Option [" + ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE + "] and ["
						+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "] can not be used at the same time.");
			case 4:
				// ======click until the pop-up shows======
				optionIndex = 0;
				SGLogger.info("      Starting clicking the button [" + text + "] with option ["
						+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_POPUP + "]");
				do {
					try {
						boolean foundPopup = false;
						do {
							wait.until(ExpectedConditions.presenceOfElementLocated(selector));
							ActionCommon.retryClick(driver, selector);
							SGLogger.info("      Click the button [" + text + "]");
							if (ActionCommon.checkAlert(driver)) {
								foundPopup = true;
								ActionCommon.checkErrorMessage(driver);
								return SGLogger.elementTimeOut("      Click the button [" + text + "] with option ["
										+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_POPUP + "]");
							}
						} while (!foundPopup && actionCount < ContextConstant.WEBELEMENT_RETRY_MAX);
						ExecutionResult er = SGLogger.codeError("      Click the button [" + text + "] with option ["
								+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_POPUP + "]");
						SGLogger.info("      There is not pop-up appears after [" + ContextConstant.WEBELEMENT_RETRY_MAX
								+ "] try. There was [" + ContextConstant.POPUP_WAIT_MAX
								+ "] seconds between each try.");
						return er.setOK(true);
					} catch (TimeoutException e) {
						return SGLogger.elementTimeOut("      Click the button [" + text + "] with option ["
								+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_POPUP + "]");
					} catch (Exception e) {
						e.printStackTrace();
						return SGLogger.codeError("      [AC_BUTT_08] Exception:[" + e.getClass().getName() + "]");
					}
				} while (actionCount < ContextConstant.WEBELEMENT_RETRY_MAX);
			case 5:
				// ======click until the button disabled and pop-up======
				optionIndex = 0;
				SGLogger.info("      Starting clicking the button [" + text + "] with option ["
						+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE + "]["
						+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_POPUP + "]");
				do {
					try {
						boolean foundPopup = false;
						do {
							wait.until(ExpectedConditions.presenceOfElementLocated(selector));
							ActionCommon.retryClick(driver, selector);
							SGLogger.info("      Click the button [" + text + "]");
							if (ActionCommon.checkAlert(driver)) {
								foundPopup = true;
								// check the button is disabled
								if (ActionCommon.isDisabled(driver, selector)) {
									ExecutionResult er = SGLogger.elementTimeOut("      Click the button [" + text + "] with option ["
											+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_POPUP + "] and ["
											+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE + "]");
									ActionCommon.checkErrorMessage(driver);
									// TODO 
									return er.setOK(true);
								} else {
									ExecutionResult er = SGLogger.error("      Click the button [" + text + "] with option ["
											+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "] and ["
											+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE + "]");
									SGLogger.info("      Pop-up detected, But the button is still active");
									ActionCommon.checkErrorMessage(driver);
									return er;
								}
							}
						} while (!foundPopup && actionCount < ContextConstant.WEBELEMENT_RETRY_MAX);
						ExecutionResult er = SGLogger.codeError("      Click the button [" + text + "] with option ["
								+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "]");
						SGLogger.info("      There is not pop-up appears after [" + ContextConstant.WEBELEMENT_RETRY_MAX
								+ "] try. There was [" + ContextConstant.POPUP_WAIT_MAX
								+ "] seconds between each try.");
						return er;
						
					} catch (TimeoutException e) {
						return SGLogger.elementTimeOut("      Click the button [" + text + "] with option ["
								+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "]");
					} catch (Exception e) {
						e.printStackTrace();
						return SGLogger.codeError("      [AC_BUTT_08] Exception:[" + e.getClass().getName() + "]");
					}
				} while (actionCount < ContextConstant.WEBELEMENT_RETRY_MAX);
			case 6:
				// ======click until the button disappears and pop-up======
				optionIndex = 0;
				do {
					try {
						boolean foundPopup = false;
						do {
							wait.until(ExpectedConditions.presenceOfElementLocated(selector));
							ActionCommon.retryClick(driver, selector);
							SGLogger.info("      Click the button [" + text + "]");
							if (ActionCommon.checkAlert(driver)) {
								foundPopup = true;
								// check the button is disappeared.
								try {
									wait.until(ExpectedConditions.presenceOfElementLocated(selector));
									ExecutionResult er = SGLogger.error("      Click the button [" + text + "] with option ["
											+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "] and ["
											+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "]");
									SGLogger.info("      Pop-up detected, But the button is still on the page");
									ActionCommon.checkErrorMessage(driver);
									return er;
								} catch (TimeoutException e) {
									ExecutionResult er = SGLogger.elementTimeOut("      Click the button [" + text + "] with option ["
											+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_POPUP + "] and ["
											+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "]");
									ActionCommon.checkErrorMessage(driver);
									return er.setOK(true);
								}
							}
						} while (!foundPopup && actionCount < ContextConstant.WEBELEMENT_RETRY_MAX);
						ExecutionResult er = SGLogger.codeError("      Click the button [" + text + "] with option ["
								+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "]");
						SGLogger.info("      There is not pop-up appears after [" + ContextConstant.WEBELEMENT_RETRY_MAX
								+ "] try. There was [" + ContextConstant.POPUP_WAIT_MAX
								+ "] seconds between each try.");
						return er;
					} catch (TimeoutException e) {
						return SGLogger.elementTimeOut("      Click the button [" + text + "] with option ["
								+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "]");
					} catch (Exception e) {
						e.printStackTrace();
						return SGLogger.codeError("      [AC_BUTT_08] Exception:[" + e.getClass().getName() + "]");
					}
				} while (actionCount < ContextConstant.WEBELEMENT_RETRY_MAX);
			case 7:
				// ======click until the button disappears, disabled and
				// pop-up======
				optionIndex = 0;
				return SGLogger.errorTitle(" [    错误选项    ] ", "      Option [" + ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISABLE + "], ["
						+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_DISAPPEARS + "] and ["
						+ ContextConstant.INSTRUCTION_BUTTON_UNTIL_POPUP + "] can not be used at the same time.");
			case 8:
				// ======random click button disappears, disabled and
				// pop-up======
				try {
					WebDriverWait driverwait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
					try {
						driverwait.until(ExpectedConditions.presenceOfElementLocated(selector));
					} catch (Exception e) {
						e.printStackTrace();
						return SGLogger.codeError("      [AC_LTN_01] Exception:[" + e.getClass().getName() + "]");
					}
					// get he button
					ActionCommon.waitForPageLoaded(driver);
					Thread.sleep(2000);
					List<WebElement> buttons = driver.findElements(selector);
					ArrayList<Integer> listNumbers = new ArrayList<Integer>();
					for (int i = 1; i < buttons.size(); i++) {
						listNumbers.add(new Integer(i));
					}
					Collections.shuffle(listNumbers);
					// generate a random selection count
					Random rand = new Random();
					int selectionCount = rand.nextInt(buttons.size()) - 1;
					boolean result = true;
					boolean bSaveText = false;
					for (int i = 1; i < selectionCount; i++) {
						System.out.println("button number " + i);
						buttons = driver.findElements(selector);
						System.out.println("Select number " + listNumbers.get(i));
						WebElement button = buttons.get(listNumbers.get(i));
						Thread.sleep(1000);
						button.click();

						String RaButtontext = button.getText();
						if (null == RaButtontext) {
							return SGLogger.codeError("      Get Text unsuccessfully.");
						}

						if (!bSaveText) {
							bSaveText = true;
							elementText = RaButtontext;
						}
						ActionCommon.waitForPageLoaded(driver);
						SGLogger.actionComplete("      Click the button [" + text + "]");
						ActionCommon.checkAlert(driver);
						if (ActionCommon.checkErrorMessage(driver)) {
							// return true;
						} else {
							result = false;
						}
					}
					return new ExecutionResult(result, result ? "clickButton成功" : "clickButton失败");
				} catch (TimeoutException e) {
					e.printStackTrace();
					return SGLogger.elementTimeOut("      Try to click a none existing button WebElement.");
				}

			case 16:
				// ========点击按钮，弹出弹框之后点击确定=======
				try {
					// optionIndex = 0;
					if (ActionCommon.retryClick(driver, selector)) {
						ActionCommon.checkAlert(driver);
						if (ActionCommon.checkErrorMessage(driver)) {
							ActionCommon.checkAlert(driver);
							SGLogger.actionComplete(msString = "#16   Click the button [" + text + "]");
							ActionCommon.checkAlert(driver);
							return new ExecutionResult(true, msString);
						}
					} else {
						return SGLogger.error("    Click the button [" + text + "]");
					}
				} catch (Exception e) {
					e.printStackTrace();
					return SGLogger.error("Exception in clickButton case 16:" + e.getClass().getName());
				}
				return new ExecutionResult("clickButton case 16 未定义错误");
			case 32:
				// ========点击按钮，弹出弹框之后点击取消=======
				try {
					// optionIndex = 0;
					if (ActionCommon.retryClick(driver, selector)) {
						ActionCommon.checkAlertNO(driver);
						if (ActionCommon.checkErrorMessage(driver)) {
							ActionCommon.checkAlertNO(driver);
							SGLogger.actionComplete(msString = "#32   Click the button [" + text + "]");
							ActionCommon.checkAlertNO(driver);
							return new ExecutionResult(true, msString);
						}
					} else {
						return SGLogger.error("    Click the button [" + text + "]");
					}
				} catch (Exception e) {
					e.printStackTrace();
					return SGLogger.error("Exception in clickButton case 32:" + e.getClass().getName());
				}
				return new ExecutionResult("clickButton case 32 未定义错误");
			case 64:
				// =========点击按钮之后弹出框有提示；用户输入预期的字符串，与弹窗上的字符作比较================
				try {
					if (ActionCommon.retryClick(driver, selector)) {
						String strAlertInfo = ActionCommon.checkAlertGetInfo(driver);
						if (ActionCommon.checkErrorMessage(driver)) {
							SGLogger.actionComplete("#64   Click the button [" + text + "]");
							String strOptionInfo = options.getOptionsString();
							if (strOptionInfo.equalsIgnoreCase(strAlertInfo)) {
								return SGLogger.infoER("options value" + " [" + strOptionInfo + "] " + "is equal alertText"
										+ " [" + strAlertInfo + "] ");
							} else {
								return SGLogger.error("options value" + " [" + strOptionInfo + "] " + "isn't equal alertText"
										+ " [" + strAlertInfo + "] ");
							}
						} else {
							return SGLogger.error("    Click the button [" + text + "]");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					return SGLogger.error("Exception in clickButton case 64:" + e.getClass().getName());
				}
				return new ExecutionResult("clickButton case 64 未定义错误");
			case 128:
				// =========点击按钮后，针对用户输入预期的字符串，与页面上的字符作比较================
				String expectValue = null;
				try {
					String strInfo = options.getOptionsString();
					expectValue = strInfo.substring(strInfo.indexOf(":") + 1, strInfo.lastIndexOf("}"));
					if (ActionCommon.retryClick(driver, selector)) {
						driver.findElement(By.xpath("//*[contains(.,'" + expectValue + "')]"));
						return SGLogger.infoER("options value" + " [" + expectValue + "] " + "is appeard!");
					}
				} catch (NoSuchElementException e) {
					return SGLogger.error("options value" + " [" + expectValue + "] " + " doesn't  exist!");
				}
				return new ExecutionResult("clickButton case 128 未定义错误");
			default:
				optionIndex = 0;
				return SGLogger.error("      Unknow options index [" + optionIndex + "], please check the options");
			}
		} else if (!BooleanStringSet.isTrue(input)) {
			return new ExecutionResult(true, "input内容包含在false集合中:" + input);
		} else {
			return SGLogger.invalidInput("      Input [" + input + "] for button WebElement is invalid.");
		}
	}

	public static ExecutionResult clickLink(WebDriver driver, By selector, String input, InstructionOptions options) {
		if (input == null) {
			return SGLogger.error(
					"      The input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (ContextConstant.matchIgnored(input)) {
			return inputIgnoreER;
		}
		if (input.isEmpty() || BooleanStringSet.isTrue(input)) {
			ActionCommon.waitForPageLoaded(driver);
			ActionCommon.checkAlert(driver);
			try {
				WebElement linkElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					linkElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
					trySaveElementText(linkElement);
				} catch (Exception e) {
					e.printStackTrace();
					return SGLogger.codeError("      [AC_LINK_01] Exception:[" + e.getClass().getName() + "]");
				}
				Thread.sleep(1000);
				/**
				 * 1. 页面文字验证====以LINK_FIND_TEXT:开头 2. 弹窗上的文字比对====以LINK_AlERT_INFO开头 4.
				 * 点击按钮之后，有弹窗弹出，根据输入的option来比对=======忽略大小写全等
				 */
				int optionIndex = 0;
				optionIndex = options.existOption(ContextConstant.INSTRUCTION_LINK_FIND_TEXT) ? (optionIndex + 1)
						: optionIndex;
				optionIndex = options.existOption(ContextConstant.INSTRUCTION_LINK_AlERT_INFO) ? (optionIndex + 2)
						: optionIndex;
				optionIndex = options.existOption(ContextConstant.LNK_CLICK_UNTIL_VERIFY_ALERT_TEXT) ? (optionIndex + 4)
						: optionIndex;

				System.out.println("optionIndex=" + optionIndex);
				switch (optionIndex) {
				case 0:
					optionIndex = 0;
					// String text = linkElement.getText();
					String text = ActionCommon.retryGetText(driver, selector);
					if (text.equals(errTxt)) {
						return SGLogger.codeError("      Get Text unsuccessfully.");
					}
					Thread.sleep(1000);
					// wait.until(ExpectedConditions.elementToBeClickable(selector)).click();
					if (!ActionCommon.retryClick(driver, selector)) {
						return SGLogger.codeError("      Click unsuccessfully.");
					}
					// linkElement.click();
					SGLogger.actionComplete(msString = "      Click the link [" + text + "] case 0");
					ActionCommon.checkAlert(driver);
					Thread.sleep(1000);
					if (ActionCommon.checkErrorMessage(driver))
						return new ExecutionResult(true, msString);
					else {
						return new ExecutionResult(msString);
					}
				case 1:
					// =========点击link后，针对用户输入预期的字符串，与页面上的字符作比较================
					optionIndex = 0;
					String expectValue = null;
					try {
						String strInfo = options.getOptionsString();
						expectValue = strInfo.substring(strInfo.indexOf(":") + 1, strInfo.lastIndexOf("}"));
						if (ActionCommon.retryClick(driver, selector)) {
							try {
								By selector2 = By.xpath("//*[contains(.,'" + expectValue + "')]");
								new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP)
										.until(ExpectedConditions.presenceOfElementLocated(selector2));
								driver.findElement(selector2);
							} catch (Exception e) {
								e.printStackTrace();
								return SGLogger.codeError("      [AC_LINK_02] Exception:[" + e.getClass().getName() + "]");
							}
							return SGLogger.infoER("options value" + " [" + expectValue + "] " + "is appeard!");
						}
					} catch (NoSuchElementException e) {
						return SGLogger.infoER("options value" + " [" + expectValue + "] " + " doesn't  exist!").setOK(false);
					}
				case 2:
					// =========点击link之后弹出框有提示；用户输入预期的字符串，与弹窗上的字符作比较================
					try {
						if (ActionCommon.retryClick(driver, selector)) {
							String StrOption = ActionCommon.checkAlertGetInfo(driver);
							String strInfo = options.getOptionsString();
							String strInfo2 = strInfo.substring(strInfo.indexOf(":") + 1, strInfo.lastIndexOf("}"));
							if (StrOption.contains(strInfo2)) {
								return SGLogger.infoER("options value" + " [" + strInfo2 + "] " + "is contained in alertText"
										+ " [" + StrOption + "] ");
							} else {
								return SGLogger.error("options value" + " [" + strInfo2 + "] " + "isn't contained in alertText"
										+ " [" + StrOption + "] ");
							}
						} else {
							return SGLogger.error("    Click the link fail!");
						}
					} catch (Exception e) {
						e.printStackTrace();
						return SGLogger.error("Exception in clickLink case 2:" + e.getClass().getName());
					}
				case 4:
					// ===============点击link之后有弹窗弹出，根据输入的文字与弹窗上的文字比对==================
					try {
						if (ActionCommon.retryClick(driver, selector)) {
							String strAlertInfo = ActionCommon.checkAlertGetInfo(driver);
							if (ActionCommon.checkErrorMessage(driver)) {
								String strOptionInfo = options.getOptionsString();
								if (strOptionInfo.equalsIgnoreCase(strAlertInfo)) {
									return SGLogger.infoER("options value" + " [" + strOptionInfo + "] " + "is equal alertText"
											+ " [" + strAlertInfo + "] ");
								} else {
									return SGLogger.infoER("options value" + " [" + strOptionInfo + "] "
											+ "isn't equal in alertText" + " [" + strAlertInfo + "] ").setOK(false);
								}
							} else {
								return new ExecutionResult("click Link case 4 未定义分支");
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						return SGLogger.error("Exception in clickLink case 4:" + e.getClass().getName());
					}
				default:
					optionIndex = 0;
					return SGLogger.error("      Unknow options index [" + optionIndex + "], please check the options");
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				return SGLogger.elementTimeOut("      Try to click a none existing link WebElement.");
			} catch (Exception e) {
				e.printStackTrace();
				return SGLogger.codeError("      [AC_LINK_00] Exception:[" + e.getClass().getName() + "]");
			}
		} else if (!BooleanStringSet.isTrue(input)) {
			return new ExecutionResult(true, "input内容包含在false集合中:" + input);
		} else {
			return SGLogger.invalidInput("      Input [" + input + "] for Link WebElement is invalid.");
		}
	}

	public static boolean hover(WebDriver driver, By selector, String input) {
		if (!ContextConstant.matchIgnored(input)) {
			return true;
		}
		if (BooleanStringSet.isTrue(input)) {
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement hoverElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.presenceOfElementLocated(selector));
				if (hoverElement != null) {
					Actions hover = new Actions(driver);
					try {
						hoverElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
								.until(ExpectedConditions.presenceOfElementLocated(selector));
					} catch (Exception e) {
						return SGLogger.codeError("      [AC_HOVE_01] Exception:[" + e.getClass().getName() + "]").bOK();
					}
					// String text = hoverElement.getText();
					String text = ActionCommon.retryGetText(driver, selector);
					if (text.equals(errTxt)) {
						return SGLogger.codeError("      Get Text unsuccessfully.").bOK();
					}
					elementText = text;
					try {
						hoverElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
								.until(ExpectedConditions.presenceOfElementLocated(selector));
					} catch (Exception e) {
						return SGLogger.codeError("      [AC_HOVE_02] Exception:[" + e.getClass().getName() + "]").bOK();
					}
					hover.moveToElement(hoverElement);
					hover.perform();
					SGLogger.actionComplete("      Hover to WebElement [" + text + "]");
					return true;
				} else {
					return false;
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					SGLogger.elementTimeOut("      WebElement not found.");
				}
				return false;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					SGLogger.codeError("      [AC_HOVE_02] Exception:[" + e.getClass().getName() + "]");
				}
				return false;
			}
		} else {
			SGLogger.invalidInput("      Input [" + input + "] for Link WebElement is invalid.");
			return false;
		}
	}

	//TODO
	private static ExecutionResult verifyTextValue(WebDriver driver, By selector, String expectedValue, boolean verifyMode,
			boolean tryMode) {
		if (expectedValue == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (ContextConstant.matchIgnored(expectedValue)) {
			return inputIgnoreER;
		} else {
			ActionCommon.waitForPageLoaded(driver);
			try {
				try {
					(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (TimeoutException e) {
					return SGLogger.codeError("      [AC_TEXT_VERIFY_00] Exception:[" + e.getClass().getName() + "]");
				}

				String actualValue = ActionCommon.retryGetValue(driver, selector);
				String actualText = ActionCommon.retryGetText(driver, selector);

				if (actualValue == null && actualText == null) {
					return SGLogger.error(
							"      Couldn't get the text and the value of the element to verify the actual value.");
				}
				System.err.println("value:" + actualValue + ", text:" + actualText);
				boolean textMatch = false;
				if (actualText != null) {
					elementText = actualText;
					textMatch = checkExpectedAndActualMatch(expectedValue, actualText);
				}

				boolean valuMatch = false;
				if (actualValue != null) {
					valuMatch = checkExpectedAndActualMatch(expectedValue, actualValue);
				}

				boolean combineMatch = false;
				if (valuMatch || textMatch) {
					combineMatch = true;
				}
				if (!combineMatch) {
					if (verifyMode) {
						return SGLogger.verifyErrorER("      Text value is unmatched. Expected [" + expectedValue
								+ "] : Actual [" + actualValue + "]");
					} else {
						return SGLogger.wrongValue("      Text value is unmatched. Expected [" + expectedValue + "] : Actual ["
								+ actualValue + "]");
					}
				} else {
					if (verifyMode) {
						if (valuMatch) {
							return SGLogger.verifySuccessER("      Text is matched. Expected [" + expectedValue + "] : Actual ["
									+ actualValue + "]");
						} else {
							return SGLogger.verifySuccessER("      Text is matched. Expected [" + expectedValue + "] : Actual ["
									+ actualText + "]");
						}
					} else {
						if (valuMatch) {
							return SGLogger.actionComplete("      Text is matched. Expected [" + expectedValue + "] : Actual ["
									+ actualValue + "]");
						} else {
							return SGLogger.actionComplete("      Text is matched. Expected [" + expectedValue + "] : Actual ["
									+ actualText + "]");
						}
					}
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					if (verifyMode) {
						return SGLogger.verifyErrorER("      Textbox element timeout wiht excepted : [" + expectedValue + "]");
					} else {
						return SGLogger.elementTimeOut("     Enter Text :[" + expectedValue + "]");
					}
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					if (verifyMode) {
						return SGLogger.verifyErrorER("      Error on finding text value on the page. Exception:["
								+ e.getClass().getName() + "]");
					} else {
						return SGLogger.error("      Got error when trying to get the actual value from the text. Exception["
								+ e.getClass().getName() + "]");
					}
				}
				return new ExecutionResult("校验文本时有异常发生:" + e.getClass());
			}
		}
	}

	/**
	 * @author qiaorui.chen
	 *
	 *         验证第一个select值
	 */
	public static boolean verifySelectValue(WebDriver driver, By selector, String expectedValue,
			InstructionOptions options) {
		do {
			WebElement dropdownElement = null;
			if (expectedValue == null) {
				SGLogger.error(
						"      the input data is NULL, please check if the excel file contains the corresponding data column.");
				break;
			}
			if (ContextConstant.matchIgnored(expectedValue)) {
				return true;
			} else {
				ActionCommon.waitForPageLoaded(driver);
				try {
					try {
						dropdownElement = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP)
								.until(ExpectedConditions.presenceOfElementLocated(selector));
					} catch (TimeoutException e) {
						SGLogger.codeError("      [AC_TEXT_VERIFY_00] Exception:[" + e.getClass().getName() + "]");
						break;
					}

					String selectedOption = new Select(dropdownElement).getFirstSelectedOption().getText();
					if (selectedOption == null) {
						SGLogger.error("      Couldn't get the text and the value of the element ");
						break;
					}

					if (selectedOption != null) {
						elementText = selectedOption;
						if (selectedOption.equals(expectedValue)) {
							SGLogger.verifySuccess("      Text is matched. Expected [" + expectedValue + "] : Actual ["
									+ selectedOption + "]");
							return true;
						} else {
							Integer expectedInt = getFirstIntInString(expectedValue);
							Integer selectedInt = getFirstIntInString(selectedOption);
							if (null != expectedInt && null != selectedInt
									&& expectedInt.intValue() == selectedInt.intValue()) {
								SGLogger.verifySuccess("     Select Text is acceptable matched. Expected ["
										+ expectedValue + "] : Actual [" + selectedOption + "]");
								return true;
							}
							SGLogger.verifyError("      Text value is unmatched. Expected [" + expectedValue
									+ "] : Actual [" + selectedOption + "]");
							break;
						}
					}

				} catch (TimeoutException e) {
					e.printStackTrace();
				}
			}
		} while (false);
		return false;
	}

	/**
	 * 验证文本被input文本包含
	 *
	 * @author Andrew chen
	 * @param driver
	 * @param selector
	 * @param expectedValue
	 * @param options
	 * @return
	 */
	public static ExecutionResult verifyIn(WebDriver driver, By selector, String expectedValue, InstructionOptions options) {
		return verifyIn(driver, selector, expectedValue, options, false);
	}

	private static ExecutionResult verifyIn(WebDriver driver, By selector, String expectedValue, InstructionOptions options,
			boolean verifyMode) {
		if (expectedValue == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (ContextConstant.matchIgnored(expectedValue)) {
			return inputIgnoreER;
		} else {
			ActionCommon.waitForPageLoaded(driver);

			try {
				try {
					(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (TimeoutException e) {
					return SGLogger.codeError("      [AC_TEXT_VERIFY_00] Exception:[" + e.getClass().getName() + "]");
				}

				// String actualValue = ActionCommon.retryGetValue(driver,
				// selector);
				String actualText = ActionCommon.retryGetText(driver, selector);
				if (null == actualText) {
					return SGLogger.error("      Couldn't get the text and the value of the element ");
				} else {
					elementText = actualText;
					if (expectedValue.contains(actualText)) {
						return SGLogger.verifySuccessER("      Text [in] action matched. Expected [" + expectedValue
								+ "] : Actual [" + actualText + "]");
					} else {
						return SGLogger.verifyErrorER("      Text [in] action's value is unmatched. Expected [" + expectedValue
								+ "] : Actual [" + actualText + "]");
					}
				}

			} catch (TimeoutException e) {
				e.printStackTrace();
				return new ExecutionResult("校验属于时有超时异常发生,定位:" + selector + ", 期望值:" + expectedValue);
			}
		}
	}

	/**
	 * 验证文本包含input文本
	 *
	 * @author Andrew chen
	 * @param driver
	 * @param selector
	 * @param expectedValue
	 * @param options
	 * @return
	 */
	public static ExecutionResult verifyContains(WebDriver driver, By selector, String expectedValue,
			InstructionOptions options) {
		return verifyContains(driver, selector, expectedValue, options, false);
	}

	private static ExecutionResult verifyContains(WebDriver driver, By selector, String expectedValue,
			InstructionOptions options, boolean verifyMode) {
		if (expectedValue == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (ContextConstant.matchIgnored(expectedValue)) {
			return inputIgnoreER;
		} else {
			ActionCommon.waitForPageLoaded(driver);

			try {
				try {
					(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (TimeoutException e) {
					return SGLogger.codeError("      [AC_TEXT_VERIFY_00] Exception:[" + e.getClass().getName() + "]");
				}

				// String actualValue = ActionCommon.retryGetValue(driver,
				// selector);
				String actualText = ActionCommon.retryGetText(driver, selector);
				if (null == actualText) {
					return SGLogger.error("      Couldn't get the text and the value of the element ");
				} else {
					elementText = actualText;
					if (actualText.contains(expectedValue)) {
						return SGLogger.verifySuccessER("      Text contains action matched. Expected [" + expectedValue
								+ "] : Actual [" + actualText + "]");
					} else {
						return SGLogger.verifyErrorER("      Text contains action's value is unmatched. Expected ["
								+ expectedValue + "] : Actual [" + actualText + "]");
					}
				}

			} catch (TimeoutException e) {
				e.printStackTrace();
				return new ExecutionResult("校验包含时有超时异常发生,定位:" + selector + ", 期望值:" + expectedValue);
			}
		}
	}

	/*
	 * author: 陈侨锐 验证文本值 action :verify
	 *
	 *
	 *
	 */
	public static ExecutionResult verifyValue(WebDriver driver, By selector, String expectedValue, InstructionOptions options) {
		return verifyValue(driver, selector, expectedValue, options, false);
	}

	private static ExecutionResult verifyValue(WebDriver driver, By selector, String expectedValue, InstructionOptions options,
			boolean verifyMode) {
		if (expectedValue == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (ContextConstant.matchIgnored(expectedValue)) {
			return inputIgnoreER;
		} else {
			ActionCommon.waitForPageLoaded(driver);
			try {
				try {
					(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (TimeoutException e) {
					return SGLogger.codeError("      [AC_TEXT_VERIFY_00] Exception:[" + e.getClass().getName() + "]");
				}

				// String actualValue = ActionCommon.retryGetValue(driver,
				// selector);
				String actualText = ActionCommon.retryGetText(driver, selector);
				if (null == actualText) {
					return SGLogger.error("      Couldn't get the text and the value of the element ");
				} else {
					elementText = actualText;
					if (actualText.equals(expectedValue)) {
						return SGLogger.verifySuccessER("      Text is matched. Expected [" + expectedValue + "] : Actual ["
								+ actualText + "]");
					} else {
						Integer expectedCount = getFirstIntInString(expectedValue);
						Integer actualCount = getFirstIntInString(actualText);
						if (null != expectedCount && null != actualCount
								&& expectedCount.intValue() == actualCount.intValue()) {
							return SGLogger.verifySuccessER("      Text is acceptable matched. Expected [" + expectedValue
									+ "] : Actual [" + actualText + "]");
						}

						return SGLogger.verifyErrorER("      Text value is unmatched. Expected [" + expectedValue
								+ "] : Actual [" + actualText + "]");
					}
				}

			} catch (TimeoutException e) {
				e.printStackTrace();
				return new ExecutionResult("校验文本值时有超时异常发生,定位:" + selector + ", 期望值:" + expectedValue);
			}
		}
	}

	/**
	 * 引用处都会有隐含比对逻辑，即只比较第一位匹配的int数字
	 *
	 * @param actualText
	 * @return
	 */
	private static Integer getFirstIntInString(String actualText) {
		Integer actualCount = null;
		if (!StringUtil.nullOrEmpty(actualText)) {
			String regex = "\\d*";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(actualText);
			while (m.find()) {
				if (!"".equals(m.group())) {
					actualCount = new Integer(m.group());
					break;
				}
			}
		}
		return actualCount;
	}

	public static ExecutionResult checkbox(WebDriver driver, By selector, String input, InstructionOptions options) {
		if (null == input) {
			return SGLogger.error(
					"      The input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(input)) {
			input = input.toLowerCase();
			if (BooleanStringSet.contains(input)) {
				ActionCommon.waitForPageLoaded(driver);
				ActionCommon.checkAlert(driver);
				WebElement checkboxElement = null;
				WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
				try {
					wait.until(ExpectedConditions.presenceOfElementLocated(selector));
					checkboxElement = ActionCommon.retryFindElement(driver, selector);
					if (checkboxElement != null) {
						String label = ActionCommon.getCheckboxLabel(driver, selector);
						if (label.equals(errTxt)) {
							return SGLogger.codeError("      Get Text unsuccessfully.");
						}
						elementText = label;
						if (checkboxElement.isSelected() == false) {
							checkboxElement.click();
						}
						// return ActionCommon.verifyCheckboxValue(driver,
						// selector, false, action,false);
						if (options.existOption(ContextConstant.INSTRUCTION_NO_VERIFY)) {
							return noVerifyER;
						} else {
							return ActionCommon.verifyCheckboxValue(driver, selector, input);
						}

					} else {
						return SGLogger.error("      Couldn't not find the checkbox on the page.");
					}
				} catch (TimeoutException e) {
					e.printStackTrace();
					return SGLogger.elementTimeOut("      Checkbox doesn't exist");
				} catch (Exception e) {
					e.printStackTrace();
					return SGLogger.codeError("      [AC_CHEK_06] Exception:[" + e.getClass().getName() + "]");
				}
			} else {
				return SGLogger.invalidInput("      Checkbox input [" + input + "] is not supported.");
			}
		}
		return inputIgnoreER;
	}

	/**
	 * @author yanfang.chen
	 * @param driver
	 * @param selector
	 * @param input
	 * @param options
	 * @return
	 */
	public static ExecutionResult isDisableVerify(WebDriver driver, By selector, String input, InstructionOptions options) {
		return isDisableVerify(driver, selector, input, options, false);
	}

	private static ExecutionResult isDisableVerify(WebDriver driver, By selector, String input, InstructionOptions options,
			boolean tryMode) {
		// 目前此方法不需要输入input
		/*
		 * if (input == null) { SGLogger.error(
		 * "      The input data is NULL point, please check if the excel file contains the corresponding data column."
		 * ); return false; }
		 */
		if (input.equals(ContextConstant.IGNORE_DEPRECATED)) {
			return inputIgnoreER;
		}
		if (input.equals("") || BooleanStringSet.isTrue(input) || input.equals(ContextConstant.SKIP_BY_NOT_FOUND)) {
			WebDriverWait wait = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP));
			WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
			String disabled = element.getAttribute("disabled");
			if (disabled == null) {
				System.out.println(msString = " element is not disabled. [" + disabled + "]");
				return new ExecutionResult(msString);
			} else {
				System.out.println(msString = " element is disabled. [" + disabled + "]");
				return new ExecutionResult(true, msString);
			}
		}
		return new ExecutionResult(true, "禁用校验通过(未定义)");
	}

	public static ExecutionResult isEnableVerify(WebDriver driver, By selector, String input, InstructionOptions options) {
		/*
		 * if (input == null) { SGLogger.error(
		 * "      The input data is NULL point, please check if the excel file contains the corresponding data column."
		 * ); return false; }
		 */
		if (ContextConstant.matchIgnored(input)) {
			return inputIgnoreER;
		}
		if (input == null || BooleanStringSet.isTrue(input) || input.equals(ContextConstant.SKIP_BY_NOT_FOUND)) {
			WebDriverWait wait = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP));
			WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
			String disabled = element.getAttribute("disabled");
			if (disabled != null) {
				System.out.println(msString = " element is not disabled. [" + disabled + "]");
				return new ExecutionResult(msString);
			} else {
				System.out.println(msString = " element is disabled. [" + disabled + "]");
				return new ExecutionResult(true, msString);
			}
		}
		return new ExecutionResult(true, "可用校验通过(未定义)");
	}

	/**
	 * @author yanfang.chen
	 * @param driver
	 * @param selector
	 * @param input
	 * @param options
	 * @return
	 */
	public static ExecutionResult existVerify(WebDriver driver, By selector, String input, InstructionOptions options) {
		if (ContextConstant.matchIgnored(input)) {
			return inputIgnoreER;
		}
		if (input.equals("") || BooleanStringSet.isTrue(input) || input.equals(ContextConstant.SKIP_BY_NOT_FOUND)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement element = null;
				element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]");
				}
				if (element != null && element instanceof WebElement)
					return new ExecutionResult(true, "存在性校验成功.");
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.elementTimeOut("     element :[" + input + "]");
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					return SGLogger.verifyErrorER("      element not found on page.");
				}
			}

		}
		return new ExecutionResult(true, "存在性校验成功(未执行).");
	}

	public static ExecutionResult nonExistVerify(WebDriver driver, By selector, String input, InstructionOptions options) {
		if (ContextConstant.matchIgnored(input)) {
			return inputIgnoreER;
		}
		if (input.equals("") || BooleanStringSet.isTrue(input) || input.equals(ContextConstant.SKIP_BY_NOT_FOUND)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement element = null;
				element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]").setOK(true);
				}
				if (null != element && element instanceof WebElement)
					return new ExecutionResult("非存在性校验失败.");
			} catch (TimeoutException e) {
				// e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.elementTimeOut("     element :[" + input + "]").setOK(true);
				}
				ExecutionResult tmp = timeoutER;
				return tmp.setOK(true);
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]").setOK(true);
				} else {
					return SGLogger.verifyErrorER("      element not found on page.").setOK(true);
				}
			}

		}
		return new ExecutionResult(true, "非存在性校验成功(未执行)");
	}

	/**
	 * @author qiaorui.chen
	 * @action disable
	 *
	 *         input值：yes 验证是否为disabled状态,如果元素为disabled返回true
	 */
//	private static boolean disableVerify(WebDriver driver, By selector, String input, InstructionOptions options) {
//		if (input == null) {
//			SGLogger.error(
//					"      The input data is NULL point, please check if the excel file contains the corresponding data column.");
//			return false;
//		}
//		if (!ContextConstant.matchIgnored(input)) {
//			return true;
//		}
//		if (BooleanStringSet.isTrue(input) || input.equals(ContextConstant.SKIP_BY_NOT_FOUND)) {
//			WebDriverWait wait = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX));
//			WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
//			String disabled = element.getAttribute("disabled");
//			if (disabled == null) {
//				System.out.println(" element is not disabled. [" + disabled + "]");
//				return false;
//			} else {
//				System.out.println(" element is disabled. [" + disabled + "]");
//				return true;
//			}
//		}
//		return true;
//	}

	private static ExecutionResult verifyCheckboxValue(WebDriver driver, By selector, String expectedValue) {
		if (expectedValue == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (ContextConstant.matchIgnored(expectedValue)) {
			return inputIgnoreER;
		} else {
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement checkboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				if (checkboxElement != null) {
					String label = ActionCommon.getCheckboxLabel(driver, selector);
					if (label.equals(errTxt)) {
						return SGLogger.codeError("      Get Text unsuccessfully.");
					}
					elementText = label;
					boolean checkboxStatus = checkboxElement.isSelected();
					if (checkboxStatus == BooleanStringSet.isTrue(expectedValue)) {
						return SGLogger.verifySuccessER("      Checkbox [" + label + "] value is matched. ");
					} else {
						return SGLogger.verifyErrorER("      Checkbox [" + label + "] value is unmatched. Expected ["
								+ expectedValue + "]: Actual [" + checkboxStatus + "]");
					}
				} else {
					return SGLogger.error("      Couldn't not find the checkbox on the page.");
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				return SGLogger.verifyErrorER("      Checkbox element timeout wiht excepted : [" + expectedValue + "]");
			} catch (Exception e) {
				return SGLogger.verifyErrorER(
						"      Error on finding checkbox on the page. Exception:[" + e.getClass().getName() + "]");
			}
		}
	}

	private static String getCheckboxLabel(WebDriver driver, By selector) {
		ActionCommon.waitForPageLoaded(driver);
		WebElement checkboxElement = null;
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(selector));
			checkboxElement = ActionCommon.retryFindElement(driver, selector);
			WebElement textboxParentElement = ActionCommon.retryFindElement(checkboxElement, By.xpath(".."));
			// table type
			String parentTag = textboxParentElement.getTagName();
			if (parentTag.equals("td")) {
				// get the label
				String label = ActionCommon.retryGetText(driver, selector);
				return label;
			}
			// check the type of the parent(no table type)
			if (parentTag.equals("label")) {
				String label = textboxParentElement.getText();
				return label;
			} else {
				WebElement labelEle = textboxParentElement.findElement(By.tagName("label"));
				if (labelEle != null) {
					String label = labelEle.getText();
					return label;
				}
			}
		} catch (TimeoutException e) {
			SGLogger.elementTimeOut("      Checkbox doesn't exist,when try to get checkbox label.");
			return "";
		}
		return "";
	}

	/**
	 * @author yanfang.chen todo 20170706
	 *
	 */
	public static ExecutionResult enterTextReadonly(WebDriver driver, By selector, String input, InstructionOptions options) {
		return ActionCommon.enterTextReadonly(driver, selector, input, options, false);
	}

	private static ExecutionResult enterTextReadonly(WebDriver driver, By selector, String input, InstructionOptions options,
			boolean tryMode) {
		if (null == input) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(input)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]");
				}
				((JavascriptExecutor) driver).executeScript(String
						.format("arguments[0].removeAttribute('readonly','readonly');arguments[0].value='%s'", input),
						textboxElement);
				String readonlyAtt = textboxElement.getAttribute("readonly");
				if (readonlyAtt == null) {
					// textboxElement.sendKeys(Keys.CONTROL + "a");
					// textboxElement.sendKeys(Keys.DELETE);
					// textboxElement.sendKeys(Keys.HOME, text);
					ActionCommon.checkAlert(driver);
					logElementText(textboxElement);
					if (options.existOption(ContextConstant.INSTRUCTION_NO_VERIFY)) {
						return noVerifyER;
					} else {
						return ActionCommon.verifyTextValue(driver, selector, input, false, false);
					}
				} else {
					return new ExecutionResult("只读属性已设置.");
				}

			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.elementTimeOut("     Enter Text :[" + input + "]");
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					return SGLogger.verifyErrorER("      Text box not found on page.");
				}
			}
		}
		return new ExecutionResult(true, "只读输入成功(未执行)");
	}

	/**
	 * @author yanfang.chen todo
	 * @param driver
	 * @param selector
	 * @param input
	 * @return
	 */
	public static ExecutionResult clearText(WebDriver driver, By selector, String input, InstructionOptions options) {
		return ActionCommon.clearText(driver, selector, input, options, false);
	}

	private static ExecutionResult clearText(WebDriver driver, By selector, String text, InstructionOptions options,
			boolean tryMode) {
		if (text == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(text)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]");
				}
				textboxElement.click();
				textboxElement.clear();
				ActionCommon.checkAlert(driver);
				logElementText(textboxElement);
				if (options.existOption(ContextConstant.INSTRUCTION_NO_VERIFY)) {
					return noVerifyER;
				} else {
					return ActionCommon.verifyTextValue(driver, selector, text, false, false);
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.elementTimeOut("     clear Text :[" + text + "]");
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					return SGLogger.verifyErrorER("      Text box not found on page.");
				}
			}
		}
		return new ExecutionResult(true, "文本清除成功(未执行)");
	}

	/**
	 * @author yanfang.chen todo
	 * @param driver
	 * @param selector
	 * @param input
	 * @return
	 */
	public static ExecutionResult modifyText(WebDriver driver, By selector, String input, InstructionOptions options) {
		return ActionCommon.modifyText(driver, selector, input, options, false);
	}

	private static ExecutionResult modifyText(WebDriver driver, By selector, String input, InstructionOptions options,
			boolean tryMode) {
		if (input == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(input)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]");
				}
				textboxElement.click();
				textboxElement.clear();
				textboxElement.sendKeys(Keys.CONTROL + "a");
				textboxElement.sendKeys(Keys.DELETE);
				textboxElement.sendKeys(Keys.HOME, input, Keys.TAB);
				ActionCommon.checkAlert(driver);
				logElementText(textboxElement);
				if (options.existOption(ContextConstant.INSTRUCTION_NO_VERIFY)) {
					return noVerifyER;
				} else {
					return ActionCommon.verifyTextValue(driver, selector, input, false, false);
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.elementTimeOut("     modify Text :[" + input + "]");
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					return SGLogger.verifyErrorER("      Text box not found on page.");
				}
			}
		} else {
			return inputIgnoreER;
		}
	}

	/**
	 * @author yanfang.chen
	 * @param driver
	 * @param selector
	 * @param input
	 * @return
	 */
	public static ExecutionResult fileUp(WebDriver driver, By selector, String input, InstructionOptions options) {
		return ActionCommon.fileUp(driver, selector, input, options, false);
	}

	private static ExecutionResult fileUp(WebDriver driver, By selector, String input, InstructionOptions options,
			boolean tryMode) {
		if (input == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(input)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);

			try {
				WebElement textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]");
				}

				textboxElement.sendKeys(input);
				ActionCommon.checkAlert(driver);
				logElementText(textboxElement);
				/*
				 * if (options.checkOption(ContextConstant.NO_VERIFY)) { return true; } else {
				 * return ActionCommon.verifyTextValue(driver, selector, text, false, false); }
				 */
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.elementTimeOut("     File Text :[" + input + "]");
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					return SGLogger.verifyErrorER("      File box not found on page.");
				}
			}
		}
		return new ExecutionResult(true, "文件上传成功(未执行)");
	}

	/**
	 * 文件上传方法，调用系统弹窗的情况：使用autoit实现，FileUploadAutoit.exe为autoit的可执行文件，兼容三种浏览器的调用：ie、Firefox、Chrome
	 *
	 * @author yanfang.chen
	 * @param driver
	 * @param selector
	 * @param text
	 *            文件上传的路径，例如：C:\Users\yanfang.chen\Desktop\png\Replace示例01.png
	 * @return boolean
	 */
	public static ExecutionResult fileUpWithWindow(WebDriver driver, By selector, String input, InstructionOptions options) {
		return ActionCommon.fileUpWithWindow(driver, selector, input, options, false);
	}

	private static ExecutionResult fileUpWithWindow(WebDriver driver, By selector, String input, InstructionOptions options,
			boolean tryMode) {
		if (input == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(input)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);

			try {
				WebElement fileUpElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					fileUpElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]");
				}
				String executefileName = "fileUploadAutoit_v1.exe";
				//String browser = "firefox";
				//String executeString = executefileName + " " + browser + " " + input;
				String executeString = executefileName + " " + input;
				int timeout = 5; // 等待时间默认设置为5秒
				fileUpElement.click();
				runAutoit(executefileName, executeString, timeout);
				ActionCommon.checkAlert(driver);
				logElementText(fileUpElement);
				/*
				 * if (options.checkOption(ContextConstant.NO_VERIFY)) { return true; } else {
				 * return ActionCommon.verifyTextValue(driver, selector, text, false, false); }
				 */
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.elementTimeOut("     File Text :[" + input + "]");
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					return SGLogger.verifyErrorER("      File box not found on page.");
				}
			}
		}
		return new ExecutionResult(true, "文件弹窗上传成功(未执行)");
	}

	/**
	 * @author yanfang.chen
	 * @param executefileName
	 *            auto3的文件名，包含路径加上文件名称，例如：d://fileUploadAutoit_v1.exe
	 * @param timeout
	 *            超时时间，单位是秒
	 * @param executeString
	 *            可执行文件运行语句 例如：此处上传文件的语句为："d://fileUploadAutoit_v1.exe" + " " + "d:\\test.eml"
	 *
	 * @param args
	 *            auto3的参数
	 * @throws IOException
	 */
	private static void runAutoit(String executefileName, String executeString, int timeout) throws IOException {
		try {
			// 如果发现进程，先杀死
			if (findProcess(executefileName)) {
				killProcess(executefileName);
			}
			// 可执行文件的运行
			Runtime.getRuntime().exec(executeString).waitFor();
			int count = 0;
			while (findProcess(executefileName) && count < timeout) {
				System.out.println(count);
				Thread.sleep(1000);
				count++;
			}
			// 如果进程未结束，杀死
			if (findProcess(executefileName)) {
				killProcess(executefileName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 查找进程
	 *
	 * @param processName
	 * @return
	 */
	private static boolean findProcess(String processName) {
		BufferedReader bufferedreader = null;
		try {
			Process process = Runtime.getRuntime().exec("tasklist /fi \" imagename eq " + processName + " \" ");
			bufferedreader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = bufferedreader.readLine()) != null) {
				if (line.contains(processName)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (bufferedreader != null) {
				try {
					bufferedreader.close();
				} catch (Exception e2) {

				}
			}
		}
	}

	/**
	 * 杀进程
	 *
	 * @param processName
	 */
	private static void killProcess(String processName) {
		BufferedReader bufferedReader = null;
		try {
			Process process = Runtime.getRuntime().exec("taskkill /F /IM " + processName);
			bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (Exception e2) {
				}
			}
		}
	}

	/*
	 * 拖拽元素
	 * 例如silder的拖动，横向拖动
	 * @input  坐标，输入两个数字，以逗号分隔，中英文逗号皆可  例如："-30,0"或者"0，100"
	 */
	public static ExecutionResult dragAndDrop(WebDriver driver, By selector, String input, InstructionOptions options) {
		return dragAndDrop(driver, selector, input, options, false);
	}

	public static ExecutionResult dragAndDrop(WebDriver driver, By selector, String input, InstructionOptions options, 
			boolean tryMode) {
		if (input == null) {
			return SGLogger.error(
					"      The element data is NULL point, please check if the element contains the corresponding data column.");
		}		
		ActionCommon.waitForPageLoaded(driver);
		ActionCommon.checkAlert(driver);
		WebElement element = null;
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(selector));
			element = ActionCommon.retryFindElement(driver, selector);
			if (element != null) {
				String text = ActionCommon.retryGetText(driver, selector);
				if (text.equals("ERROR")) {
					return SGLogger.codeError("      Get Text unsuccessfully.");
				}
				elementText = text;
				String[] sourceStr = input.split(",|，");
				int[] getxy = new int[sourceStr.length];
				for(int i=0;i<sourceStr.length;i++){
					getxy[i]=Integer.parseInt(sourceStr[i]);
				}				
				int getX = getxy[0] ;
				int getY = getxy[1] ;				
				Actions builder = new Actions(driver);
				builder.moveToElement(element).click().dragAndDropBy(element, getX, getY).build().perform();
				if (ActionCommon.checkErrorMessage(driver)) {
					return new ExecutionResult(true, "拖拽操作成功");
				} else {
					return new ExecutionResult("拖拽操作失败");
				}
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
			return SGLogger.elementTimeOut("      Try to dragAndDrop none existing WebElement.");
		} catch (Exception e) {
			return SGLogger.codeError("      [AC_CLIK_00] Exception:[" + e.getClass().getName() + "]");
		}
		return new ExecutionResult("拖拽操作失败(未执行)");
	}				

	// 将鼠标移动到元素上
	public static ExecutionResult moveToElement(WebDriver driver, By selector, String input, InstructionOptions options) {
		return moveToElement(driver, selector, input, options, false);
	}

	public static ExecutionResult moveToElement(WebDriver driver, By selector, String action, InstructionOptions options, 
			boolean tryMode) {
		if (action == null) {
			return SGLogger.error(
					"      The element data is NULL point, please check if the element contains the corresponding data column.");
		}
		ActionCommon.waitForPageLoaded(driver);
		ActionCommon.checkAlert(driver);
		WebElement element = null;
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(selector));
			element = ActionCommon.retryFindElement(driver, selector);
			if (element != null) {
				String text = ActionCommon.retryGetText(driver, selector);
				if (text.equals("ERROR")) {
					return SGLogger.codeError("      Get Text unsuccessfully.");
				}
				elementText = text;
				Actions actions = new Actions(driver);
				actions.moveToElement(element).perform();

				if (ActionCommon.checkErrorMessage(driver)) {
					return new ExecutionResult(true, "移动到元素成功");
				} else {
					return new ExecutionResult("移动到元素失败");
				}
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
			return SGLogger.elementTimeOut("      Try to moveToElement none existing WebElement.");
		} catch (Exception e) {
			return SGLogger.codeError("      [AC_CLIK_00] Exception:[" + e.getClass().getName() + "]");
		}
		return new ExecutionResult("移动到元素失败(未执行)");
	}

	// ------------------------
	/**
	 * 文件下载/文件导出
	 *
	 * @author yanfang.chen
	 * @param driver
	 * @param selector
	 * @param input
	 *            设置文件的下载路径，页面上的文件名是默认的或者随机的，故此处给出文件夹路径即可；如：d:\\
	 * @return
	 */
	public static ExecutionResult fileDown(WebDriver driver, By selector, String input, InstructionOptions options) {
		return ActionCommon.fileDown(driver, selector, input, options, false);
	}

	private static ExecutionResult fileDown(WebDriver driver, By selector, String text, InstructionOptions options,
			boolean tryMode) {
		String urlStr = driver.getCurrentUrl();
		/*
		 * if (text == null) { SGLogger.error(
		 * "      the input data is NULL, please check if the excel file contains the corresponding data column."
		 * ); return false; }
		 */
		if (!ContextConstant.matchIgnored(text)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement fileDownElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					fileDownElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]");
				}
				java.util.Set<Cookie> cookie = driver.manage().getCookies();
				Iterator<Cookie> iterator = cookie.iterator();
				while (iterator.hasNext()) {
					Cookie cookie2 = iterator.next();
					System.out.println("Cookie Name:" + cookie2.getName() + "-----" + "cookie Domain:"
							+ cookie2.getDomain() + "----" + "cookie value:" + cookie2.getValue());
				}

				FirefoxProfile profile = new FirefoxProfile();
				int index = text.lastIndexOf("\\");
				String filename = text.substring(index + 1);
				String path = text;// 下载文件的存放路径，文件夹
				File file = new File(path);
				if (file.exists()) {
					file.delete();
				}
				profile.setPreference("browser.download.dir", text);// 配置响应参数：下载路径
				profile.setPreference("browser.download.folderList", 2);// 2为指定路径，0为默认路径
				profile.setPreference("browser.download.manager.showWhenStarting", false);// 是否显示开始
				// 禁止弹出保存框，value是文件格式
				profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
						"application/zip,application/msword,text/plain,application/vnd.ms-excel,text/csv,text/comma-separated-values,application/octet-stream,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.openxmlformats-officedocument.wordprocessingml.document");
				driver = new FirefoxDriver(profile);

				driver.get(urlStr);
				fileDownElement.click();
				waitTime(3000);
				Actions action = new Actions(driver);
				action.sendKeys(Keys.ENTER).perform();
				waitTime(3000);
				ActionCommon.checkAlert(driver);
				logElementText(fileDownElement);

				// 对文件下载的验证，若本地存在，则下载成功，若本地不存在，则下载失败
				String js_exist = "alert(\"download successfully\")";
				String js_not_exist = "alert(\"download unsuccessfully\")";
				if (file.exists()) {
					return SGLogger.verifySuccessER("[" + filename + "]" + js_exist);
				} else {
					return SGLogger.verifyErrorER("[" + filename + "]" + js_not_exist);
				}

				/*
				 * if (options.checkOption(ContextConstant.NO_VERIFY)) { return
				 * true; } else { return ActionCommon.verifyTextValue(driver,
				 * selector, false, text, false); }
				 */
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.elementTimeOut("     fileDown Text :[" + text + "]");
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					return SGLogger.verifyErrorER("      fileDown box not found on page.");
				}
			}
		}
		return new ExecutionResult(true, "文件下载校验成功(未执行)");
	}

	public static void waitTime(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @author yanfang.chen MD5文件比对
	 * @param filePath
	 *            文件的路径
	 * @return 返回字符串，最终通过比较字符串是否相同来确认文件是否一样
	 * @throws Exception
	 */
	public String getFileMD5(String filePath) throws Exception {
		File file = new File(filePath);
		InputStream in = new FileInputStream(file);
		MessageDigest digest = MessageDigest.getInstance("MD5");
		byte buffer[] = new byte[1024];
		int length;
		while ((length = in.read(buffer)) != -1) {
			digest.update(buffer, 0, length);
		}
		in.close();
		BigInteger bigInteger = new BigInteger(1, digest.digest());
		return bigInteger.toString(16);
	}

	/**
	 * @author Andrew.Chen 对可计数的元素进行xpath方式的计数
	 * @param driver
	 * @param selector
	 * @param input
	 * @param options
	 * @return
	 */
	public static ExecutionResult getItemsCount(WebDriver driver, By selector, String input, InstructionOptions options) {
		return getItemsCount(driver, selector, input, options, false);
	}

	private static ExecutionResult getItemsCount(WebDriver driver, By selector, String input, InstructionOptions options,
			boolean tryMode) {
		ActionCommon.waitForPageLoaded(driver);
		List<WebElement> items = retryFindElements(driver, selector);
		try {

			Integer expectedCount = getFirstIntInString(input);
			if (null == expectedCount || expectedCount < 0) {
				return SGLogger.error("      Do Element Count Error : Expected [" + input
						+ "] is not valid : And actual count [" + items.size() + "]");
			}
			if (items.size() == expectedCount) {
				return SGLogger.verifySuccessER("      Do Element Count Successful  : Expected [" + expectedCount
						+ "] : Actual [" + items.size() + "]");
			} else {
				return SGLogger.error("      Do Element Count Error : Expected [" + expectedCount + "] : Actual ["
						+ items.size() + "]");
			}
		} catch (NumberFormatException e) {
			return SGLogger.codeError("      [Do Element Count] Exception:[" + e.getClass().getName() + "]");
		}
	}

	/**
	 * @author yanfang.chen 此方法主要测试页面上是否存在某字符串
	 *         编写时的使用方式【type：textbox；因为这类型的情况不多，类型暂时用的是textbox，后续如果有必要可以加上一个类型用于处理此类型的测试；
	 *         input：要判断的页面上的字符串； action：indText】
	 * @param driver
	 * @param selector
	 * @param input
	 * @return
	 */
	public static boolean textExit(WebDriver driver, By selector, String input, InstructionOptions options) {
		return ActionCommon.textExit(driver, input, selector, options, false);
	}

	private static boolean textExit(WebDriver driver, String input, By selector, InstructionOptions options,
			boolean tryMode) {
		boolean status = false;
		if (input == null) {
			SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
			status = false;
		}
		if (!ContextConstant.matchIgnored(input)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);

			try {
				WebElement textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					status = SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]").bOK();
				}
				elementText = textboxElement.getText();
				status = elementText.contains(input);

			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					SGLogger.elementTimeOut("     modify Text :[" + input + "]");
				}
				status = false;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					SGLogger.verifyError("      Text box not found on page.");
				}
				status = false;
			}
		}
		return status;
	}

	public static ExecutionResult enterText(WebDriver driver, By selector, String input, InstructionOptions options) {
		return ActionCommon.enterText(driver, selector, input, options, false);
	}

	private static ExecutionResult enterText(WebDriver driver, By selector, String input, InstructionOptions options,
			boolean tryMode) {
		if (null == input) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(input)) {
			ActionCommon.checkAlert(driver);
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				try {
					textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.presenceOfElementLocated(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_TEXT_01] Exception:[" + e.getClass().getName() + "]");
				}

				textboxElement.sendKeys(Keys.CONTROL + "a");
				textboxElement.sendKeys(Keys.DELETE);
				textboxElement.sendKeys(Keys.HOME, input, Keys.TAB);
				ActionCommon.checkAlert(driver);
				logElementText(textboxElement);
				if (options.existOption(ContextConstant.INSTRUCTION_NO_VERIFY)) {
					return noVerifyER;
				} else {
					return ActionCommon.verifyTextValue(driver, selector, input, false, false);
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.elementTimeOut("     Enter Text :[" + input + "]");
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_TEXT_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					return SGLogger.verifyErrorER("      Text box not found on page.");
				}
			}
		}
		return new ExecutionResult(true, "输入文本成功(未执行)");
	}

	public static boolean enterTextTwoTab(WebDriver driver, By selector, String input) {
		if (input == null) {
			SGLogger.error(
					"      The input data is NULL, please check if the excel file contains the corresponding data column.");
			return false;
		}
		if (!ContextConstant.matchIgnored(input)) {
			ActionCommon.waitForPageLoaded(driver);
			boolean endTry = false;
			try {
				WebElement textboxElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				String originValue = textboxElement.getAttribute("value");
				System.out.println("Id :  [" + textboxElement.getAttribute("id") + "]");
				System.out.println("Enabled :  [" + textboxElement.isEnabled() + "]");
				System.out.println("Selectable :  [" + textboxElement.isSelected() + "]");
				System.out.println("Displayed :  [" + textboxElement.isDisplayed() + "]");
				if (ActionCommon.sepecialTextBoxNeedHomeKey(originValue)) {
					textboxElement = driver.findElement(selector);
					textboxElement.sendKeys(Keys.HOME, input, Keys.TAB, Keys.TAB);
				} else if (ActionCommon.sepecialTextBoxNeedDeleteOrginalValue(originValue)) {
					textboxElement = driver.findElement(selector);
					textboxElement.sendKeys(Keys.CONTROL + "a");
					textboxElement.sendKeys(Keys.DELETE);
					textboxElement.sendKeys(Keys.HOME, input, Keys.TAB, Keys.TAB);
				} else {
					textboxElement = driver.findElement(selector);
					textboxElement.sendKeys(input, Keys.TAB, Keys.TAB);
				}
				ActionCommon.waitForPageLoaded(driver);
				(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				textboxElement = driver.findElement(selector);
				String actualValue = textboxElement.getAttribute("value");
				trySaveElementText(actualValue);
				if (!checkExpectedAndActualMatch(input, actualValue)) {
					SGLogger.wrongValue(
							"     Wrong value is set. Expected [" + input + "] : Actual [" + actualValue + "]");
					return false;
				} else {
					SGLogger.actionComplete(
							"     Value is set. Expected [" + input + "] : Actual [" + actualValue + "]");
					return true;
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (endTry) {
					SGLogger.elementTimeOut("     Enter Text :[" + input + "]");
					return false;
				}
			}
		}
		return true;
	}

	public static ExecutionResult selectDropdown(WebDriver driver, By selector, String selection, InstructionOptions options) {
		return ActionCommon.selectDropdown(driver, selector, selection, options, false);
	}

	private static ExecutionResult selectDropdown(WebDriver driver, By selector, String selection, InstructionOptions options,
			boolean tryMode) {
		if (selection == null) {
			return SGLogger.error(
					"      The input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		WebElement dropdownElement = null;
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		if (!ContextConstant.matchIgnored(selection)
				&& !com.meowlomo.ci.ems.bundle.utils.ContextConstant.LOCATION_LIST.contains(selection)) {
			ActionCommon.waitForPageLoaded(driver);
			ActionCommon.checkAlert(driver);
			try {
				wait.until(ExpectedConditions.presenceOfElementLocated(selector));
				dropdownElement = ActionCommon.retryFindElement(driver, selector);
				ActionCommon.checkAlert(driver);
				ActionCommon.waitForPageLoaded(driver);
				ActionCommon.waitDropdownToPopolulate(driver, selector);
				// try to select first
				try {
					dropdownElement = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
					// check drop down disabled before select
					String disabled = dropdownElement.getAttribute("disabled");
					if (disabled != null) {
						String currentValue = new Select(dropdownElement).getFirstSelectedOption().getText();
						trySaveElementText(currentValue);
						ExecutionResult er = SGLogger.error("      The Dropdown is disabled. The current value is [" + currentValue + "]");
						SGLogger.info(
								"      If the current value matched the value you want to set. Please put [!IGNORE!] as your input.");
						return er;
					}
					Select select = new Select(dropdownElement);
					select.selectByVisibleText(selection);
					ActionCommon.checkAlert(driver);
			
					if (options.existOption(ContextConstant.INSTRUCTION_NO_VERIFY)) {
						return noVerifyER;
					} else {
						return ActionCommon.verifyDropdownValue(driver, selector, selection, false, false);
					}
					// if the
				} catch (org.openqa.selenium.NoSuchElementException e) {
					e.printStackTrace();
					if (!ContextConstant.inTryMode()) {
						// get all the options from the dropdown
						try {
							List<WebElement> optionElements = dropdownElement.findElements(By.tagName("option"));
							HashSet<String> optionsSet = new HashSet<String>();
							for (int optionElementCount = 0; optionElementCount < optionElements
									.size(); optionElementCount++) {
								String option = optionElements.get(optionElementCount).getText();
								trySaveElementText(option);
								optionsSet.add(option);
								if (option.equals(selection)) {
									break;
								}
							}
							if (optionsSet.contains(selection)) {// options is
																	// in the
																	// list
								// wait the drop down to populate
								dropdownElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
										.until(ExpectedConditions.presenceOfElementLocated(selector));
								// dropdownElement =
								// driver.findElement(By.id(dropdownID));
								Select select = new Select(dropdownElement);
								select.selectByVisibleText(selection);
								// after select wait the page to be loaded
								ActionCommon.waitForPageLoaded(driver);
								ActionCommon.waitDropdownToPopolulate(driver, selector);
								try {
									dropdownElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
											.until(ExpectedConditions.elementToBeClickable(selector));
								} catch (Exception e1) {
									return SGLogger.codeError(
											"      [AC_DROP_03] Exception:[" + e1.getClass().getName() + "]");
								}
								String selectedOption = new Select(driver.findElement(selector))
										.getFirstSelectedOption().getText();
								if (!selectedOption.equals(selection)) {
									return SGLogger.wrongValue("      Select dropdown : [" + selection + "]. Expected ["
											+ selection + "] : Actual [" + selectedOption + "]");
								} else {
									return SGLogger.actionComplete("      Select dropdown : [" + selection + "]. Expected ["
											+ selection + "] : Actual [" + selectedOption + "]");
								}
							} else {
								// loop through the set to see if an option
								// contains the input
								boolean foundContain = false;
								String correctedSelection = "";
								for (String option : optionsSet) {
									if (option.contains(selection)) {
										foundContain = true;
										correctedSelection = option;
									}
								}
								// check result
								if (foundContain) {
									return ActionCommon.selectDropdown(driver, selector, correctedSelection, options);
								} else {
									ExecutionResult er = SGLogger.notValidSelection(
											"      Selection [" + selection + "] is not valid. Option doesn't exsit.");
									SGLogger.info("      Valid options are [" + optionsSet + "]");
									return er;
								}
							}
						} catch (TimeoutException k) {
							k.printStackTrace();
							return SGLogger.elementTimeOut("      Select dropdown : [" + selection + "]");
						}
					} else {
						return new ExecutionResult("下拉选择失败(未执行)");
					}
				} catch (Exception e) {
					e.printStackTrace();
					return SGLogger.codeError("      [AC_DROP_04] Exception:[" + e.getClass().getName() + "]");
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					SGLogger.elementTimeOut("      Select dropdown : [" + selection + "]");
				}
				return timeoutER;
			} catch (Exception e) {
				if (!ContextConstant.inTryMode()) {
					return SGLogger.codeError("      [AC_DROP_00] Exception:[" + e.getClass().getName() + "]");
				} else {
					return SGLogger.verifyErrorER("      error trying to found element on page.");
				}
			}
		} else if (!ContextConstant.matchIgnored(selection)
				&& com.meowlomo.ci.ems.bundle.utils.ContextConstant.LOCATION_LIST.contains(selection)) {
			WebElement selectList = driver.findElement(selector);
			List<WebElement> optionList = selectList.findElements(By.tagName("option"));
			for (WebElement elem : optionList) {
				if (elem.getText().contains(
						com.meowlomo.ci.ems.bundle.utils.ContextConstant.LOCATION_MAPPING.get(selection))) {
					elem.click();
					SGLogger.actionComplete("      Select dropdown : [" + selection + "].");

					WebElement element = null;
					ActionCommon.waitForElementPresent(driver, selector);
					try {
						element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
								.until(ExpectedConditions.elementToBeClickable(selector));
					} catch (Exception e1) {
						return SGLogger.codeError("      [AC_DROP_05] Exception:[" + e1.getClass().getName() + "]");
					}
					Select selectAfterSelect = new Select(element);
					WebElement option = selectAfterSelect.getFirstSelectedOption();
					String actualValue = option.getText();
					trySaveElementText(actualValue);
					if (!actualValue.contains(
							com.meowlomo.ci.ems.bundle.utils.ContextConstant.LOCATION_MAPPING.get(selection))) {
						return SGLogger.wrongValue("      Select Dropdown : [" + selection + "]. Expected [" + selection
								+ "] : Actual [" + actualValue + "]");
					} else {
						return SGLogger.actionComplete("      Select dropdown : [" + selection + "]. Expected [" + selection
								+ "] : Actual [" + actualValue + "]");
					}

				}
			}
			return SGLogger.wrongValue("      Select dropdown : [" + selection + "]. Expected ("
					+ com.meowlomo.ci.ems.bundle.utils.ContextConstant.LOCATION_MAPPING.get(selection) + ")");
		} else {
			return new ExecutionResult(true, "下拉选择成功(未执行)");
		}
	}

	private static ExecutionResult verifyDropdownValue(WebDriver driver, By selector, String expectedValue, boolean verifyMode,
			boolean tryMode) {
		System.out.println("Dropdown verify selector info [" + selector.toString() + "]");
		if (expectedValue == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (ContextConstant.matchIgnored(expectedValue)) {
			return inputIgnoreER;
		} else {
			WebElement dropdownElement = null;
			ActionCommon.waitForPageLoaded(driver);
			try {
				try {
					dropdownElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.elementToBeClickable(selector));
				} catch (Exception e) {
					dropdownElement = ActionCommon.retryFindElement(driver, selector);
				}
				if (dropdownElement != null) {
					String selectedOption = new Select(dropdownElement).getFirstSelectedOption().getText();
					trySaveElementText(selectedOption);
					if (selectedOption.equals(expectedValue)) {
						if (verifyMode) {
							return SGLogger.verifySuccessER("      Verify dropdown : Expected [" + expectedValue + "] : Actual ["
									+ selectedOption + "]");
						} else {
							return SGLogger.actionComplete("      Select dropdown : [" + expectedValue + "]. Expected ["
									+ expectedValue + "] : Actual [" + selectedOption + "]");
						}
					} else {
						if (verifyMode) {
							return SGLogger.verifyErrorER("      Verify dropdown : Expected [" + expectedValue + "] : Actual ["
									+ selectedOption + "]");
						} else {
							return SGLogger.wrongValue("      Select dropdown : [" + expectedValue + "]. Expected ["
									+ expectedValue + "] : Actual [" + selectedOption + "]");
						}
					}
				} else {
					if (!ContextConstant.inTryMode()) {
						return SGLogger.error("      Couldn't not find the dropdown on the page.");
					}
					return new ExecutionResult("未能选取到下拉选项");
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					if (verifyMode) {
						return SGLogger.verifyErrorER("      Dropdown element timeout wiht excepted : [" + expectedValue + "]");
					} else {
						return SGLogger.elementTimeOut("      Dropdown : [" + expectedValue + "]");
					}
				}
				return timeoutER;
			} catch (Exception e) {
				e.printStackTrace();
				if (!ContextConstant.inTryMode()) {
					if (verifyMode) {
						return SGLogger.verifyErrorER("      Error on finding dropdown on the page. Exception:["
								+ e.getClass().getName() + "]");
					} else {
						return SGLogger.codeError("      [AC_DROPDOWN_VERIFY_00] Exception:[" + e.getClass().getName() + "]");
					}
				}
				return new ExecutionResult("校验下拉值时有异常发生:" + e.getClass().getName());
			}
		}
	}
//
//	private static boolean selectCardTab(WebDriver driver, By selector, String selection) {
//		if (selection == null) {
//			SGLogger.error(
//					"      The input data is NULL, please check if the excel file contains the corresponding data column.");
//			return false;
//		}
//		if (!ContextConstant.inputIgnored(selection)) {
//			ActionCommon.waitForPageLoaded(driver);
//			try {
//				(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//						.until(ExpectedConditions.elementToBeClickable(selector));
//				List<WebElement> cardTabElements = ActionCommon.retryFindElements(driver, selector);
//				// loop through elements to check text match
//				String targetCardTabID = "";
//				boolean targetFound = false;
//				HashSet<String> selections = new HashSet<String>();
//				if (cardTabElements.size() != 0) {
//					for (int elementCount = 0; elementCount < cardTabElements.size(); elementCount++) {
//						WebElement cardTabElement = cardTabElements.get(elementCount);
//						String cardTabElementText = cardTabElement.getText();
//						if (!cardTabElementText.isEmpty()) {
//							if (!elementText.isEmpty())
//								elementText = cardTabElementText;
//							selections.add(cardTabElementText);
//						}
//						if (cardTabElementText.equals(selection)) {
//							targetCardTabID = cardTabElement.getAttribute("id");
//							targetFound = true;
//							break;
//						}
//					}
//				} else {
//					SGLogger.error("      No card tab cant be found with the CssSelector given, Please check the ID");
//					return false;
//				}
//				// check the result
//				if (targetFound) {
//					WebElement cardTebElement = null;
//					try {
//						cardTebElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//								.until(ExpectedConditions.elementToBeClickable(By.id(targetCardTabID)));
//					} catch (Exception e) {
//						SGLogger.codeError("      [AC_CARD_01] Exception:[" + e.getClass().getName() + "]");
//						return false;
//					}
//					cardTebElement.click();
//					ActionCommon.waitForPageLoaded(driver);
//					try {
//						cardTebElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//								.until(ExpectedConditions.elementToBeClickable(By.id(targetCardTabID)));
//					} catch (Exception e) {
//						SGLogger.codeError("      [AC_CARD_02] Exception:[" + e.getClass().getName() + "]");
//						return false;
//					}
//					// check the card tab is selected
//					WebElement targetParent = ActionCommon.retryFindElement(cardTebElement, By.xpath(".."));
//					String cardTabparentElementClass = targetParent.getAttribute("class");
//					if (cardTabparentElementClass.contains("cardTab_on")) {
//						SGLogger.actionComplete("      Select card tab [" + selection + "]");
//						return true;
//					} else {
//						// find the selected card tab
//						String selectedCardTeb = "";
//						cardTabElements = ActionCommon.retryFindElements(driver, selector);
//						for (int elementCount = 0; elementCount < cardTabElements.size(); elementCount++) {
//							WebElement cardTabElement = cardTabElements.get(elementCount);
//							String cardTabElementText = cardTabElement.getAttribute("class");
//							if (cardTabElementText.contains("cardTab_on")) {
//								selectedCardTeb = cardTabElement.getText();
//								break;
//							}
//							selections.add(cardTabElementText);
//						}
//						SGLogger.wrongValue("      Card tab [" + selection + "] is not selected. [" + selectedCardTeb
//								+ "] is selected");
//						return false;
//					}
//				} else {
//					SGLogger.notValidSelection("      Couldn't find card tab [" + selection
//							+ "], valid selections are [" + selections + "]");
//					return false;
//				}
//			} catch (TimeoutException e) {
//				e.printStackTrace();
//				SGLogger.elementTimeOut("      Select radio button [" + selection + "]");
//				return false;
//			} catch (Exception e) {
//				e.printStackTrace();
//				SGLogger.codeError("      [AC_CARD_00] Exception:[" + e.getClass().getName() + "]");
//				return false;
//			}
//		} else {
//			return true;
//		}
//	}

	public static ExecutionResult selectRadio(WebDriver driver, By selector, String selection, InstructionOptions options) {
		if (selection == null) {
			return SGLogger.error(
					"      The input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(selection)) {
			ActionCommon.waitForPageLoaded(driver);
			ActionCommon.checkAlert(driver);
			try {
				(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.elementToBeClickable(selector));
				List<WebElement> raidoElements = driver.findElements(selector);
				// loop through elements to check text match
				WebElement targetRadioElement = null;
				boolean targetFound = false;
				HashSet<String> selections = new HashSet<String>();
				if (raidoElements.size() != 0) {
					for (int elementCount = 0; elementCount < raidoElements.size(); elementCount++) {
						WebElement raidoElement = raidoElements.get(elementCount);
						String raidoElementText = raidoElement.getText();
						if (!elementText.isEmpty())
							elementText = raidoElementText;
						if (raidoElementText.equals(selection)) {
							targetRadioElement = raidoElements.get(elementCount);
							targetFound = true;
							SGLogger.info("Found radio button [" + selection + "]");
							break;
						}
						selections.add(raidoElementText);
					}
				} else {
					return SGLogger.error(
							"      No radio button cant be found with the CssSelector given, Please check the ID");
				}
				if (targetFound) {
					targetRadioElement.click();
					if (options.existOption(ContextConstant.INSTRUCTION_NO_VERIFY)) {
						return noVerifyER;
					} else {
						return ActionCommon.verifyRadioValue(driver, selector, false, selection, false);
					}
				} else {
					return SGLogger.notValidSelection(
							"      Select radio button [" + selection + "], valid selections are [" + selections + "]");
				}
			} catch (TimeoutException e) {
				e.printStackTrace();
				return SGLogger.elementTimeOut("      Select radio button [" + selection + "]");
			} catch (Exception e) {
				e.printStackTrace();
				return SGLogger.codeError("      [AC_RADO_00] Exception:[" + e.getClass().getName() + "]");
			}
		} else {
			return inputIgnoreER;
		}
	}

	private static ExecutionResult verifyRadioValue(WebDriver driver, By selector, boolean verifyMode, String expectedValue,
			boolean tryMode) {
		if (expectedValue == null) {
			return SGLogger.error(
					"      the input data is NULL, please check if the excel file contains the corresponding data column.");
		}
		if (ContextConstant.matchIgnored(expectedValue)) {
			return inputIgnoreER;
		} else {
			ActionCommon.waitForPageLoaded(driver);
			// find the radio button first
			WebElement radioElement = null;
			if (verifyMode) {
				try {
					(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.elementToBeClickable(selector));
					WebElement upperElement = driver.findElement(selector);
					List<WebElement> raidoElements = upperElement.findElements(By.tagName("input"));
					// loop through elements to check text match
					WebElement targetRadioElement = null;
					HashSet<String> selections = new HashSet<String>();
					if (raidoElements.size() != 0) {
						for (int elementCount = 0; elementCount < raidoElements.size(); elementCount++) {
							WebElement raidoElement = raidoElements.get(elementCount);
							String raidoElementText = raidoElement.getText();
							if (!elementText.isEmpty())
								elementText = raidoElementText;
							if (raidoElementText.equals(expectedValue)) {
								targetRadioElement = raidoElements.get(elementCount);
								break;
							}
							selections.add(raidoElementText);
						}
					} else {
						return SGLogger.error(
								"      No radio button cant be found with the CssSelector given, Please check the ID");
					}
					ActionCommon.waitForPageLoaded(driver);
					if (!targetRadioElement.isSelected()) {
						// check sub element
						List<WebElement> subElements = targetRadioElement.findElements(By.tagName("input"));
						for (int i = 0; i < subElements.size(); i++) {
							if (subElements.get(i).isSelected()) {
								return SGLogger.verifySuccessER("      Radio button [" + expectedValue + "] is selected");
							}
						}
						return SGLogger.verifyErrorER("      Radio button " + expectedValue
								+ " is not selected. Expected [selected]: Actual [unselected]");
					} else {
						return SGLogger.verifySuccessER("      Radio button [" + expectedValue + "] is selected");
					}
				} catch (TimeoutException e) {
					e.printStackTrace();
					if (!ContextConstant.inTryMode()) {
						SGLogger.verifyError("      Link element timeout wiht excepted : [" + expectedValue + "]");
					}
					return timeoutER;
				} catch (Exception e) {
					if (!ContextConstant.inTryMode()) {
						SGLogger.verifyError("      [AC_RADIO_VERIFY_00] Exception:[" + e.getClass().getName() + "]");
					}
					return new ExecutionResult("校验Radio值时有异常发生:" + e.getClass().getName());
				}
			} else {
				ActionCommon.waitForPageLoaded(driver);
				try {
					radioElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
							.until(ExpectedConditions.elementToBeClickable(selector));
				} catch (Exception e) {
					return SGLogger.codeError("      [AC_RADO_02] Exception:[" + e.getClass().getName() + "]");
				}
				// radioElement = driver.findElement(By.id(targetRadioID));
				if (!radioElement.isSelected()) {
					return SGLogger.wrongValue("      Radio button " + expectedValue
							+ " is not selected. Expected [selected]: Actual [unselected]");
				} else {
					return SGLogger.actionComplete("      Select radio button [" + expectedValue + "]");
				}
			}
		}
	}
//
//	private static String getTextBoxValue(WebDriver driver, By selector) {
//		try {
//			WebElement element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//					.until(ExpectedConditions.presenceOfElementLocated(selector));
//			if (element != null) {
//				String value = element.getAttribute("value");
//				trySaveElementText(value);
//				return value;
//			} else {
//				return null;
//			}
//		} catch (TimeoutException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}
//
//	private static String getCheckValue(WebDriver driver, By selector) {
//		try {
//			WebElement element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//					.until(ExpectedConditions.presenceOfElementLocated(selector));
//			if (element != null) {
//				trySaveElementText(element);
//				if (element.isSelected()) {
//					SGLogger.actionComplete("      The checkbox is checked");
//					return "checked";
//				} else {
//					SGLogger.actionComplete("      The checkbox is unchecked");
//					return "unchecked";
//				}
//			} else {
//				return null;
//			}
//		} catch (TimeoutException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

//	private static String getDropDownValue(WebDriver driver, By selector) {
//		try {
//			WebElement element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//					.until(ExpectedConditions.presenceOfElementLocated(selector));
//			if (element != null) {
//				Select selectAfterSelect = new Select(element);
//				WebElement option = selectAfterSelect.getFirstSelectedOption();
//				String selectedOption = option.getText();
//				trySaveElementText(selectedOption);
//				SGLogger.actionComplete("      The option [" + selectedOption + "] is selected in the dropdown list");
//				return selectedOption;
//			} else {
//				return null;
//			}
//		} catch (TimeoutException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

//	private static String getLinkValue(WebDriver driver, By selector) {
//		try {
//			WebElement element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//					.until(ExpectedConditions.presenceOfElementLocated(selector));
//			if (element != null) {
//				String value = element.getText();
//				trySaveElementText(value);
//				SGLogger.actionComplete("      The link is [" + value + "]");
//				return value;
//			} else {
//				SGLogger.error("      Can not get the text from the link");
//				return null;
//			}
//		} catch (TimeoutException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

//	private static String getTextValue(WebDriver driver, By selector) {
//		try {
//			WebElement element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//					.until(ExpectedConditions.presenceOfElementLocated(selector));
//			if (element != null) {
//				String value = element.getText();
//				trySaveElementText(value);
//				SGLogger.actionComplete("      Got text [" + value + "]");
//				return value;
//			} else {
//				SGLogger.error("      Can not get the text");
//				return null;
//			}
//		} catch (TimeoutException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

//	private static String getButtonValue(WebDriver driver, By selector) {
//		try {
//			WebElement element = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//					.until(ExpectedConditions.presenceOfElementLocated(selector));
//			if (element != null) {
//				String value = element.getText();
//				trySaveElementText(value);
//				SGLogger.actionComplete("      Button's name is [" + value + "]");
//				return value;
//			} else {
//				return null;
//			}
//		} catch (TimeoutException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

//	private static String getRaidoValue(WebDriver driver, By selector) {
//		try {
//			(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX))
//					.until(ExpectedConditions.presenceOfElementLocated(selector));
//			List<WebElement> elements = driver.findElements(selector);
//			if (!elements.isEmpty()) {
//				for (int i = 0; i < elements.size(); i++) {
//					WebElement ele = elements.get(i);
//					// get the value
//					String currentValue = ele.getAttribute("value");
//					if (currentValue != null) {
//						trySaveElementText(currentValue);
//						if (currentValue.equals("true")) {
//							String value = elements.get(i + 1).getText();
//							SGLogger.actionComplete("      Selected raido button name is [" + value + "]");
//							return value;
//						}
//					}
//				}
//			} else {
//				return null;
//			}
//			return null;
//		} catch (TimeoutException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

	public static void takeNormalScreenshotBegin(WebDriver driver, String fileName) {
		tail = "Begin";
		takeNormalScreenshot(driver, fileName + " begin");
	}

	public static void takeNormalScreenshotEnd(WebDriver driver, String fileName) {
		tail = "End";
		takeNormalScreenshot(driver, fileName + " end");
	}

	public static void takeNormalScreenshot(WebDriver driver, String FileName) {
		String Path = FileUtilConstant.LOG_FOLDER;
		try {
			ActionCommon.waitForPageLoaded(driver);
			// like 0003
			String screenShootNumber = String.format("%1$04d", FileUtilConstant.screenShootIndex);
			File outputFile = new File(Path + screenShootNumber + FileName + "_" + System.currentTimeMillis() + ".jpg");
			File srcFile = new File("tmp.jpg");
			try {
				// 全屏截图，通过滚动条的滚动来截取
				Screenshot screenshot = new AShot().shootingStrategy(ShootingStrategies.viewportPasting(100))
						.takeScreenshot(driver);
				ImageIO.write(screenshot.getImage(), "jpg", srcFile);
				FileUtils.copyFile(srcFile, outputFile);
				System.err.println("takeNormalScreenshot srcFile:" + srcFile.getAbsolutePath());
				System.err.println("takeNormalScreenshot outputFile:" + outputFile.getAbsolutePath());
				writeToFileServer(screenShootNumber, outputFile, false);
				// TODO 截图完成之后，将页面的滚动条回到顶部
				pageBackToTop(driver);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			FileUtilConstant.screenShootIndex++;
			FileUtilConstant.remoteScreenShootIndex++;
		} catch (HeadlessException e) {
			e.printStackTrace();
		}
	}

	private static void pageBackToTop(WebDriver driver) {
		String setScroll = "document.documentElement.scrollTop=0";
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript(setScroll);
	}

	//TODO
	public static void writeToFileServerNew(String screenShootNumber, File srcFile, boolean bVideoFile)
			throws IOException {
		String fileName = bVideoFile ? srcFile.getName()
				: String.format("%04d%s%s.jpg", FileUtilConstant.remoteScreenShootIndex, FileUtilConstant.remoteScreenShootFileName, tail);

		System.err.println("[writeToFileServerNew] instructionRunId:" + ActionCommon.instructionResultId);
		System.err.println(fileService);
		System.err.println("[writeToFileServerNew] srcFile:" + srcFile);
		
		//TODO 不使用ActionCommon.instructionResultId,使用WebDriverImplNew.localState.instructionRunId
		if (ActionCommon.instructionResultId > 0 && null != fileService && fileService.inited() && null != srcFile) {

			// log/勿删-Andrew测试勿删除_/ ==> log/
			String remotePath = fileService.remotePath(bVideoFile);
			System.err.println("remotePath:" + remotePath);
			
			Path remotePathName = Paths.get(remotePath + fileName);
			byte[] picContent = Files.readAllBytes(srcFile.toPath());

			System.err.println("remotePathName:" + remotePathName.toString());
			fileService.create(remotePathName, picContent, false);
			byte[] content = fileService.exist(remotePathName) ? fileService.readFile(remotePathName) : null;

			// TODO 图片数据两处的长度无法保证完全一致,原因暂未查明
			if (null != content && null != picContent && (content.length == picContent.length
					|| Arrays.equals(picContent, content)
					|| (content.length > 0.8 * picContent.length && content.length < 1.1 * picContent.length))) {
				// back to atm
				// report it.
				JSONObject fileInfo = new JSONObject();
				fileInfo.put("name", fileName + ".jpg");
				fileInfo.put("uri", remotePathName.toString());
				if (bVideoFile) {
					fileInfo.put("type", "Video");
					fileInfo.put("runId", runId);
					System.err.println("runId:" + runId);
				} else {
					fileInfo.put("type", "Screenshot");
					fileInfo.put("instructionResultId", instructionResultId);
					System.err.println("instructionResultId:" + instructionResultId);
				}
				
				httpUtil.addStepFileLog(fileInfo.toString());
			} else
				System.out.println("Report file to atm failed .");
		} else {
			System.out.println("Report file to atm failed.");
		}
	}

	public static void writeToFileServer(String screenShootNumber, File srcFile, boolean bVideoFile)
			throws IOException {
		if (ContextConstant.refactor) {
			writeToFileServerNew(screenShootNumber, srcFile, bVideoFile);
			return;
		}
		
		String fileName = bVideoFile ? srcFile.getName()
				: String.format("%04d%s%s.jpg", FileUtilConstant.remoteScreenShootIndex, FileUtilConstant.remoteScreenShootFileName, tail);
		boolean bUseNewName = true;
		if (bUseNewName) {
			if (ActionCommon.instructionResultId > 0 && null != fileService && fileService.inited()
					&& null != srcFile) {

				// log/勿删-Andrew测试勿删除_/ ==> log/
				String remotePath = ActionCommon.fileServicePrefix + FileUtilConstant.REMOTE_TEST_CASE_RESULT_FOLDER;
				remotePath += bVideoFile ? "video/" : FileUtilConstant.REMOTE_INSTRUCTION_RESULT_FOLDER;

				Path remotePathName = Paths.get(remotePath + fileName);
				byte[] picContent = Files.readAllBytes(srcFile.toPath());

				fileService.create(remotePathName, picContent, false);
				byte[] content = fileService.exist(remotePathName) ? fileService.readFile(remotePathName) : null;

				// TODO 图片数据两处的长度无法保证完全一致,原因暂未查明
				if (null != content && null != picContent && (content.length == picContent.length
						|| Arrays.equals(picContent, content)
						|| (content.length > 0.8 * picContent.length && content.length < 1.1 * picContent.length))) {
					// back to atm
					// report it.
					if (null != ActionCommon.addStepFileLog && null != ActionCommon.httpUtil) {
						String url = ActionCommon.addStepFileLog.optString("url");
						String methodType = ActionCommon.addStepFileLog.optString("method");

						JSONObject fileInfo = new JSONObject();
						fileInfo.put("name", fileName + ".jpg");
						fileInfo.put("uri", remotePathName.toString());
						if (bVideoFile) {
							fileInfo.put("type", "Video");
							fileInfo.put("runId", ActionCommon.runId);
						} else {
							fileInfo.put("type", "Screenshot");
							fileInfo.put("instructionResultId", ActionCommon.instructionResultId);
						}
						JSONArray fileArray = new JSONArray();
						fileArray.put(fileInfo);

						ActionCommon.httpUtil.request(url, fileArray.toString(),
								MethodType.valueOf(methodType.toUpperCase()));
					} else
						System.out.println("Report file to atm failed.");
				} else {
					System.out.println("Report file to atm failed.");
				}
			}
		} else if (ActionCommon.instructionResultId > 0 && null != fileService && fileService.inited()
				&& null != srcFile) {
			String remotePath = FileUtilConstant.TEST_CASE_RESULT_FOLDER;
			if (remotePath.startsWith("/")) {
				remotePath = remotePath.substring(1);
			} else if (remotePath.startsWith("\\")) {
				remotePath = remotePath.substring(2);
			}

			Path path = Paths
					.get(remotePath + screenShootNumber + fileName + "_" + System.currentTimeMillis() + ".jpg");
			byte[] picContent = Files.readAllBytes(srcFile.toPath());

			fileService.create(path, picContent, false);
			byte[] content = fileService.exist(path) ? fileService.readFile(path) : null;

			if (null != content && null != picContent && (content.length == picContent.length
					|| Arrays.equals(picContent, content)
					|| (content.length > 0.8 * picContent.length && content.length < 1.1 * picContent.length))) {
				// back to atm
				// report it.
				if (null != ActionCommon.addStepFileLog && null != ActionCommon.httpUtil) {
					String url = ActionCommon.addStepFileLog.optString("url");
					String methodType = ActionCommon.addStepFileLog.optString("method");

					JSONObject file = new JSONObject();
					file.put("name", fileName + ".jpg");
					file.put("uri", path.toString());
					file.put("type", "Screenshot");
					file.put("instructionResultId", ActionCommon.instructionResultId);
					JSONArray fileArray = new JSONArray();
					fileArray.put(file);

					ActionCommon.httpUtil.request(url, fileArray.toString(),
							MethodType.valueOf(methodType.toUpperCase()));
				} else
					System.out.println("Report file to atm failed.");
			} else {
				System.out.println("Report file to atm failed.");
			}
		}
	}

	//TODO 无任何引用处,2019年任何时候看到请删除
//	private static void takeFulleScreenshot(WebDriver driver, String Path, String FileName) {
//		ActionCommon.waitForPageLoaded(driver);
//		int scrollbarThickness = 0;
//		JavascriptExecutor executor = (JavascriptExecutor) driver;
//		By eSelector = By.cssSelector("table[class=contentTable]");
//		Dimension PageSize = null;
//
//		try {
//			PageSize = driver.findElement(eSelector).getSize();
//			driver.switchTo().defaultContent();
//			//// System.out.println("Found table class = contentTable. Resizing
//			//// to W: "+PageSize.width+" H:"+PageSize.height);
//			executor.executeScript("window.frames['top_frame'].document.getElementById('mainTable').style.height = '"
//					+ (PageSize.height + scrollbarThickness) + "px';"
//					+ "window.frames['top_frame'].document.getElementById('mainTable').style.width = '"
//					+ (PageSize.width + scrollbarThickness) + "px';"
//					+ "window.frames['top_frame'].document.body.style.overflow = 'hidden';");
//
//			driver.switchTo().frame("top_frame");
//
//		} catch (NoSuchElementException e) {
//			// e.printStackTrace();
//		}
//		ActionCommon.waitForPageLoaded(driver);
//		File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
//		try {
//			String screeenShootNumber = "";
//			String format = "%1$04d";
//			screeenShootNumber = String.format(format, ActionCommon.screenShootIndex);
//			FileUtils.copyFile(scrFile, new File(Path + screeenShootNumber + FileName + ".png"));
//			screenShootIndex++;
//			remoteScreenShootIndex++;
//		} catch (IOException e) {
//			e.printStackTrace();
//			SGLogger.error("      ERROR ON CREATING SCREENSHOT FILES");
//		}
//
//		if (PageSize != null) {
//			driver.switchTo().defaultContent();
//
//			//// System.out.println("Reverting table size");
//			executor.executeScript("window.frames['top_frame'].document.getElementById('mainTable').style.height = '';"
//					+ "window.frames['top_frame'].document.getElementById('mainTable').style.width = '';"
//					+ "window.frames['top_frame'].document.body.style.overflow = '';");
//
//			driver.switchTo().frame("top_frame");
//		}
//		ActionCommon.waitForPageLoaded(driver);
//	}

	private static void waitForElementPresent(WebDriver driver, final By by) {
		WebDriverWait wait = (WebDriverWait) new WebDriverWait(driver, 5, ContextConstant.WEBELEMENT_WAIT_SLEEP)
				.ignoring(StaleElementReferenceException.class);
		try {
			wait.until(new ExpectedCondition<Boolean>() {
				@Override
				public Boolean apply(WebDriver webDriver) {
					WebElement element = webDriver.findElement(by);
					return element != null && element.isDisplayed() && element.isEnabled();
				}
			});
		} catch (WebDriverException e) {
			wait.until(new ExpectedCondition<Boolean>() {
				@Override
				public Boolean apply(WebDriver webDriver) {
					WebElement element = webDriver.findElement(by);
					return element != null && element.isEnabled();
				}
			});
		}
	}

	private static boolean checkAlert(WebDriver driver) {
		String alertText = "";
		try {
			ActionCommon.waitForPageLoaded(driver);
			Alert alert = driver.switchTo().alert();
			takeNormalScreenshot(driver, FileUtilConstant.tailIndexName(tail = "Pop Up Begin"));

			alertText = alert.getText();
			SGLogger.alert("      Got alert pop up windows from the browser with content [" + alertText + "]");
			alert.accept();
			ActionCommon.waitForPageLoaded(driver);

			takeNormalScreenshot(driver, FileUtilConstant.tailIndexName(tail = "Pop Up End"));
			return true;
		} catch (org.openqa.selenium.NoAlertPresentException ex) {

		} catch (Exception ex) {
			ex.printStackTrace();
			// swallow deliberately
		}
		return false;
	}

	private static boolean checkAlertNO(WebDriver driver) {
		String alertText = "";
		try {
			ActionCommon.waitForPageLoaded(driver);
			Alert alert = driver.switchTo().alert();
			takeNormalScreenshot(driver, FileUtilConstant.tailIndexName(tail = "Pop Up Begin"));

			alertText = alert.getText();
			SGLogger.alert("      Got alert pop up windows from the browser with content [" + alertText + "]");
			alert.dismiss();
			ActionCommon.waitForPageLoaded(driver);

			takeNormalScreenshot(driver, FileUtilConstant.tailIndexName(tail = "Pop Up End"));
			return true;
		} catch (org.openqa.selenium.NoAlertPresentException ex) {

		} catch (Exception ex) {
			ex.printStackTrace();
			// swallow deliberately
		}
		return false;
	}

	private static String checkAlertGetInfo(WebDriver driver) {
		String alertText = "";
		try {
			ActionCommon.waitForPageLoaded(driver);
			Alert alert = driver.switchTo().alert();
			takeNormalScreenshot(driver, FileUtilConstant.tailIndexName(tail = "Pop Up Begin"));

			alertText = alert.getText();
			SGLogger.alert("      Got alert pop up windows from the browser with content [" + alertText + "]");
			alert.accept();

			ActionCommon.waitForPageLoaded(driver);
			takeNormalScreenshot(driver, FileUtilConstant.tailIndexName(tail = "Pop Up End"));
			return alertText;
		} catch (org.openqa.selenium.NoAlertPresentException ex) {

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return alertText;

	}

	private static boolean checkErrorMessage(WebDriver driver) {
		ActionCommon.waitForPageLoaded(driver);
		// check error message pop up
		List<WebElement> messages = driver.findElements(By.className("message"));
		if (messages.size() == 0) {
			return true;
		} else {
			// get the error messages
			boolean errorFound = false;
			for (int messagesCount = 0; messagesCount < messages.size(); messagesCount++) {
				WebElement messageElement = messages.get(messagesCount);
				String message = messageElement.getText();
				// check error icon
				try {
					WebElement error = messageElement.findElement(By.className("error_icon"));
					SGLogger.errorTitle(SGLogger.errTitle, "      " + message);
					// check the content of the message
					Pattern reg = Pattern.compile(
							"The\\sobject\\syou\\sare\\strying\\sto\\supdate\\swas\\s\\'PolicyPeriod\\:cc\\:\\d+\\',\\sand\\sit\\swas\\schanged\\sby\\sSuper\\sUser\\sat\\s\\d{2}\\/[A-Za-z]{3}\\/\\d{4}\\s\\d{2}\\:\\d{2}\\s[AP]M\\.\\sPlease\\scancel\\sand\\sretry\\syour\\schange\\.");
					Matcher matcher = reg.matcher(message);
					if (error != null) {
						if (matcher.find()) {
							List<WebElement> policyPeriodMessage = null;
							int tryCount = 0;
							// for(int tryCount = 0 ; tryCount < maxTry;
							// tryCount++){
							do {
								SGLogger.info("      Found special Message, will perform a click.");

								By nextButtonSelector = By.cssSelector("[id*=FNOLWizard\\:Next]");
								WebElement nextButtonElement = null;
								WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
								try {
									nextButtonElement = wait
											.until(ExpectedConditions.presenceOfElementLocated(nextButtonSelector));
								} catch (Exception e) {
									nextButtonElement = ActionCommon.retryFindElement(driver, nextButtonSelector);
								}
								if (nextButtonElement != null) {
									tail = "[Click [" + (tryCount + 1) + "] begin]";
									ActionCommon.takeNormalScreenshot(driver, FileUtilConstant.tailIndexName(tail));

									nextButtonElement.click();
									SGLogger.info("      Clicked the [" + (tryCount + 1) + "]nd time");

									tail = "[Click [" + (tryCount + 1) + "] end]";
									ActionCommon.takeNormalScreenshot(driver, FileUtilConstant.tailIndexName(tail));
								}
								ActionCommon.waitForPageLoaded(driver);
								try {
									policyPeriodMessage = (new WebDriverWait(driver,
											ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
													.until(ExpectedConditions
															.presenceOfAllElementsLocatedBy(By.className("message")));
								} catch (TimeoutException e) {
									break;
								}
								tryCount++;
							} while (policyPeriodMessage != null);
							errorFound = false;
						} else {
							errorFound = true;
						}
					} else {
						SGLogger.warn("      " + message);
					}
				} catch (org.openqa.selenium.NoSuchElementException e) {
					return true;
				}
			}

			if (errorFound) {
				return false;
			} else {
				return true;
			}
		}
	}

	// private static boolean regexCheckString(String expected, String acutal){
	// char[] expectedArray = expected.toCharArray();
	// String regexPa = "";
	// for(int i = 0; i <expectedArray.length; i++ ){
	// regexPa = regexPa + expectedArray[i] + "[\\-\\s\\/\\:]?x?";
	// }
	//
	// if(ActionCommon.removeAllSpecialCharsForCompare(expected).equals(ActionCommon.removeAllSpecialCharsForCompare(acutal))){
	// return true;
	// }else if(expected.matches("^\\d+$") && acutal.matches("^\\d+$")){
	// return expected.equals(acutal);
	// }else if(acutal.matches(regexPa)){
	// return true;
	// }else{
	// return false;
	// }
	// }

	private static boolean checkExpectedAndActualMatch(String expected, String actual) {
		if (expected == null) {
			if (actual == null) {
				return true;
			} else {
				return false;
			}
		}

		boolean result = false;
		// Pattern postalcodePattern =
		// Pattern.compile("^\\d{5}(-\\d{4})?$)|(^[ABCEGHJKLMNPRSTVXY]{1}\\d{1}[A-Z]{1}
		// *\\d{1}[A-Z]{1}\\d{1}$");
		// exact match
		if (expected.equals(actual)) {
			//// System.out.println("i am here 0");
			return true;
		}

		if (expected.isEmpty()) {
			if (actual.isEmpty()) {
				return true;
			} else {
				return false;
			}
		}

		// check numbers
		String trimedExcepted = expected.replace(",", "");
		// System.out.println(trimedExcepted);
		String trimedActual = actual.replace(",", "");
		// System.out.println(trimedActual);
		boolean expectedIsNumber = StringUtils.isNumeric(trimedExcepted);
		boolean actualIsNumber = StringUtils.isNumeric(trimedActual);
		if (expectedIsNumber && actualIsNumber) {
			Double expectedNumber = Double.parseDouble(trimedExcepted);
			// System.out.println(expectedNumber);
			Double actualNumber = Double.parseDouble(trimedActual);
			// System.out.println(actualNumber);
			if (expectedNumber.compareTo(actualNumber) == 0) {
				// System.out.println("i am here 0.9");
				return true;
			} else {
				return false;
			}
		}

		// remove space to check, include postal code
		String expectedNoSpace = expected.replace(" ", "");
		String actualNoSpace = actual.replace(" ", "");
		if (actualNoSpace.equals(expectedNoSpace)) {//
			// System.out.println("i am here 1");
			return true;
		}

		// remove none digi
		String actualNoneDigi = actual.replaceAll("[^\\d]", "");
		if (expected.equals(actualNoneDigi)) {//
			//// System.out.println(expected);
			//// System.out.println(actualNoneDigi);
			// System.out.println("i am here 1.5");
			return true;
		}

		// remove slash to check, date only
		String expectedNoSlash = expected.replace(" ", "");
		String actualNoSlash = actual.replace(" ", "");
		if (expectedNoSlash.equals(actualNoSlash)) {
			// System.out.println("i am here 2");
			return true;
		}

		// telephone plus extension code match
		Pattern telephoneAndExetensionCodePattern = Pattern
				.compile("^(\\d{3})\\-(\\d{3})\\-(\\d{4})\\sx(\\d{4}[\\s\\d]?)$");
		Matcher telephoneAndExetensionCodeMatcher = telephoneAndExetensionCodePattern.matcher(actual);
		if (telephoneAndExetensionCodeMatcher.find()) {
			// System.out.println("i am here 3");
			String number1 = telephoneAndExetensionCodeMatcher.group(1);
			String number2 = telephoneAndExetensionCodeMatcher.group(2);
			String number3 = telephoneAndExetensionCodeMatcher.group(3);
			String number4 = telephoneAndExetensionCodeMatcher.group(4).replace(".", "");
			String actualNumberTrim = number1 + number2 + number3 + number4;
			//// System.out.println(expected);
			//// System.out.println(actualNumberTrim);
			if (actualNumberTrim.equals(expected)) {
				return true;
			}
		}

		// telephone plus extension code match
		Pattern telephonePattern = Pattern.compile("^(\\d{3})\\-(\\d{3})\\-(\\d{4})$");
		Matcher telephoneMatcher = telephonePattern.matcher(actual);
		if (telephoneMatcher.find()) {
			// System.out.println("i am here 4");
			String number1 = telephoneMatcher.group(1);
			String number2 = telephoneMatcher.group(2);
			String number3 = telephoneMatcher.group(3);
			String actualNumberTrim = number1 + number2 + number3;
			if (actualNumberTrim.equals(expected)) {
				return true;
			}
		}

		// time match
		Pattern timePattern = Pattern.compile("^(\\d{2})\\:(\\d{2})\\s([aApP][Mm])$");
		Matcher timeMatcher = timePattern.matcher(actual);
		if (timeMatcher.find()) {
			// System.out.println("i am here 5");
			String hours = timeMatcher.group(1);
			String mins = timeMatcher.group(2);
			String apm = timeMatcher.group(3);
			String actualTrim = hours + mins + apm;
			if (actualTrim.equals(expected)) {
				return true;
			}
		}

		// date and time match
		Pattern dateAndTimePattern = Pattern
				.compile("^(\\d{2})\\/(\\d{2})\\/(\\d{4})\\s(\\d{2})\\:(\\d{2})\\s([AP]M)$");
		Matcher dateAndTimeMatcher = dateAndTimePattern.matcher(actual);
		if (dateAndTimeMatcher.find()) {
			// System.out.println("i am here 6");
			String day = dateAndTimeMatcher.group(1);
			String month = dateAndTimeMatcher.group(2);
			String year = dateAndTimeMatcher.group(3);
			String hour = dateAndTimeMatcher.group(4);
			String min = dateAndTimeMatcher.group(5);
			String apm = dateAndTimeMatcher.group(6);
			String actualTrim = day + month + year + hour + min + apm;
			if (actualTrim.equals(expected)) {
				return true;
			}
		}
		return result;
	}

	private static boolean sepecialTextBoxNeedHomeKey(String originValue) {
		if (originValue.equals("...-...-.... x....")) {
			return true;
		} else if (originValue.equals("../../....")) {
			return true;
		} else if (originValue.equals("..:.. ..")) {
			return true;
		} else if (originValue.equals("../../.... ..:.. ..")) {
			return true;
		} else if (originValue.equals("... ...")) {
			return true;
		} else {
			return false;
		}
	}

	private static boolean sepecialTextBoxNeedDeleteOrginalValue(String originValue) {
		if (originValue.matches("^\\d{2}\\/\\d{2}\\/\\d{4}$")) {
			return true;
		} else if (originValue.matches("^\\d{2}\\:\\d{2}\\s(PM|AM)?$")) {
			return true;
		} else {
			return false;
		}
	}

	private static void waitDropdownToPopolulate(final WebDriver driver, final By selector) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
			(new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
					.until(ExpectedConditions.presenceOfElementLocated(selector));
			wait.until(new ExpectedCondition<Boolean>() {
				@Override
				public Boolean apply(WebDriver input) {
					return driver.findElement(selector).findElements(By.tagName("option")).size() > 1;
				}
			});
		} catch (org.openqa.selenium.TimeoutException e) {
			// e.printStackTrace();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public static void waitForPageLoaded(WebDriver driver) {
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		try {
			wait.until(new ExpectedCondition<Boolean>() {
				@Override
				public Boolean apply(WebDriver driver) {
					return ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
				}
			});
		} catch (org.openqa.selenium.UnhandledAlertException e) {
			e.printStackTrace();
			ActionCommon.checkAlert(driver);
		} catch (org.openqa.selenium.WebDriverException e) {
			e.printStackTrace();
		}
	}

	private static boolean retryClick(WebDriver driver, By selector) {
		boolean result = false;
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		int attempts = 0;
		do {
			try {
				wait.until(ExpectedConditions.elementToBeClickable(selector)).click();
				result = true;
				break;
			} catch (StaleElementReferenceException e) {
				SGLogger.error("      Error on check element clickable.");
			} catch (Exception e) {
				e.printStackTrace();
				SGLogger.error("      Got error when try to click the element on the screen.Type is:" + e.getClass());
				// TODO 这是什么?
				//System.exit(0);
				return false;
			}
			attempts++;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (attempts < 5);
		return result;
	}

	private static String retryGetText(WebDriver driver, By selector) {
		String result = errTxt;
		int attempts = 0;
		while (attempts < 2) {
			try {
				result = driver.findElement(selector).getText();
				break;
			} catch (StaleElementReferenceException e) {
			}
			attempts++;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

//	private String retryGetText(WebElement element) {
//		String result = errTxt;
//		int attempts = 0;
//		while (attempts < 2) {
//			try {
//				result = element.getText();
//				break;
//			} catch (StaleElementReferenceException e) {
//			}
//			attempts++;
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		return result;
//	}

	private static String retryGetValue(WebDriver driver, By selector) {
		String result = errTxt;
		int attempts = 0;
		while (attempts < 2) {
			try {
				result = driver.findElement(selector).getAttribute("value");
				break;
			} catch (org.openqa.selenium.StaleElementReferenceException e) {
				e.printStackTrace();
			} catch (org.openqa.selenium.NoSuchElementException e) {
				e.printStackTrace();
			}
			attempts++;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

//	private String retryGetAttribute(WebDriver driver, By selector, String attributeName) {
//		String result = null;
//		int attempts = 0;
//		while (attempts < 2) {
//			try {
//				WebElement ele = driver.findElement(selector);
//				System.out.println("Retry Id [" + ele.getAttribute("id") + "]");
//				result = ele.getAttribute(attributeName);
//				break;
//			} catch (org.openqa.selenium.StaleElementReferenceException e) {
//			} catch (org.openqa.selenium.NoSuchElementException e) {
//			}
//			attempts++;
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		return result;
//	}

	public static WebElement retryFindElement(WebDriver driver, By selector) {
		WebElement element = null;
		int attempts = 0;
		while (attempts < 2) {
			try {
				element = driver.findElement(selector);
				break;
			} catch (org.openqa.selenium.StaleElementReferenceException e) {
			} catch (org.openqa.selenium.NoSuchElementException e) {
			}
			attempts++;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return element;
	}

	private static List<WebElement> retryFindElements(WebDriver driver, By selector) {
		List<WebElement> elements = null;
		int attempts = 0;
		while (attempts < 2) {
			try {
				elements = driver.findElements(selector);
				break;
			} catch (org.openqa.selenium.StaleElementReferenceException e) {
			} catch (org.openqa.selenium.NoSuchElementException e) {
			}
			attempts++;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return elements;
	}

	private static WebElement retryFindElement(WebElement parentElement, By selector) {
		WebElement ele = null;
		int attempts = 0;
		while (attempts < 2) {
			try {
				ele = parentElement.findElement(selector);
				if (ele != null) {
					break;
				}
			} catch (org.openqa.selenium.StaleElementReferenceException e) {
			} catch (org.openqa.selenium.NoSuchElementException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
			attempts++;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return ele;
	}

//	private boolean isOnFocus(WebDriver driver, WebElement element) {
//		if (driver == null || element == null) {
//			return false;
//		}
//		if (element.equals(driver.switchTo().activeElement())) {
//			return true;
//		} else {
//			return false;
//		}
//	}

	private static boolean isDisabled(WebDriver driver, By selector) {
		WebDriverWait wait = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP));
		WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
		String disabled = element.getAttribute("disabled");
		if (disabled == null) {
			System.out.println(" Button is not disabled. [" + disabled + "]");
			return false;
		} else {
			System.out.println(" Button is disabled. [" + disabled + "]");
			return true;
		}
	}

//	private WebElement fluentWait(WebDriver driver, final By locator) {
//		Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
//				.withTimeout(ContextConstant.WEBELEMENT_WAIT_MAX, TimeUnit.SECONDS).pollingEvery(1, TimeUnit.SECONDS)
//				.ignoring(NoSuchElementException.class);
//
//		WebElement foo = wait.until(new Function<WebDriver, WebElement>() {
//			@Override
//			public WebElement apply(WebDriver driver) {
//				return driver.findElement(locator);
//			}
//		});
//		return foo;
//	}
//
//	private static boolean swtichToTab(WebDriver driver, String tabName) {
//		driver.switchTo().window(tabName);
//		String currentWindow = driver.getWindowHandle();
//		if (currentWindow.equals(tabName) || currentWindow.contains(tabName)) {
//			return true;
//		} else {
//			return false;
//		}
//	}
//
//	private static boolean disableReadonly(WebDriver driver, By locator, String input) {
//		if (!ContextConstant.inputIgnored(input)) {
//			WebElement ele = driver.findElement(locator);
//			((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('readonly','readonly')", ele);
//			// get the attribute
//			String readonlyAtt = ele.getAttribute("readonly");
//			if (readonlyAtt == null) {
//				return true;
//			} else {
//				return false;
//			}
//		} else {
//			return true;
//		}
//	}

	public static ExecutionResult browserNavigate(WebDriver driver, By selector, String input, InstructionOptions options) {
		try {
			driver.navigate().to(input);
			return new ExecutionResult(true, "	[打开页面成功]" + input);
		} catch (Exception e) {
			e.printStackTrace();
			return SGLogger.error("	[打开页面失败] 输入{" + input + "}无效或者网络不通.");
		}
	}

	public static ExecutionResult browserClose(WebDriver driver, By selector, String input, InstructionOptions options) {
		// driver.manage().timeouts().pageLoadTimeout(Integer.parseInt(input),TimeUnit.SECONDS);
		try {
			Actions keyDonwAction = new Actions(driver);
			keyDonwAction.keyDown(Keys.CONTROL).sendKeys("w").keyUp(Keys.CONTROL).sendKeys(Keys.NULL).perform();
			return new ExecutionResult(true, "	[关闭浏览器]Ctrl + A 已发送");
		} catch (Exception e) {
			e.printStackTrace();
			return SGLogger.error("	[关闭页面失败]");
		}
	}

	public static ExecutionResult browserBack(WebDriver driver, By selector, String input, InstructionOptions options) {
		try {
			driver.navigate().back();
			return new ExecutionResult(true, "	[浏览器]已后退");
		} catch (Exception e) {
			e.printStackTrace();
			return SGLogger.error("	[页面后退失败]");
		}
	}
	
	public static ExecutionResult browserSwitchToNewtab(WebDriver driver, By selector, String input, InstructionOptions options) {
		try {
			ArrayList<String> tabs = new ArrayList<String> (driver.getWindowHandles());
	        driver.switchTo().window(tabs.get(1));
			return new ExecutionResult(true, "	[浏览器]已定位到页面"+input);
		} catch (Exception e) {
			e.printStackTrace();
			return SGLogger.error("	[浏览器]定位到页面失败");
		}
	}

	@Deprecated
	public static boolean browserWait(WebDriver driver, By selector, String input, InstructionOptions options) {
		try {
			driver.manage().timeouts().pageLoadTimeout(Integer.parseInt(input), TimeUnit.SECONDS);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			SGLogger.error("	[页面等待失败]");
		}
		return false;
	}

	public static ExecutionResult browserSleep(WebDriver driver, By selector, String input, InstructionOptions options) {
		try {
			// Thread.sleep(Long.parseLong(input));//将时间时间单位由毫秒改为秒
			Thread.sleep(Long.parseLong(input) * 1000);
			return new ExecutionResult(true, "	[浏览器]已静置" + input + "秒");
		} catch (NumberFormatException | InterruptedException e) {
			e.printStackTrace();
			return SGLogger.error("	[页面静置失败]");
		}
	}

	public static ExecutionResult browserForward(WebDriver driver, By selector, String input, InstructionOptions options) {
		try {
			driver.navigate().back();
			return new ExecutionResult(true, "	[浏览器]已后退");
		} catch (Exception e) {
			e.printStackTrace();
			return SGLogger.error("	[页面前进失败]");
		}
	}

	public static ExecutionResult browserRefresh(WebDriver driver, By selector, String input, InstructionOptions options) {
		try {
			driver.navigate().refresh();
			return new ExecutionResult(true, "	[浏览器]已刷新");
		} catch (Exception e) {
			e.printStackTrace();
			return SGLogger.error("	[页面刷新失败]");
		}
	}

	/**
	 *
	 * @param con
	 * @return boolean
	 */
	protected static boolean supportBatch(Connection con) {
		try {
			// get the meta data for db.
			DatabaseMetaData md = con.getMetaData();
			return md.supportsBatchUpdates();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected static int[] doBatchSqlExecute(Connection con, String[] sqls) throws SQLException {
		if (null == sqls)
			return null;

		Statement sm = null;
		try {
			sm = con.createStatement();
			for (int i = 0; i < sqls.length; i++) {
				sm.addBatch(sqls[i].trim());
			}
			// 一次执行多条SQL语句
			return sm.executeBatch();
		} catch (SQLException e) {
			e.printStackTrace();
			SGLogger.error(String.format("		执行SQL语句时有异常发生:%s。异常为%s", String.join(";", sqls), e.getMessage()));
		} finally {
			sm.close();
		}
		return null;
	}

	/*
	 * 执行SQL语句
	 */
	@Deprecated
	public static boolean sqlScriptExecute(String locatorValue, String inputSql) {
		String sql = inputSql.trim();
		Connection con = null;
		DataSource targetDS = null;
		
		try {
			IDataSource ds = BaseBundleActivator.getTheServiceObject("db", IDataSource.class);
			if (null != ds) {
				if (sql.endsWith(";"))
					sql = sql.substring(0, sql.length() - 1);
				targetDS = getTargetDataSource(locatorValue, ds);

				if (null != targetDS) {
					con = targetDS.getConnection();
					// multi statement
					if (sql.contains(";")) {
						if (supportBatch(con)) {
							try {
								int[] results = doBatchSqlExecute(con, sql.split(";"));
								if (null != results) {
									SGLogger.info(String.format("      [成功]执行SQL语句:%s:结果为:%s.", sql, results));
									return true;
								} else {
									SGLogger.error(String.format("      [失败]执行SQL语句:%s:结果为null.", sql));
									return false;
								}
							} catch (SQLException e) {
								SGLogger.error(
										String.format("		[失败]执行SQL语句时有异常发生:%s。异常为%s", sql, e.getMessage()));
							}
						} else {
							SGLogger.error(String.format("      [失败]执行SQL语句:%s:目前暂不支持多语句.", sql));
						}
						return false;
					}

					Statement st = con.createStatement();
					String lowerCaseSQL = sql.toLowerCase();
					if (!lowerCaseSQL.startsWith("select") && (lowerCaseSQL.contains("insert")
							|| lowerCaseSQL.contains("delete") || lowerCaseSQL.contains("update"))) {
						int nResult = st.executeUpdate(sql);
						con.close();
						if (nResult >= 0) {
							SGLogger.info(String.format("		[成功]执行SQL语句:%s,影响:%d条", sql, nResult));
							return true;
						} else {
							SGLogger.error(String.format("      [失败]执行SQL语句:%s:结果有误,为:%d条.", sql, nResult));
						}
					} else {
						SGLogger.error(String.format("      [失败]暂不支持的SQL语句:%s.", sql));
					}
				} else {
					SGLogger.error(String.format("      [失败]The Data Source Name:%s is null.", locatorValue));
				}
			} else {
				SGLogger.error("      [失败]The IDataSource Service is null. OSGi framework should have loaded it.");
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			SGLogger.error(String.format("		[失败]执行SQL语句时有异常发生:%s。异常为%s", sql, e.getMessage()));
		} finally {
			if (null != con) {
				try {
					if (!con.isClosed())
						con.close();
				} catch (SQLException e) {
					SGLogger.info(String.format("		[失败]执行SQL语句关闭连接时有异常发生:%s。异常为%s", sql, e.getMessage()));
				}
			}
		}
		return false;
	}

	/**
	 * excutet javascript
	 *
	 * @param inputJS
	 * @author qiaorui.chen
	 */
	public static ExecutionResult jScriptExecute(WebDriver driver, String inputJS, InstructionOptions options) {		
		if (StringUtil.nullOrEmpty(inputJS)) {
			System.out.println(msString = "inputJS is null or empty,please input correct jscipt");
			return new ExecutionResult(false, msString);
		} else {
			String jscript = inputJS.replaceAll("\"", "\'").trim();
			try {
				JavascriptExecutor js = (JavascriptExecutor) driver;
				Object object = js.executeScript(jscript);
				
				JSONObject updateParams = new JSONObject();
				updateParams.put("inputData", inputJS);
				updateParams.put("returnValue", null == object ? "null" : object.toString());
				httpUtil.updateInstructionResult(updateParams.toString());
				
				if (object == null) {
					if (options.existOption(ContextConstant.OPTION_COMPARE_RETURN_VALUE)
							|| options.existOption(ContextConstant.OPTION_SAVE_TEXT)) {
						return SGLogger.error("执行JSCRIPT语句后没有返回值。");
					}
					return SGLogger.infoER(String.format("[成功]执行JSCRIPT语句:%s。执行JSCRIPT语句后没有返回值。", jscript));
				} else {
					if (options.existOption(ContextConstant.OPTION_SAVE_TEXT)) {
						if (object instanceof String) {
							String returnValue = (String) object;
							elementText = returnValue;
						} else if (object instanceof Long) {
							Long returnValue = (Long) object;
							elementText = returnValue.toString();
						} else {
							try {
								elementText = object.toString();
							} catch (Exception e) {
								return SGLogger.error(String.format("执行JSCRIPT语句,转换返回值时有异常发生:" + e.getMessage() + ",转换的返回值为" + object));
							}
						}
					}

					if (options.existOption(ContextConstant.OPTION_COMPARE_RETURN_VALUE)) {
						String expectValue = options.getValue(ContextConstant.OPTION_COMPARE_RETURN_VALUE);
						if (object instanceof String) {
							String returnValue = (String) object;
							if (expectValue.equals(returnValue)) {
								return SGLogger.verifySuccessER("ExpectValue match. Expected [" + expectValue + "] : Actual ["
										+ returnValue + "]");
							} else {
								return SGLogger.verifyErrorER("ExpectValue match not match. Expected [" + expectValue
										+ "] : Actual [" + returnValue + "]");
							}
						}

						else if (object instanceof Long) {
							Long returnValue = (Long) object;
							if (expectValue.equals(returnValue.toString())) {
								return SGLogger.verifySuccessER("ExpectValue match. Expected [" + expectValue + "] : Actual ["
										+ returnValue.toString() + "]");
							} else {
								return SGLogger.verifyErrorER("ExpectValue match not match. Expected [" + expectValue
										+ "] : Actual [" + returnValue.toString() + "]");
							}
						} else {
							try {
								if (expectValue.equals(object.toString())) {
									return SGLogger.verifySuccessER("ExpectValue match. Expected [" + expectValue
											+ "] : Actual [" + object.toString() + "]");
								} else {
									return SGLogger.verifyErrorER("ExpectValue match not match. Expected [" + expectValue
											+ "] : Actual [" + object.toString() + "]");
								}
							} catch (Exception e) {
								return SGLogger.error(String
										.format("[失败]执行JSCRIPT语句,转换返回值时有异常发生:" + e.getMessage() + ",转换的返回值为" + object));
							}
						}
					}
					return SGLogger.infoER(String.format("[成功]执行JSCRIPT语句:%s。返回内容为:%s", jscript, object));
				}
			} catch (Exception e) {
				return SGLogger.error(String.format("[失败]执行JSCRIPT语句时有异常发生:%s。异常为%s", jscript, e.getMessage()));
			}
		}
	}

	private static DataSource getTargetDataSource(String locatorValue, IDataSource ds) {
		DataSource targetDS = ds.getDataSource(locatorValue);
		System.out.println("the db datasource is " + ds.toString());
		if (null == targetDS) {
			System.err.println("Get data source named [" + locatorValue + "] failed. try to init data source.");
			// TODO init by task before
			try {
				int nCount = 3;
				while (null == targetDS && nCount-- > 0) {
					Thread.sleep(1000);
					targetDS = ds.getDataSource(locatorValue);
					System.err.println("Sleep the " + (3 - nCount) + "st . and target "
							+ (null == targetDS ? "is still null." : "got."));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return targetDS;
	}

	/*public static ExecutionResult switchToFrame(WebDriver driver, String frameId, InstructionOptions options) {
		try {
			if (frameId == null || frameId.isEmpty()) {
				driver.switchTo().defaultContent();
				return SGLogger.infoER("frameId is null or empty,Switched to defaultContent ");
			} else {
				driver.switchTo().frame(frameId);
				return SGLogger.infoER("Current focus frame id is [" + frameId + "]");
			}
		} catch (org.openqa.selenium.NoSuchFrameException e) {
			return SGLogger.infoER("No frame element found by name or id  " + frameId);
		}
	}*/
	
	/**
	 * 
	 * @param driver
	 * @param selector
	 * @param input
	 * @param options
	 * @return
	 */
	public static ExecutionResult switchToFrame(WebDriver driver, By selector, String input, InstructionOptions options) {
		return switchToFrame(driver, selector, input, false, options);
	}

	public static ExecutionResult switchToFrame(WebDriver driver, By selector, String action, boolean tryMode,
			InstructionOptions options) {
		if (action == null) {
			return SGLogger.error(
					"      The input data is NULL point, please check if the input contains the corresponding data column.");
		}
		ActionCommon.waitForPageLoaded(driver);
		ActionCommon.checkAlert(driver);
		WebElement element = null;
		WebDriverWait wait = new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP);
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(selector));
			element = ActionCommon.retryFindElement(driver, selector);
			if (element != null) {		 
				driver.switchTo().frame(element);
				return SGLogger.infoER("Current focus frame is [" + selector + "]");				
			} else {
				driver.switchTo().defaultContent();
				return SGLogger.infoER("element is null or empty,Switched to defaultContent ");
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
			return SGLogger.elementTimeOut("      Try to jsExcute none existing WebElement.");
		} catch (Exception e) {
			return new ExecutionResult(false, "switchto frame执行时发生未知错误");
		}		
	}
	
	public static void resetFileService() {
		ActionCommon.fileService = null;
		ActionCommon.fileServicePrefix = "";
		ActionCommon.addStepFileLog = null;
		ActionCommon.httpUtil = null;
		ActionCommon.runId = 0L;
		ActionCommon.instructionResultId = 0L;
	}

	public static void setFileService(IFileService fs, String fileServicePrefix, JSONObject addStepFileLog,
			IHttpUtil httpUtil, long runId, long instructionResultId) {
		if (null == fs || null == addStepFileLog || null == httpUtil || 0 >= instructionResultId)
			resetFileService();
		else {
			ActionCommon.fileService = fs;

			if (!StringUtil.nullOrEmpty(fileServicePrefix)) {
				if (fileServicePrefix.startsWith("\\") || fileServicePrefix.startsWith("/"))
					fileServicePrefix = fileServicePrefix.substring(1);
				if (fileServicePrefix.length() > 0
						&& !(fileServicePrefix.endsWith("\\") || fileServicePrefix.endsWith("/")))
					fileServicePrefix = fileServicePrefix + "/";
			}

			ActionCommon.fileServicePrefix = fileServicePrefix;
			ActionCommon.addStepFileLog = addStepFileLog;
			ActionCommon.httpUtil = httpUtil;
			ActionCommon.runId = runId;
			ActionCommon.instructionResultId = instructionResultId;
		}
	}
	
	public static void setFileService(IFileService fs, IHttpUtil httpUtil) {
		ActionCommon.fileService = fs;
		ActionCommon.httpUtil = httpUtil;
	}
	
	public static void setFileService(long instructionRunId) {
		ActionCommon.instructionResultId = instructionRunId;
	}
	
	public static boolean takeNormalScreenshotWithHighLight(WebDriver driver, String FileName, WebElement webElement) {
		System.err.println("takeNormalScreenshotWithHighLight.LOG_FOLDER:" + FileUtilConstant.LOG_FOLDER);
		String Path = FileUtilConstant.LOG_FOLDER;
		boolean bOK = false;
		try {
			ActionCommon.waitForPageLoaded(driver);
			// like 0003
			String screenShootNumber = String.format("%1$04d", FileUtilConstant.screenShootIndex);
			File outputFile = new File(Path + screenShootNumber + FileName + "_" + System.currentTimeMillis() + ".jpg");
			File srcFile = new File("tmp2.jpg");
			try {
				// 设置高亮的样式
				String jsHightLight = "arguments[0].style.border='6px solid red'";
				JavascriptExecutor jsHightLightExc = (JavascriptExecutor) driver;
				jsHightLightExc.executeScript(jsHightLight, webElement);

				// 全屏截图，通过滚动条的滚动来截取
				Screenshot screenshot = new AShot().shootingStrategy(ShootingStrategies.viewportPasting(100))
						.takeScreenshot(driver);
				ImageIO.write(screenshot.getImage(), "jpg", srcFile);
				FileUtils.copyFile(srcFile, outputFile);
				writeToFileServer(screenShootNumber, outputFile, false);
				// TODO 截图完成之后，将页面的滚动条回到顶部
				String setScroll = "document.documentElement.scrollTop=0";
				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript(setScroll);
				bOK = true;
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				bOK = false;
			}
			FileUtilConstant.screenShootIndex++;
			FileUtilConstant.remoteScreenShootIndex++;
		} catch (HeadlessException e) {
			e.printStackTrace();
			bOK = false;
		}
		
		return bOK;
	}
	
	public static void removeElementHighLightStyle(WebDriver driver, WebElement webElement) {
		try {
			ActionCommon.waitForPageLoaded(driver);
			// like 0003
			try {
				// 设置高亮的样式
				// String jsRemoveHightLight = "arguments[0].style.border='6px solid red'";
				String jsRemoveHightLight = "arguments[0].removeAttribute('style')";
				JavascriptExecutor jsHightLightExc = (JavascriptExecutor) driver;
				jsHightLightExc.executeScript(jsRemoveHightLight, webElement);

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			FileUtilConstant.screenShootIndex++;
			FileUtilConstant.remoteScreenShootIndex++;
		} catch (HeadlessException e) {
			e.printStackTrace();
		}
	}
}
