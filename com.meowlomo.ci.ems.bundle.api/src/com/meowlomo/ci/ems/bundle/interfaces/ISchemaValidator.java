package com.meowlomo.ci.ems.bundle.interfaces;

import com.meowlomo.ci.ems.bundle.utils.StringUtil;

/**
 * @author Andrew Chen
 *
 */
public interface ISchemaValidator extends IBatchProcess{
	
	void notifyTimeout();
	
	//function level
	ValidateResult validateJSONSchema(String json, String schema);//ValidateResult fromString
	ValidateResult validateXMLSchema(String json, String schema);//ValidateResult fromString
	
	public class ValidateResult{
		public ValidateResult(){
			ok = false;
			msg = "";
		}
		
		@Override
		public String toString() {
			return String.format("Result is: %s, Msg is :%s", ok, msg);
		}
		
		public static ValidateResult fromString(String src){
			if (!StringUtil.nullOrEmpty(src)){
				ValidateResult vr = new ValidateResult();
				if (src.equalsIgnoreCase("Result is: true, Msg is :")){
					vr.ok = true;
					vr.msg = "";
					return vr;
				}
				
				String[] values = src.trim().split(", Msg is :");
				if (null != values && 2 == values.length){
					String[] oks = values[0].split(" is: ");
					if (null != oks && 2 == oks.length){
						if (oks[0].equals("Result")){
							try{
								vr.ok = Boolean.valueOf(oks[1]);
								vr.msg = values[1];
								return vr;
							}catch(Exception e){
								//eat exception
							}
						}
					}
				}
			}

			return null;
		}
		
		public boolean ok;
		public String msg;	//used while it failed.
	}
}
