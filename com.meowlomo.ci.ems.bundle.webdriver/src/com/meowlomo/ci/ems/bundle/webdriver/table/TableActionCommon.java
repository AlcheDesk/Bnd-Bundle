package com.meowlomo.ci.ems.bundle.webdriver.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
//import com.meowlomo.ci.ems.bundle.webdriver.Constant;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;

/*
 * Version information : 1.0
 *
 * Date : 2017-07-24
 *
 * Owner : scott.fu@meowlomo.com	
 * 
 * Auditor
 */

public class TableActionCommon {
	
	
	private static final Logger logger = LoggerFactory.getLogger(TableActionCommon.class);
	
	/**
	 * Select.
	 *
	 * @param driver the webdriver
	 * @param rowTag the html row tag
	 * @param cellTag the html cell tag
	 * @param cellOrderList the cell order list
	 * @param inputValues the input values
	 * @param selectAllRows select one row or multiple rows, true means multiple rows
	 * @return the list
	 */
	public static List<Row> select(WebDriver driver, TableDataContainer tableDataContainer, Map<String,String> inputMaps, boolean selectAllRows) {		
		List<Row> selectResult = new ArrayList<Row>();		
		//create the table object first
		Table table = new Table(driver, tableDataContainer);
		//get all the rows from the table
		List<Row> rows = table.getRows();
		System.out.println("found rows "+rows.size());
		//loop the rows one by one
		rowLoop:
		for(int rowCount = 0; rowCount < rows.size();rowCount++) {
			Row row = rows.get(rowCount);
			List<Cell> cells = row.getCells();
			System.out.println("found cells "+cells.size());
			//matching the expected value one by one
			//get all the keys from the values map
			Set<String> cellNames = inputMaps.keySet();
			cellLoop:
			for(int cellCount = 0; cellCount < cells.size(); cellCount++) {
				Cell cell = cells.get(cellCount);
				String cellName = cell.getCellName();
				if(cellNames.contains(cellName.toLowerCase())) {
					//get the value of the cell
					String cellValue = cell.getCellValue();
					System.out.println("got cell value ["+cellValue+"]");
					if(!TableActionCommon.checkExpectedAndActualMatch(inputMaps.get(cellName), cellValue)) {
						//no matching, break the loop
						SGLogger.info("Got cell value ["+cellValue+"] not matched expected value ["+inputMaps.get(cellName)+"] will skipp this row");
						break cellLoop;
					}
					else {
						//matched,remove from the set
						SGLogger.info("Got cell ["+cellName+"] value ["+cellValue+"] matched expected value ["+inputMaps.get(cellName)+"] will check next cell");
						cellNames.remove(cellName);
					}
				}
			}
			
			///check the final result, if all names are removed from the set, that means the row match
			if(cellNames.isEmpty()) {
				selectResult.add(row);
				if(!selectAllRows) {
					//dont need to search all the rows
					break rowLoop;
				}
			}			
		}
		return selectResult;
	}
	
	/**
	 * Update. update row/rows in the table
	 *
	 * @param driver the driver
	 * @param rowTag the row tag
	 * @param cellTag the cell tag
	 * @param cellOrderList the cell order list
	 * @param selectValues the select values : the value mapping used to locate the row/rows
	 * @param inputValues the input values
	 * @param updateAllRows update all rows or only one row, true means all possiable rows
	 * @return the int : the number of rows got updated
	 * @throws Exception 
	 */
	public static int update (WebDriver driver, TableDataContainer tableDataContainer, 
								Map<String,String> selectValues,
								Map<String,String> inputValues,
								boolean updateAllRows) throws Exception{
		
		/*
		 * steps
		 * 1: select the row/rows first
		 * 2: edit the cell base on the input values
		 */		
		List<Row> targetRows = TableActionCommon.select(driver, tableDataContainer, selectValues, updateAllRows);
		//loop the rows one by one
		rowLoop:
		for(int rowCount = 0; rowCount < targetRows.size(); rowCount++) {
			Row row = targetRows.get(rowCount);
			Set<String> targetCellNames = inputValues.keySet(); 
			//get the cells
			List<Cell> cells = row.getCells();
			//loop the cells
			cellLoop:
			for(int cellCount = 0; cellCount<cells.size(); cellCount++) {
				Cell cell = cells.get(cellCount);
				//check if the cell need to be updated
				String cellName = cell.getCellName();
				if(targetCellNames.contains(cellName.toLowerCase())) {
					//update the cell
					String value = inputValues.get(cellName);
					if (!ContextConstant.matchIgnored(value)) {
						boolean result = cell.setCellValue(value);
						if(result) {
							targetCellNames.remove(cellName);
						}
						else {
							throw new Exception("The value of cell ["+cellName+"] was not updated successfully.");
						}
					}
					else {
						targetCellNames.remove(cellName);
					}
				}
				
				//check all cell name are updated
				if(targetCellNames.isEmpty()) {
					break cellLoop;
				}
			}
		}		
		return targetRows.size();		
	}
	/**此方法针对奇迹分页案例
	 * Paging.  分页:1/计算行列数，当行数超10有分页按钮 
	 *              2/判断第一列第一行单元格值为1,以此保证分页序号连续
	 *
	 * @param driver the webdriver
	 * @param rowTag the html row tag
	 * @param cellTag the html cell tag
	 * @param cellOrderList the cell order list
	 * @param inputValues the input values
	 * 
	 * @return boolean
	 * @author qiaorui.chen
	 */
	public static boolean paging(WebDriver driver, TableDataContainer tableDataContainer, Map<String,String> inputMaps, boolean selectAllRows) {		
		Boolean result=false;
		int rowSize=0;
		List<Cell> cells=null;
		//create the table object first
		Table table = new Table(driver, tableDataContainer);		
		//get all the rows from the table
		List<Row> rows = table.getRows();
		 rowSize= rows.size();
		System.out.println("found rows "+rowSize);
		//当行数超10有分页按钮 
		if(rowSize==10){
			result = checkSpecialContent(driver,"下一页");
			if(!result){
				SGLogger.error("行数超10没有出现分页按钮");
				return false;
			}else{
				SGLogger.info("行数超10出现分页按钮");
			}
		}		
		//get first row	
		Row row = rows.get(0);
		cells = row.getCells();	
		System.out.println("found cells "+cells.size());
		//get all the keys from the values map						
		Set<String> cellNames = inputMaps.keySet();				
		for(int acount=0;acount<cells.size();acount++){
		Cell cell = cells.get(acount);
		String cellName = cell.getCellName();
		System.out.println("cell name: "+cellName);		
		//判断序号是否为"1",若为1证明分页序号连续
		if(cellNames.contains(cellName.toLowerCase())) {
					//get the value of the cell
					String cellValue = cell.getCellValue();
					System.out.println("got cell value ["+cellValue+"]");					
					if(!TableActionCommon.checkExpectedAndActualMatch(inputMaps.get(cellName), cellValue)) {
						//no matching, break the loop
						SGLogger.info("Got cell value ["+cellValue+"] not matched expected value ["+inputMaps.get(cellName)+"] ");
						result=false;
						return result;						
					}
					else {
						//matched,remove from the set
						SGLogger.info("Got cell ["+cellName+"] value ["+cellValue+"] matched expected value ["+inputMaps.get(cellName)+"] ");
						result=true;
					}
				}
		}
        return result;		
	}
	/**
	 * 检查页面上含有特定内容
	 * @author qiaorui.chen
	 * @param driver
	 * @param checkValue 
	 * @return true, if match
	 */
	public static boolean checkSpecialContent(WebDriver driver,String checkValue){
	    boolean status =false;
		try {  
        driver.findElement(By.xpath("//*[contains(.,'" + checkValue + "')]"));  
        System.out.println(checkValue + " is appeard!");
        status=true;
         }catch (NoSuchElementException e) {
    	System.out.println("'" + checkValue + "' doesn't exist!");  
         status=false;
         }
		return status;
    }

	
	
	/**
	 * Check expected and actual match.
	 *
	 * @param expected the expected value
	 * @param actual the actual value
	 * @return true, if match
	 */
	public static boolean checkExpectedAndActualMatch(String expected,String actual){
		if(expected == null){
			if(actual == null){
				return true;
			}else{
				return false;
			}
		}
		
		boolean result = false;
		//exact match 
		if(expected.equals(actual)){
			////System.out.println("i am here 0");
			return true;
		}
		
		if(expected.isEmpty()){
			if(actual.isEmpty()){
				return true;
			}else{
				return false;
			}
		}
		
		//System.out.println(trimedActual);
		boolean expectedIsNumber = StringUtils.isNumeric(expected);
		boolean actualIsNumber = StringUtils.isNumeric(actual);
		if(expectedIsNumber && actualIsNumber){
			NumberUtils.createDouble(expected);
			Double expectedNumber = NumberUtils.createDouble(expected);
			Double actualNumber = NumberUtils.createDouble(actual);		
			if( expectedNumber.compareTo(actualNumber)== 0){
				//System.out.println("i am here 0.9");
				return true;
			}else{
				return false;
			}
		}
		return result;
	}
}
