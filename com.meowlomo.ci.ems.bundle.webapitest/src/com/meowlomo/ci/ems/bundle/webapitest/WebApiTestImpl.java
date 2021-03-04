package com.meowlomo.ci.ems.bundle.webapitest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil.CompositeRequestResult;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil.MethodType;
import com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator;
import com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator.ValidateResult;
import com.meowlomo.ci.ems.bundle.interfaces.IWebApiTest;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.utils.StringUtil;

public class WebApiTestImpl implements IWebApiTest {

	private static final Logger logger = LoggerFactory.getLogger(WebApiTestImpl.class);
	private BaseBundleActivator activator;
	
	private IHttpUtil http = null;
	public WebApiTestImpl(BaseBundleActivator bba){
		activator = bba;
	}
	
	protected WebApiTestImpl(){
		
	}
	
	class IntRange{
		//0 [], 1[), 2(], 3()
		int rangeCase = -1;
		int a;
		int b;
		public IntRange(String rangeStr) throws Exception{
			String begin;
			String end;
			String[] ranges = rangeStr.split(",");
			if (2 == ranges.length){
				begin = ranges[0].trim();
				end = ranges[1].trim();
				if (begin.length() < 2 || end.length() < 2) {
					throw new Exception("Range 格式不对，空数据");
				} else {
					if (begin.startsWith("[")){
						if (end.endsWith("]")){
							rangeCase = 0;
						} else if (end.endsWith(")")){
							rangeCase = 1;
						} else {
							throw new Exception("Range 格式不对，止符号不匹配.");
						}
					} else if (begin.startsWith("(")){
						if (end.endsWith("]")){
							rangeCase = 2;
						} else if (end.endsWith(")")){
							rangeCase = 3;
						} else {
							throw new Exception("Range 格式不对，止符号不匹配。");
						}
					} else {
						throw new Exception("Range 格式不对，起始符号不匹配.");
					}
				}
			}else{
				throw new Exception("Range 格式不对");
			}
			
			begin = begin.substring(1);
			end = end.substring(0, end.length() - 1);
			
			a = Integer.valueOf(begin);
			b = Integer.valueOf(end);
			
			if (a > b) {
				throw new Exception("Range 逻辑错误，终止大于起始");
			} else if (a == b && 0 != rangeCase) {
				throw new Exception("Range 逻辑错误，没有区间");
			}
		}
		
		public boolean inRange(int code){
			if (code > a && code < b) {
				return true;
			} else if (a == code && (0 == rangeCase || 1 == rangeCase)) {
				return true;
			} else if (b == code && (0 == rangeCase || 2 == rangeCase)) {
				return true;
			} 
			
			return false;
		}
	}
	
	protected JSONObject checkParams(String jsonTask, StringBuilder sb){
		JSONObject task = new JSONObject(jsonTask);
		
		Map<String, Class<?>> paramOptions = new HashMap<String, Class<?>>();
		
		paramOptions.put("Type", 		String.class);
		paramOptions.put("Action", 		String.class);
		paramOptions.put("Url",  		String.class);
		paramOptions.put("RequestHeaders", 	JSONObject.class);
		paramOptions.put("RequestBody", 	JSONObject.class);
		paramOptions.put("QueryParameters", JSONObject.class);
		paramOptions.put("HttpResponseCode",JSONArray.class);	//两选一
		paramOptions.put("JsonSchema", 	JSONObject.class);	//两选一
//		paramOptions.put("keyword", 	String.class);		//两选一
		
		Set<String> optionalParams = new HashSet<String>();
		optionalParams.add("RequestHeaders");
		optionalParams.add("RequestBody");
		optionalParams.add("JsonSchema");
		optionalParams.add("HttpResponseCode");
		optionalParams.add("QueryParameters");
//		optionalParams.add("keyword");
		
		if (!task.has("JsonSchema") && !task.has("HttpResponseCode")){
			sb.append("jsonSchema | httpResponseCode 两个字段应至少有一个");
			return null;
		}
		
		for (Map.Entry<String, Class<?>> entry : paramOptions.entrySet()){
			if (task.has(entry.getKey())){
				Class<?> typeClass = entry.getValue();
				if (task.get(entry.getKey()).getClass() == typeClass){
					//OK;
					continue;
				}else{
					if (optionalParams.contains(entry.getKey())){
						continue;
					}else{
						sb.append(entry.getKey() + " 类型不对,应是 " + typeClass.toGenericString());
						return null;
					}
				}
			}
		}
		
		if (0 != task.getString("Action").compareTo("GET")){
			if (!task.has("RequestBody") || !(task.get("RequestBody") instanceof String))
			{
				sb.append("requestBody 不应为空且应为string类型的json字符串");
				return null;
			}
		}
		
		return task;
	}
	
	private JSONObject updateInstructionRunToRemote(Long instructionRunId, JSONObject updateInstructionResult){
//		updateInstructionResult
		do {
			if (null == updateInstructionResult || 0 == instructionRunId || null == http)
				break;
			else {
				String url = updateInstructionResult.getString("url");
				String methodType = updateInstructionResult.getString("method");

				JSONObject methodParams = new JSONObject();
				methodParams.put("id", instructionRunId);
				
				String[] fields = {"inputData", "inputType", "inputParameter", "outputData", "outputType", "outputParameter"};
				
				for(String field : fields){
					Object value = updateInstructionResult.get(field);
					if (null != value)
						methodParams.put(field, value);					
				}
				JSONArray params = new JSONArray();
				params.put(methodParams);

				String result = http.request(url, params.toString(), MethodType.valueOf(methodType.toUpperCase()));
				if (null == result || result.startsWith("<"))
					break;
				return new JSONObject(result);
			}
		} while (false);

		return null;
	}
	
	@Override
	public String doTestProcess(String jsonTask, List<String> infoOut){
		int iType = 0;//0 OK 	1 params valid		2.execute valid
		String msgReport = "";
		JSONObject updateInfo = null;
		if (!infoOut.isEmpty()){
			String instructionResultUpdateInfo = infoOut.remove(0);
			updateInfo = new JSONObject(instructionResultUpdateInfo);
		}
		
		do{
			try{
				//1.paramters
				StringBuilder sb = new StringBuilder();
				JSONObject task = checkParams(jsonTask, sb);
				if (null == task){
					iType = 1;
					msgReport = sb.toString();
					if (msgReport.isEmpty())
						msgReport = "接口测试参数不满足";
					break;
				}
					
				//2.get web api tool
				getHttpTool();
		
				//3.headers
				String requestType = task.getString("Action");
				String headerObjStr = task.has("RequestHeaders") ? task.getString("RequestHeaders") : "{\"Content-Type\":\"applicaiton:json\"}";
				String requestBody = "";
				if (task.has("RequestBody")){
					String requestBodyStr = task.getString("RequestBody");
					JSONObject queryBody = new JSONObject(requestBodyStr.isEmpty() ? "{}" : requestBodyStr);
					requestBody = queryBody.toString();
				}
				
				//4.do 
				String url = task.getString("Url");
				url = url.trim();
				if (task.has("QueryParameters") && task.get("QueryParameters") instanceof JSONObject){
					JSONObject queryParams = task.getJSONObject("QueryParameters");
					List<NameValuePair> params = new LinkedList<NameValuePair>();
					
					Iterator<String> is = queryParams.keys();
					while(is.hasNext()){
						String key = is.next();
						params.add(new BasicNameValuePair(key, queryParams.get(key).toString()));
					}
					String paramString = URLEncodedUtils.format(params, "utf-8");
					//URIBuilder URLEncodedUtils
					if(!url.endsWith("?"))
				        url += "?";
					url += paramString;
				}
				CompositeRequestResult crr = http.requestHeader(url, headerObjStr, requestBody, MethodType.valueOf(requestType.toUpperCase()));
				
				//		8. jsonPath? TODO
				String jp = null;
				String jpResult = null;
				if (task.has("JsonPathPackage")){
					String jsonPathPackages = task.getString("JsonPathPackage");
					if (!StringUtil.nullOrEmpty(jsonPathPackages)) {
						JSONObject jsonPath = new JSONObject(jsonPathPackages);
						if (jsonPath.has("jsonPath")){
							jp = jsonPath.getString("jsonPath");
							jp = jp.trim();
							if (!jp.isEmpty()){
								//TODO check crr.content for json format
								Object jsonPathResult = JsonPath.read(crr.content, jp);
								jpResult = jsonPathResult.toString();
							}
						}
					}
				}
				
				//此处回传 api 信息
				if (null != updateInfo){
					Long instructionRunId = updateInfo.optLong("instructionRunId");
					appendInputInfo(updateInfo, requestType, headerObjStr, requestBody, url);
					appendOutputInfo(updateInfo, crr, jp ,jpResult);
					updateInstructionRunToRemote(instructionRunId, updateInfo);
				}
				
//				CompositeRequestResult crr = requestResult.isEmpty() ? null : CompositeRequestResult.fromString(requestResult);		
				if (null == crr || crr.code < 0 ){
					msgReport = "接口调用失败";
					iType = 2;
					break;
				}
				
				//5.result 20X ? 
				if (task.has("HttpResponseCode")){
					String codesStr = task.getString("HttpResponseCode");
					boolean rangeType = false;
					IntRange codeRange = null;
					try{
						codeRange = new IntRange(codesStr);
						rangeType = true;
					} catch(Exception e){
						logger.error(e.getMessage());
					}
					
					if (rangeType && codeRange.inRange(crr.code)){
						msgReport = String.format("返回code [%d] 在预期code %s 中", crr.code, codesStr);
					}else if (!rangeType && codesStr.contains(String.valueOf(crr.code))){
						//OK
						msgReport = String.format("返回code [%d] 在预期code %s 中", crr.code, codesStr);
					}else{
						msgReport = String.format("返回code [%d] 并不在预期code %s 中", crr.code, codesStr);
						iType = 3;
						break;
					}
				}
				
				//6.schema ?
				if (task.has("JsonSchema")){
					String jsonSchema = task.getString("JsonSchema");
					if (null != jsonSchema && !jsonSchema.isEmpty() && 0 != jsonSchema.compareToIgnoreCase("{}") 
							&& 0 != jsonSchema.compareToIgnoreCase("null")) {
						ISchemaValidator schemaValidator = BaseBundleActivator.getTheServiceObject("schemavalidator", ISchemaValidator.class);
						if (null != schemaValidator){
							String json = crr.content;
							ValidateResult vr = schemaValidator.validateJSONSchema(json, jsonSchema);
							if (null == vr || !vr.ok){
								iType = 4;
								msgReport = (vr.msg.isEmpty() ? "返回JSON的schema校验失败" : vr.msg);
								break;
							}
						}else{
							iType = 5;
							msgReport = "Schema Validator 功能未能就绪";
							break;
						}
					}
				}
				
				//7. keyword?
				if (task.has("Keyword") && !crr.content.isEmpty()){
					if (!crr.content.contains(task.getString("keyword"))){
						iType = 8;
						msgReport = "返回内容未包含关键字: " + task.getString("Keyword");
						break;
					}
				}
				
			}catch(Exception e){
				iType = 9;
				msgReport = "异常: " + e.toString();
				break;
			}
			
		} while(false);
		
		infoOut.add(msgReport);
		
		//7.result  PASS OR FAIL
		if (iType > 0){
			if (6 == iType || 9 == iType)
				return "false;exception;" + msgReport;
			else
				return "false;error;" + msgReport;
		}
		return "true;pass;接口通过";
	}

	private void appendOutputInfo(JSONObject updateInfo, CompositeRequestResult crr, String jsonPath, String jpResult) {
		updateInfo.put("outputData", 		crr.content);
		updateInfo.put("outputType", 		crr.contentHeader);
		
		JSONObject outputParameter = new JSONObject();
		outputParameter.put("code", crr.code);
		outputParameter.put("phase", crr.phase);
		outputParameter.put("protocol", crr.protocol);
		outputParameter.put("responseHeaders", crr.responseHeaders);
		if (null != jsonPath){
			outputParameter.put("jsonPath", jsonPath);
		}
		if (null != jpResult){
			outputParameter.put("jsonPathResult", jpResult);
		}
		updateInfo.put("outputParameter", 	outputParameter);
	}

	private void appendInputInfo(JSONObject updateInfo, String requestType, String headerObjStr,
			String requestBody, String url) {
		updateInfo.put("inputData", 		url);
		updateInfo.put("inputType", 		"api");
		
		JSONObject intputParameter = new JSONObject();
		intputParameter.put("header", headerObjStr);
		intputParameter.put("body", requestBody);
		intputParameter.put("methodType", requestType);
		updateInfo.put("inputParameter", 	intputParameter);
	}

	private void getHttpTool() {
		if (null == http) {
			http = IHttpUtil.getHttpTool();
		}
	}

	@Override
	public void notifyTimeout() {
		SGLogger.timeoutError();
	}

	@Override
	public String step(String instructionJson, List<String> paramsInOrOut) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExecutionEnvironmentInfo(String info) {
		return null;
	}
}
