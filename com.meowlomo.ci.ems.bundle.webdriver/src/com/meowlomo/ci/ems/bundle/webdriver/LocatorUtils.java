package com.meowlomo.ci.ems.bundle.webdriver;

import org.openqa.selenium.By;

public class LocatorUtils {
	public static By getLocator(String locatorType, String locatorString){
		String type = locatorType.toLowerCase();
		switch (type){
		case "id" :
			System.err.println("use id to locate "+locatorString);
			return By.id(locatorString);
		case "css":
			return By.cssSelector(locatorString);
		case "name":
			return By.name(locatorString);
		case "xpath":
			return By.xpath(locatorString);
		case "class":
			return By.className(locatorString);
		case "tag":
			return By.tagName(locatorString);
		case "linktext":
			return By.linkText(locatorString);
		default:
			return null;
		}
	}
}
