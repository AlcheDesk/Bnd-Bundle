package com.meowlomo.ci.ems.bundle.webdriver;

import com.meowlomo.ci.ems.bundle.utils.StringUtil;

public class StringUtils {
	public static String longestSubstring(String str1, String str2) {

		StringBuilder sb = new StringBuilder();		
		if (StringUtil.nullOrEmpty(str1) || StringUtil.nullOrEmpty(str2))
			return "";

		// ignore case
		//str1 = str1.toLowerCase();
		//str2 = str2.toLowerCase();

		// java initializes them already with 0
		int[][] num = new int[str1.length()][str2.length()];
		int maxlen = 0;
		int lastSubsBegin = 0;

		for (int i = 0; i < str1.length(); i++) {
			for (int j = 0; j < str2.length(); j++) {
				if (str1.charAt(i) == str2.charAt(j)) {
					if ((i == 0) || (j == 0))
						num[i][j] = 1;
					else
						num[i][j] = 1 + num[i - 1][j - 1];

					if (num[i][j] > maxlen) {
						maxlen = num[i][j];
						// generate substring from str1 => i
						int thisSubsBegin = i - num[i][j] + 1;
						if (lastSubsBegin == thisSubsBegin) {
							//if the current LCS is the same as the last time this block ran
							sb.append(str1.charAt(i));
						} else {
							//this block resets the string builder if a different LCS is found
							lastSubsBegin = thisSubsBegin;
							sb = new StringBuilder();
							sb.append(str1.substring(lastSubsBegin, i + 1));
						}
					}
				}
			}
		}

		return sb.toString();
	}
	
	private static String findLongestPrefixSuffix(String... inputs) {
		String returnValue = "";
		if(inputs.length ==1){
			return inputs[0];
		}else{
			returnValue = inputs[0];
			for(int j = 1 ; j < inputs.length; j++){
				for( int i = Math.min(returnValue.length(), inputs[j].length()); ; i--) {
					if(returnValue.endsWith(inputs[j].substring(0, i))) {
						returnValue =  returnValue.substring(0, i);
					}
				}    
			}
		}
		return returnValue;
	}
	
	public static String fixedLengthString(String string, int length) {
	    return String.format("%1$"+length+ "s", string);
	}
	
	public static boolean isNumeric(String str)  
	{  
	  try  
	  {  
	    double d = Double.parseDouble(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
	    return false;  
	  }  
	  return true;  
	}
}
