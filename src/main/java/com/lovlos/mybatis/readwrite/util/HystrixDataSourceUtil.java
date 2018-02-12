package com.lovlos.mybatis.readwrite.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.lovlos.mybatis.readwrite.base.HystrixTask;
import com.lovlos.util.ThreadPoolUtil;

public class HystrixDataSourceUtil {
	
	/**
	 * 默认熔断数据源数额
	 */
	public static final Integer DEFAULT_HYSTRIX_NUM = 1;
	
	/**
	 * 数据源异常次数限制
	 */
	private static Map<String, Integer> dataSourceErrLimit = new HashMap<>();
	
	/**
	 * 上报数据源错误次数
	 */
	private static volatile Map<String, AtomicInteger> dataSourceErrTimes = new ConcurrentHashMap<>();
	
	/**
	 * 已处理数据源
	 */
	private static volatile Map<String, HystrixTask> hasDeal = new ConcurrentHashMap<>();
	
	/**
	 * 熔断器工作线程
	 * 设计为单线程可回收线程池
	 */
	private static final ExecutorService hystrixService = ThreadPoolUtil.singleCachePool();

	public static Map<String, Integer> getDataSourceErrLimit() {
		return dataSourceErrLimit;
	}

	public static void setDataSourceErrLimit(Map<String, Integer> dataSourceErrLimit) {
		HystrixDataSourceUtil.dataSourceErrLimit = dataSourceErrLimit;
	}

	public static Map<String, AtomicInteger> getDataSourceErrTimes() {
		return dataSourceErrTimes;
	}

	public static void setDataSourceErrTimes(Map<String, AtomicInteger> dataSourceErrTimes) {
		HystrixDataSourceUtil.dataSourceErrTimes = dataSourceErrTimes;
	}

	public static ExecutorService getHystrixservice() {
		return hystrixService;
	}

	public static Map<String, HystrixTask> getHasDeal() {
		return hasDeal;
	}

	public static void setHasDeal(Map<String, HystrixTask> hasDeal) {
		HystrixDataSourceUtil.hasDeal = hasDeal;
	}

}
