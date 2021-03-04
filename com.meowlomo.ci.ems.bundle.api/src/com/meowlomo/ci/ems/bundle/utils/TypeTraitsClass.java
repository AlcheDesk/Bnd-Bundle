/**
 * 
 */
package com.meowlomo.ci.ems.bundle.utils;

import java.lang.reflect.ParameterizedType;

/**
 * @author tester
 *
 */
public class TypeTraitsClass<T> {
	protected T _instance;
	private Class<T> clazz;
	public TypeTraitsClass(Object obj){
		clazz = (Class<T>) ((ParameterizedType)obj.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		_instance = (T)obj;
	}
}
