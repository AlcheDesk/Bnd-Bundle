package com.meowlomo.ci.ems.bundle.webdriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

//import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
//import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.meowlomo.ci.ems.bundle.webdriver.table.TableDataContainer;

public class RepoXMLLoader {
	private RepoLoader repoLoader;
	
	
	public RepoXMLLoader(RepoLoader repoLoader){
		this.repoLoader = repoLoader;
	}
	
	public Map<String,Map<String,String>> loadRepoXML(URL repoXML) {
		Map<String,Map<String,String>> repoMap = new HashMap<String,Map<String,String>>();
		org.dom4j.Document doc;
		try {
			doc = this.repoLoader.parse(repoXML);
			 Iterator<Element> projects = repoLoader.loadProjects(doc.getRootElement());
			 while(projects.hasNext()){
				 Element project = projects.next();
				 String projectName = project.attributeValue("name");
				 Iterator<Element> applications = repoLoader.loadApplicationsFromProject(project);
				 while(applications.hasNext()){
					 Element application = applications.next();
					 String applicationName = application.attributeValue("name");
					 Iterator<Element> sections = repoLoader.loadSectionsFromApplication(application);
					 while(sections.hasNext()){
						 Element section = sections.next();
						 String sectionName = section.attributeValue("name");
						 //the key ends to the section. the value is the content under the key
						 Iterator<Element> elements = section.elementIterator();
						 while(elements.hasNext()){
							 Element element = elements.next();
							 Map<String,String> elementContentMap = new HashMap<String,String>();
							 String elementName = element.attributeValue("name");
							 String key = (projectName+"."+applicationName+"."+sectionName+"."+elementName).toLowerCase();
							 elementContentMap.putAll(repoLoader.processElementMap("", element));
							 repoMap.put(key, elementContentMap);
						 }						 
					 }
				 }
			 }
			
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return repoMap;
	}
	
	public static Map<String,TableDataContainer> loadTableXML(File repoXmlFile) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			org.w3c.dom.Document xmlDocument  = builder.parse(new FileInputStream(repoXmlFile));
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			//xpath expression for table node
			String tableNodeXpath = "//table";
			
			NodeList nodeList = (NodeList) xpath.compile(tableNodeXpath).evaluate(xmlDocument, XPathConstants.NODESET);
			//process the node list to data container
			Map<String,TableDataContainer> tableData = new HashMap<String,TableDataContainer>();
			for(int tableCount = 0 ; tableCount < nodeList.getLength(); tableCount++) {
				TableDataContainer tableDataContainer = new TableDataContainer();
				ColumnDataContainer columnDataContainer = new ColumnDataContainer();
				ColumnDataContainer headerDataContainer = new ColumnDataContainer();
				ColumnDataContainer footerDataContainer = new ColumnDataContainer();				
				HashMap<String,String> columnNameLocatorTypeMap = new HashMap<String,String> ();
				HashMap<String,String> columnNameLocatorStringMap = new HashMap<String,String> ();
				HashMap<String,String> columnNameElementTypeMap = new HashMap<String,String> ();
				HashMap<String, Integer> columnNameIndexMap = new HashMap<String,Integer> ();
				HashMap<String,String> headerColumnNameLocatorTypeMap = new HashMap<String,String> ();
				HashMap<String,String> headerColumnNameLocatorStringMap = new HashMap<String,String> ();
				HashMap<String,String> headerColumnNameElementTypeMap = new HashMap<String,String> ();
				HashMap<String, Integer> headerColumnNameIndexMap = new HashMap<String,Integer> ();
				HashMap<String,String> footerColumnNameLocatorTypeMap = new HashMap<String,String> ();
				HashMap<String,String> footerColumnNameLocatorStringMap = new HashMap<String,String> ();
				HashMap<String,String> footerColumnNameElementTypeMap = new HashMap<String,String> ();
				HashMap<String, Integer> footerColumnNameIndexMap = new HashMap<String,Integer> ();
				
				Node tableNode = nodeList.item(tableCount);
				//get table info
				String tableName = tableNode.getAttributes().getNamedItem("name").getNodeValue();
				String tableLocatorType = tableNode.getAttributes().getNamedItem("locator-type").getNodeValue();
				String tableLocatorValue = tableNode.getAttributes().getNamedItem("locator-value").getNodeValue();
				tableDataContainer.setName(tableName);
				tableDataContainer.setLocatorType(tableLocatorType);
				tableDataContainer.setLocatorValue(tableLocatorValue);
				
				//get the row info
				/*
				 * get the sub node first then the sub nodes o get the row node, header node, footer node and all the columns
				 */
				NodeList tableSubNodes = tableNode.getChildNodes();
				for(int tableSubNodeCount = 0 ; tableSubNodeCount < tableSubNodes.getLength(); tableSubNodeCount++) {
					Node subNode = tableSubNodes.item(tableSubNodeCount);
					//get the name of the node
					String nodeName = subNode.getNodeName();
					if(nodeName.equalsIgnoreCase("row")) {
						//process the row 
						String rowLocatorType = subNode.getAttributes().getNamedItem("locator-type").getNodeValue();
						String rowLocatorValue = subNode.getAttributes().getNamedItem("locator-value").getNodeValue();
						tableDataContainer.setRowLocatorType(rowLocatorType);
						tableDataContainer.setRowLocatorValue(rowLocatorValue);
					}
					else if(nodeName.equalsIgnoreCase("column")) {
						String columnLocatorType = subNode.getAttributes().getNamedItem("locator-type").getNodeValue();
						String columnLocatorValue = subNode.getAttributes().getNamedItem("locator-value").getNodeValue();
						String columnName = subNode.getAttributes().getNamedItem("name").getNodeValue();
						String columnType = subNode.getAttributes().getNamedItem("type").getNodeValue();
						int columnIndex = Integer.parseInt(subNode.getAttributes().getNamedItem("index").getNodeValue());
						columnNameLocatorTypeMap.put(columnName, columnLocatorType);
						columnNameElementTypeMap.put(columnName, columnType);
						columnNameLocatorStringMap.put(columnName, columnLocatorValue);
						columnNameIndexMap.put(columnName, columnIndex);
						columnDataContainer.setCellNameElementTypeMap(columnNameElementTypeMap);
						columnDataContainer.setCellNameLocatorStringMap(columnNameLocatorStringMap);
						columnDataContainer.setCellNameLocatorTypeMap(columnNameLocatorTypeMap);
						columnDataContainer.setCellNameIndexMap(columnNameIndexMap);
					}
					else if(nodeName.equalsIgnoreCase("footer")) {
						if(subNode.hasChildNodes() && subNode.hasAttributes()) {
							String footerLocatorType = subNode.getAttributes().getNamedItem("locator-type").getNodeValue();
							String footerLocatorValue = subNode.getAttributes().getNamedItem("locator-value").getNodeValue();
							System.out.println("FOOTER LOCATOR TYPE "+footerLocatorType);
							System.out.println("FOOTER LOCATOR VALUE "+footerLocatorValue);
							//get all the header columns
							NodeList footerColumnNodes = subNode.getChildNodes();
							Map<String, Map<String, String>> footerColumnData = new HashMap<String,Map<String,String>>();
							for(int footerColumnCount = 0; footerColumnCount < footerColumnNodes.getLength(); footerColumnCount++) {								
								Node footerColumnNode = footerColumnNodes.item(footerColumnCount);
								if(footerColumnNode.getNodeName().equalsIgnoreCase("column")) {
									String footerColumnLocatorType = footerColumnNode.getAttributes().getNamedItem("locator-type").getNodeValue();
									String footerColumnLocatorValue = footerColumnNode.getAttributes().getNamedItem("locator-value").getNodeValue();
									String footerColumnName = footerColumnNode.getAttributes().getNamedItem("name").getNodeValue();
									String footerColumnType = footerColumnNode.getAttributes().getNamedItem("type").getNodeValue();
									int footerColumnIndex = Integer.parseInt(footerColumnNode.getAttributes().getNamedItem("index").getNodeValue());
									footerColumnNameLocatorTypeMap.put(footerColumnName, footerColumnLocatorType);
									footerColumnNameLocatorStringMap.put(footerColumnName, footerColumnLocatorValue);
									footerColumnNameElementTypeMap.put(footerColumnName, footerColumnType);
									footerColumnNameIndexMap.put(footerColumnName, footerColumnIndex);
									footerDataContainer.setCellNameElementTypeMap(footerColumnNameElementTypeMap);
									footerDataContainer.setCellNameLocatorStringMap(footerColumnNameLocatorStringMap);
									footerDataContainer.setCellNameLocatorTypeMap(footerColumnNameLocatorTypeMap);
									footerDataContainer.setCellNameIndexMap(footerColumnNameIndexMap);
								}
							}
							tableDataContainer.setFooterLocatorType(footerLocatorType);
							tableDataContainer.setFooterLocatorValue(footerLocatorValue);
						}
					}
					else if(nodeName.equalsIgnoreCase("header")) {
						if(subNode.hasChildNodes() && subNode.hasAttributes()) {
							String headerLocatorType = subNode.getAttributes().getNamedItem("locator-type").getNodeValue();
							String headerLocatorValue = subNode.getAttributes().getNamedItem("locator-value").getNodeValue();
							//get all the header columns
							NodeList headerColumnNodes = subNode.getChildNodes();
							Map<String, Map<String, String>> headerColumnData = new HashMap<String,Map<String,String>>();
							for(int headerColumnCount = 0; headerColumnCount < headerColumnNodes.getLength(); headerColumnCount++) {
								Node headerColumnNode = headerColumnNodes.item(headerColumnCount);
								if(headerColumnNode.getNodeName().equalsIgnoreCase("column")) {
									String headerColumnLocatorType = headerColumnNode.getAttributes().getNamedItem("locator-type").getNodeValue();
									String headerColumnLocatorValue = headerColumnNode.getAttributes().getNamedItem("locator-value").getNodeValue();
									String headerColumnName = headerColumnNode.getAttributes().getNamedItem("name").getNodeValue();
									String headerColumnType = headerColumnNode.getAttributes().getNamedItem("type").getNodeValue();
									int headerColumnIndex = Integer.parseInt(headerColumnNode.getAttributes().getNamedItem("index").getNodeValue());
									headerColumnNameLocatorTypeMap.put(headerColumnName, headerColumnLocatorType);
									headerColumnNameLocatorStringMap.put(headerColumnName, headerColumnLocatorValue);
									headerColumnNameElementTypeMap.put(headerColumnName, headerColumnType);
									headerColumnNameIndexMap.put(headerColumnName, headerColumnIndex);
									headerDataContainer.setCellNameElementTypeMap(headerColumnNameElementTypeMap);
									headerDataContainer.setCellNameLocatorStringMap(headerColumnNameLocatorStringMap);
									headerDataContainer.setCellNameLocatorTypeMap(headerColumnNameLocatorTypeMap);
									headerDataContainer.setCellNameIndexMap(headerColumnNameIndexMap);
								}
							}
							tableDataContainer.setHeaderLocatorType(headerLocatorType);
							tableDataContainer.setHeaderLocatorValue(headerLocatorValue);
						}
					}
				}
				tableDataContainer.setBodyColumnData(columnDataContainer);;
				tableDataContainer.setFooterColumnData(footerDataContainer);
				tableDataContainer.setHeaderColumnData(headerDataContainer);
				tableData.put(tableName, tableDataContainer);
			}
			return tableData;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}
}
