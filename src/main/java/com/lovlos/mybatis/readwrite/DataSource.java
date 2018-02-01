package com.lovlos.mybatis.readwrite;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lovlos.mybatis.readwrite.config.DataSourceConfig;

/**
 * 采用注解可以走指定数据源
 * 不使用注解时，根据sql性质判定数据源
 * @author lovlos
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {
	
	/**
	 * 数据源类型(默认从库)
	 * @return
	 */
	DataSourceConfig dataSourceConfig() default DataSourceConfig.SLAVE;
    
    /**
     * 指定数据库
     * @return
     */
    String[] dbName() default {};
    
}
