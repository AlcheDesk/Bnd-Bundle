package com.meowlomo.ci.ems.bundle.webdriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;

import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.InstructionUtils;
//import com.meowlomo.ci.ems.bundle.webdriver.ExcelAdapter;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.webdriver.table.Row;
import com.meowlomo.ci.ems.bundle.webdriver.table.TableActionCommon;
import com.meowlomo.ci.ems.bundle.webdriver.table.TableDataContainer;

/**
 * The Class TableExecutor.
 * use to execute table module instructions
 */
public class TableExecutor {

	private WebDriver driver = null;
	private JSONObject instructionArray = null;
	private JSONObject repoFile = null;
	
	public TableExecutor(WebDriver driver, JSONObject elementMap, JSONObject instructions) {
		this.driver = driver;
		this.instructionArray = instructions;
		this.repoFile = elementMap;
	}
	
	public Iterator<JSONObject> getIndexedObject(JSONArray sheet){
		ArrayList<JSONObject> cells = new ArrayList<JSONObject>();
		for(Object r : sheet) {
			if (null != r && r instanceof JSONObject){
				JSONObject cell = (JSONObject)r;
				cells.add(cell);
			}
		}
		return cells.iterator();
	}
	
	public Iterator<Object> getIndexedObject(JSONArray sheet, String key) {
		ArrayList<Object> cells = new ArrayList<Object>();
		for(Object r : sheet) {
			JSONObject tmpRow = (JSONObject)r;
			Object cell = (Object)tmpRow.get(key);
			cells.add(cell);
		}
		return cells.iterator();
	}
	
	public JSONObject getRow(Iterator<JSONObject> objIterator, String key, String input){
		while(objIterator.hasNext()){
			JSONObject row = objIterator.next();
			if (row.has(key) && row.getString(key).equals(input)){
				return row;
			}
		}
		return null;
	}
	
	public HashMap<String, String> jsonObject2MapString(JSONObject json){
		Map<String, Object> map = json.toMap();
		
		HashMap<String, String> hashMap = new HashMap<String, String>();
		Set<Entry<String, Object>> sets = map.entrySet();
		Iterator<Entry<String, Object>> iter = sets.iterator();
		
		if (iter.hasNext()){
			Entry<String, Object> entry = iter.next();
			hashMap.put(entry.getKey(), (String)entry.getValue());
		}
		
		return hashMap;
	}
	
	public HashMap<String, Integer> jsonObject2MapInteger(JSONObject json){
		Map<String, Object> map = json.toMap();
		
		HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
		Set<Entry<String, Object>> sets = map.entrySet();
		Iterator<Entry<String, Object>> iter = sets.iterator();
		
		if (iter.hasNext()){
			Entry<String, Object> entry = iter.next();
			hashMap.put(entry.getKey(), new Integer((String)entry.getValue()));
		}
		
		return hashMap;
	}
	
	public ColumnDataContainer JSON2ColumnDataContainer(JSONObject columns){
		ColumnDataContainer cdc = new ColumnDataContainer();
		
		JSONObject element = columns.getJSONObject("element");
		JSONObject index = columns.getJSONObject("index");
		JSONObject locatorString = columns.getJSONObject("locator-value");
		JSONObject locatorType = columns.getJSONObject("locator-type");
		
		HashMap<String, String> elementMap = jsonObject2MapString(element);
		HashMap<String, Integer> indexMap = jsonObject2MapInteger(index);
		HashMap<String, String> locatorStringMap = jsonObject2MapString(locatorString);
		HashMap<String, String> locatorTypeMap = jsonObject2MapString(locatorType);

		cdc.setCellNameElementTypeMap(elementMap);
		cdc.setCellNameIndexMap(indexMap);
		cdc.setCellNameLocatorStringMap(locatorStringMap);
		cdc.setCellNameElementTypeMap(locatorTypeMap);
		return cdc;
	}

	private TableDataContainer setHeader(TableDataContainer tdc, JSONObject table){
		JSONObject footer = table.getJSONObject("footer");
		String footerType = footer.getString("locator-type");
		String footerValue = footer.getString("locator-value");
		JSONObject footerColumns = footer.getJSONObject("columns");
		ColumnDataContainer footerCdc = JSON2ColumnDataContainer(footerColumns);
		
		tdc.setFooterColumnData(footerCdc);
		tdc.setFooterLocatorType(footerType);
		tdc.setFooterLocatorValue(footerValue);
		return tdc;
	}
	
	private TableDataContainer setFooter(TableDataContainer tdc, JSONObject table){
		JSONObject header = table.getJSONObject("header");
		String headerType = header.getString("locator-type");
		String headerValue = header.getString("locator-value");
		JSONObject headerColumns = header.getJSONObject("columns");
		ColumnDataContainer headerCdc = JSON2ColumnDataContainer(headerColumns);
		
		tdc.setHeaderColumnData(headerCdc);
		tdc.setHeaderLocatorType(headerType);
		tdc.setHeaderLocatorValue(headerValue);
		return tdc;
	}
	
	public TableDataContainer toTableContainer(JSONObject table) throws Exception{
		//TODO
		TableDataContainer tdc = new TableDataContainer();
		tdc = setHeader(tdc, table);
		tdc = setFooter(tdc, table);

		JSONObject columns = table.getJSONObject("columns");
		ColumnDataContainer bodyCdc = JSON2ColumnDataContainer(columns);
		
		tdc.setBodyColumnData(bodyCdc);
		tdc.setName(table.getString("name"));
		tdc.setLocatorType(table.getString("locator-type"));
		tdc.setLocatorValue(table.getString("locator-value"));
		tdc.setRowLocatorType(table.getString("row-locator-type"));
		tdc.setRowLocatorValue(table.getString("row-locator-value"));

		return tdc;
	}
	
	public Map<String, String> getTargetObject(JSONArray headers, Iterator<JSONObject> IdColoumnCells, String targetVal) throws Exception{
		JSONObject selectDataRow = getRow(IdColoumnCells, "ID", targetVal);
		if (null == selectDataRow){
			throw new Exception("Excel 表格中没有找到ID为" + targetVal + "的行。");
		}
		Map<String, String> selectInputValues = new HashMap<String, String>();
		for(Object header : headers){
			String columnName = (String)header;
			if(!columnName.equalsIgnoreCase("id")) {
				String cellValue = selectDataRow.has(columnName) ? selectDataRow.getString(columnName) : "";
				selectInputValues.put(columnName, cellValue);
			}
		}
		return selectInputValues;
	}
	
	public boolean excute(String instructionObject, String action, String input, InstructionOptions options ) throws Exception{
		//decode the instruction object info from the input
		Map<String, String> instructionObjectInfoMap = InstructionUtils.decodeTargetObjectString(instructionObject);	
		JSONArray sheet = instructionArray.getJSONArray(instructionObjectInfoMap.get("elementname"));
		JSONArray tabNameRow = sheet.getJSONArray(0);
		Iterator<JSONObject> IdColoumnCells = getIndexedObject(sheet);
		JSONObject repoDataContainer = repoFile.getJSONObject("tables");
		JSONObject targetTableDataContainer = repoDataContainer.getJSONObject(instructionObjectInfoMap.get("elementname"));
		String rowTag = targetTableDataContainer.getString("row-locator-value");
		
		//execute the table instruction
		if(action.equalsIgnoreCase("select")) {
			Map<String,String> selectInputValues = getTargetObject(tabNameRow, IdColoumnCells, input);
			List<Row> selectResult = TableActionCommon.select(driver, toTableContainer(targetTableDataContainer), selectInputValues, options.existOption(ContextConstant.ALL));
			System.out.println("table select result "+selectResult.size());
			return 0 != selectResult.size();
		}
		else if(action.equalsIgnoreCase("paging")) {
			Map<String,String> selectInputValues = new HashMap<String,String>();
			boolean pagingResult = TableActionCommon.paging(driver, toTableContainer(targetTableDataContainer), selectInputValues, options.existOption(ContextConstant.ALL));	
			System.out.println("paging Result "+pagingResult);
			return  pagingResult;		
		}
		else if(action.equalsIgnoreCase("update")) {
			String[] inputValues = input.split(","); 
			System.out.println("update input "+Arrays.asList(inputValues));
			Map<String,String> selectInputValues = getTargetObject(tabNameRow, IdColoumnCells, inputValues[0]);
			Map<String,String> updateInputValues = getTargetObject(tabNameRow, IdColoumnCells, inputValues[0]);
			System.out.println(selectInputValues.toString());
			int updateResult = TableActionCommon.update(driver, toTableContainer(targetTableDataContainer), selectInputValues, updateInputValues,options.existOption(ContextConstant.ALL));
			System.out.println("update result "+updateResult);
			return 0 != updateResult;
		}
		else{
			SGLogger.error("Action ["+action+"] is not supported by table module");
			return false;
		}
	}
	
	private ArrayList<ArrayList<LinkedHashMap<String, String>>> readExcelTestCase(JSONObject wb){
		ArrayList<ArrayList<LinkedHashMap<String, String>>> testCases = new ArrayList<ArrayList<LinkedHashMap<String, String>>>();
		//TODO 
		return testCases;
//		//get the instruction sheet
//		Sheet instructionSheet = wb.getSheet("Instructions");
//		if(instructionSheet != null){
//			testCases = this.readInstrcutionsFromSheet(instructionSheet);
//		}else{
//			SGLogger.error(this.excelFilePath + " 文件 没有包含instrcution表。");
//		}
//		return testCases;
	}
	
	public ArrayList<ArrayList<LinkedHashMap<String, String>>> readInstrcutionsFromSheet(JSONObject sheet){
		ArrayList<ArrayList<LinkedHashMap<String, String>>> instrcutions = new ArrayList<ArrayList<LinkedHashMap<String, String>>>();
		//TODO
		return instrcutions;
		//processor the instuction sheet
		//get the column first
//		Row tabnameRow =  sheet.getRow(0);
//		//get the Column Cells
//		Cell commentCell = excelAdapter.getCell(tabnameRow, "Comment");
//		Cell actionCell =excelAdapter.getCell(tabnameRow, "Action");
//		Cell inputCell = excelAdapter.getCell(tabnameRow, "Input");
//		Cell optionCell = excelAdapter.getCell(tabnameRow, "Options");
//		Cell objectCell = excelAdapter.getCell(tabnameRow, "Object");
//		if(	actionCell != null ){
//			//get the Columns
//			Iterator<Cell> actionColumns = excelAdapter.getColoumn(sheet, actionCell.getColumnIndex());
//			List<Cell> actionCellList = new ArrayList<Cell>();
//			actionColumns.forEachRemaining(actionCellList::add);
//			Iterator<Cell> inputColumns = excelAdapter.getColoumn(sheet, inputCell.getColumnIndex());
//			List<Cell> inputCellList = new ArrayList<Cell>();
//			inputColumns.forEachRemaining(inputCellList::add);
//			Iterator<Cell> optionColumns = excelAdapter.getColoumn(sheet, optionCell.getColumnIndex());
//			List<Cell> optionCellList = new ArrayList<Cell>();
//			optionColumns.forEachRemaining(optionCellList::add);
//			Iterator<Cell> objectColumns = excelAdapter.getColoumn(sheet, objectCell.getColumnIndex());
//			List<Cell> objectCellList = new ArrayList<Cell>();
//			objectColumns.forEachRemaining(objectCellList::add);
//			instrcutions = this.excelContentProcessor(objectCellList,actionCellList,inputCellList,optionCellList);
//		}else{
//			SGLogger.info("      在当前Excel表格中没有找到Action列。");
//		}
//		return instrcutions;
	}
	
	private ArrayList<ArrayList<LinkedHashMap<String,String>>> excelContentProcessor(List<JSONObject>objectCellList
			, List<JSONObject>actionCellList
			, List<JSONObject>inputCellList
			, List<JSONObject> optionCellList ){
		ArrayList<ArrayList<LinkedHashMap<String,String>>> testCases = new ArrayList<ArrayList<LinkedHashMap<String,String>>>();
		//TODO
		return testCases;
//		if(actionCellList.size() == inputCellList.size()){
//			//loop through the list
//			ArrayList<LinkedHashMap<String, String>> testCase = null;
//			String testCaseName = "";
//			boolean testCaseStarted = false;
//			boolean testCaseEnded = true;
//			FileLoop:
//			for(int i = 0; i < actionCellList.size(); i++){
//				Cell objectCell = objectCellList.get(i);
//				Cell actionCell = actionCellList.get(i);
//				Cell inputCell = inputCellList.get(i);
//				Cell optionCell = optionCellList.get(i);				
//				String objectCommand = excelAdapter.getCellValue(objectCell);
//				//check empty actions
//				if(objectCommand != null){
//					String excelRowID = String.valueOf(objectCell.getRowIndex());
//					String actionCommand = excelAdapter.getCellValue(actionCell);
//					String inputCommand = excelAdapter.getCellValue(inputCell);
//					String optionCommand = excelAdapter.getCellValue(optionCell);
//					if(objectCommand.equals("File.End") && inputCommand.equals("File.End")){
//						SGLogger.info("      Found [File.End]. Finish test case loading.");
//						break FileLoop;
//					}else if(objectCommand.equals("TestCase.Start")){
//						if(testCaseEnded){
//							testCase = new ArrayList<LinkedHashMap<String,String>>();
//							testCaseStarted = true;
//							testCaseName = inputCommand;
//							testCaseEnded = false;
//							SGLogger.info(" Start loading a test case named ["+inputCommand+"] in the excel file ["+excelFilePath+"]");
//							LinkedHashMap<String,String> oneRowData = new LinkedHashMap<String,String>();
//							oneRowData.put("Object", objectCommand);
//							oneRowData.put("Action", actionCommand);
//							oneRowData.put("Input", inputCommand);
//							oneRowData.put("Options", optionCommand);
//							oneRowData.put("ExcelRowID", excelRowID);
//							testCase.add(oneRowData);
//						}else{
//							SGLogger.error(excelFilePath + " is trying to start a new test case before ending the previous test case");
//						}
//					}else if(objectCommand.equals("TestCase.End")){
//						if(testCaseStarted && !testCaseName.isEmpty() && !testCaseEnded){
//							if(!inputCommand.equals(testCaseName)){
//								SGLogger.error(excelFilePath + " is trying to end a test case ["+testCaseName+"] which is not started");
//							}else{
//								LinkedHashMap<String,String> oneRowData = new LinkedHashMap<String,String>();
//								oneRowData.put("Object", objectCommand);
//								oneRowData.put("Action", actionCommand);
//								oneRowData.put("Input", inputCommand);
//								oneRowData.put("Options", optionCommand);
//								oneRowData.put("ExcelRowID", excelRowID);
//								testCase.add(oneRowData);
//								testCases.add(testCase);
//								testCase = null;
//								testCaseStarted = false;
//								testCaseEnded = true;
//								SGLogger.info(" End loading a test case named ["+inputCommand+"] in the excel file ["+excelFilePath+"]");
//							}
//						}
//					}else if(testCaseStarted && !testCaseEnded){
//						LinkedHashMap<String,String> oneRowData = new LinkedHashMap<String,String>();
//						oneRowData.put("Object", objectCommand);
//						oneRowData.put("Action", actionCommand);
//						oneRowData.put("Input", inputCommand);
//						oneRowData.put("Options", optionCommand);
//						oneRowData.put("ExcelRowID", excelRowID);
//						testCase.add(oneRowData);
//					}
//				}else{
//					SGLogger.info("      An empty actions instruction is found. The row will be ignored.");
//				}
//			}
//		}else{
//			SGLogger.error(this.excelFilePath + " has different numbers for Action and input");
//		}
//		
//		if(testCases.isEmpty()){
//			SGLogger.error("    Cent find a completed test case in the excel file. Please the excel file to make sure test cases are properly started and ended.");
//		}
//		return testCases;
	}
	
	private boolean isTestCaseStartOrEnd(String instruction){
		if(instruction.equals("TestCase.Start")){
			return true;
		}else if(instruction.equals("TestCase.End")){
			return true;
		}else{
			return false;
		}
	}
	
	private String trimInstruction(String orgInstruction){
		Pattern re = Pattern.compile("^(AOTAIN\\.)(BDP\\.)?(.*Section\\.)*(.*?)$");
		Matcher m = re.matcher(orgInstruction);
		if(m.find()){
			orgInstruction = m.group(1)+((m.group(2) == null)? "":m.group(2))+m.group(4);
		}
		
		return orgInstruction;
	}
}
