package com.lovlos.util;

import org.springframework.context.ApplicationContext;

public class SpringUtil {
	
	private static ApplicationContext context;
	
	public static void setContext(ApplicationContext context){
		SpringUtil.context = context;
	}
	
	public static Object getBean(String beanName){
		return context.getBean(beanName);
	}
	
	public static ApplicationContext getContext(){
		return context;
	}
	
}
