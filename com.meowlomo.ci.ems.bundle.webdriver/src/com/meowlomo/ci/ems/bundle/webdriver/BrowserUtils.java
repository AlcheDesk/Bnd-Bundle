package com.meowlomo.ci.ems.bundle.webdriver;

import java.io.IOException;

public class BrowserUtils {
	public static void endCurrentBrowserSession(String browser) throws IOException{
		if (null != browser){ 
			if (browser.equalsIgnoreCase("Chrome")){
				BrowserUtils.run_DOS_Command("taskkill /f /im chrome.exe");
				BrowserUtils.run_DOS_Command("taskkill /f /im chromedriver.exe");
				BrowserUtils.run_DOS_Command("taskkill /f /im chromedriver-windows-32.exe");
				BrowserUtils.run_DOS_Command("taskkill /f /im chromedriver-windows-64.exe");
				return;
			}else if (browser.equalsIgnoreCase("Edge")){
				BrowserUtils.run_DOS_Command("taskkill /f /im \"Microsoft Edge\"");
				BrowserUtils.run_DOS_Command("taskkill /f /im \"Microsoft Web Driver\"");
				return;
			}else {
				BrowserUtils.run_DOS_Command("taskkill /f /im firefox.exe");
			}
		}else {
			BrowserUtils.run_DOS_Command("taskkill /f /im firefox.exe");			
		}
		
		BrowserUtils.run_DOS_Command("taskkill /f /im geckodriver.exe");
		BrowserUtils.run_DOS_Command("taskkill /f /im geckodriver-windows-64.exe");
		BrowserUtils.run_DOS_Command("taskkill /f /im geckodriver-windows-32.exe");
		
	}

	/**
	 * Run the input DOS command.  When debugging, use "pb.redirectOutput(Redirect.INHERIT); pb.redirectError(Redirect.INHERIT);"
	 */
	private static void run_DOS_Command(String command) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
		pb.start();
	}
	
}
