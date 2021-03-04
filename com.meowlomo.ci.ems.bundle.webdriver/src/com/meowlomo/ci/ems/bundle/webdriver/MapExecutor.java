package com.meowlomo.ci.ems.bundle.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.webdriver.map.MapActionCommon;



public class MapExecutor {
private WebDriver driver = null;	
   
    
	public MapExecutor(WebDriver driver) {
		this.driver=driver;
	}

	public boolean excute(By locator, String instructionObject, String action, String input,InstructionOptions options) {
		Boolean result=false;
		if (action.equalsIgnoreCase("zoom")) {
			result = MapActionCommon.mapZoom(driver, locator, input, options);
		}
		else if (action.equalsIgnoreCase("location")) {
			result = MapActionCommon.getLocationByLngAndLat(driver, locator, input, options);
		}
		else if (action.equalsIgnoreCase("lngAndLat")) {
			result = MapActionCommon.getLngAndLatByLocation(driver, locator, input, options);
		}
		else if (action.equalsIgnoreCase("drag")) {
			result = MapActionCommon.dragMap(driver, locator, input, options);
		}	
		return result;
	}
}
