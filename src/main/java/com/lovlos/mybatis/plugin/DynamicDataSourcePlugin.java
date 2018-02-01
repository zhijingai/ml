package com.lovlos.mybatis.plugin;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.mapping.MappedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.lovlos.mybatis.readwrite.base.DataSourceSelect;
import com.lovlos.mybatis.readwrite.core.DynamicDataSourceHolder;
import com.lovlos.mybatis.readwrite.core.load.DetermineDataSourceCacheLoader;
import com.lovlos.mybatis.readwrite.util.DynamicDataSourceMapperUtil;

/**
 * 动态切换数据源插件
 * @author lovlos
 */
public class DynamicDataSourcePlugin {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourcePlugin.class);
        
    private static Lock lock = new ReentrantLock();

    public static void intercept(Object[] objects, MappedStatement ms) throws Throwable {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
        	// mapper数据源择取列表
        	DataSourceSelect determineDataSourceList = DynamicDataSourceMapperUtil.getCacheMap().get(ms.getId());            
            if ((determineDataSourceList == null)) {
            	// 取不到缓存提交缓存更新任务
            	// 刚启动时有并发问题待优化
            	// 直接走默认数据库
            	if(lock.tryLock()){
            		try {
            			// Thread.sleep(5000);
            			DetermineDataSourceCacheLoader.loadForOneMethod(objects, ms);
            			logger.info("***实时加载mapper缓存完毕***");
					} finally {
						lock.unlock();
					}
            	}
            	return;
            }
            // 缓存数据源
            DynamicDataSourceHolder.putDataSource(determineDataSourceList);
        }
    }
 
}
