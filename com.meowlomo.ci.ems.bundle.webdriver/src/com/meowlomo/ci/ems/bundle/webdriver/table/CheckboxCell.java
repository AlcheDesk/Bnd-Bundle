package com.meowlomo.ci.ems.bundle.webdriver.table;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.meowlomo.ci.ems.bundle.webdriver.table.Cell;
import com.meowlomo.ci.ems.bundle.webdriver.table.Row;
import com.meowlomo.ci.ems.bundle.webdriver.table.Table;

public class CheckboxCell  extends Cell{

	public CheckboxCell(Row row, int index, String name, By locator, int type) {
		super(row, index, name, locator, type);
		// TODO Auto-generated constructor stub
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
		return CELL_TYPE_CHECKBOX;
	}

	@Override
	public boolean setCellValue(String value) {
		
		if(cellElement.getTagName().equals("input") && cellElement.getAttribute("type").equals("checkbox")) {
			cellElement.click();
			if(cellElement.isSelected()) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			WebElement checkboxElement = cellElement.findElement(By.xpath("input[@type=\"checkbox\"]"));
			checkboxElement.click();
			if(checkboxElement.isSelected()) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	@Override
	public String getCellValue() {
		if(cellElement.getTagName().equals("input") && cellElement.getAttribute("type").equals("checkbox")) {
			if(cellElement.isSelected()) {
				return "true";
			}
			else {
				return "false";
			}
		}
		else {
			WebElement checkboxElement = cellElement.findElement(By.xpath("input[@type=\"checkbox\"]"));
			if(checkboxElement.isSelected()) {
				return "true";
			}
			else {
				return "false";
			}
		}
	}

	@Override
	public String getCellName() {
		return name;
	}
}
