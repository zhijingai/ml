package com.lovlos.mybatis.readwrite.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.lovlos.mybatis.readwrite.base.DataSource;
import com.lovlos.mybatis.readwrite.config.DataSourceConfig;
import com.lovlos.util.ThreadPoolUtil;

public class DataSourceUtil {
		
	/**
	 * 可用数据源列表
	 */
	private static volatile Map<DataSourceConfig, List<DataSource>> dataSources;	
	
	/**
	 * 数据源存活列表
	 */
	private static volatile Map<String, DataSource> validDataSources;
	
	/**
	 * 数据源失效列表
	 */
	private static volatile Map<String, DataSource> invalidDataSources;
	
	/**
	 * 全部数据源
	 */
	private static volatile Map<String, DataSource> allDataSources;
	
	/**
	 * 全部数据源的配置
	 * 数据源beanName-->读写Master/Slave
	 * 如：ReadDataSourceTwo-->Slave 
	 */
	private static volatile Map<String, DataSourceConfig> dataSourceConfigs;
	
	/**
	 * 数据源-mapper订阅列表
	 * 即一个数据源被哪些mapper路由
	 */
	private static volatile Map<String, Map<String, Object>> dataSourceSubscribes;
	
	/**
	 * heartbeat线程池
	 */
	private static final ExecutorService executorService = ThreadPoolUtil.singleScheduledPool(1);
	
	public static Map<DataSourceConfig, List<DataSource>> getDataSources() {
		return dataSources;
	}

	public static void setDataSources(Map<DataSourceConfig, List<DataSource>> dataSources) {
		DataSourceUtil.dataSources = dataSources;
	}

	public static Map<String, DataSource> getAllDataSources() {
		return allDataSources;
	}

	public static void setAllDataSources(Map<String, DataSource> allDataSources) {
		DataSourceUtil.allDataSources = allDataSources;
	}

	public static ExecutorService getExecutorService() {
		return executorService;
	}

	public static Map<String, DataSourceConfig> getDataSourceConfigs() {
		return dataSourceConfigs;
	}

	public static void setDataSourceConfigs(Map<String, DataSourceConfig> dataSourceConfigs) {
		DataSourceUtil.dataSourceConfigs = dataSourceConfigs;
	}

	public static Map<String, DataSource> getValidDataSources() {
		return validDataSources;
	}

	public static void setValidDataSources(Map<String, DataSource> validDataSources) {
		DataSourceUtil.validDataSources = validDataSources;
	}

	public static Map<String, DataSource> getInvalidDataSources() {
		return invalidDataSources;
	}

	public static void setInvalidDataSources(Map<String, DataSource> invalidDataSources) {
		DataSourceUtil.invalidDataSources = invalidDataSources;
	}

	public static Map<String, Map<String, Object>> getDataSourceSubscribes() {
		return dataSourceSubscribes;
	}

	public static void setDataSourceSubscribes(Map<String, Map<String, Object>> dataSourceSubscribes) {
		DataSourceUtil.dataSourceSubscribes = dataSourceSubscribes;
	}
		
}
