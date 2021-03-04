package com.meowlomo.ci.ems.bundle.webdriver.abandoned;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;

public class FixInstructions {
	
	private WebDriver driver = null;
	
	public FixInstructions(WebDriver driver){
		this.driver = driver;
	}
	
	public Boolean instrcutionProcessor(String instruction, String action, String input){
		Boolean result = null;
		if(instruction.toLowerCase().equals(("Engine.Browser.Navigate").toLowerCase())){
			driver.navigate().to(input);
			result = true;
		}
		else if(instruction.toLowerCase().equals(("Engine.Browser.Wait").toLowerCase())){
			driver.manage().timeouts().pageLoadTimeout(Integer.parseInt(input), TimeUnit.SECONDS);
			return true;
		}
		else if(instruction.toLowerCase().equals(("Engine.Browser.Sleep").toLowerCase())){
			try {
				Thread.sleep(Long.parseLong(input));
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		/**
		 * @author yanfang.chen
		 * 浏览器关闭当前页面
		 */
		else if (instruction.toLowerCase().equals(("Engine.Browser.Close").toLowerCase())) {
			driver.manage().timeouts().pageLoadTimeout(Integer.parseInt(input),TimeUnit.SECONDS);
			Actions keyDonwAction = new Actions(driver);
			keyDonwAction.keyDown(Keys.CONTROL).sendKeys("w").keyUp(Keys.CONTROL).sendKeys(Keys.NULL).perform();
			return true;
		}
		/**
		 * @author qiaorui.chen
		 * 操作浏览器后退
		 */
		else if (instruction.toLowerCase().equals(("Engine.Browser.Back").toLowerCase())) {
			driver.navigate().back();
			return true;
		}
		
		else{
			SGLogger.info("     Instrcution ["+instruction+"] is not a fixed intruction.");
			return null;
		}
		
		return result;
	}
}
