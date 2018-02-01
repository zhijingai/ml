package com.lovlos.event.ext.xml;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import com.lovlos.event.ext.xml.base.DataSourceBeanDefinitionParser;
import com.lovlos.event.ext.xml.base.LovlosSettings;

public class DataSourceSupport extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("setting", new DataSourceBeanDefinitionParser(LovlosSettings.class));
		registerBeanDefinitionParser("mappers", new DataSourceBeanDefinitionParser(LovlosSettings.class));
		registerBeanDefinitionParser("master", new DataSourceBeanDefinitionParser(LovlosSettings.class));
		registerBeanDefinitionParser("slave", new DataSourceBeanDefinitionParser(LovlosSettings.class));	
		registerBeanDefinitionParser("balance", new DataSourceBeanDefinitionParser(LovlosSettings.class));	
	}

}