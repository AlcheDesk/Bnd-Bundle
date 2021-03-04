package com.meowlomo.ci.ems.bundle.webdriver.table;

import org.openqa.selenium.By;

import com.meowlomo.ci.ems.bundle.webdriver.table.Cell;
import com.meowlomo.ci.ems.bundle.webdriver.table.Row;
import com.meowlomo.ci.ems.bundle.webdriver.table.Table;

public class TextCell  extends Cell{

	public TextCell(Row row, int index, String name, By locator, int type) {
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
		return CELL_TYPE_TEXT;
	}

	@Override
	public boolean setCellValue(String value) {
		return true;
	}

	@Override
	public String getCellValue() {
		//find 
		return cellElement.getText();
	}

	@Override
	public String getCellName() {
		return name;
	}
}
