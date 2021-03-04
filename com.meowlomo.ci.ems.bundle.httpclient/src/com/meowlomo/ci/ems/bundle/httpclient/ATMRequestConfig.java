package com.meowlomo.ci.ems.bundle.httpclient;

import org.json.JSONArray;
import org.json.JSONObject;

import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil.MethodType;
import com.meowlomo.ci.ems.bundle.utils.JSONUtil;
import com.meowlomo.ci.ems.bundle.utils.StringUtil;

public class ATMRequestConfig {
	//接口名称,用于写本地Log
	private String name = "";
	private String atmApiUrl = "";
	private String atmApiUrlTemplate = "";
	private IHttpUtil.MethodType atmApiMethodType = MethodType.POST;
	private JSONArray atmApiParam = null;
	
	private String logPrefix = "";
	private IHttpUtil httpUtil = null;
	
	private JSONObject originalConfig = null;
	
	public String url() {return atmApiUrl;}
	public String urlTemplate() {return atmApiUrlTemplate;}
	public String paramStr() {return null == atmApiParam ? null : atmApiParam.toString();}
	public String methdType() {return atmApiMethodType.toString();}
	
	ATMRequestConfig(String selfName, String url, MethodType type, JSONArray param, IHttpUtil http){
		name = selfName; 
//		logPrefix = prefix;
		atmApiUrl = url;
		atmApiMethodType = type;
		atmApiParam = param;
		httpUtil = http;
	}
	
	ATMRequestConfig(String selfName, String url, MethodType type, JSONObject param, IHttpUtil http){
		name = selfName;
//		logPrefix = prefix;
		atmApiUrl = url;
		atmApiMethodType = type;
		atmApiParam = new JSONArray();
		atmApiParam.put(param);
		httpUtil = http;
	}
	
	ATMRequestConfig(String selfName, String prefix, String url, MethodType type, IHttpUtil http){
		name = selfName;
		logPrefix = prefix;
		atmApiUrl = url;
		atmApiMethodType = type;
		atmApiParam = new JSONArray();
		httpUtil = http;
	}
	
	ATMRequestConfig(String selfName, String url, MethodType type, IHttpUtil http){
		atmApiUrlTemplate = atmApiUrl = url;
		atmApiMethodType = type;
		atmApiParam = new JSONArray();
		httpUtil = http;
	}
	
	public static ATMRequestConfig genEmpty() {
		return new ATMRequestConfig();
	}
	
	// 隐藏无参数ctor
	private ATMRequestConfig() {
		
	}
	
	@Override
	public String toString() {
		JSONObject tmp = new JSONObject();
		tmp.put("name", name);
		tmp.put("url", atmApiUrl);
		tmp.put("methodType", atmApiMethodType);
		tmp.put("urlTemplate", atmApiUrlTemplate);
		tmp.put("param", atmApiParam);
		return tmp.toString();
	}
	
	public String request() {
		return httpUtil.request(atmApiUrl, atmApiParam.toString(), atmApiMethodType);
	}
	
	public String request(String prefix) {
		System.err.println("The " + prefix + " ATMRequestConfig request:");
		System.err.println("url: " + atmApiUrl);
		System.err.println("param: " + atmApiParam.toString());
		System.err.println("type: " + atmApiMethodType.toString());
		
		return httpUtil.request(atmApiUrl, atmApiParam.toString(), atmApiMethodType);
	}
	
	public String request(String param, boolean directParam) {
		return httpUtil.request(atmApiUrl, directParam ? param : JSONUtil.wrapInArray(param), atmApiMethodType);
	}
	
	
	public void addParam(JSONObject param) {
		if (null != atmApiParam)
			atmApiParam.put(param);
	}
	
	public ATMRequestConfig addObjectField(String key, String value) {
		if (null != atmApiParam) {
			if (0 == atmApiParam.length()) {
				JSONObject param = new JSONObject();
				param.put(key, value);
				atmApiParam.put(param);
			} else {
				// TODO
				atmApiParam.getJSONObject(0).put(key, value);
			}
		} else {
			atmApiParam = new JSONArray();
			JSONObject tmp = new JSONObject();
			tmp.put(key, value);
			atmApiParam.put(tmp);
		}
		return this;
	}
	
	public ATMRequestConfig addObjectField(String key, long value) {
		if (null != atmApiParam) {
			if (0 == atmApiParam.length()) {
				JSONObject param = new JSONObject();
				param.put(key, value);
				atmApiParam.put(param);
			} else {
				// TODO
				atmApiParam.getJSONObject(0).put(key, value);
			}
		} else {
			atmApiParam = new JSONArray();
			JSONObject tmp = new JSONObject();
			tmp.put(key, value);
			atmApiParam.put(tmp);
		}
		return this;
	}
	
	public ATMRequestConfig addObjectString(String objStr) {
		if (!StringUtil.nullOrEmpty(objStr) && null != atmApiParam) {
			try {
				JSONObject param = new JSONObject(objStr);
				if (0 == atmApiParam.length()) {
					atmApiParam.put(param);
				} else {
					JSONObject oldObj = atmApiParam.getJSONObject(0);
					JSONObject newObj = JSONUtil.merge(param, oldObj);
					atmApiParam = new JSONArray();
					atmApiParam.put(newObj);
				}
			} catch (Exception e) {
				//TODO
				e.printStackTrace();
			}
		}
		return this;
	}
	
	public JSONArray resetParam() {
		return atmApiParam = new JSONArray();
	}
	
	public IHttpUtil.MethodType resetMethodType() {
		return atmApiMethodType = MethodType.POST;
	}
	
	public String setUrl(String url) {
		return atmApiUrl = url;
	}
	
	public String updateUrl(CharSequence regx, CharSequence value) {
		if (atmApiUrlTemplate.contains(regx))
			return atmApiUrl = atmApiUrlTemplate.replace(regx, value);
		return atmApiUrl;
	}
	
	public ATMRequestConfig setOriginalConfig(JSONObject config) {
		originalConfig = config;
		return this;
	}
	
	public JSONObject originalConfig() {
		return originalConfig;
	}
}
