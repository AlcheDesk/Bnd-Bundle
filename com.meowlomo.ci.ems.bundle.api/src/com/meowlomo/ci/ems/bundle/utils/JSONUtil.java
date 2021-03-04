package com.meowlomo.ci.ems.bundle.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JSONUtil {
	private static enum ConflictStrategy {

        THROW_EXCEPTION, PREFER_FIRST_OBJ, PREFER_SECOND_OBJ, PREFER_NON_NULL;
    }
	private static class JsonObjectExtensionConflictException extends Exception {

        public JsonObjectExtensionConflictException(String message) {
            super(message);
        }

    }
	
	private static class GsonTools {

	    public static void extendJsonObject(JsonObject destinationObject, ConflictStrategy conflictResolutionStrategy, JsonObject ... objs) 
	            throws JsonObjectExtensionConflictException {
	        for (JsonObject obj : objs) {
	            extendJsonObject(destinationObject, obj, conflictResolutionStrategy);
	        }
	    }

	    private static void extendJsonObject(JsonObject leftObj, JsonObject rightObj, ConflictStrategy conflictStrategy) 
	            throws JsonObjectExtensionConflictException {
	        for (Map.Entry<String, JsonElement> rightEntry : rightObj.entrySet()) {
	            String rightKey = rightEntry.getKey();
	            JsonElement rightVal = rightEntry.getValue();
	            if (leftObj.has(rightKey)) {
	                //conflict                
	                JsonElement leftVal = leftObj.get(rightKey);
	                if (leftVal.isJsonArray() && rightVal.isJsonArray()) {
	                    JsonArray leftArr = leftVal.getAsJsonArray();
	                    JsonArray rightArr = rightVal.getAsJsonArray();
	                    //concat the arrays -- there cannot be a conflict in an array, it's just a collection of stuff
	                    for (int i = 0; i < rightArr.size(); i++) {
	                        leftArr.add(rightArr.get(i));
	                    }
	                } else if (leftVal.isJsonObject() && rightVal.isJsonObject()) {
	                    //recursive merging
	                    extendJsonObject(leftVal.getAsJsonObject(), rightVal.getAsJsonObject(), conflictStrategy);
	                } else {//not both arrays or objects, normal merge with conflict resolution
	                    handleMergeConflict(rightKey, leftObj, leftVal, rightVal, conflictStrategy);
	                }
	            } else {//no conflict, add to the object
	                leftObj.add(rightKey, rightVal);
	            }
	        }
	    }

	    private static void handleMergeConflict(String key, JsonObject leftObj, JsonElement leftVal, JsonElement rightVal, ConflictStrategy conflictStrategy) 
	            throws JsonObjectExtensionConflictException {
	        {
	            switch (conflictStrategy) {
	                case PREFER_FIRST_OBJ:
	                    break;//do nothing, the right val gets thrown out
	                case PREFER_SECOND_OBJ:
	                    leftObj.add(key, rightVal);//right side auto-wins, replace left val with its val
	                    break;
	                case PREFER_NON_NULL:
	                    //check if right side is not null, and left side is null, in which case we use the right val
	                    if (leftVal.isJsonNull() && !rightVal.isJsonNull()) {
	                        leftObj.add(key, rightVal);
	                    }//else do nothing since either the left value is non-null or the right value is null
	                    break;
	                case THROW_EXCEPTION:
	                    throw new JsonObjectExtensionConflictException("Key " + key + " exists in both objects and the conflict resolution strategy is " + conflictStrategy);
	                default:
	                    throw new UnsupportedOperationException("The conflict strategy " + conflictStrategy + " is unknown and cannot be processed");
	            }
	        }
	    }
	}
	private static final Logger logger = LoggerFactory.getLogger(JSONUtil.class);
	
	private static JSONObject use4NoJSONClassImportCase = null;
	private static Map<String, JsonObject> use4NoJSONClassImportCaseMap = new HashMap<String, JsonObject>();
	
	public static void beginJSONObject() {
		use4NoJSONClassImportCase = new JSONObject();
	}
	
	public static void beginJSONObject(String channel) {
		use4NoJSONClassImportCaseMap.put(channel, new JsonObject());
	}
	
	public static void addJSONField(String key, String value) {
		if (null != use4NoJSONClassImportCase && !StringUtil.nullOrEmpty(key)) {
			use4NoJSONClassImportCase.put(key, value);
		}
	}
	
	public static void addJSONField(String channel, String key, String value) {
		if (null != use4NoJSONClassImportCaseMap && !StringUtil.nullOrEmpty(key) && null != use4NoJSONClassImportCaseMap.get(channel)) {
			use4NoJSONClassImportCaseMap.get(channel).addProperty(key, value);
		}
	}
	
	public static String endJSONObject() {
		if (null != use4NoJSONClassImportCase) {
			String tmp = use4NoJSONClassImportCase.toString();
			use4NoJSONClassImportCase = null;
			return tmp;
		}
		return null;
	}
	
	public static String endJSONObject(String channel, boolean clear) {
		if (null != use4NoJSONClassImportCaseMap && null != use4NoJSONClassImportCaseMap.get(channel)) {
			String tmp = use4NoJSONClassImportCaseMap.get(channel).toString();
			if (clear)
				use4NoJSONClassImportCaseMap.remove(channel);
			return tmp;
		}
		return null;
	}
	
	public static boolean isJSONValid(String jsonString) {
	    try {
	        new JSONObject(jsonString);
	    } catch (JSONException ex) {
	        // edited, to include @Arthur's comment
	        // e.g. in case JSONArray is valid as well...
	    	ex.printStackTrace();
	        try {
	            new JSONArray(jsonString);
	        } catch (JSONException ex1) {
	            return false;
	        }
	    }
	    return true;
	}
	
	public static boolean writeJSONObjectToFile(JSONObject jsonObject, String path){
		PrintStream out = null;
		try{
			String text = jsonObject.toString();
			//FileWriter file = null;
			
			File localTempFile= new File(path);
			if(localTempFile.exists()){
				//remove it, the recreate it
				localTempFile.delete();
				localTempFile.createNewFile();
			}else{
				localTempFile.getParentFile().mkdirs();
				logger.info("Creating "+localTempFile.getAbsolutePath()+" with content : "+text);
				localTempFile.createNewFile();
			}
			
			
			out = new PrintStream(new FileOutputStream(path),true,"UTF-8");
		    out.print(text);
		    return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error("Got FileNotFoundException writing joson obejct to file : "+path,e);
			return false;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			logger.error("Got UnsupportedEncodingException wrtiing joson obejct to file : "+path,e);
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Got IOException wrtiing joson obejct to file : "+path,e);
			return false;
		} finally {
			out.close();			
		}
	}
	
	public static boolean writeJSONArrayToFile(JSONArray jsonArray, String path){
		PrintStream out = null;
		try{
			//FileWriter file = null;
			File jsonFile= new File(path);
			if(jsonFile.exists()){
			//	file = new FileWriter(jsonFile,false);
			}else{
				jsonFile.getParentFile().mkdirs();
			//	file = new FileWriter(jsonFile);
			}
			String text = jsonArray.toString();
			out = new PrintStream(new FileOutputStream(path));
		    out.print(text);
		    return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} finally {
			out.close();
		}
	}
	
	public static JSONObject readFromJSONFile(String jsonFilePath){
		try {
			List<String> lines = Files.readAllLines(Paths.get(jsonFilePath));
			String jsonString = "";
			for(int lineCount = 0 ; lineCount< lines.size(); lineCount++){
				jsonString = jsonString+lines.get(lineCount);
			}
			return new JSONObject(jsonString);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 
	 * @param add 覆盖source中的字段如果相同时
	 * @param source
	 * @return
	 */
	public static JSONObject merge(JSONObject add, JSONObject source) {
		if(source == null){
			return add;
		}
		
		if(add == null){
			return source;
		}
		JSONObject mergedJSON = new JSONObject();
		try {
			mergedJSON = new JSONObject(source.toString());
			Iterator<String> json2keys = add.keys();
			while(json2keys.hasNext()) {
				String key = json2keys.next();
				if(mergedJSON.isNull(key)){
					mergedJSON.put(key, add.get(key));
				}else{
					mergedJSON.remove(key);
					mergedJSON.put(key, add.get(key));
				}
			}
 
		} catch (JSONException e) {
			throw new RuntimeException("JSON Exception" + e);
		}
		return mergedJSON;
	}
	
	/**
	 * 合并两个JSONObject,未考察递归溢出的常规上限,但一般的文本操作不应成问题
	 * 相同字段存在时，用add覆盖source
	 * @param add
	 * @param source
	 * @return
	 * @throws JSONException
	 */
	public static JSONObject deepMerge(JSONObject add, JSONObject source) throws JSONException {
		if(null == add){
			return source;
		}
		
		if(null == source){
			return add;
		}
		
		JSONObject mergedJSON = source;
		for (String key: add.keySet()) {
	        Object value = add.get(key);
	        if (!mergedJSON.has(key)) {
	            // new value for "key":
	        	mergedJSON.put(key, value);
	        } else {
	            // existing value for "key" - recursively deep merge:
	            if (value instanceof JSONObject) {
	                JSONObject valueJson = (JSONObject)value;
	                deepMerge(valueJson, mergedJSON.getJSONObject(key));
	            } else {
	            	mergedJSON.put(key, value);
	            }
	        }
	    }
	    return mergedJSON;
	}
	

	/**
	 * 将传入的JSONObject的string,放入JSONArray中后以String形式返回
	 * @param objStr
	 * @return
	 */
	public static String wrapInArray(String objStr) {
		try {
			if (null == objStr)
				return objStr;
			JSONObject object = new JSONObject(objStr);
			JSONArray result = new JSONArray();
			result.put(object);
			return result.toString();
		}catch (Exception e) {
			System.err.println("[Exception] " + e.getClass() + " occured.");
			return objStr;
		}
	}
	
	public static String wrapInArray(String objStr, JSONObject param) {
		try {
			if (null == objStr)
				return objStr;
			JSONObject object = new JSONObject(objStr);
			
			JSONObject params = JSONUtil.merge(object, param);
			JSONArray result = new JSONArray();
			result.put(params);
			return result.toString();
		}catch (Exception e) {
			System.err.println("[Exception] " + e.getClass() + " occured.");
			return objStr;
		}
	}
}

