/**
 * 
 */
package com.meowlomo.ci.ems.bundle.httpclient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.UnsupportedCharsetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.utils.JSONUtil;
import com.meowlomo.ci.ems.bundle.utils.ReflectionUtil;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.utils.StringUtil;
/**
 * @author tester
 *
 */
public class HttpUtilImpl implements IHttpUtil {
	static final Logger logger = LoggerFactory.getLogger(HttpUtilImpl.class);
	private static RequestConfig requestConfig = null;
	private static RequestConfig requestHeaderConfig = null;

	boolean inTestCase = false;
	boolean standSingleton = true;
	CloseableHttpClient httpHeaderClient = null;
	
	String defaultRequestConfigRestAPI;
	String requestConfigRestAPI;
	
	
	long instructionRunId = 0L;
	long instructionId = 0L;
	long testCaseId = 0L;
	
	Map<String, JSONObject> originalInstructionMap = new HashMap<String, JSONObject>();
	
	//原始instruction
	Map<String, JSONObject> addInstructionResultMap = new HashMap<String, JSONObject>();
	
	private static DateFormat  dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private static DateFormat  startDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	
	private static String[] endStrings = new String[]{"INSTRUCTION_END","FAIL","ERROR","TIME_OUT","PASS","ELEMENT_NOT_FOUND"};
	private static List<String> endStringList = Arrays.asList(endStrings);

	private static String[] infoStrings = new String[]{"INSTRUCTION"};
	private static List<String> infoStringList = Arrays.asList(infoStrings);
	
	private static ATMRequestConfig executionStepLogConfig = ATMRequestConfig.genEmpty();
	private static ATMRequestConfig stepLogConfig = ATMRequestConfig.genEmpty();
	
	private static ATMRequestConfig finishInstructionConfig;
	private static ATMRequestConfig addInstructionResultConfig;
	private static ATMRequestConfig finishRunConfig;	
	private static ATMRequestConfig addStepFileLogConfig;
	
	private static ATMRequestConfig updateRunConfig;
	
	//TODO 回传instruction Result,用于api
	private static ATMRequestConfig updateInstructionResultConfig;

	@Override
	public void beginTestCase(){
		inTestCase = true;
		httpHeaderClient = null;
	}
	
	@Override
	public void endTestCase(){
		inTestCase = false;
		if (null != httpHeaderClient){
			try {
				httpHeaderClient.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error("Threw a IOException in HttpUtilImpl::endTestCase, full stack trace follows:",e);
			}
		}
		
		if (null != httpHeaderClient){
			httpHeaderClient = null;
		}
	}
	
	@Override
	public void setRequestHeaderConfig(String json) {
		requestHeaderConfig = requestConfig;
		requestConfigRestAPI = defaultRequestConfigRestAPI;
		
		if (StringUtil.nullOrEmpty(json)) return;
		try {
			JSONObject config = new JSONObject(json);
			RequestConfig.Builder builder = RequestConfig.custom();
			JSONObject configObj = new JSONObject();
			
			boolean hasProperties = false;
			//TODO 此处若属性增多变得繁杂,可以考虑将属性数组化反射执行
			if (config.has("ConnectTimeout")) {
				builder.setConnectTimeout(config.getInt("ConnectTimeout") * 1000);
				configObj.put("ConnectTimeout", config.getInt("ConnectTimeout") + "秒");
				hasProperties = true;
			}
			
			if (config.has("SocketTimeout")) {
				builder.setSocketTimeout(config.getInt("SocketTimeout") * 1000);
				configObj.put("SocketTimeout", config.getInt("SocketTimeout") + "秒");
				hasProperties = true;
			}
			
			if (config.has("ConnectionRequestTimeout")) {
				builder.setConnectionRequestTimeout(config.getInt("ConnectionRequestTimeout") * 1000);
				configObj.put("ConnectionRequestTimeout", config.getInt("ConnectionRequestTimeout") + "秒");
				hasProperties = true;
			}
			
			if (hasProperties) {
				requestConfigRestAPI = configObj.toString();
				requestHeaderConfig = builder.build();
			}
			
		}catch(Exception e) {
			System.err.println("设置api 配置时有异常:" + e.getMessage());
		}
	}
	
	@Override
	public String getHttpUtilConfigInfo() {
		return requestConfigRestAPI;
	}
	
	public HttpUtilImpl(int connectRequestTimeout, int connectTimeout, int socketTimeout){
		requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(connectRequestTimeout * 1000)
				.setConnectTimeout(connectTimeout * 1000)
				.setSocketTimeout(socketTimeout * 1000)
				.build();
		
		requestHeaderConfig = requestConfig;
		
		JSONObject configObj = new JSONObject();
		configObj.put("ConnectTime", connectTimeout + "秒");
		configObj.put("SocketTimeout", socketTimeout + "秒");
		configObj.put("ConnectionRequestTimeout", connectRequestTimeout + "秒");
		
		defaultRequestConfigRestAPI = requestConfigRestAPI = configObj.toString();

		SGLogger.attachHttp(this);
//		stepLogConfig = new ATMRequestConfig("stepLogs", "", MethodType.POST, this);
//		executionStepLogConfig = new ATMRequestConfig("executionLogs", "", MethodType.POST, this);
	}
	
	@Override
	protected void finalize() throws Throwable {
		SGLogger.attachHttp(null);
		super.finalize();
	}
	

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IHttpUtil#request(java.lang.String, java.lang.String, com.meowlomo.ci.beaver.bundle.interfaces.IHttpUtil.MethodType)
	 */
	@SuppressWarnings("restriction")
	@Override
	public String request(String url, String paramsBody, MethodType methodType) {
		// TODO Auto-generated method stub
		String result = null;
		logger.info("HttpUtilImpl sending [" + methodType + "] request to [" + url + "] with data \n" + paramsBody);
		
		CloseableHttpClient httpclient = null;
		CloseableHttpResponse response = null;
		try {
			httpclient = HttpClients.createDefault();
			URLEncodedUtils ur;
			HttpRequestBase request = generateMethod(url, paramsBody, methodType, true, false);
			response = httpclient.execute(request);
			logger.info("Sent \n" + request.toString());
			
			//get the response status
			int status = response.getStatusLine().getStatusCode();
			logger.debug("HttpUtilImpl::send got response with status code " + status);
			HttpEntity entity = response.getEntity();
			if(null == entity){
				logger.info("No response content returned");
				result = null;
			}else{
				//if it is ok check the json message
				String contentString = EntityUtils.toString(entity, "UTF-8");
				logger.info("HttpUtilImpl::send got response: " + contentString);
				result = contentString;
			}
		}catch (org.apache.http.conn.HttpHostConnectException e){
			logger.error("Threw a HttpHostConnectException in HttpUtilImpl::send, full stack trace :",e);
			return null;
		} catch (ClientProtocolException e) {
			logger.error("Threw a ClientProtocolException in HttpUtilImpl::send, full stack trace follows:",e);
			return null;
		} catch (IOException e) {
			logger.error("Threw a IOException in HttpUtilImpl::send, full stack trace follows:",e);
			return null;
		} catch (Exception e) {
			logger.error("Threw a "+e.getClass().getName()+" in HttpUtilImpl::send, full stack trace follows:",e);
			return null;
		}finally{
			try {
				if(response != null){
					response.close();
				}
				httpclient.close();
			} catch (IOException e) {
				logger.error("Threw a IOException in HttpUtilImpl::send, full stack trace follows:",e);
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IHttpUtil#get(java.lang.String, java.lang.String)
	 */
	@Override
	public String get(String url, String params) {
		// TODO Auto-generated method stub
		return request(url, params, MethodType.GET);
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IHttpUtil#post(java.lang.String, java.lang.String)
	 */
	@Override
	public String post(String url, String params) {
		// TODO Auto-generated method stub
		return request(url, params, MethodType.POST);
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IHttpUtil#isPrivateIP()
	 */
	@Override
	public boolean isPrivateIP(int ip) {
		// TODO Auto-generated method stub
		return (
	            (ip & 0xFF000000) == 0x00000000 || //# 0.0.0.0/8
	            (ip & 0xFF000000) == 0x0A000000 || //# 10.0.0.0/8
	            (ip & 0xFF000000) == 0x7F000000 || //# 127.0.0.0/8
	            (ip & 0xFFF00000) == 0xAC100000 || //# 172.16.0.0/12
	            (ip & 0xFFFF0000) == 0xA9FE0000 || //# 169.254.0.0/16
	            (ip & 0xFFFF0000) == 0xC0A80000);  //# 192.168.0.0/16
	}

	private HttpRequestBase generateMethod(String url, String paramsBody, MethodType methodType, boolean addUtf8, boolean useUserConfig){
		HttpRequestBase httpRequest = null;
		HttpEntityEnclosingRequestBase httpEntity = null;
		switch(methodType){
		case GET:
			httpRequest = new HttpGet(url);
			break;
		case POST:
			httpEntity = optionMethodBody(new HttpPost(url), paramsBody, addUtf8, useUserConfig);
			break;
		case PUT:
			httpEntity = optionMethodBody(new HttpPut(url), paramsBody, addUtf8, useUserConfig);
			break;
		case DELETE:
			httpRequest = new HttpDelete(url);
			break;
		case OPTIONS:
			httpRequest = new HttpOptions(url);
			break;
		case HEAD:
			httpRequest = new HttpHead(url);
			break;
		case TRACE:
			httpRequest = new HttpTrace(url);
			break;
		case PATCH:
			httpEntity = optionMethodBody(new HttpPatch(url), paramsBody, addUtf8, useUserConfig);
			break;
		default:
			break;
		}
		
		if (null != httpEntity) return httpEntity;
		if (null != httpRequest){
//			if (addUtf8) {
//				httpRequest.setHeader("Content-Type","application/json");
//			}
			
			httpRequest.setConfig(useUserConfig ? requestHeaderConfig : requestConfig);
		}
		return httpRequest;
	}
	
	private HttpRequestBase getMethod(String url, String params, MethodType methodType){
		HttpRequestBase httpRequest = null;
		HttpEntityEnclosingRequestBase httpEntity = null;
		switch(methodType){
		case GET:
			httpRequest = new HttpGet(url);
			httpRequest.setConfig(HttpUtilImpl.requestConfig);
			break;
		case POST:
			httpEntity = optionMethod(new HttpPost(url), params);
			break;
		case PUT:
			httpEntity = optionMethod(new HttpPut(url), params);
			break;
		case DELETE:
			httpRequest = new HttpDelete(url);
			httpRequest.setConfig(HttpUtilImpl.requestConfig);
			break;
		case OPTIONS:
			httpRequest = new HttpOptions(url);
			httpRequest.setConfig(HttpUtilImpl.requestConfig);
			break;
		case HEAD:
			httpRequest = new HttpHead(url);
			httpRequest.setConfig(HttpUtilImpl.requestConfig);
			break;
		case TRACE:
			httpRequest = new HttpTrace(url);
			httpRequest.setConfig(HttpUtilImpl.requestConfig);
			break;
		case PATCH:
			httpEntity = optionMethod(new HttpPatch(url), params);
			break;
		default:
			break;		
		}
		
		if (null != httpEntity) return httpEntity;
		if (null != httpRequest){
			httpRequest.setHeader("Content-Type","application/json");
		}
		return httpRequest;
	}
	
	private HttpEntityEnclosingRequestBase optionMethodBody(HttpEntityEnclosingRequestBase method, String paramsBody, boolean addJsonHeader, boolean useUserConfig){
		method.setConfig(useUserConfig ? requestHeaderConfig : requestConfig);
		
		if(paramsBody != null){
			StringEntity requestEntity = null;
			try {
				requestEntity = new StringEntity(paramsBody, "UTF-8");
			} catch (UnsupportedCharsetException e) {
				requestEntity = null;
			}
			if (null != requestEntity){
				if (addJsonHeader) {
					requestEntity.setContentType("application/json");
				}
				method.setEntity(requestEntity);
			}
		}
		return method;
	}
	
	private HttpEntityEnclosingRequestBase optionMethod(HttpEntityEnclosingRequestBase method, String params){
		method.setConfig(HttpUtilImpl.requestConfig);
		method.addHeader("Content-Type","application/json");
		if(params != null){
			StringEntity requestEntity = null;
			try {
				requestEntity = new StringEntity(params, "UTF-8");
			} catch (UnsupportedCharsetException e) {
				requestEntity = null;
			}
			if (null != requestEntity){
				requestEntity.setContentType("application/json");
				method.setEntity(requestEntity);
			}
		}
		return method;
	}

	@Override
	public CompositeRequestResult requestHeader(String url, String paramsHeader, String paramsBody, MethodType methodType) {
		CompositeRequestResult requestResult = new CompositeRequestResult();
		logger.info("\n\t[httpclient] Sending [" + methodType + "] request to [" + url + "] with data \n\t" + paramsBody);
		
		CloseableHttpResponse response = null;
		try {
			if (null == httpHeaderClient || !inTestCase)
				httpHeaderClient = HttpClients.createDefault();
					
			HttpRequestBase request = generateMethod(url, paramsBody, methodType, true, true);
			String headerArrayStr = fillRequestHeaders(paramsHeader, request);
			
			response = httpHeaderClient.execute(request);
			logger.info("\n\t[http] Request has sent " + request.toString() + "\n With headers: " + headerArrayStr);
			
			//get the response status
			requestResult.code = response.getStatusLine().getStatusCode();
			requestResult.phase = response.getStatusLine().getReasonPhrase();
			requestResult.protocol = response.getStatusLine().getProtocolVersion().toString();
			logger.info("\n\t[http] Request got response with status code [" + requestResult.code + "]");
			if (requestResult.code > 299){
				logger.info("\n\t[http] protocol is " + requestResult.protocol);
				logger.info("\n\t[http] Request got response phase :" + requestResult.phase);
			}
			HttpEntity entity = response.getEntity();
			Header[] responseHeaders = response.getAllHeaders();
			
			requestResult.responseHeaders = new String[responseHeaders.length];
			for(int i = 0; i < responseHeaders.length; ++i){
				requestResult.responseHeaders[i] = responseHeaders[i].toString();
			}
			
			if(null == entity){
				logger.info("No response content returned");
				requestResult.content = null;
			}else{
				Header contentType = entity.getContentType();
				requestResult.contentHeader = contentType.toString();
				if (contentType.getValue().startsWith("image/")){
//					writeFileAtLocal(entity, contentType);
					requestResult.code = -1;
				}else if (contentType.getValue().contains("text/") || contentType.getValue().contains("application/")){
					String typeValue = contentType.getValue().toLowerCase();
					String encodeMethod = "UTF-8";
					if (typeValue.contains("charset=")){
						encodeMethod = typeValue.substring(typeValue.indexOf("=") + 1);
						if (encodeMethod.length() > 0)
							encodeMethod = encodeMethod.trim();
					}
					String contentString = EntityUtils.toString(entity, encodeMethod);
					if (contentString.length() <= 1000)
						logger.info("\n\t[http] Request got response: " + contentString);
					else
						logger.info("\n\t[http] Request got response lenth: " + contentString.length()
							+ " and first part is :" + contentString.substring(0, 100).trim() + " ...");
					requestResult.content = contentString;
				}else{
					logger.info("\n\t[http] Request got content type: " + contentType + ". Not supported.");
					requestResult.content = "";
				}
			}
		}catch (org.apache.http.conn.HttpHostConnectException e){
			logger.error("Threw a HttpHostConnectException in HttpUtilImpl::send, full stack trace :",e);
			return null;
		} catch (ClientProtocolException e) {
			logger.error("Threw a ClientProtocolException in HttpUtilImpl::send, full stack trace follows:",e);
			return null;
		} catch (IOException e) {
			logger.error("Threw a IOException in HttpUtilImpl::send, full stack trace follows:",e);
			return null;
		} catch (Exception e) {
			logger.error("Threw a "+e.getClass().getName()+" in HttpUtilImpl::send, full stack trace follows:",e);
			return null;
		}finally{
			try {
				if (null != response){
					response.close();
				}
			} catch (IOException e) {
				logger.error("Threw a IOException in HttpUtilImpl::send, full stack trace follows:",e);
			}
		}
		
		return requestResult;
	}

	private void writeFileAtLocal(HttpEntity entity, Header contentType) throws FileNotFoundException, IOException {
		String picFileType = contentType.getValue().substring(contentType.getValue().indexOf("/") + 1);
		OutputStream picFile = new FileOutputStream("D:/cqTmp." + picFileType);
		entity.writeTo(picFile);
		picFile.close();
	}

	private String fillRequestHeaders(String paramsHeader, HttpRequestBase request) {
		if (null != request){
			if (!StringUtil.nullOrEmpty(paramsHeader)){
				JSONObject headerObj = new JSONObject(paramsHeader);
				
				Map<String, Object> headerMap = headerObj.toMap();
				for(Entry<String, Object> entry : headerMap.entrySet()){
					request.addHeader(entry.getKey(), (String)entry.getValue());
				}

				//TODO 删除 默认的 app/json header，若有需求，则放到页面上加上
//				if (!headerObj.has("Content-Type")){
//					request.addHeader("Content-Type","application/json;Charset=utf-8");				
//				}
//				
				return Arrays.toString(headerMap.keySet().toArray());
			}
		}
		return new String();
	}
	
	@Override
	public boolean clearState() {
		// TODO Auto-generated method stub
		resetLogicalStepLog();
		executionStepLogConfig.setUrl("");
		
		if (testCaseId > 0) {
			testCaseId = 0;
			System.err.println("finishRun Result:" + finishRun(null));
		}
		return IHttpUtil.super.clearState();
	}

	private void resetLogicalFlag() {
		stepLogConfig.setUrl("");
		SGLogger.stepInfoLimit = 0;
		SGLogger.stepEndOne = false;
	}
	
	private void resetLogicalStepLog() {
		resetLogicalFlag();
		executionStepLogConfig.resetMethodType();
		executionStepLogConfig.resetParam();
		stepLogConfig.resetMethodType();
	}
	
	private ATMRequestConfig genRequestConfig(String name, JSONObject json) {
		return new ATMRequestConfig(name, json.getString("url"), getMethodType(json), this).setOriginalConfig(json);
	}
	
	private ATMRequestConfig genRequestConfigUseName(String name, JSONObject json) {
		JSONObject partialJson = json.getJSONObject(name);
		return new ATMRequestConfig(name, partialJson.getString("url"), getMethodType(partialJson), this).setOriginalConfig(partialJson);
	}

	@Override
	public void attachInstructionRunData(String info) {
		JSONObject infoObj = new JSONObject(info);
		logger.info("[attachInstructionRunData]: {}", info);
		
		this.instructionRunId = infoObj.optLong("instructionRunId");
		this.instructionId = infoObj.optInt("instructionId");
		resetLogicalFlag();
		System.err.println("HttpUtilImp.attachInstructionRunData.instructionRunId:" + instructionRunId);
		System.err.println("HttpUtilImp.attachInstructionRunData.instructionId:" + instructionId);
		String url = stepLogConfig.updateUrl("{instructionResultId}", String.valueOf(instructionRunId));
		System.err.println("HttpUtilImp.attachInstructionRunData.url:" + url);
		
		executionStepLogConfig.addObjectField("instructionResultId", instructionRunId);
		finishInstructionConfig.addObjectField("id", instructionRunId);
		
		updateInstructionResultConfig.resetParam();
		updateInstructionResultConfig.addObjectField("id", instructionRunId);
		updateInstructionResultConfig.addObjectField("inputType", "api");
	}
	
	@Override
	public JSONObject attachData(String data) {
		// TODO Auto-generated method stub
		JSONObject param = IHttpUtil.super.attachData(data);
		
		JSONObject parameters = param.getJSONObject("parameters");
		JSONObject taskData = param.getJSONObject("taskData");
		testCaseId = taskData.getLong("id");
		standSingleton = param.optBoolean("standSingleton");
		System.out.println("standSingleton:" + standSingleton);
		
		JSONArray instructions = taskData.optJSONArray("instructions");
		for (Object item : instructions) {
			if (item instanceof JSONObject) {
				JSONObject instruction = (JSONObject) item;
//				int instructionId = instruction.optInt("id");
				String logicalOrderIndex = instruction.getString("logicalOrderIndex");
				originalInstructionMap.put(logicalOrderIndex, instruction);
				
				JSONObject addInstructionObj = new JSONObject();
				addInstructionObj.put("target", instruction.has("target") ? instruction.get("target") : "");
				addInstructionObj.put("action", instruction.has("elementAction") ? instruction.get("elementAction") : "");
				addInstructionObj.put("input", instruction.get("input"));
//				addInstructionObj.put("instruction", originalInstruction);
				addInstructionObj.put("logicalOrderIndex", instruction.get("logicalOrderIndex"));
				addInstructionResultMap.put(logicalOrderIndex, addInstructionObj);
			}
		}
		stepLogConfig = genRequestConfig("stepLogs", parameters.getJSONObject("addStepLog"));
		
		resetLogicalFlag();
		executionStepLogConfig = genRequestConfig("executionLogs", parameters.getJSONObject("addExecutionStepLog"));
		executionStepLogConfig.addObjectField("runId", runId());
//		executionStepLogConfig.addObjectField("instructionResultId", instructionRunId);
		
		//TODO testCaseId与runId 应在VMC中匹配完成
		finishInstructionConfig = genFinishInstructionConfig(parameters);
		addInstructionResultConfig = genAddInstructionRunConfig(parameters);
		finishRunConfig = genFinishRunConfig(parameters);
	
		addStepFileLogConfig = genRequestConfigUseName("addStepFileLog", parameters);
		System.err.println("HttpUtilImpl.attachData:addStepFileLogConfig init" + addStepFileLogConfig);
		updateInstructionResultConfig = genRequestConfigUseName("updateInstructionResult", parameters);
		updateRunConfig = genRequestConfigUseName("updateRun", parameters);
		
		System.err.println("Run Id:" + runId() + " start");
		
		return param;
	}
	
//	// TODO 此处应与api 绑定
//	private void startSGLoogerSaveToRemote(Long instructionRunId, JSONObject parameters) {
//		if (null == parameters)
//			return;
//
//		JSONObject addStepLog = parameters.getJSONObject("addStepLog");
//		String url = addStepLog.getString("url");
//		String methodType = addStepLog.getString("method");
//
//		JSONObject addExecutionStepLog = parameters.getJSONObject("addExecutionStepLog");
//		String urlExecution = addExecutionStepLog.getString("url");
//		String methodTypeExecution = addExecutionStepLog.getString("method");
//
//		// "http://10.0.100.177:8080/atm/instructionResults/{instructionResultId}/stepLogs";
//		// url = "http://10.0.100.185:8080/EMS/rest/agent/llog";
//		if (url.contains("{instructionResultId}"))
//			url = url.replace("{instructionResultId}", String.valueOf(instructionRunId));
//
//		SGLogger.attachLogBackRemoteApi(url, Enum.valueOf(MethodType.class, methodType.toUpperCase().trim()),
//				urlExecution, Enum.valueOf(MethodType.class, methodTypeExecution.toUpperCase().trim()), runId(),
//				instructionRunId);
//	}

	private ATMRequestConfig genFinishRunConfig(JSONObject parameters) {
		JSONObject finishRun = parameters.getJSONObject("finishRun");
		String finishRunUrl = finishRun.getString("url");
		finishRunUrl = finishRunUrl.replace("{testCaseId}", String.valueOf(testCaseId));
		JSONObject finishContent = finishRun.getJSONObject("content");
		finishContent.put("id", runId());
		return new ATMRequestConfig("finishRun", finishRunUrl, getMethodType(finishRun), finishContent, this);
	}

	private ATMRequestConfig genAddInstructionRunConfig(JSONObject parameters) {
		JSONObject addInstructionRun = parameters.getJSONObject("addInstructionResult");
		String addInstructionResultUrl = addInstructionRun.getString("url");
		addInstructionResultUrl = addInstructionResultUrl.replace("{testCaseId}", String.valueOf(testCaseId));
		addInstructionResultUrl = addInstructionResultUrl.replace("{runId}", String.valueOf(runId()));
		return new ATMRequestConfig("addInstructionResult", addInstructionResultUrl, getMethodType(addInstructionRun), this);
	}

	private ATMRequestConfig genFinishInstructionConfig(JSONObject parameters) {
		JSONObject finishInstructionRun = parameters.getJSONObject("finishInstructionResult");
		String finishInstructionRunUrl = finishInstructionRun.getString("url");
		finishInstructionRunUrl = finishInstructionRunUrl.replace("{testCaseId}", String.valueOf(testCaseId));
		JSONObject finishInstructionParams = finishInstructionRun.getJSONObject("content");
		finishInstructionParams.put("id", 0);//instructionRunId);
		return new ATMRequestConfig("finishInstructionResult", finishInstructionRunUrl, getMethodType(finishInstructionRun), finishInstructionParams, this);
	}

	// TODO 此方法与 SGLoggger的doublePrintOut功能完全一致,最好合并
	@Override
	public String addLogicalStepLog(String info) {
		if (standSingleton) return "";
		
		JSONObject infoObj = new JSONObject(info);
		String line = infoObj.optString("line");
		String logLevel = infoObj.optString("logLevel");
		String stepLogType = infoObj.optString("stepLogType");
		SGLogger.cacheMsg(dateFormat.format(new Date()) + line);

		// 过去的step log, 业务级别的 step log type
		do {
			if (!stepLogConfig.url().isEmpty() && !stepLogType.isEmpty()) {
				// TODO 控制相关语句最多一句,此处作多写控制,error时可能报告的信息没有落在关键点上
				if (infoStringList.contains(stepLogType)) {
					if (SGLogger.stepInfoLimit < 2)
						SGLogger.stepInfoLimit++;
					else
						break;
				} else if (endStringList.contains(stepLogType)) {
					if (!SGLogger.stepEndOne)
						SGLogger.stepEndOne = true;
					else
						break;
				}
				JSONObject stepParams = new JSONObject();
				stepParams.put("message", line);
				stepParams.put("type", stepLogType);
				addStepLog(stepParams.toString());
			}
		} while (false);

		JSONObject msg = new JSONObject();
		msg.put("message", line);
		msg.put("logLevel", logLevel);
		addExecutionStepLog(msg.toString());
		
		return "";
	}
	
	@Override
	public String addExecutionStepLog(String info) {
		if (standSingleton) return "";
		return executionStepLogConfig.addObjectString(info).request();
	}

	@Override
	public String addStepLog(String info) {
		if (standSingleton) return "";
		stepLogConfig.addObjectString(info);
		System.err.println("HttpUitlImpl.addStepLog:" + stepLogConfig.toString());
		return stepLogConfig.request();
	}

	/**
	 * 添加Instruction执行记录
	 * info null
	 */
	@Override
	public String addInstructionResult(String logicalOrderIndex) {
		addInstructionResultConfig.resetParam();
	
		JSONObject addInstruction = addInstructionResultMap.get(logicalOrderIndex);
		JSONObject originalInstruction = originalInstructionMap.get(logicalOrderIndex);
	
		if (null != addInstruction && null != originalInstruction) {
			logger.info("[attachInstructionRunData].branch");
			addInstruction.put("instruction", originalInstruction);
			addInstructionResultConfig.addParam(addInstruction);
		}
	
		// TODO check
		logger.info("[addInstructionResult] param:{}", addInstructionResultConfig.paramStr());
		logger.info("[addInstructionResult] url:{}", addInstructionResultConfig.url());
		logger.info("[addInstructionResult] type:{}", addInstructionResultConfig.methdType());

		return addInstructionResultConfig.request();
	}

	/**
	 * 一般用于API型Instruction回传执行记录
	 */
	@Override
	public String updateInstructionResult(String info) {
		if (standSingleton) return "";
		return updateInstructionResultConfig.addObjectString(info).request("updateInstructionResult");
	}


	/**
	 * 由于runId已经提前添加,不再需要经由addRun 调用添加
	 */
	@Override
	public String addRun(String info) {
		return null;
	}


	/**
	 * testcase执行结束时调用,关闭run
	 */
	@Override
	public String finishRun(String info) {
		return finishRunConfig.addObjectString(info).request();
	}


	/**
	 * info is null
	 * update instructionId
	 */
	@Override
	public String finishInstructionResult(String info) {
		// TODO Auto-generated method stub
		return finishInstructionConfig.addObjectString(info).request();
	}


	@Override
	public String addStepFileLog(String info) {
		// TODO Auto-generated method stub
		if (null != addStepFileLogConfig) {
			System.err.println("addStepFileLog.addStepFileLogConfig:" + addStepFileLogConfig.toString());
		}else {
			System.err.println("addStepFileLog.addStepFileLogConfig:null");
		}
		return addStepFileLogConfig.addObjectString(info).request();
	}

	/**
	 * 更新运时TestCase时的各种配置和用例信息
	 */
	@Override
	public String updateRun(String info) {
		JSONObject content = updateRunConfig.originalConfig().getJSONObject("content");
		String runStr = request(updateRunConfig.url() + "/" + content.optInt("id"), "{}", MethodType.GET);
	
		if (!runStr.startsWith("<")) {
			String[] ins = info.split(",");
			System.err.println("HttpUtilImpl.updateRun:" + String.join(",", ins));
			Set<String> bundleTailName = new HashSet<String>(Arrays.asList(ins));
			
			JSONObject runObj = new JSONObject(runStr);
			JSONObject originalParameter = runObj.getJSONArray("data").getJSONObject(0).getJSONObject("parameter");
			String strSystemInfo = genDriverInfo(bundleTailName, content);
			
			JsonParser parser = new JsonParser();
			JsonObject parameter = parser.parse(strSystemInfo).getAsJsonObject();
			JsonObject newParameter = parser.parse(originalParameter.toString()).getAsJsonObject();
			newParameter.add("environment", parameter);
			JsonObject bodyItem = new JsonObject();
			bodyItem.addProperty("id", content.optInt("id"));
			bodyItem.add("parameter", newParameter);
		
			return updateRunConfig.request("[" + bodyItem.toString() + "]", true);
		}
		return "";
	}
	
	@Override
	public String updateRunWIPStatus() {
		JSONObject content = updateRunConfig.originalConfig().getJSONObject("content");
		JsonObject bodyItem = new JsonObject();
		bodyItem.addProperty("id", content.optInt("id"));
		bodyItem.addProperty("status", "WIP");
		bodyItem.addProperty("startAt", startDateFormat.format(new Date()));
		return updateRunConfig.request("[" + bodyItem.toString() + "]", true);
	}
	
	private String genDriverInfo(Set<String> bundleTailName, JSONObject content) {
		JSONUtil.beginJSONObject("genDriverInfo");
		for(String bundleName : bundleTailName) {
			Object bundleImplObj = BaseBundleActivator.getTheServiceObject(bundleName);
			
			if (this == bundleImplObj)
				continue;
			
			if (null != bundleImplObj) {
				String bundleConfig = (String)ReflectionUtil.doFunctionCall(bundleImplObj, "getExecutionEnvironmentInfo", content.toString());
				if  (!StringUtil.nullOrEmpty(bundleConfig)) {
					JSONObject bundleInfo = new JSONObject(bundleConfig);
					for(String key : bundleInfo.keySet()) {
						JSONUtil.addJSONField("genDriverInfo", key, bundleInfo.getString(key));
					}
				}
			}
		}
		
		JSONObject systemInfoObj = new JSONObject(getExecutionEnvironmentInfo());
		for(String key : systemInfoObj.keySet()) {
			JSONUtil.addJSONField("genDriverInfo", key, systemInfoObj.getString(key));
		}
		return JSONUtil.endJSONObject("genDriverInfo", true);
	}
	
	private String getExecutionEnvironmentInfo() {
		JSONUtil.beginJSONObject("operating system");
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("systeminfo");
			Process shell;
			shell = pb.start();
			synchronized (pb) {
				pb.wait(3000);
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(shell.getInputStream(), "GBK"));
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				if (sCurrentLine.startsWith("OS ") && sCurrentLine.contains(":")) {
					String[] lines = sCurrentLine.split(":");
					if (2 == lines.length && !lines[0].trim().isEmpty()) {
						JSONUtil.addJSONField("operating system", lines[0].trim(), lines[1].trim());
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return JSONUtil.endJSONObject("operating system", true);
	}
}
