package com.lovlos.mybatis.readwrite.monitor.heart;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.lovlos.mybatis.readwrite.base.DataSource;
import com.lovlos.mybatis.readwrite.monitor.ConnectionChecker;
import com.lovlos.mybatis.readwrite.monitor.DataSourceManager;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;
import com.lovlos.util.FastJsonUtil;

/**
 * 守护任务单线程池
 * @author lovlos
 */
public class DataSourceHeartBeatKeeper extends DataSourceHeartBeat {
	
	private static final Logger logger = LoggerFactory.getLogger(DataSourceHeartBeatKeeper.class);

	@Override
	public void checkDataSources() {
		//全部数据源
		Map<String, DataSource> invalidDataSources = DataSourceUtil.getInvalidDataSources();					
		if (!CollectionUtils.isEmpty(invalidDataSources)) {		
			for(Entry<String, DataSource> _dataSource : invalidDataSources.entrySet()) {
				DataSource dataSource = _dataSource.getValue();
				String dataSourceName = dataSource.getDataSourceName();
				boolean alive = ConnectionChecker.checkDataSourceConnection(dataSource);
				if(alive && !DataSourceUtil.getValidDataSources().containsKey(dataSourceName) && DataSourceUtil.getInvalidDataSources().containsKey(dataSourceName)) {
					// 失效转生效
					DataSourceManager.provideDataSource(dataSourceName);
				}else if(!alive && DataSourceUtil.getValidDataSources().containsKey(dataSourceName) && !DataSourceUtil.getInvalidDataSources().containsKey(dataSourceName)) {
					// 生效转失效
					//DataSourceManager.hystrixDataSource(dataSourceName, false);
				}
			}
		}
		logger.info("DataSource alive list：" + FastJsonUtil.toJSONString(DataSourceUtil.getDataSources()));
	}

}
