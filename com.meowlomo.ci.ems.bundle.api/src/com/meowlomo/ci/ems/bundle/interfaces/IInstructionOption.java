//package com.meowlomo.ci.ems.bundle.interfaces;
//
//public interface IInstructionOption {
//	
//	/**
//	 * 对instruction的input执行转换(存值和函数)
//	 * @param input
//	 * @return
//	 */
//	String translateInput(String input);
//	String translateInputs(String... inputs);
//	
//	/**
//	 * 对element定位值执行转换(存值和函数)
//	 * @param locatorValue
//	 * @return
//	 */
//	String translateLocatorValue(String locatorValue);
//	
//	/**
//	 * 对所有输入参数执行函数
//	 * @param inputs
//	 * @return
//	 */
//	String[] doFunction(String... inputs);
//	
//	/**
//	 * 直接跳过Instruction
//	 * @return
//	 */
//	boolean simpleBreak();
//	
//	/**
//	 * 执行后忽略结果跳过
//	 * @return
//	 */
//	boolean executeThenBreak();
//}
