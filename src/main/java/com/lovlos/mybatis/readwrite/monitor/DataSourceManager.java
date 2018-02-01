package com.lovlos.mybatis.readwrite.monitor;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import com.lovlos.mybatis.readwrite.base.DataSource;
import com.lovlos.mybatis.readwrite.base.DataSourceSelect;
import com.lovlos.mybatis.readwrite.base.HystrixTask;
import com.lovlos.mybatis.readwrite.config.DataSourceConfig;
import com.lovlos.mybatis.readwrite.monitor.hystrix.DataSourceHystrixer;
import com.lovlos.mybatis.readwrite.monitor.provide.DataSourceProvider;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;
import com.lovlos.mybatis.readwrite.util.DynamicDataSourceMapperUtil;
import com.lovlos.mybatis.readwrite.util.HystrixDataSourceUtil;

/**
 * 失效转移器
 * @author lovlos
 */
public class DataSourceManager {
	
	/**
	 * 数据源生效
	 * @param dataSource
	 */
	public static void provide(DataSource dataSource) {
		String dataSourceName = dataSource.getDataSourceName();
		
		// 1.更新存活列表
		DataSourceUtil.getValidDataSources().put(dataSourceName, dataSource);
		// 2.更新死亡列表
		DataSourceUtil.getInvalidDataSources().remove(dataSourceName);
		DataSourceConfig dataSourceConfig = DataSourceUtil.getDataSourceConfigs().get(dataSourceName); // ReadDataSource-->Slave
		// 3.更新可用数据源列表
		DataSourceUtil.getDataSources().get(dataSourceConfig).add(dataSource);
		// 4.1 查数据源订阅的全部mapper
		Map<String, Object> dataSourceSubscribe = DataSourceUtil.getDataSourceSubscribes().get(dataSourceName);
		// 4.2 查mapper路由
		Map<String, DataSourceSelect> cacheMap = DynamicDataSourceMapperUtil.getCacheMap();
		for(Entry<String, Object> subscribeMapper : dataSourceSubscribe.entrySet()) {
			String msId = subscribeMapper.getKey();
			// 4.3 更新mapper路由
			DataSourceSelect dataSourceSelect = cacheMap.get(msId);
			if(!dataSourceSelect.getDateSourceList().contains(dataSourceName)) {
				dataSourceSelect.getDateSourceList().add(dataSourceName);
			}
		}
		
		// 5.数据源失效变成生效须更新数据源熔断列表
		Map<String, HystrixTask> hystrixTasks = HystrixDataSourceUtil.getHasDeal();
		if(hystrixTasks != null && !hystrixTasks.isEmpty()) {
			for(Entry<String, HystrixTask> hystrixTask : hystrixTasks.entrySet()) {
				if(dataSourceName.equals(hystrixTask.getKey())) {
					try {
						// 5.1 等待已经获取数据源连接的线程池执行完成(并行调用完成)
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// 5.2 重置数据源上报错误次数
					HystrixDataSourceUtil.getDataSourceErrTimes().put(hystrixTask.getKey(), new AtomicInteger(0));
					// 5.3 剔除已熔断数据源
					hystrixTasks.remove(dataSourceName);
					break;
				}
			}
		}
	}

	/**
	 * 数据源失效
	 * @param dataSource
	 */
	public static void close(DataSource dataSource) {
		String dataSourceName = dataSource.getDataSourceName();
		// 1.更新存活列表
		DataSourceUtil.getValidDataSources().remove(dataSourceName);
		// 2.更新死亡列表
		DataSourceUtil.getInvalidDataSources().put(dataSourceName, dataSource);
		DataSourceConfig dataSourceConfig = DataSourceUtil.getDataSourceConfigs().get(dataSourceName);
		// 3.更新可用数据源列表
		List<DataSource> dataSourceList = DataSourceUtil.getDataSources().get(dataSourceConfig);
		if(dataSourceList != null && !dataSourceList.isEmpty()) {
			for(int index = dataSourceList.size() - 1; index >= 0; index--) {
				if(dataSourceList.get(index).equals(dataSource)) {
					dataSourceList.remove(dataSource);
				}
		    }
		}
		// 4.1 查数据源订阅的全部mapper
		Map<String, Object> dataSourceSubscribe = DataSourceUtil.getDataSourceSubscribes().get(dataSourceName);
		// 4.2 查mapper路由
		Map<String, DataSourceSelect> cacheMap = DynamicDataSourceMapperUtil.getCacheMap();
		for(Entry<String, Object> subscribeMapper : dataSourceSubscribe.entrySet()) {
			String msId = subscribeMapper.getKey();
			// 4.3 更新mapper路由
			DataSourceSelect dataSourceSelect = cacheMap.get(msId);
			if(dataSourceSelect.getDateSourceList().contains(dataSourceName)) {
				dataSourceSelect.getDateSourceList().remove(dataSourceName);
			}
		}	
	}

	/**
	 * 上报数据源异常
	 * @param dataSourceName
	 */
	public static void notifyHystrixDataSource(String dataSourceName) {
		if(HystrixDataSourceUtil.getHasDeal().containsKey(dataSourceName)) {
			// 处理任务存在则表示待处理或已处理
			// 无需上报异常
			return;
		}
		Map<String, AtomicInteger> dataSourceErrTimes = HystrixDataSourceUtil.getDataSourceErrTimes();
		if(dataSourceErrTimes.get(dataSourceName).incrementAndGet() == HystrixDataSourceUtil.getDataSourceErrLimit().get(dataSourceName)) {
			// 第n次上报异常时发现达到熔断阀值即熔断异常(执行一次)
			hystrixDataSource(dataSourceName, true);
		}
	}
	
	/**
	 * 执行数据源熔断隔离
	 * @param dataSourceName
	 */
	public static void hystrixDataSource(String dataSourceName, boolean needCheckConnection) {		
		HystrixDataSourceUtil.getHasDeal().put(dataSourceName, new HystrixTask());
		// 熔断任务
		HystrixDataSourceUtil.getHystrixservice().execute(new DataSourceHystrixer(dataSourceName, needCheckConnection));
	}
	
	/**
	 * 共用熔断任务线程
	 * @param dataSourceName
	 */
	public static void provideDataSource(String dataSourceName) {
		HystrixDataSourceUtil.getHystrixservice().execute(new DataSourceProvider(dataSourceName));
	}
}
