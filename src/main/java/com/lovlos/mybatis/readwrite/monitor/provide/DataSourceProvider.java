package com.lovlos.mybatis.readwrite.monitor.provide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lovlos.mybatis.readwrite.base.DataSource;
import com.lovlos.mybatis.readwrite.monitor.DataSourceManager;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;

public class DataSourceProvider extends DataSourceProvide {
	
	private static final Logger logger = LoggerFactory.getLogger(DataSourceProvider.class);
	
	private String dataSourceName;
	
	public DataSourceProvider(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	@Override
	public void provide() {
		DataSource dataSource = DataSourceUtil.getAllDataSources().get(dataSourceName);
		DataSourceManager.provide(dataSource);
		logger.info("DataSourceProvider - 数据源：[" + dataSourceName + "]");
	}

}
