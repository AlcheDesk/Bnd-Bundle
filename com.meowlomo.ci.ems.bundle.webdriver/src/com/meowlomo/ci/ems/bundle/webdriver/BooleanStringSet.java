package com.meowlomo.ci.ems.bundle.webdriver;

import java.util.HashSet;

public class BooleanStringSet {
	private static HashSet<String> trueSet = new HashSet<String>() {
		/**
		* 
		*/
		private static final long serialVersionUID = -894235943574084423L;

		{
			add("true");
			add("yes");
			add("check");
			add("True");
			add("Yes");
			add("Check");
			add("Ok");
			add("Click");
			add("click");
		}
	};
	
	private static HashSet<String> falseSet = new HashSet<String>() {
		/**
		* 
		*/
		private static final long serialVersionUID = -3082081860742750933L;

		{
			add("false");
			add("no");
			add("uncheck");
			add("False");
			add("No");
			add("Uncheck");
		}
	};
		public static boolean isTrue(String input){
			if(BooleanStringSet.falseSet.contains(input)){
				return false;
			}else if(BooleanStringSet.trueSet.contains(input)){
				return true;
			}else{
				return false;
			}
		}
		
		public static boolean contains(String input){
			if(BooleanStringSet.falseSet.contains(input)){
				return true;
			}else if(BooleanStringSet.trueSet.contains(input)){
				return true;
			}else{
				return false;
			}
		}
		
}

