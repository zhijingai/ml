package com.lovlos.mybatis.mapper.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;

/**
 * 处理主要逻辑
 * <p>
 * 项目地址 : <a href="https://github.com/abel533/Mapper"
 * target="_blank">https://github.com/abel533/Mapper</a>
 * </p>
 * 
 * @author liuzh
 */
public class MapperHelper {
	/**
	 * 注册的通用Mapper接口
	 */
	private Map<Class<?>, MapperTemplate> registerMapper = new ConcurrentHashMap<>();
	/**
	 * 缓存msid和MapperTemplate
	 */
	private Map<String, MapperTemplate> msIdCache = new ConcurrentHashMap<>();
	/**
	 * 缓存skip结果
	 */
	private final Map<String, Boolean> msIdSkip = new ConcurrentHashMap<>();

	/**
	 * 通过通用Mapper接口获取对应的MapperTemplate
	 * 
	 * @param mapperClass
	 * @return
	 * @throws Exception
	 */
	private MapperTemplate fromMapperClass(Class<?> mapperClass) {
		Method[] methods = mapperClass.getDeclaredMethods();
		Class<?> templateClass = null;
		Class<?> tempClass = null;
		Set<String> methodSet = new HashSet<String>();
		for (Method method : methods) {
			if (method.isAnnotationPresent(SelectProvider.class)) {
				SelectProvider provider = method.getAnnotation(SelectProvider.class);
				tempClass = provider.type();
				methodSet.add(method.getName());
			} else if (method.isAnnotationPresent(InsertProvider.class)) {
				InsertProvider provider = method.getAnnotation(InsertProvider.class);
				tempClass = provider.type();
				methodSet.add(method.getName());
			} else if (method.isAnnotationPresent(DeleteProvider.class)) {
				DeleteProvider provider = method.getAnnotation(DeleteProvider.class);
				tempClass = provider.type();
				methodSet.add(method.getName());
			} else if (method.isAnnotationPresent(UpdateProvider.class)) {
				UpdateProvider provider = method.getAnnotation(UpdateProvider.class);
				tempClass = provider.type();
				methodSet.add(method.getName());
			}
			if (templateClass == null) {
				templateClass = tempClass;
			} else if (templateClass != tempClass) {
				throw new RuntimeException("一个通用Mapper中只允许存在一个MapperTemplate子类!");
			}
		}
		if (templateClass == null || !MapperTemplate.class.isAssignableFrom(templateClass)) {
			throw new RuntimeException("接口中不存在包含type为MapperTemplate的Provider注解，这不是一个合法的通用Mapper接口类!");
		}
		MapperTemplate mapperTemplate = null;
		try {
			mapperTemplate = (MapperTemplate) templateClass.getConstructor(Class.class, MapperHelper.class).newInstance(mapperClass, this);
		} catch (Exception e) {
			throw new RuntimeException("实例化MapperTemplate对象失败:" + e.getMessage());
		}
		// 注册方法
		for (String methodName : methodSet) {
			try {
				mapperTemplate.addMethodMap(methodName, templateClass.getMethod(methodName, MappedStatement.class));
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(templateClass.getCanonicalName() + "中缺少" + methodName + "方法!");
			}
		}
		return mapperTemplate;
	}

	/**
	 * 注册通用Mapper接口
	 * 
	 * @param mapperClass
	 * @throws Exception
	 */
	public void registerMapper(Class<?> mapperClass) {
		if (registerMapper.get(mapperClass) == null) {
			registerMapper.put(mapperClass, fromMapperClass(mapperClass));
		} else {
			throw new RuntimeException("已经注册过的通用Mapper[" + mapperClass.getCanonicalName() + "]不能多次注册!");
		}
	}

	/**
	 * 注册通用Mapper接口
	 * 
	 * @param mapperClass
	 * @throws Exception
	 */
	public void registerMapper(String mapperClass) {
		try {
			registerMapper(Class.forName(mapperClass));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("注册通用Mapper[" + mapperClass + "]失败，找不到该通用Mapper!");
		}
	}

	/**
	 * IDENTITY的可选值
	 */
	public enum IdentityDialect {
		DB2("VALUES IDENTITY_VAL_LOCAL()"), MYSQL("SELECT LAST_INSERT_ID()"), SQLSERVER("SELECT SCOPE_IDENTITY()"), CLOUDSCAPE(
				"VALUES IDENTITY_VAL_LOCAL()"), DERBY("VALUES IDENTITY_VAL_LOCAL()"), HSQLDB("CALL IDENTITY()"), SYBASE("SELECT @@IDENTITY"), DB2_MF(
				"SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1"), INFORMIX(
				"select dbinfo('sqlca.sqlerrd1') from systables where tabid=1");
		private String identityRetrievalStatement;

		private IdentityDialect(String identityRetrievalStatement) {
			this.identityRetrievalStatement = identityRetrievalStatement;
		}

		public String getIdentityRetrievalStatement() {
			return identityRetrievalStatement;
		}

		public static IdentityDialect getDatabaseDialect(String database) {
			IdentityDialect returnValue = null;
			if ("DB2".equalsIgnoreCase(database)) {
				returnValue = DB2;
			} else if ("MySQL".equalsIgnoreCase(database)) {
				returnValue = MYSQL;
			} else if ("SqlServer".equalsIgnoreCase(database)) {
				returnValue = SQLSERVER;
			} else if ("Cloudscape".equalsIgnoreCase(database)) {
				returnValue = CLOUDSCAPE;
			} else if ("Derby".equalsIgnoreCase(database)) {
				returnValue = DERBY;
			} else if ("HSQLDB".equalsIgnoreCase(database)) {
				returnValue = HSQLDB;
			} else if ("SYBASE".equalsIgnoreCase(database)) {
				returnValue = SYBASE;
			} else if ("DB2_MF".equalsIgnoreCase(database)) {
				returnValue = DB2_MF;
			} else if ("Informix".equalsIgnoreCase(database)) {
				returnValue = INFORMIX;
			}
			return returnValue;
		}
	}

	// 基础可配置项
	private class Config {
		private String UUID;
		private String IDENTITY;
		private boolean BEFORE = false;
		private boolean cameHumpMap = false;
		private String seqFormat;
	}

	private Config config = new Config();

	public void setUUID(String UUID) {
		config.UUID = UUID;
	}

	public void setIDENTITY(String IDENTITY) {
		IdentityDialect identityDialect = IdentityDialect.getDatabaseDialect(IDENTITY);
		if (identityDialect != null) {
			config.IDENTITY = identityDialect.getIdentityRetrievalStatement();
		} else {
			config.IDENTITY = IDENTITY;
		}
	}

	public void setSeqFormat(String seqFormat) {
		config.seqFormat = seqFormat;
	}

	public void setBEFORE(String BEFORE) {
		config.BEFORE = "BEFORE".equalsIgnoreCase(BEFORE);
	}

	public void setCameHumpMap(String cameHumpMap) {
		config.cameHumpMap = "TRUE".equalsIgnoreCase(cameHumpMap);
	}

	public String getUUID() {
		if (config.UUID != null && config.UUID.length() > 0) {
			return config.UUID;
		}
		return "@java.util.UUID@randomUUID().toString().replace(\"-\", \"\")";
	}

	public String getIDENTITY() {
		if (config.IDENTITY != null && config.IDENTITY.length() > 0) {
			return config.IDENTITY;
		}
		// 针对mysql的默认值
		return IdentityDialect.MYSQL.getIdentityRetrievalStatement();
	}

	public boolean getBEFORE() {
		return config.BEFORE;
	}

	public boolean isCameHumpMap() {
		return config.cameHumpMap;
	}

	public String getSeqFormat() {
		if (config.seqFormat != null && config.seqFormat.length() > 0) {
			return config.seqFormat;
		}
		return "{0}.nextval";
	}

	/**
	 * 判断当前的接口方法是否需要进行拦截
	 * 
	 * @param msId
	 * @return
	 */
	public boolean isMapperMethod(String msId) {
		if (msIdSkip.get(msId) != null) {
			return msIdSkip.get(msId);
		}
		for (Map.Entry<Class<?>, MapperTemplate> entry : registerMapper.entrySet()) {
			if (entry.getValue().supportMethod(msId)) {
				msIdSkip.put(msId, true);
				return true;
			}
		}
		msIdSkip.put(msId, false);
		return false;
	}

	/**
	 * 获取MapperTemplate
	 * 
	 * @param msId
	 * @return
	 */
	private MapperTemplate getMapperTemplate(String msId) {
		MapperTemplate mapperTemplate = null;
		if (msIdCache.get(msId) != null) {
			mapperTemplate = msIdCache.get(msId);
		} else {
			for (Map.Entry<Class<?>, MapperTemplate> entry : registerMapper.entrySet()) {
				if (entry.getValue().supportMethod(msId)) {
					mapperTemplate = entry.getValue();
					break;
				}
			}
			msIdCache.put(msId, mapperTemplate);
		}
		return mapperTemplate;
	}

	/**
	 * 重新设置SqlSource
	 * 
	 * @param ms
	 */
	public void setSqlSource(MappedStatement ms) {
		MapperTemplate mapperTemplate = getMapperTemplate(ms.getId());
		try {
			mapperTemplate.setSqlSource(ms);
		} catch (Exception e) {
			throw new RuntimeException("调用方法异常:" + e.getMessage());
		}
	}

	/**
	 * 处理Key为驼峰式
	 * 
	 * @param result
	 * @param ms
	 */
	@SuppressWarnings({ "rawtypes"})
	public void cameHumpMap(Object result, MappedStatement ms) {
		ResultMap resultMap = ms.getResultMaps().get(0);
		Class<?> type = resultMap.getType();
		// 只有有返回值并且type是Map的时候,还不能是嵌套复杂的resultMap,才需要特殊处理
		if (result instanceof List && ((List) result).size() > 0 && Map.class.isAssignableFrom(type) && !resultMap.hasNestedQueries()
				&& !resultMap.hasNestedResultMaps()) {
			List resultList = (List) result;
			// 1.resultType时
			if (resultMap.getId().endsWith("-Inline")) {
				for (Object re : resultList) {
					processMap((Map) re);
				}
			} else {// 2.resultMap时
				for (Object re : resultList) {
					processMap((Map) re, resultMap.getResultMappings());
				}
			}
		}
	}

	/**
	 * 处理简单对象
	 * 
	 * @param map
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processMap(Map map) {
		Map cameHumpMap = new HashMap();
		Iterator<Map.Entry> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry entry = iterator.next();
			String key = (String) entry.getKey();
			String cameHumpKey = key.toLowerCase();// //EntityHelper.underlineToCamelhump(key.toLowerCase());
			if (!key.equals(cameHumpKey)) {
				cameHumpMap.put(cameHumpKey, entry.getValue());
				iterator.remove();
			}
		}
		map.putAll(cameHumpMap);
	}

	/**
	 * 配置过的属性不做修改
	 * 
	 * @param map
	 * @param resultMappings
	 */
	@SuppressWarnings({ "rawtypes", "unchecked"})
	private void processMap(Map map, List<ResultMapping> resultMappings) {
		Set<String> propertySet = toPropertySet(resultMappings);
		Map cameHumpMap = new HashMap();
		Iterator<Map.Entry> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry entry = iterator.next();
			String key = (String) entry.getKey();
			if (propertySet.contains(key)) {
				continue;
			}
			String cameHumpKey = key.toLowerCase();// EntityHelper.underlineToCamelhump(key.toLowerCase());
			if (!key.equals(cameHumpKey)) {
				cameHumpMap.put(cameHumpKey, entry.getValue());
				iterator.remove();
			}
		}
		map.putAll(cameHumpMap);
	}

	/**
	 * 列属性转Set
	 * 
	 * @param resultMappings
	 * @return
	 */
	private Set<String> toPropertySet(List<ResultMapping> resultMappings) {
		Set<String> propertySet = new HashSet<String>();
		for (ResultMapping resultMapping : resultMappings) {
			propertySet.add(resultMapping.getProperty());
		}
		return propertySet;
	}
}
