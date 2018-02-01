package com.lovlos.mybatis.readwrite.monitor.hystrix;

import com.lovlos.mybatis.readwrite.base.DataSource;
import com.lovlos.mybatis.readwrite.monitor.DataSourceManager;
import com.lovlos.mybatis.readwrite.monitor.ConnectionChecker;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;

/**
 * 数据源隔离熔断器
 * @author lovlos
 */
public class DataSourceHystrixer extends DataSourceHystrix {
	
	/**
	 * 数据源名称
	 */
	private String dataSourceName;
	
	/**
	 * 是否需要判断连接可用
	 * 默认false强制隔离数据源
	 */
	private boolean needCheckConnection;
	
	public DataSourceHystrixer(String dataSourceName, boolean needCheckConnection) {
		this.dataSourceName = dataSourceName;
		this.needCheckConnection = needCheckConnection;
	}
	
	/**
	 * 默认熔断数据源机制
	 */
	@Override
	public void close() {
		boolean alive = false;
		DataSource dataSource = DataSourceUtil.getAllDataSources().get(dataSourceName);
		if(needCheckConnection) {
			alive = ConnectionChecker.checkDataSourceConnection(dataSource);
		}
		if(!alive && DataSourceUtil.getValidDataSources().containsKey(dataSourceName) && !DataSourceUtil.getInvalidDataSources().containsKey(dataSourceName)) {
			DataSourceManager.close(dataSource);
			System.out.println("数据源：[" + dataSourceName + "] 失效");	
		}	
		System.out.println("熔断器检验-数据源 [ "+ dataSourceName + (alive ? "{ 存活 }":"{ 失效 }") +" ] ");
	}
}
