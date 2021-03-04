package com.meowlomo.ci.ems.bundle.webdriver.table;

import com.meowlomo.ci.ems.bundle.webdriver.ColumnDataContainer;

public class TableDataContainer {
	private String name = null;
	private String locatorType = null;
	private String locatorValue = null;
	
	private String rowLocatorType = null;
	private String rowLocatorValue = null;
	
	
	private String headerLocatorType = null;
	private String headerLocatorValue = null;	
	private String footerLocatorType = null;
	private String footerLocatorValue = null;
	
	private ColumnDataContainer bodyColumnData = null;
	private ColumnDataContainer headerColumnData = null;
	private ColumnDataContainer footerColumnData = null;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLocatorType() {
		return locatorType;
	}
	public void setLocatorType(String locatorType) {
		this.locatorType = locatorType;
	}
	public String getLocatorValue() {
		return locatorValue;
	}
	public void setLocatorValue(String locatorValue) {
		this.locatorValue = locatorValue;
	}
	public String getRowLocatorType() {
		return rowLocatorType;
	}
	public void setRowLocatorType(String rowLocatorType) {
		this.rowLocatorType = rowLocatorType;
	}
	public String getRowLocatorValue() {
		return rowLocatorValue;
	}
	public void setRowLocatorValue(String rowLocatorValue) {
		this.rowLocatorValue = rowLocatorValue;
	}
	public String getHeaderLocatorType() {
		return headerLocatorType;
	}
	public void setHeaderLocatorType(String headerLocatorType) {
		this.headerLocatorType = headerLocatorType;
	}
	public String getHeaderLocatorValue() {
		return headerLocatorValue;
	}
	public void setHeaderLocatorValue(String headerLocatorValue) {
		this.headerLocatorValue = headerLocatorValue;
	}
	public String getFooterLocatorType() {
		return footerLocatorType;
	}
	public void setFooterLocatorType(String footerLocatorType) {
		this.footerLocatorType = footerLocatorType;
	}
	public String getFooterLocatorValue() {
		return footerLocatorValue;
	}
	public void setFooterLocatorValue(String footerLocatorValue) {
		this.footerLocatorValue = footerLocatorValue;
	}
	public ColumnDataContainer getBodyColumnData() {
		return bodyColumnData;
	}
	public void setBodyColumnData(ColumnDataContainer bodyColumnData) {
		this.bodyColumnData = bodyColumnData;
	}
	public ColumnDataContainer getHeaderColumnData() {
		return headerColumnData;
	}
	public void setHeaderColumnData(ColumnDataContainer headerColumnData) {
		this.headerColumnData = headerColumnData;
	}
	public ColumnDataContainer getFooterColumnData() {
		return footerColumnData;
	}
	public void setFooterColumnData(ColumnDataContainer footerColumnData) {
		this.footerColumnData = footerColumnData;
	}


}
