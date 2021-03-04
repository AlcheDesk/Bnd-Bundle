package com.meowlomo.ci.ems.bundle.webdriver.table;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class Cell {
	
	public static final int CELL_TYPE_TEXT = 0;
	public static final int CELL_TYPE_BUTTON = 1;
	public static final int CELL_TYPE_LINK = 2;
	public static final int CELL_TYPE_DROPDOWN = 3;
	public static final int CELL_TYPE_RADIO = 4;
	public static final int CELL_TYPE_TEXTBOX = 5;
    /**
     * @author yanfang.chen
     * todo
     */
	public static final int CELL_TYPE_FILEUP = 6;
	public static final int CELL_TYPE_CHECKBOX = 7;
	public static final int CELL_TYPE_FILEDOWN = 8;
	
	protected int index = 0;
	protected WebElement rowElement = null;
	protected By locator = null;
	protected String name = null;
	protected int type;
	protected WebElement cellElement = null;
	protected Row row = null;
	protected int rowIndex;
	
	public Cell(Row row, int index, String name, By locator, int type){
		this.name = name;
		this.locator = locator;
		this.type = type;
		this.rowElement = row.getRowElement();
		this.index = index;
		this.row = row;
		this.rowIndex = this.row.getRowIndex();
		setCellElement(rowElement, index, locator);
	}

    public abstract int getColumnIndex();
    public abstract int getRowIndex();
    public abstract Table getTable();
    public abstract Row getRow();
    public abstract int getCellType();
    public abstract boolean setCellValue(String value);
    public abstract String getCellValue();
    public abstract String getCellName();
    
    private void setCellElement(WebElement rowElement, int index, By locator){
    	List<WebElement> cellElements = rowElement.findElements(locator);
    	//loop through the cell elements to find the one has the only child element
    	for(int cellCount = 0 ; cellCount < cellElements.size(); cellCount++){
    		if(cellCount == index){
    			this.cellElement = cellElements.get(cellCount);
    		}
    	}
    }
}
