package com.meowlomo.ci.ems.bundle.webdriver.map;

import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.FileUtilConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.webdriver.ActionCommon;
import com.meowlomo.ci.ems.bundle.webdriver.BooleanStringSet;



public class MapActionCommon {
	/**
	 * 地图缩放功能 
	 * @author qiaorui.chen xml中locator-value取地图id。
	 * type:map,action:zoom,input:缩放次数。此方法先放大后缩小,input数值介于1-9(包含)
	 * 
	 */
	public static boolean mapZoom(WebDriver driver, By selector, String zoomTime, InstructionOptions options) {
		return MapActionCommon.mapZoom(driver, zoomTime, selector, options);
	}

	public static boolean mapZoom(WebDriver driver, String zoomTime, By selector, InstructionOptions options) {
		int zoomTimes = 0;// 缩放次数
		if (zoomTime == null || zoomTime.equals("")) {
			SGLogger.error(
					"      the input data is NULL or EMPTY, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(zoomTime)) {
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement mapElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.presenceOfElementLocated(selector));
				mapElement = driver.findElement(selector);
				mapElement.click();
				zoomTimes = Integer.parseInt(zoomTime);
				if (0 < zoomTimes && zoomTimes < 10) {
					for (int a = 0; a < zoomTimes; a++) {
						JavascriptExecutor js = (JavascriptExecutor) driver;
						js.executeScript("map.zoomIn()");
						Thread.sleep(2000);
					}
					String logFileName = FileUtilConstant.tailIndexName("After magnifying map");
					ActionCommon.tail = "after magnifying map";
					ActionCommon.takeNormalScreenshot(driver, logFileName);
					for (int a = 0; a < zoomTimes; a++) {
						JavascriptExecutor js = (JavascriptExecutor) driver;
						js.executeScript("map.zoomOut()");
						Thread.sleep(2000);
					}
					String logFileName2 = FileUtilConstant.normalIndexName() + "After shrinking map";
					ActionCommon.tail = "After shrinking map";
					ActionCommon.takeNormalScreenshot(driver, logFileName2);
				} else {
					SGLogger.error("zoomTimes should between 1-9");
					return false;
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
				SGLogger.error("as a string, zoomTimes cann't turn to be a int type ");
				return false;
			} catch (Exception e) {
				e.printStackTrace();
				SGLogger.errorTitle(SGLogger.errTitle, "zoomMap failed!");
				return false;
			}
		}
		return true;
	}

	/**
	 * @author qiaorui.chen
	 * @param lngAndLat 地图上点的经纬度,格式如: 116.404, 39.915 
	 * 根据经纬度获取位置,并且将地图定位到该位置
	 * 
	 */
	public static boolean getLocationByLngAndLat(WebDriver driver, By selector, String lngAndLat,InstructionOptions options) {
		boolean result = false;
		String location="";
		// longtitude
		String lng = null;
		// latitude
		String lat = null;
		if (lngAndLat == null || lngAndLat.equals("")) {
			SGLogger.error(
					"      the input data is NULL or EMPTY, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(lngAndLat)) {
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement mapElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP)).until(ExpectedConditions.presenceOfElementLocated(selector));
				mapElement = driver.findElement(selector);
				mapElement.click();
				//校验输出的经纬度参数. 经度： -180.0～+180.0（整数部分为0～180，必须输入1到8位小数）, 纬度： -90.0～+90.0（整数部分为0～90，必须输入1到8位小数）			
				String regex = "^[\\-\\+]?(0?\\d{1,2}\\.\\d{1,8}|1[0-7]?\\d{1}\\.\\d{1,8}|180\\.0{1,8}),[\\-\\+]?([0-8]?\\d{1}\\.\\d{1,8}|90\\.0{1,8})";
				boolean isMatch = Pattern.matches(regex, lngAndLat);
				if (isMatch == true) {
				//把经纬度分割成经度、纬度
					String[] strs = lngAndLat.split(",");
					lng = strs[0].toString();
					lat = strs[1].toString();
					
					JavascriptExecutor js = (JavascriptExecutor) driver;
					js.executeScript(
							"window.meowLoc = '';" 
						  + "var center=map.getCenter();"
						  + "var lng=arguments[0];"
						  + "var lat=arguments[1];"
						  + "var point=new BMap.Point(lng,lat);" 
						  + "var zoom=map.getZoom();"
						  + "map.centerAndZoom(point,zoom);" 
						  + "var gc=new BMap.Geocoder();"
						  + "gc.getLocation(point,function(rs){" 
						  + "var addc=rs.addressComponents;"
						  + "window.meowLoc=addc.city+addc.district+addc.street+addc.streetNumber;"
						  + "});",lng, lat);
					Thread.sleep(1000);
					location = (String) js.executeScript("return window.meowLoc;");					
					SGLogger.info("get location" + " [     " + location + "       ] ");					
					String logFileName = FileUtilConstant.tailIndexName("After locator map");
					ActionCommon.tail = "after locator map";
					ActionCommon.takeNormalScreenshot(driver, logFileName);
					result = true;					
				} else {
					SGLogger.error("please input correct longitude and latitude. ");
					result = false;
				}
			} catch (Exception e) {
				e.printStackTrace();
				SGLogger.errorTitle(SGLogger.errTitle, "locator failed!");
				result = false;
			}
		}
		return result;
	}

	/**
	 * @author qiaorui.chen
	 * @param location  表位置 ;  格式如: 深圳市xxx 或 深圳市南山区软件产业基地
	 * 根据位置获取经纬度，并且将地图定位到该位置
	 * 	
	 */
	public static boolean getLngAndLatByLocation(WebDriver driver, By selector, String location,InstructionOptions options) {
		boolean result = false;
		if (location == null || location.equals("")) {
			SGLogger.error(
					"      the input data is NULL or EMPTY, please check if the excel file contains the corresponding data column.");
		}
		if (!ContextConstant.matchIgnored(location)) {
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement mapElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP))
						.until(ExpectedConditions.presenceOfElementLocated(selector));
				mapElement = driver.findElement(selector);
				mapElement.click();
				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript(
						"window.meowLngAndLat = '';" 
				      + "var localSearch = new BMap.LocalSearch(map);"
					  + "localSearch.setSearchCompleteCallback(" 
				      + "function(searchResult){"
					  + "var poi = searchResult.getPoi(0);" 
				      + "window.meowLngAndLat = poi.point.lng+','+poi.point.lat;"
					  + "var point = new BMap.Point(poi.point.lng,poi.point.lat);"
				      + "var meowZoom=map.getZoom();"
					  + "map.centerAndZoom(point,meowZoom);"
				      + "return window.meowLngAndLat;" 
					  + "});"
					  + "localSearch.search(arguments[0]);", location);
				Thread.sleep(1000);
				String lngAndLat = (String) js.executeScript("return window.meowLngAndLat;");
				Thread.sleep(2000);
				String logFileName = FileUtilConstant.tailIndexName("After locator map");
				ActionCommon.tail = "after locator map";
				ActionCommon.takeNormalScreenshot(driver, logFileName);
				SGLogger.info("get longtitude and latitude" + " [        " + lngAndLat + "] ");
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
				SGLogger.errorTitle(SGLogger.errTitle, "get longitude and latitude  failed!");
				result = false;
			}
		}
		return result;
	}

	/**
	 * @author qiaorui.chen
	 * @param action:yes
	 * 通过拖动地图来判断javascript code 有没有使用 disableDragging()
	 * 
	 */
	public static boolean dragMap(WebDriver driver, By selector, String action, InstructionOptions options) {
		if (action == null) {
			SGLogger.error(
					"      The input data is NULL point, please check if the excel file contains the corresponding data column.");
			return false;
		}
		if (ContextConstant.matchIgnored(action)) {
			return true;
		}
		// check the input is valid.
		if (BooleanStringSet.isTrue(action) || action.equals(ContextConstant.SKIP_BY_NOT_FOUND)) {
			ActionCommon.waitForPageLoaded(driver);
			try {
				WebElement mapElement = (new WebDriverWait(driver, ContextConstant.WEBELEMENT_WAIT_MAX, ContextConstant.WEBELEMENT_WAIT_SLEEP)).until(ExpectedConditions.presenceOfElementLocated(selector));
				mapElement = driver.findElement(selector);
				mapElement.click();
				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript(
						"window.meowResult=false;" 
				      + "map.addEventListener('dragend',checkDragMethod);"
					  + "function checkDragMethod(){"
				      + "window.meowResult=true;"
					  + "};");
				Actions action2 = new Actions(driver);
				action2.dragAndDropBy(mapElement, 180, 180).perform();
				Thread.sleep(1000);
				Boolean result = (Boolean) js.executeScript("return window.meowResult;");
				if (result == true) {
					SGLogger.info("drap map successfully.");
				} else {
					SGLogger.errorTitle(" [      错误      ] ", "drap map fail.please check if javascript code use disableDragging method. ");
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
				SGLogger.errorTitle(SGLogger.errTitle, "drap map failed!");
				return false;
			}
		}
		return true;
	}

}
