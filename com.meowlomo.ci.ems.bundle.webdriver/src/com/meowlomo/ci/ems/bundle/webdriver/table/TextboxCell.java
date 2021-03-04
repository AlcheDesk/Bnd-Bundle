package com.meowlomo.ci.ems.bundle.webdriver.table;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.meowlomo.ci.ems.bundle.webdriver.table.Cell;
import com.meowlomo.ci.ems.bundle.webdriver.table.Row;
import com.meowlomo.ci.ems.bundle.webdriver.table.Table;

public class TextboxCell extends Cell{

	public TextboxCell(Row row, int index, String name, By locator, int type) {
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
		return CELL_TYPE_TEXTBOX;
	}

	@Override
	public boolean setCellValue(String value) {
		if(cellElement.getTagName().equalsIgnoreCase("input")) {
			cellElement.sendKeys(value);
		}
		else {
			WebElement inputElement = cellElement.findElement(By.tagName("input"));
			inputElement.sendKeys(value);
		}
		if(cellElement.getText().equals(value)) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public String getCellValue() {
		return cellElement.getText();
	}

	@Override
	public String getCellName() {
		return name;
	}
}
