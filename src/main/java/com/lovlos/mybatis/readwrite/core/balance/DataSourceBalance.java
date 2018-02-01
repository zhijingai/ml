package com.lovlos.mybatis.readwrite.core.balance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.lovlos.mybatis.readwrite.util.BalanceUtil;

/**
 * 数据源负载策略
 * 
 * @author lovlos
 */
public interface DataSourceBalance {
	
	/**
	 * 数据源调用最少次数
	 */
	final AtomicInteger MIN_COUNT = new AtomicInteger(0);
	
	/**
	 * 数据源调用最多次数
	 */
	final AtomicInteger MAX_COUNT = new AtomicInteger(Integer.MAX_VALUE);
	
	/**
	 * 负载清空限制
	 */
	final Integer BALANCE_COUNT = BalanceUtil.getBalanceCount() != null ? BalanceUtil.getBalanceCount() : BalanceUtil.getDefaultBalanceCount();

	/**
	 * 负载策略
	 * @param dataSourceList
	 * @return
	 */
	String getDataSource(List<String> dataSourceList);
	
	/**
	 * 重置负载策略
	 */
	default void clearBalance() {};
}
