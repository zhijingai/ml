package com.lovlos.mybatis.readwrite.util;

import com.lovlos.mybatis.readwrite.core.balance.DataSourceBalance;

/**
 * 负载均衡器
 * @author lovlos
 */
public class BalanceUtil {
	
	/** 负载均衡器 */
	private static DataSourceBalance dataSourceBalance;
	
	/** 默认数据源调用则重置负载均衡器 */
	private static Integer defaultBalanceCount;
	
	/** 数据源调用则重置负载均衡器 */
	private static Integer balanceCount;
	
	/** 默认负载均衡 */
	private static String defaultBalanceLocation;
	
	/** 指定负载均衡 */
	private static String balanceLocation;

	public static Integer getDefaultBalanceCount() {
		return defaultBalanceCount;
	}

	public static void setDefaultBalanceCount(Integer defaultBalanceCount) {
		BalanceUtil.defaultBalanceCount = defaultBalanceCount;
	}

	public static Integer getBalanceCount() {
		return balanceCount;
	}

	public static void setBalanceCount(Integer balanceCount) {
		BalanceUtil.balanceCount = balanceCount;
	}

	public static DataSourceBalance getDataSourceBalance() {
		return dataSourceBalance;
	}

	public static void setDataSourceBalance(DataSourceBalance dataSourceBalance) {
		BalanceUtil.dataSourceBalance = dataSourceBalance;
	}

	public static String getDefaultBalanceLocation() {
		return defaultBalanceLocation;
	}

	public static void setDefaultBalanceLocation(String defaultBalanceLocation) {
		BalanceUtil.defaultBalanceLocation = defaultBalanceLocation;
	}

	public static String getBalanceLocation() {
		return balanceLocation;
	}

	public static void setBalanceLocation(String balanceLocation) {
		BalanceUtil.balanceLocation = balanceLocation;
	}
}
