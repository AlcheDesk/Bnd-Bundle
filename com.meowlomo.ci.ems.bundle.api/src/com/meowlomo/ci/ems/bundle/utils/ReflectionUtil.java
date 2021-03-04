package com.meowlomo.ci.ems.bundle.utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class ReflectionUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);
	public static Object doFunctionCall(Object serviceObj, String funcName, String jsonParam){
		Object result = null;
		boolean bExecuted = false;

		Method[] methods = serviceObj.getClass().getMethods();
		
		logger.info("Methods for {}: {}"
				, serviceObj.getClass().getName()
				, String.join(",", Lists.transform(Arrays.asList(methods), new Function<Method, String>(){
			public String apply(Method method) {
				return method.getName();
			}
		})));
		
		for(Method method : methods){
			if (method.getName().equalsIgnoreCase(funcName)){
				Class<?>[] paramTypes = method.getParameterTypes();
				if (1 == paramTypes.length){
					Class<?> paramType1 = paramTypes[0];
					if (paramType1 == String.class){
						try {
							bExecuted = true;
							result = method.invoke(serviceObj, jsonParam);
						} catch (Exception e) {
							e.printStackTrace();
							result = null;
						}
						break;
					}
				}
			}
		}
		if (!bExecuted)
			logger.info(String.format("No execution for object - %s : method - %s", serviceObj.getClass().toString(), funcName));
		return result;
	}
}
