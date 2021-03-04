package com.meowlomo.ci.ems.bundle.webdriver;

import java.io.File;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.webdriver.TableExecutor;

public class ModuleExecutorSelector {
	private WebDriver driver = null;
	private JSONObject instructionArray = null;
	private JSONObject elementMap = null;
	
	public ModuleExecutorSelector(WebDriver driver, JSONObject elements, JSONObject instructions) {
		this.driver = driver;
		this.instructionArray = instructions;
		this.elementMap = elements;
	}

	
	public boolean selectModuleToExecute(By locator,String elementType,String instructionObject, String action, String input, InstructionOptions options) {
		try {
			if(elementType.equalsIgnoreCase("table")) {
				TableExecutor tableExecutor  = new TableExecutor(this.driver,this.elementMap,this.instructionArray);	
				return tableExecutor.excute(instructionObject, action, input, options);	
			}
			if(elementType.equalsIgnoreCase("map")) {
				MapExecutor mapExecutor =new MapExecutor(this.driver);
				return mapExecutor.excute(locator,instructionObject, action, input, options);							
			}
			else {
				//TODO send msg to a msg bus ,or other infrastructure like broadcast
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
	}
}
