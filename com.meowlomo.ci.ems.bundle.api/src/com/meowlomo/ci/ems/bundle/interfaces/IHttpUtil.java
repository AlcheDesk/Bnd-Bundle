package com.meowlomo.ci.ems.bundle.interfaces;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

import org.json.JSONObject;

/**
 * @author 陈琪
 *
 */
public interface IHttpUtil extends IBundleStateClearable, IBundleStepStateClearable{
	
	/**
	 * 
	 * @param info
	 * @return
	 */
	public String addLogicalStepLog(String info);
	
	/**
	 * Instruction执行碎碎念,用于代码逻辑
	 * @param info
	 * @return
	 */
	public String addExecutionStepLog(String info);
	
	/**
	 * Instruction执行碎碎念,用于业务逻辑
	 * @param info
	 * @return
	 */
	public String addStepLog(String info);
	
	/**
	 * Instruction的执行记录开始,与finishInstructionResult成对
	 * @param info
	 * @return
	 */
	public String addInstructionResult(String info);
	
	/**
	 * Intruction的执行记录更新
	 * @param info
	 * @return
	 */
	public String updateInstructionResult(String info);
	
	/**
	 * Instruction的执行记录结束,与addInstructionResult成对
	 * @param info
	 * @return
	 */
	public String finishInstructionResult(String info);
	
	/**
	 * 添加用例的Run,testcase级别
	 * @param info
	 * @return
	 */
	public String addRun(String info);
	
	/**
	 * 结束用例的Run,与addRun结对使用
	 * @param info
	 * @return
	 */
	public String finishRun(String info);
	
	/**
	 * 更新用例的Run,testcase级别,主要用来更新run的parameter字段中的运行环境,比如浏览器配置,db配置,操作系统配置等等
	 * @param info
	 * @return
	 */
	public String updateRun(String info);
	
	/**
	 * 用于压测类型时将Run置为WIP状态
	 * @param status
	 * @return
	 */
	public String updateRunWIPStatus();
	
	/**
	 * 文件拷贝到server后,将相应的文件路径与runId/instructionResultId发送回ATM
	 * @param info
	 * @return
	 */
	public String addStepFileLog(String info);
	
	public enum MethodType{
		GET,POST,PUT,DELETE,OPTIONS,HEAD,TRACE,PATCH//2.0 PATCH,MOVE,COPY,LINK,UNLINK,WRAPPED,Extension-method
	}
	
	default MethodType getMethodType(JSONObject param) {
		String methodType = param.getString("method");
		return Enum.valueOf(MethodType.class, methodType.toUpperCase().trim());
	}
	
	public static IHttpUtil getHttpTool() {
		IHttpUtil http = BaseBundleActivator.getTheServiceObject("httpclient", IHttpUtil.class);
		if (null == http)
			http = BaseBundleActivator.getTheServiceObject("curl", IHttpUtil.class);
		return http;
	}
	
	public void beginTestCase();
	public void endTestCase();
	
	public void setRequestHeaderConfig(String json);
	public String getHttpUtilConfigInfo();
	
	public String request(String url, String paramsBody, MethodType methodType);
	public String get(String url, String params);
	public String post(String url, String params);
	
	//CompositeRequestResult <==> String
	public CompositeRequestResult requestHeader(String url, String paramsHeader, String paramsBody, MethodType methodType);
	
	public boolean isPrivateIP(int ip);
	
	public class CompositeRequestResult implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = -1165241666869984082L;
		public int code = -1;
		public String phase = new String();
		public String protocol = new String();
		public String[] responseHeaders;
		public String content = new String();			//if get a file,big file , not support yet.
		public String contentHeader = new String();
		
		public static CompositeRequestResult fromString(String s) {
			byte[] data = Base64.getDecoder().decode(s);
			CompositeRequestResult result = null;
			try{
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
				Object o = ois.readObject();
				ois.close();
				if (o instanceof CompositeRequestResult)
					result = (CompositeRequestResult)o;
			}catch (IOException | ClassNotFoundException e) {
				result = null;
			}
			return result;
		}

		public static String toString(CompositeRequestResult cr) throws IOException {
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        ObjectOutputStream oos = new ObjectOutputStream( baos );
	        oos.writeObject(cr);
	        oos.close();
	        return Base64.getEncoder().encodeToString(baos.toByteArray()); 
	    }
	}
}
