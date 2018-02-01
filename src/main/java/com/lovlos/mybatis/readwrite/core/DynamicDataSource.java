package com.lovlos.mybatis.readwrite.core;

import java.util.Map;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.lovlos.mybatis.readwrite.base.DataSourceSelect;
import com.lovlos.mybatis.readwrite.core.balance.DataSourceBalance;

/**
 * 数据源择取规则
 * @author lovlos
 */
public class DynamicDataSource extends AbstractRoutingDataSource {
    
    private static DataSourceBalance dataSourceBalance;
    
    /**
     * 取消默认校验机制
     */
    @Override
    public void afterPropertiesSet() {
    	
    }

    /**
     * 自定义数据源切换策略
     * @author lovlos
     */
    @Override
    protected Object determineCurrentLookupKey() { 
        // threadlocal取上层声明数据源路由列表
        DataSourceSelect dataSourceSelect = DynamicDataSourceHolder.getDataSource();
        DynamicDataSourceHolder.clearDataSource();
        if(dataSourceSelect == null || dataSourceSelect.getDateSourceList() == null || dataSourceSelect.getDateSourceList().isEmpty()) {
        	// 默认数据源defaultDataSource
        	DynamicDataSourceHolder.putDataSourceName("Default");
        	return null;
        }
        return dataSourceBalance.getDataSource(dataSourceSelect.getDateSourceList());
    }
    
    /**
     * 自定义替换druid数据源
     * @param defaultDataSource
     * @param targetDataSources
     */
    public void setTargetDataSource(Object defaultDataSource, Map<Object, Object> targetDataSources) {
    	setDefaultTargetDataSource(defaultDataSource);
    	setTargetDataSources(targetDataSources);
    	super.afterPropertiesSet();
    }

	public static void setDataSourceBalance(DataSourceBalance dataSourceBalance) {
		DynamicDataSource.dataSourceBalance = dataSourceBalance;
	}
   
}