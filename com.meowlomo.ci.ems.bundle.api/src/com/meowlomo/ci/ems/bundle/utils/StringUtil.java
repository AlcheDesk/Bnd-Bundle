package com.meowlomo.ci.ems.bundle.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
	
	private static String zhPattern = "[\\u4e00-\\u9fa5]+";  
	public static String encode(String str, String charset)  throws UnsupportedEncodingException {  
        str = str.replaceAll(" ", "+");
        Pattern p = Pattern.compile(zhPattern);  
        Matcher m = p.matcher(str);  
        StringBuffer b = new StringBuffer();  
        while (m.find()) {  
            m.appendReplacement(b, URLEncoder.encode(m.group(0), charset));  
        }  
        m.appendTail(b);  
        return b.toString();  
    }
	
	public static boolean nullOrEmpty(String target) {
		return null == target || target.isEmpty();
	}
}
