package com.meowlomo.ci.ems.bundle.webdriver.table;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.meowlomo.ci.ems.bundle.webdriver.LocatorUtils;
import com.meowlomo.ci.ems.bundle.webdriver.ColumnDataContainer;
import com.meowlomo.ci.ems.bundle.webdriver.table.ButtonCell;
import com.meowlomo.ci.ems.bundle.webdriver.table.CheckboxCell;
import com.meowlomo.ci.ems.bundle.webdriver.table.DropDownCell;
import com.meowlomo.ci.ems.bundle.webdriver.table.FileDownCell;
import com.meowlomo.ci.ems.bundle.webdriver.table.FileUpCell;
import com.meowlomo.ci.ems.bundle.webdriver.table.LinkCell;
import com.meowlomo.ci.ems.bundle.webdriver.table.RadioCell;
import com.meowlomo.ci.ems.bundle.webdriver.table.TextCell;
import com.meowlomo.ci.ems.bundle.webdriver.table.TextboxCell;


public class Row{
	public static final int HEADER_ROW_INDEX = -1;
	public static final int FOOTER_ROW_INDEX = -2;

	
    private int rowIndex = 0;
	//this is the map contains <column name, locator type>
	private HashMap<String,String> colNameLocatorTypeMap = new HashMap<String,String>();
	//this is the map contains <column name, locator value>
	private HashMap<String,String> colNameLocatorValueMap = new HashMap<String,String>();
	//this is the map contains <column name, element type>
	private HashMap<String,String> colNameElementTypeMap = new HashMap<String,String>();
	//this is the map contains <column name, column index>
	private HashMap<String,Integer> colNameColIndexMap = new HashMap<String,Integer>();
	private WebElement rowElement = null;
	private List<Cell> cellList = null;
	private ColumnDataContainer columnDataContainer = null;
	private Table table = null;
	
	
	//creator of the row
	Row(Table table, WebElement rowElement, int rowIndex, ColumnDataContainer columnDataContainer){
		this.table = table;
		this.rowElement = rowElement;
		this.rowIndex = rowIndex;
		this.columnDataContainer = columnDataContainer;
		this.colNameElementTypeMap = this.columnDataContainer.getCellNameElementTypeMap();
		this.colNameLocatorTypeMap = this.columnDataContainer.getCellNameLocatorTypeMap();
		this.colNameLocatorValueMap = this.columnDataContainer.getCellNameLocatorStringMap();
		this.colNameColIndexMap = this.columnDataContainer.getCellNameIndexMap();
	}
	
	public Cell getCell(String cellName){
		//loop the cell
		for(int i = 0 ; i < this.cellList.size(); i++){
			Cell cell = this.cellList.get(i);
			if(cell.getCellName().equals(cellName)){
				return cell;
			}
		}
		return null;
	}
	
	public List<Cell> getCells(){
		if(this.cellList == null) {
			this.generateCells();
		}
		return this.cellList;		
	}
	
	private void generateCells(){
		List<Cell> cellList = new ArrayList<Cell>();
		Set<String> colNames = this.columnDataContainer.getCellNameLocatorStringMap().keySet();
		for(String colName : colNames){
			//get the locator
			By locator = LocatorUtils.getLocator(this.colNameLocatorTypeMap.get(colName), this.colNameLocatorValueMap.get(colName));
			//get the type 
			String type = this.colNameElementTypeMap.get(colName);
			int index = this.colNameColIndexMap.get(colName);
			//generate the cell
			Cell cell = this.generateCell(this, index, colName, locator, type);
			cellList.add(cell);
		}
		this.cellList =  cellList;
	}
	
	
	
	private Cell generateCell(Row row,int index, String name, By locator, String type){
		switch (type.toLowerCase()){
		case "textbox" :
			return new TextboxCell(row,index, name,locator,Cell.CELL_TYPE_TEXTBOX);
		case "button" :
			return new ButtonCell(row,index, name,locator,Cell.CELL_TYPE_BUTTON);
		case "link" :
			return new LinkCell(row,index, name,locator,Cell.CELL_TYPE_LINK);
		case "dropdown" :
			return new DropDownCell(row,index, name,locator,Cell.CELL_TYPE_DROPDOWN);
		case "radio" :
			return new RadioCell(row,index, name,locator,Cell.CELL_TYPE_RADIO);
		case "text" :
			return new TextCell(row,index, name,locator,Cell.CELL_TYPE_TEXT);
			
		/**
		 * @author yanfang.chen
		 */
		case "fileup" :
			return new FileUpCell(row, index, name, locator, Cell.CELL_TYPE_FILEUP);
		case "filedown" :
			return new FileDownCell(row, index, name, locator, Cell.CELL_TYPE_FILEDOWN);
		case "checkbox" :
			return new CheckboxCell(row, index, name, locator, Cell.CELL_TYPE_CHECKBOX);
		default  :
			return new TextCell(row,index, name,locator,Cell.CELL_TYPE_TEXT);
		}
	}

	public HashMap<String, String> getColNameLocatorTypeMap() {
		return colNameLocatorTypeMap;
	}

	public void setColNameLocatorTypeMap(HashMap<String, String> colNameLocatorTypeMap) {
		this.colNameLocatorTypeMap = colNameLocatorTypeMap;
	}

	public HashMap<String, String> getColNameLocatorValueMap() {
		return colNameLocatorValueMap;
	}

	public void setColNameLocatorValueMap(HashMap<String, String> colNameLocatorValueMap) {
		this.colNameLocatorValueMap = colNameLocatorValueMap;
	}

	public HashMap<String, String> getColNameElementTypeMap() {
		return colNameElementTypeMap;
	}

	public void setColNameElementTypeMap(HashMap<String, String> colNameElementTypeMap) {
		this.colNameElementTypeMap = colNameElementTypeMap;
	}

	public HashMap<String, Integer> getColNameColIndexMap() {
		return colNameColIndexMap;
	}

	public void setColNameColIndexMap(HashMap<String, Integer> colNameColIndexMap) {
		this.colNameColIndexMap = colNameColIndexMap;
	}

	public WebElement getRowElement() {
		return rowElement;
	}

	public void setRowElement(WebElement rowElement) {
		this.rowElement = rowElement;
	}

	public List<Cell> getCellList() {
		return cellList;
	}

	public void setCellList(List<Cell> cellList) {
		this.cellList = cellList;
	}

	public ColumnDataContainer getColumnDataContainer() {
		return columnDataContainer;
	}

	public int getRowIndex() {
		return rowIndex;
	}

	public Table getTable() {
		return table;
	}
}
