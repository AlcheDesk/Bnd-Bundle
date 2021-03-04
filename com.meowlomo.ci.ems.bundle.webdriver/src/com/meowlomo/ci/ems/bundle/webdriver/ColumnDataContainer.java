package com.meowlomo.ci.ems.bundle.webdriver;

import java.util.HashMap;

import org.openqa.selenium.By;

/**
 * The Class ColumnDataContainer.
 * this class used to store the data maps which contain different info and can be accessible by the column name 
 */
public class ColumnDataContainer {
	private HashMap<String,String> cellNameLocatorTypeMap = new HashMap<String,String> ();
	private HashMap<String,String> cellNameLocatorStringMap = new HashMap<String,String> ();
	private HashMap<String,String> cellNameElementTypeMap = new HashMap<String,String> ();
	private HashMap<String,Integer> cellNameIndexMap = new HashMap<String,Integer> ();
	
	ColumnDataContainer(HashMap<String,String> cellNameLocatorTypeMap,
						HashMap<String,String> cellNameLocatorStringMap,
						HashMap<String,String> cellNameElementTypeMap){
		this.cellNameLocatorTypeMap = cellNameLocatorTypeMap;
		this.cellNameLocatorStringMap =cellNameLocatorStringMap;
		this.cellNameElementTypeMap = cellNameElementTypeMap;
	}
	
	ColumnDataContainer(){
		
	}

	public HashMap<String, String> getCellNameLocatorTypeMap() {
		return cellNameLocatorTypeMap;
	}

	public void setCellNameLocatorTypeMap(HashMap<String, String> cellNameLocatorTypeMap) {
		this.cellNameLocatorTypeMap = cellNameLocatorTypeMap;
	}

	public HashMap<String, String> getCellNameLocatorStringMap() {
		return cellNameLocatorStringMap;
	}

	public void setCellNameLocatorStringMap(HashMap<String, String> cellNameLocatorStringMap) {
		this.cellNameLocatorStringMap = cellNameLocatorStringMap;
	}

	public HashMap<String, String> getCellNameElementTypeMap() {
		return cellNameElementTypeMap;
	}

	public void setCellNameElementTypeMap(HashMap<String, String> cellNameElementTypeMap) {
		this.cellNameElementTypeMap = cellNameElementTypeMap;
	}

	public HashMap<String, Integer> getCellNameIndexMap() {
		return cellNameIndexMap;
	}

	public void setCellNameIndexMap(HashMap<String, Integer> cellNameIndexMap) {
		this.cellNameIndexMap = cellNameIndexMap;
	}
}
