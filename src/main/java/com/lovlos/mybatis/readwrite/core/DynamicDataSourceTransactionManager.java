package com.lovlos.mybatis.readwrite.core;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import com.lovlos.mybatis.readwrite.base.DataSourceSelect;
import com.lovlos.mybatis.readwrite.config.DataSourceConfig;
import com.lovlos.mybatis.readwrite.core.load.DetermineDataSourceCacheLoader;

public class DynamicDataSourceTransactionManager extends DataSourceTransactionManager {

	private static final long serialVersionUID = 6425322555416632032L;

	/**
     * 只读事务到读库，读写事务到写库
     * @param transaction
     * @param definition
     */
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {       
        DataSourceConfig dataSourceConfig = null;
        if (definition.isReadOnly()) {
        	dataSourceConfig = DataSourceConfig.SLAVE;
        } else {
        	dataSourceConfig = DataSourceConfig.MASTER;
        }
        // 设置数据源
        DataSourceSelect determineDataSourceList = new DataSourceSelect();
        DetermineDataSourceCacheLoader.setDataSourceByConfig(dataSourceConfig, determineDataSourceList);
        DynamicDataSourceHolder.putDataSource(determineDataSourceList);
        super.doBegin(transaction, definition);
    }

    /**
     * 清理本地线程的数据源
     * @param transaction
     */
    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        super.doCleanupAfterCompletion(transaction);
        DynamicDataSourceHolder.clearDataSource();
    }
}