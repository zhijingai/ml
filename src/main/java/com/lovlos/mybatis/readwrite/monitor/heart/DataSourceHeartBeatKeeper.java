package com.lovlos.mybatis.readwrite.monitor.heart;

import java.util.Map;
import java.util.Map.Entry;

import com.lovlos.mybatis.readwrite.base.DataSource;
import com.lovlos.mybatis.readwrite.monitor.DataSourceManager;
import com.lovlos.mybatis.readwrite.monitor.ConnectionChecker;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;
import com.lovlos.util.FastJsonUtil;

/**
 * 守护任务单线程池
 * @author lovlos
 */
public class DataSourceHeartBeatKeeper extends DataSourceHeartBeat {

	@Override
	public void checkDataSources() {
		//全部数据源
		Map<String, DataSource> allDataSources = DataSourceUtil.getAllDataSources();						
		for(Entry<String, DataSource> _dataSource : allDataSources.entrySet()) {
			DataSource dataSource = _dataSource.getValue();
			String dataSourceName = dataSource.getDataSourceName();
			boolean alive = ConnectionChecker.checkDataSourceConnection(dataSource);
			if(alive && !DataSourceUtil.getValidDataSources().containsKey(dataSourceName) && DataSourceUtil.getInvalidDataSources().containsKey(dataSourceName)) {
				// 失效转生效
				DataSourceManager.provideDataSource(dataSourceName);
			}else if(!alive && DataSourceUtil.getValidDataSources().containsKey(dataSourceName) && !DataSourceUtil.getInvalidDataSources().containsKey(dataSourceName)) {
				// 生效转失效
				DataSourceManager.hystrixDataSource(dataSourceName, false);
			}
		}		
		System.out.println("DataSource alive list：" + FastJsonUtil.toJSONString(DataSourceUtil.getDataSources()));
	}

}
