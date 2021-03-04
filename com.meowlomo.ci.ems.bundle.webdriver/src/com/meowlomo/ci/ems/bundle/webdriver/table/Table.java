package com.meowlomo.ci.ems.bundle.webdriver.table;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.meowlomo.ci.ems.bundle.webdriver.LocatorUtils;

public class Table {
	private int ROW_TAG_INDEX = 0;
	
	private WebDriver driver = null;
	private WebElement tableElement = null;
	private By tableLocator = null;
	private By rowLocator = null;	
	private List<Row> rows = null;
	private TableDataContainer tableDataContainer = null;
	
	Table(WebDriver driver,TableDataContainer tableDataContainer){
		this.tableLocator = LocatorUtils.getLocator(tableDataContainer.getLocatorType(), tableDataContainer.getLocatorValue());
		this.driver = driver;
		this.tableDataContainer = tableDataContainer;
		this.tableElement = this.driver.findElement(tableLocator);
		System.out.println("row locater type "+tableDataContainer.getRowLocatorType());
		System.out.println("row locater value "+tableDataContainer.getRowLocatorValue());
		this.rowLocator = LocatorUtils.getLocator(tableDataContainer.getRowLocatorType(), tableDataContainer.getRowLocatorValue());
		
//		//process all the maps
//		this.processCellMap();
	}
	
	/**
	 * Size.
	 *
	 * @return the int : number of the rows in the table
	 */
	public int size(){
		if(this.rows == null) {
			this.getRows();
		}
		return this.rows.size();
	}
	
	/**
	 * Gets the row.
	 *
	 * @param rowIndex the row index start with 0
	 * @return the row
	 */
	public Row getRow(int rowIndex){
		//tr:nth-child(4)
		//get the row
		//check if the row list is still null, if yes, get all the rows from the table first
		if(this.rows == null) {
			this.rows = this.getRows();
			return this.rows.get(rowIndex);
		}
		else {
			return this.rows.get(rowIndex);
		}
	}
	
	public List<Row> getRows(){
		String rowLocatorType = this.tableDataContainer.getRowLocatorType();
		String rowLocatorValue = this.tableDataContainer.getRowLocatorValue();
		String headerLocatorType = this.tableDataContainer.getHeaderLocatorType();
		String headerLocatorValue = this.tableDataContainer.getHeaderLocatorValue();
		String footerLocatorType = this.tableDataContainer.getFooterLocatorType();
		String footerLocatorValue = this.tableDataContainer.getFooterLocatorValue();
		int headerOffect = 0;
		if(headerLocatorType != null && 
				headerLocatorValue != null && 
				headerLocatorType.equalsIgnoreCase(rowLocatorType) && 
				headerLocatorValue.equalsIgnoreCase(rowLocatorValue)) {
			headerOffect = -1;
		}
		
		int footerOffect = 0;
		if(footerLocatorType != null && 
				footerLocatorValue != null && 
				footerLocatorType.equalsIgnoreCase(rowLocatorType) && 
				footerLocatorValue.equalsIgnoreCase(rowLocatorValue)) {
			footerOffect = -1;
		}
		List<Row> rowList = new ArrayList<Row>();
		List<WebElement> rows = tableElement.findElements(this.rowLocator);
		//need to remove  header and footer
		System.out.println("HEADER COUNT "+headerOffect);
		System.out.println("FOOTER COUNT "+footerOffect);
		System.out.println("TOTAL ROW COUNT "+rows.size());
		for(int elementCount = 0; elementCount < rows.size(); elementCount++){
			WebElement rowWebElement = rows.get(elementCount);
			if(elementCount == 0 && headerOffect == -1) {

			}
			else if(elementCount == rows.size() -1 && footerOffect == -1) {
			
			}
			else {
				Row row = new Row(this,rowWebElement,elementCount,this.tableDataContainer.getBodyColumnData());
				System.out.println("add row");
				rowList.add(row);
			}
		}		
		return rowList;		
	}
	
	public Row getHeader() {		
		By headerLocator = LocatorUtils.getLocator(this.tableDataContainer.getHeaderLocatorType(), this.tableDataContainer.getHeaderLocatorValue());
		WebElement headerWebElement = tableElement.findElement(headerLocator);
		Row headerRow = new Row(this, headerWebElement, Row.HEADER_ROW_INDEX, this.tableDataContainer.getHeaderColumnData());
		return headerRow;
	}
	
	public Row getFooter() {
		By footerLocator = LocatorUtils.getLocator(this.tableDataContainer.getFooterLocatorType(), this.tableDataContainer.getFooterLocatorValue());
		WebElement footerWebElement = tableElement.findElement(footerLocator);
		Row footerRow = new Row(this, footerWebElement, Row.FOOTER_ROW_INDEX, this.tableDataContainer.getFooterColumnData());
		return footerRow;
	}
}
