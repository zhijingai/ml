package com.lovlos.mybatis.readwrite.core.balance;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.lovlos.mybatis.readwrite.core.DynamicDataSourceHolder;
import com.lovlos.util.FastJsonUtil;
import com.lovlos.util.ThreadPoolUtil;

/**
 * 轮询
 * @author lovlos
 */
public final class RoundRobinDataSourceBalance implements DataSourceBalance {

	/** 记录数据源实时调用次数 */
	private static final ConcurrentHashMap<String, AtomicInteger> COUNT_MAP = new ConcurrentHashMap<>();
	
	/** 负载重置器 */
	private static final ExecutorService clearService = ThreadPoolUtil.singleCachePool();
			
	public static ConcurrentHashMap<String, AtomicInteger> getCountMap() {
		return COUNT_MAP;
	}
	
	@Override
	public String getDataSource(List<String> dataSourceList) {
		if (dataSourceList == null || dataSourceList.isEmpty()) {
			return null;
		}
		// 轻负载
		String name = null;
		AtomicInteger min = MAX_COUNT;
		AtomicInteger max = MIN_COUNT;
		for (String dataSource : dataSourceList) {
			AtomicInteger count = COUNT_MAP.get(dataSource);
			if (min.get() > count.get()) {
				// 最轻负载次数
				min = count;
				// 最轻负载数据源
				name = dataSource;
			}
			if (max.get() < count.get()) {
				// 深负载数据源
				max = count;
			}
		}
		int curr = min.incrementAndGet();		
		if (max.get() == BALANCE_COUNT) {
			clearBalance();
		}
		System.out.println("负载均衡器 可选列表:{"+ FastJsonUtil.toJSONString(dataSourceList) +"} 数据源:{" + name + "} 调度次数 {" + curr + "}");
		DynamicDataSourceHolder.putDataSourceName(name);
		return name;
	}

	@Override
	public void clearBalance() {
		clearService.execute(new Runnable() {			
			@Override
			public void run() {
				for (String dataSource : COUNT_MAP.keySet()) {
					COUNT_MAP.put(dataSource, new AtomicInteger(0));
				}
			}
		});	
	}	
}
