package com.meowlomo.ci.ems.bundle.webdriver.table;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.meowlomo.ci.ems.bundle.webdriver.table.Cell;
import com.meowlomo.ci.ems.bundle.webdriver.table.Row;
import com.meowlomo.ci.ems.bundle.webdriver.table.Table;

public class RadioCell  extends Cell{

	public RadioCell(Row row, int index, String name, By locator, int type) {
		super(row, index, name, locator, type);
	}

	@Override
	public int getColumnIndex() {
		return index;
	}

	@Override
	public int getRowIndex() {
		return row.getRowIndex();
	}

	@Override
	public Table getTable() {
		return row.getTable();
	}

	@Override
	public Row getRow() {
		return row;
	}

	@Override
	public int getCellType() {
		return CELL_TYPE_RADIO;
	}

	@Override
	public boolean setCellValue(String value) {		
		List<WebElement> radioElements = cellElement.findElements(By.xpath("input[@type=\"radio\"]"));
		for(int i = 0 ; i < radioElements.size(); i++) {
			WebElement radioElement = radioElements.get(i);
			if(radioElement.isSelected()) {
				String radioValue =  radioElement.getAttribute("value");
				if(radioValue.equalsIgnoreCase(value)) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public String getCellValue() {
		List<WebElement> radioElements = cellElement.findElements(By.xpath("input[@type=\"radio\"]"));
		for(int i = 0 ; i < radioElements.size(); i++) {
			WebElement radioElement = radioElements.get(i);
			if(radioElement.isSelected()) {
				return radioElement.getAttribute("value");
			}
		}
		return null;
	}

	@Override
	public String getCellName() {
		return name;
	}
}
