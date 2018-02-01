package com.lovlos.mybatis.readwrite.monitor.provide;

import com.lovlos.mybatis.readwrite.base.DataSource;
import com.lovlos.mybatis.readwrite.monitor.DataSourceManager;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;

public class DataSourceProvider extends DataSourceProvide {
	
	private String dataSourceName;
	
	public DataSourceProvider(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	@Override
	public void provide() {
		DataSource dataSource = DataSourceUtil.getAllDataSources().get(dataSourceName);
		DataSourceManager.provide(dataSource);
		System.out.println("数据源：[" + dataSourceName + "] 生效");	
	}

}
