package com.meowlomo.ci.ems.bundle.interfaces;

import org.json.JSONObject;

public class MOJSONObject extends JSONObject {
	public String getString(String key) {
		if (has(key) && !isNull(key)) {
			return super.getString(key);
		}
		return null;
	}
	
	public boolean getBoolean(String key) {
		if (has(key) && !isNull(key)) {
			return super.getBoolean(key);
		}
		return false;
	}
	
	public long getLong(String key) {
		if (has(key) && !isNull(key)) {
			return super.getLong(key);
		}
		return 0l;
	}
	
	public MOJSONObject(String source) {
		super(source);
	}
}
