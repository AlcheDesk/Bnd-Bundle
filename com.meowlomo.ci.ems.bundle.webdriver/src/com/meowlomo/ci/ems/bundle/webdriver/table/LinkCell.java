package com.meowlomo.ci.ems.bundle.webdriver.table;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.meowlomo.ci.ems.bundle.webdriver.table.Cell;
import com.meowlomo.ci.ems.bundle.webdriver.table.Row;
import com.meowlomo.ci.ems.bundle.webdriver.table.Table;

public class LinkCell  extends Cell{

	public LinkCell(Row row, int index, String name, By locator, int type) {
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
		return CELL_TYPE_LINK;
	}

	@Override
	public boolean setCellValue(String value) {
		boolean b = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("t") || 
				value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("y") || 
				value.equalsIgnoreCase("click");
		if(b) {
			System.out.println("click the cell");
			if(cellElement.getTagName().equalsIgnoreCase("a")) {
				cellElement.click();
			}
			else {
				WebElement linkElement = cellElement.findElement(By.tagName("a"));
				linkElement.click();
			}
		}
		return true;
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
