package com.lovlos.event.ext.xml.base;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class DataSourceBeanDefinitionParser implements BeanDefinitionParser {

	private final Class<?> beanClass;
	
	public DataSourceBeanDefinitionParser(Class<?> beanClass) {
        this.beanClass = beanClass;
    }
	
	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        return beanDefinition;
	}

}
