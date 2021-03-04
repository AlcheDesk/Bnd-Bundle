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
import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil.CompositeRequestResult;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil.MethodType;
import com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator;
import com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator.ValidateResult;
import com.meowlomo.ci.ems.bundle.interfaces.IWebApiTest;
import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.utils.StringUtil;

public class WebApiTestImplNew implements IWebApiTest {

	private static final Logger logger = LoggerFactory.getLogger(WebApiTestImpl.class);
	private BaseBundleActivator activator;
	
	private IHttpUtil http = null;
	public WebApiTestImplNew(BaseBundleActivator bba){
		activator = bba;
	}
	
	protected WebApiTestImplNew(){
		
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
	
	protected JSONObject checkParams(String instruction, StringBuilder sb){
		JSONObject instructionObj = new JSONObject(instruction);
		
		System.err.println("checkParams:" + instruction);
		//提取下述字段
		if (instructionObj.has("extraData")) {
			JSONObject data = instructionObj.getJSONObject("extraData");
			
			instructionObj.put("requestHeaders", data.get("requestHeaders").toString());
			instructionObj.put("requestBody", data.has("body") ? data.get("body").toString() : "{}");
			instructionObj.put("queryParameters", data.get("queryParameters").toString());
			if (!data.has("responseCode") || data.isNull("responseCode") ||data.getString("responseCode").isEmpty())
				instructionObj.put("HttpResponseCode", "[200,300)");//TODO
			else
				instructionObj.put("HttpResponseCode", data.getString("responseCode"));
			//json schema
			if (data.has("jsonSchema")){
				instructionObj.put("jsonSchema", data.getString("jsonSchema"));
			}else{
				instructionObj.put("jsonSchema", JSONObject.NULL);
			}
			//json node
			if (data.has("jsonPathPackage")){
				instructionObj.put("jsonPathPackage", data.getJSONObject("jsonPathPackage"));
			}else{
				instructionObj.put("jsonPathPackage", JSONObject.NULL);
			}
			
			instructionObj.put("protocol", data.getString("protocol"));
			instructionObj.put("host", data.has("host") ? data.getString("host") : "");
			instructionObj.put("port", data.has("port") ? data.getString("port") : "");
			instructionObj.put("baseUrl", data.has("baseUrl") ? data.getString("baseUrl") : "");
			//转换
			String input = instructionObj.getString("input");
			if (!input.contains(data.getString("protocol"))){
				input = data.getString("protocol") + input;
			}
			instructionObj.put("url", input);
		}
		
		Map<String, Class<?>> paramOptions = new HashMap<String, Class<?>>();
		
		paramOptions.put("instructionType", 		String.class);
		paramOptions.put("action",			 		String.class);
		paramOptions.put("url",  					String.class);
		paramOptions.put("requestHeaders", 	JSONObject.class);
		paramOptions.put("requestBody", 	JSONObject.class);
		paramOptions.put("queryParameters", JSONObject.class);
		paramOptions.put("HttpResponseCode",JSONArray.class);	//两选一
		paramOptions.put("jsonSchema", 	JSONObject.class);	//两选一
//		paramOptions.put("keyword", 	String.class);		//两选一
		
		Set<String> optionalParams = new HashSet<String>();
		optionalParams.add("requestHeaders");
		optionalParams.add("requestBody");
		optionalParams.add("jsonSchema");
		optionalParams.add("HttpResponseCode");
		optionalParams.add("queryParameters");
//		optionalParams.add("keyword");
		
		if (!instructionObj.has("jsonSchema") && !instructionObj.has("HttpResponseCode")){
			sb.append("jsonSchema | httpResponseCode 两个字段应至少有一个");
			return null;
		}
		
		for (Map.Entry<String, Class<?>> entry : paramOptions.entrySet()){
			if (instructionObj.has(entry.getKey())){
				Class<?> typeClass = entry.getValue();
				if (instructionObj.get(entry.getKey()).getClass() == typeClass){
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
		
		if (0 != instructionObj.getString("action").compareTo("GET")){
			if (!instructionObj.has("requestBody") || !(instructionObj.get("requestBody") instanceof String))
			{
				sb.append("requestBody 不应为空且应为string类型的json字符串");
				return null;
			}
		}
		
		return instructionObj;
	}
	
	private JSONObject updateInstructionRunToRemoteNew(String expectedValue, String jpResult, JSONObject updateInstructionResult){
//		updateInstructionResult
		do {
			if (null == http)
				break;
			else {
				JSONObject methodParams = new JSONObject();
				methodParams.put("returnValue", jpResult);
				if (null != expectedValue)
					methodParams.put("expectedValue", expectedValue);
				
				String[] fields = {"inputData", "inputType", "inputParameter", "outputData", "outputType", "outputParameter"};
				
				for(String field : fields){
					Object value = updateInstructionResult.get(field);
					if (null != value)
						methodParams.put(field, value);					
				}
				String result = http.updateInstructionResult(methodParams.toString());
				System.err.println("新updateInstructionResult结果:" + result);
				if (null == result || result.startsWith("<"))
					break;
				return new JSONObject(result);
			}
		} while (false);

		return null;
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
		System.err.println("WebApiTestImplNew.doTestProcess:");
		System.err.println("jsonTask:" + jsonTask);
		System.err.println("infoOut:" + infoOut);
		int iType = 0;//0 OK 	1 params valid		2.execute valid
		int iStep = 0;
		String msgReport = "";
		String saveKey = null;
		String saveValue = null;
	
		do{
			try{
				//1.paramters
				StringBuilder sb = new StringBuilder();
				iStep = 1;
				JSONObject instructionObject = checkParams(jsonTask, sb);
				iStep = 2;
				if (null == instructionObject){
					iType = 1;
					msgReport = sb.toString();
					if (msgReport.isEmpty())
						msgReport = "接口测试参数不满足";
					break;
				}
				iStep = 3;
				//2.get web api tool
				getHttpTool();
				iStep = 4;
				//3.headers
				String requestType = instructionObject.getString("action");
				String headerObjStr = instructionObject.has("requestHeaders") ? instructionObject.getString("requestHeaders") : "{\"Content-Type\":\"applicaiton:json\"}";
				String requestBody = "";
				if (instructionObject.has("requestBody")){
					iStep = 5;
					String requestBodyStr = instructionObject.getString("requestBody");
					
					if (null == requestBodyStr || "null".equalsIgnoreCase(requestBodyStr)) {
						requestBodyStr = null;
						requestBody = "";
						iStep = 51;
					}
					else {
						requestBodyStr = InstructionOptions.instance().doWithAllTransfer(requestBodyStr);
						requestBodyStr = requestBodyStr.trim();
						
						if (requestBodyStr.startsWith("[") && requestBodyStr.endsWith("]")) {
							JSONArray queryBody = new JSONArray(requestBodyStr);
							iStep = 7;
							requestBody = queryBody.toString();
						} else {
							JSONObject queryBody = new JSONObject(StringUtil.nullOrEmpty(requestBodyStr) ? "{}" : InstructionOptions.instance().doWithAllTransfer(requestBodyStr));
							iStep = 6;
							requestBody = queryBody.toString();
						}
					}
				}
				//TODO option 内容替换等				
				//4.do 
				iStep = 7;
				String url = instructionObject.getString("url");
				url = url.trim();
				url = InstructionOptions.instance().doWithAllTransfer(url);
				do {
					iStep = 8;
					if (instructionObject.has("queryParameters")) {
						JSONObject queryParams = null;
						Object queryParametersObj = instructionObject.get("queryParameters");
						if (queryParametersObj instanceof JSONObject){
							queryParams = instructionObject.getJSONObject("queryParameters");
							queryParams = new JSONObject(InstructionOptions.instance().doWithAllTransfer(queryParams.toString()));
						} else if (queryParametersObj instanceof String) {
							queryParams = new JSONObject(InstructionOptions.instance().doWithAllTransfer(instructionObject.getString("queryParameters")));
						} else {
							break;
						}
						iStep = 9;
						List<NameValuePair> params = new LinkedList<NameValuePair>();
						
						
						Iterator<String> iterator = queryParams.keys();
						while(iterator.hasNext()){
							String key = iterator.next();
							params.add(new BasicNameValuePair(key, queryParams.get(key).toString()));
						}
						String paramString = URLEncodedUtils.format(params, "utf-8");
						//URIBuilder URLEncodedUtils
						if(!paramString.isEmpty())
							if (!url.endsWith("?"))
								url += ("?" + paramString);
							else if (url.contains("?"))
								url += ("&" + paramString);
						iStep = 10;
					}
				} while (false);
				CompositeRequestResult crr = http.requestHeader(url, headerObjStr, requestBody, MethodType.valueOf(requestType.toUpperCase()));
				iStep = 11;
				if (null == crr || crr.code < 0 ){
					msgReport = "接口调用失败";
					iType = 2;
					break;
				}
				iStep = 12;
				//4.1	result 20X ? 
				if (instructionObject.has("httpResponseCode")){
					String codesStr = instructionObject.getString("httpResponseCode");
					boolean rangeType = false;
					IntRange codeRange = null;
					iStep = 13;
					try{
						codeRange = new IntRange(codesStr);
						rangeType = true;
					} catch(Exception e){
						logger.error(e.getMessage());
					}
					
					if (rangeType && codeRange.inRange(crr.code)){
						msgReport = String.format("返回code [%d] 在预期code %s 中.", crr.code, codesStr);
					}else if (!rangeType && codesStr.contains(String.valueOf(crr.code))){
						//OK
						msgReport = String.format("返回code [%d] 在预期code %s 中..", crr.code, codesStr);
					}else{
						msgReport = String.format("返回code [%d] 并不在预期code %s 中", crr.code, codesStr);
						iType = 3;
						break;
					}
					iStep = 14;
				}
				
				//5.1	 jsonPath? TODO
				String jpCondition = null;
				String jpResult = null;
				iStep = 15;
				String expectedValue = null;
				if (instructionObject.has("extraData")){
					JSONObject extra = instructionObject.optJSONObject("extraData");
					if (null != extra) {
						JSONObject jsonPath = extra.optJSONObject("jsonPathPackage");
						if (null != jsonPath){
							iStep = 16;
							if (jsonPath.has("jsonPath")){
								jpCondition = jsonPath.getString("jsonPath");
								jpCondition = jpCondition.trim();
								if (!jpCondition.isEmpty()){
									iStep = 17;
									Object jsonPathResult = JsonPath.read(crr.content, jpCondition);
									jpResult = jsonPathResult.toString();
								}
								
								iStep = 18;
								if (jsonPath.has("expectedValue") && !jsonPath.getString("expectedValue").isEmpty()) {
									expectedValue = jsonPath.getString("expectedValue");
									expectedValue = InstructionOptions.instance().doWithSavedData(expectedValue);
									msgReport = String.format("预期值为:[" + expectedValue + "];" + "实际得到的jsonpath结果为:[" + jpResult + "]");
									if (!jpResult.equals(expectedValue)) {
										iType = 10;
										msgReport += "!";
									}
								}
							}
							else if (jsonPath.has("expectedValue")) {
								iStep = 19;
								System.out.println(jsonPath.getString("expectedValue"));
								iType = 11;
								msgReport = "请先输入jsonpath,再输入expectedValue;否则无法比较";
							}
						}
					}
				}
				
				iStep = 21;
				//5.2	option保存json path结果
				ExecutionResult er = InstructionOptions.instance().genFromOptionStr(instructionObject.getString("options"));//options
				if (!er.bOK()) return er.toString();
				
				if (InstructionOptions.instance().notEmpty()) {
					if (InstructionOptions.instance().existOption(ContextConstant.OPTION_SAVE_JSONPATH)) {
						iStep = 22;
						saveKey = InstructionOptions.instance().getValue(ContextConstant.OPTION_SAVE_JSONPATH);
						if (null != saveKey && !saveKey.isEmpty()) {
							saveValue = jpResult;
							InstructionOptions.instance().saveData(saveKey, saveValue);
							logger.info("[OPTION INFO] [DTA_SAVE_JSONPATH] save key:" + saveKey + ", and value:" + saveValue);
						}
						else{
							logger.info("[OPTION ERROR] [DTA_SAVE_JSONPATH] will be ignored: for save key it is empty!");
						}
					}
				}
				System.err.println("WebApiTestImpl.step.options:" + InstructionOptions.instance().savedDatas());
				//6
				//TODO
				//此处回传 api 信息
				iStep = 23;
				JSONObject updateInfo = new JSONObject();
				appendInputInfo(updateInfo, requestType, headerObjStr, requestBody, url);
				appendOutputInfo(updateInfo, crr, jpCondition ,jpResult);
				updateInstructionRunToRemoteNew(expectedValue, jpResult, updateInfo);
				iStep = 24;
				//7.schema ?
				if (instructionObject.has("jsonSchema") && !instructionObject.isNull("jsonSchema")){
					String jsonSchema = instructionObject.getString("jsonSchema");
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
			}catch(Exception e){
				iType = 90000 + iStep;
				msgReport = "异常: " + e.toString();
				break;
			}
			
		} while(false);
		
		infoOut.add(msgReport);
		
		//7.result  PASS OR FAIL
		if (iType > 0){
			if (6 == iType || 9 == iType)
				return new ExecutionResult(false, iType, msgReport).toString();
			else
				return new ExecutionResult(false, iType, msgReport).toString();
		}
		return new ExecutionResult(true, "接口通过").toString();
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
		System.err.println("[Web api test] step begin");
		return doTestProcess(instructionJson, paramsInOrOut);
	}

	@Override
	public JSONObject attachData(String data) {
		return IWebApiTest.super.attachData(data);
		//TODO
	}

	@Override
	public String getExecutionEnvironmentInfo(String info) {
		IHttpUtil httpUtil = BaseBundleActivator.getTheServiceObject("httpclient", IHttpUtil.class);
		if (null != httpUtil) {
			return httpUtil.getHttpUtilConfigInfo();
		}
		return "";
	}
}
