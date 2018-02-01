package com.lovlos.mybatis.readwrite.monitor.provide;

import com.lovlos.mybatis.readwrite.core.balance.DataSourceBalance;
import com.lovlos.mybatis.readwrite.util.BalanceUtil;

/**
 * 数据源生效器
 * @author lovlos
 */
abstract class DataSourceProvide implements Runnable {

	private static DataSourceBalance dataSourceBalance = BalanceUtil.getDataSourceBalance();
	
	@Override
    public void run() {
		// 重置数据源负载策略 
		dataSourceBalance.clearBalance();
		// 生效数据源
		provide();
    }

	public abstract void provide();
}
