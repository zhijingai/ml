package com.lovlos.mybatis.readwrite.core.load;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lovlos.mybatis.readwrite.DataSource;
import com.lovlos.mybatis.readwrite.base.DataSourceSelect;
import com.lovlos.mybatis.readwrite.config.DataSourceConfig;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;
import com.lovlos.mybatis.readwrite.util.DynamicDataSourceMapperUtil;

/**
 * mapper&数据源路由 缓存加载器
 * @author lovlos
 */
public class DetermineDataSourceCacheLoader {
	
	private static final Logger logger = LoggerFactory.getLogger(DetermineDataSourceCacheLoader.class);
	
	private static final String REGEX = ".*insert\\u0020.*|.*delete\\u0020.*|.*update\\u0020.*";
	
	public static DataSourceSelect loadForOneMethod(Object[] objects, MappedStatement ms) throws Throwable {	
        // 是否指定读写配置
        boolean special = false;
        DataSourceConfig dynamicDataSourceGlobal = null;
        String className = ms.getId().substring(0, ms.getId().lastIndexOf("."));
        Method[] methods = Class.forName(className).getMethods();
        DataSourceSelect determineDataSourceList = new DataSourceSelect();
        for (Method method : methods) {
            int index = ms.getId().lastIndexOf(method.getName());
            if (index == -1) {
                continue;
            }
            if (method.isAnnotationPresent(DataSource.class)) {
            	// 注解方法
            	dynamicDataSourceGlobal = putForAnnotationMethod(method, determineDataSourceList);
            	special = true;
            	break;
            }
        }
        if (!special) {
        	// 非注解方法
        	dynamicDataSourceGlobal = putForNotAnnotationMethod(objects, ms, determineDataSourceList);     	
        }
        logger.warn("设置方法[{}] use [{}] Strategy, SqlCommandType [{}]..", 
                		ms.getId(), dynamicDataSourceGlobal.name(),
                		ms.getSqlCommandType().name());
        // 缓存mapper路由列表
        DynamicDataSourceMapperUtil.getCacheMap().put(ms.getId(), determineDataSourceList);
        // 缓存数据源-mapper订阅列表
        DataSourceMapperLoader.cacheDataSourceSubscribes(ms.getId(), determineDataSourceList);
        
        return determineDataSourceList;
	}

	/**
	 * 未注解mapper方法根据操作类别使用不同数据源
	 * @param objects
	 * @param ms
	 * @param determineDataSourceList
	 * @param dynamicDataSourceGlobal
	 */
	public static DataSourceConfig putForNotAnnotationMethod(Object[] objects, MappedStatement ms, DataSourceSelect determineDataSourceList) {
		DataSourceConfig dynamicDataSourceGlobal = null;
		
		// 针对mapper方法没有使用@DataSource
        // 读取动作
        if (SqlCommandType.SELECT.equals(ms.getSqlCommandType())) {
            if (ms.getId().contains(SelectKeyGenerator.SELECT_KEY_SUFFIX)) {
            	 // !selectKey 为自增id查询主键(SELECT LAST_INSERT_ID() )方法，使用主库
                dynamicDataSourceGlobal = DataSourceConfig.MASTER;
            } else {
                BoundSql boundSql = ms.getSqlSource().getBoundSql(objects[1]);
                String sql = boundSql.getSql().toLowerCase(Locale.CHINA).replaceAll("[\\t\\n\\r]", " ");
                if (sql.matches(REGEX)) {
                    dynamicDataSourceGlobal = DataSourceConfig.MASTER;
                } else {
                    dynamicDataSourceGlobal = DataSourceConfig.SLAVE;
                }
            }
        } else {
        	// 更改动作
            dynamicDataSourceGlobal = DataSourceConfig.MASTER;
        }
        // 配置数据源列表
        setDataSourceByConfig(dynamicDataSourceGlobal, determineDataSourceList);
        
        return dynamicDataSourceGlobal;
	}

	/**
	 * 注解mapper方法加载
	 * @param method
	 * @param determineDataSourceList
	 * @param dynamicDataSourceGlobal
	 */
	public static DataSourceConfig putForAnnotationMethod(Method method, DataSourceSelect determineDataSourceList) {
		if(method == null || !method.isAnnotationPresent(DataSource.class)) {
			return null;
		}
		DataSourceConfig dynamicDataSourceGlobal = method.getAnnotation(DataSource.class).dataSourceConfig();
        
        // 处理@DataSource注解中直接指定数据源
        String[] selectDataSources = method.getAnnotation(DataSource.class).dbName();
    	if(selectDataSources != null && selectDataSources.length > 0) {    
    		// 情况1:注解中直接指定了数据源
    		List<com.lovlos.mybatis.readwrite.base.DataSource> dataSources = DataSourceUtil.getDataSources().get(dynamicDataSourceGlobal);
    		if(dataSources != null && dataSources.size() > 0) {
    			for (String selectDataSource : selectDataSources) {
    				for(com.lovlos.mybatis.readwrite.base.DataSource _dataSource : dataSources) {
    					// 检验注解声明的数据源合法
        				if(StringUtils.isNotBlank(_dataSource.getDataSourceName()) && _dataSource.getDataSourceName().equals(selectDataSource)) {
        					determineDataSourceList.addDateSource(_dataSource.getDataSourceName());
        				}
        			}
    			}
    		}
    	}else{
    		// 情况2:注解中未指定数据源 通过默认主从配置选择数据源
            setDataSourceByConfig(dynamicDataSourceGlobal, determineDataSourceList);
    	}
    	return dynamicDataSourceGlobal;
	}

	/**
     * 通过主从配置直接获取数据源列表
     * @param dataSourceConfig
     */
    public static void setDataSourceByConfig(DataSourceConfig dataSourceConfig, DataSourceSelect determineDataSourceList) {
    	Map<DataSourceConfig, List<com.lovlos.mybatis.readwrite.base.DataSource>> dataSourceMap = DataSourceUtil.getDataSources();
    	List<com.lovlos.mybatis.readwrite.base.DataSource> dataSources = dataSourceMap.get(dataSourceConfig);
 		if(dataSources != null && !dataSources.isEmpty()) {
 			for(com.lovlos.mybatis.readwrite.base.DataSource _dataSource : dataSources) {
 				if(StringUtils.isNotBlank(_dataSource.getDataSourceName())) {
 					// 从缓存数据源引用中直接获取数据源列表
 					determineDataSourceList.addDateSource(_dataSource.getDataSourceName());
 				}
 			}
 		}
    }
}
