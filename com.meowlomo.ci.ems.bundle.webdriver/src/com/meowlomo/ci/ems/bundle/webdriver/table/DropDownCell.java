package com.meowlomo.ci.ems.bundle.webdriver.table;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import com.meowlomo.ci.ems.bundle.webdriver.table.Cell;
import com.meowlomo.ci.ems.bundle.webdriver.table.Row;
import com.meowlomo.ci.ems.bundle.webdriver.table.Table;

public class DropDownCell  extends Cell{

	public DropDownCell(Row row, int index, String name, By locator, int type) {
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
		return CELL_TYPE_DROPDOWN;
	}

	@Override
	public boolean setCellValue(String value) {
		Select select = new Select(cellElement);
		select.selectByVisibleText(value);
		String selectedValue = select.getFirstSelectedOption().getText();
		if(selectedValue.equals(value)) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public String getCellValue() {
		Select select = new Select(cellElement);
		return select.getFirstSelectedOption().getText();
	}

	@Override
	public String getCellName() {
		return name;
	}
}
