package com.lovlos.mybatis.readwrite.core;

import com.lovlos.mybatis.readwrite.base.DataSourceSelect;

public class DynamicDataSourceHolder {

	/**
	 * 数据源择取列表
	 */
    private static final ThreadLocal<DataSourceSelect> dataSourceHolder = new ThreadLocal<>();
    
    private static final ThreadLocal<String> dataSource = new ThreadLocal<>();

    private DynamicDataSourceHolder() {
        
    }

    public static void putDataSource(DataSourceSelect dataSource) {
        dataSourceHolder.set(dataSource);
    }

    public static DataSourceSelect getDataSource() {
        return dataSourceHolder.get();
    }

    public static void clearDataSource() {
        dataSourceHolder.remove();
    }
    
    public static void putDataSourceName(String dataSourceName) {
    	dataSource.set(dataSourceName);
    }
    
    public static String getDataSourceName() {
    	String dataSourceName = dataSource.get();
    	//dataSource.remove();
    	return dataSourceName;
    }

}