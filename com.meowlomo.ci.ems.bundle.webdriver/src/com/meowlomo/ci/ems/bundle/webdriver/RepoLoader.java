package com.meowlomo.ci.ems.bundle.webdriver;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class RepoLoader {
    protected Document parse(URL url) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(url);
        return document;
    }
    
    protected Iterator<Element> loadProjects(Element rootElement){
    	return rootElement.element("projects").elementIterator("project");
    }
    
    protected Iterator<Element> loadApplicationsFromProject(Element projectElement){
    	return projectElement.element("applications").elementIterator("application");
    }
    
    protected Iterator<Element> loadSectionsFromApplication(Element application){
    	return application.element("sections").elementIterator("section");
    }
    
    protected Map<String, String> processSectionElement(Element section){
    	//get the sub elements first
    	Map<String,String> collectionMap = new HashMap<String,String>();
    	Iterator<Element> subElements = section.elementIterator();
    	//intertor the element is the elements
    	while(subElements.hasNext()){
    		Element subElement = subElements.next();
    		//check is the element has a name attribute 
    		if(subElement.attributeValue("name") != null){
    			collectionMap.putAll(this.processElementMap("", subElement));
    		}
    	}
    	return collectionMap;
    }
    
    private Map<String,String> processElementAttributes(String name, String upperName, Element element){
    	Map<String,String> collectionMap = new HashMap<String,String>();
    	List<Attribute> attributes = element.attributes();
    	for(Attribute attr : attributes){
    		String key = (upperName.isEmpty() ? "" : upperName+".")+name+"."+attr.getName();
    		String value = attr.getValue();
    		System.out.println("key=["+key+"] value=["+value+"]");
    		collectionMap.put(key.toLowerCase(),value);
    	}
    	return collectionMap;
    }
    
    /*
     * @author 陈琪
     * 重构便于理解
     */
    public Map<String,String> processElementMap(String upperName, Element element){
    	Map<String,String> collectionMap = null;
    	String name = element.attributeValue("name");
    	Iterator<Element> subElements = element.elementIterator();
    	//case 1, has name and ended
    	//case 3, no name and ended
    	if (!subElements.hasNext()){
    		System.out.println((null == name ? "no" : "has") + " name and ended");
    		collectionMap = processElementAttributes(null == name ? element.getName() : name, upperName, element);
    	}
    	//case 2, has name but not ended
    	//case 4, no name but not ended
    	else if (subElements.hasNext()){
    		System.out.println((null == name ? "no" : "has") + " name but not ended");
    		collectionMap = processElementAttributes(null == name ? element.getName() : name, upperName, element);
        	//process the child node and add the child node's name to the upper name
        	while(subElements.hasNext()){
        		Element subElement = subElements.next();
        		collectionMap.putAll(this.processElementMap((upperName.isEmpty() ? "" : upperName+".")+name, subElement));
        	}
    	}
    	//never here
    	else{
    		System.out.println("else");
    		collectionMap = new HashMap<String,String>();
    		while(subElements.hasNext()){
    			System.out.println("#");
    			collectionMap.putAll(this.processElementMap(element.getName(), subElements.next()));
    		}
    	}
    	return collectionMap;
    }
}
