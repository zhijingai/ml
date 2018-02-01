package com.lovlos.mybatis.readwrite.core.load;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ConfigurableApplicationContext;

import com.lovlos.mybatis.readwrite.DataSource;
import com.lovlos.mybatis.readwrite.base.DataSourceSelect;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;
import com.lovlos.mybatis.readwrite.util.DynamicDataSourceMapperUtil;
import com.lovlos.util.ClazzUtil;
import com.lovlos.util.FastJsonUtil;
import com.lovlos.util.SpringUtil;

/**
 * mapper数据源缓存
 * @author lovlos
 */
public class DataSourceMapperLoader {

	private static String mapperPackages;
	
	public static String getMapperPackages() {
		return mapperPackages;
	}

	public static void setMapperPackages(String mapperPackages) {
		DataSourceMapperLoader.mapperPackages = mapperPackages;
	}

	/**
	 * 初始化mapper数据源配置
	 */
	public static void loadForMapper() {
		if(StringUtils.isBlank(mapperPackages)) {
			// 没有配置缓存预热
			return;
		}
		String[] mapperPackageList = org.springframework.util.StringUtils.tokenizeToStringArray(mapperPackages, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
		for(String mapperPackage : mapperPackageList){
			if(StringUtils.isNotBlank(mapperPackage)){
				// 扫描包下的所有mapper
				List<Class<?>> classes = ClazzUtil.getClasses(mapperPackage);
				for(@SuppressWarnings("rawtypes") Class clazz : classes) {
					if(clazz.isInterface()) {
						// 处理一个mapper预热数据源配置缓存
						loadForOneMapper(clazz.getName(), mapperPackage);
					}
				}
			}
		}
		// 加载完毕
		mapperPackages = null;
	}

	/**
	 * 加载缓存
	 * @param clazz
	 * @param mapperPackage
	 * @throws Exception
	 */
	private static void loadForOneMapper(String clazzPathName, String mapperPackage) {
		if(StringUtils.isBlank(clazzPathName)){
			return;
		}
		String clazzName = clazzPathName.substring(clazzPathName.lastIndexOf(".") + 1);
		clazzName = clazzName.substring(0, 1).toLowerCase() + clazzName.substring(1);
		Object bean = SpringUtil.getBean(clazzName);
		if(bean == null) {
			// 非mapper
			return;
		}
		Method[] methods = null;
		try {
			methods = Class.forName(clazzPathName).getMethods();
		} catch (Exception e) {
			// 加载方法失败
			return;
		}
		if(methods == null || methods.length == 0) {
			return;
		}
		
		DataSourceSelect determineDataSourceList = null;
		for(Method method : methods) {
			if (method.isAnnotationPresent(DataSource.class)) {
				determineDataSourceList = new DataSourceSelect();
				String msId = clazzPathName + "." + method.getName();
				// 缓存声明数据源的方法(初始化过程中加载，添加了注解的方法使用注解声明中的数据源)
				determineDataSourceList = new DataSourceSelect();
				DetermineDataSourceCacheLoader.putForAnnotationMethod(method, determineDataSourceList);
				// 缓存进mapper路由列表
				DynamicDataSourceMapperUtil.getCacheMap().put(msId, determineDataSourceList);
				// 缓存进数据源-mapper订阅列表
				cacheDataSourceSubscribes(msId, determineDataSourceList);
				System.out.println("初始化："+ msId + " 配置数据源："+FastJsonUtil.toJSONString(determineDataSourceList));
			}
		}
	}

	/**
	 * 缓存数据源-mapper订阅列表
	 * @param msId
	 * @param determineDataSourceList
	 */
	public static void cacheDataSourceSubscribes(String msId, DataSourceSelect determineDataSourceList) {
		if(determineDataSourceList == null || determineDataSourceList.getDateSourceList() == null || determineDataSourceList.getDateSourceList().isEmpty()) {
			return;
		}
		Map<String, Map<String, Object>> dataSourceSubscribes = DataSourceUtil.getDataSourceSubscribes();
		for(String dataSource : determineDataSourceList.getDateSourceList()) {
			Map<String, Object> dataSourceSubscribe = dataSourceSubscribes.get(dataSource);
			if(dataSourceSubscribe == null) {
				dataSourceSubscribe = new ConcurrentHashMap<>();
				DataSourceUtil.getDataSourceSubscribes().put(dataSource, dataSourceSubscribe);
			}
			if(!dataSourceSubscribe.containsKey(msId)) {
				dataSourceSubscribe.put(msId, new Object());
			}
		}
	}
}
