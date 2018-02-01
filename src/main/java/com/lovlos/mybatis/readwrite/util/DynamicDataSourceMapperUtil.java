package com.lovlos.mybatis.readwrite.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.lovlos.mybatis.readwrite.base.DataSourceSelect;

/**
 * mapper路由
 * @author lovlos
 */
public class DynamicDataSourceMapperUtil {

	// 缓存mapper接口对应的数据源择取列表
    private static volatile Map<String, DataSourceSelect> cacheMap = new ConcurrentHashMap<>();
    
    /**
     * 缓存加载
     * @param methodId
     * @param determineDataSource
     */
	public static void addCache(String methodId, DataSourceSelect determineDataSource) {
		cacheMap.put(methodId, determineDataSource);
	}

	public static Map<String, DataSourceSelect> getCacheMap() {
		return cacheMap;
	}

	public static void setCacheMap(Map<String, DataSourceSelect> cacheMap) {
		DynamicDataSourceMapperUtil.cacheMap = cacheMap;
	}
	
}
