package com.lovlos.mybatis.plugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lovlos.mybatis.readwrite.core.DynamicDataSourceHolder;
import com.lovlos.mybatis.readwrite.monitor.DataSourceManager;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;

/**
 * MyBatis 性能拦截器，用于输出每条 SQL 语句及其执行时间
 * @author lovlos
 */
@Intercepts({
		@Signature(type = Executor.class, method = "query", args = {
				MappedStatement.class, Object.class, RowBounds.class,
				ResultHandler.class }),
		@Signature(type = Executor.class, method = "update", args = {
				MappedStatement.class, Object.class }) })
public class PerformancePlugin implements Interceptor {
	
	private static final Logger logger = LoggerFactory.getLogger(PerformancePlugin.class);
	
	private static final String DATE_FORMAR = "yyyy-MM-dd HH:mm:ss";
	
	private final DateFormat FORMATER = new SimpleDateFormat(DATE_FORMAR);

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
		Object parameterObject = null;
		if (invocation.getArgs().length > 1) {
			parameterObject = invocation.getArgs()[1];
		}
		String statementId = mappedStatement.getId();
		BoundSql boundSql = mappedStatement.getBoundSql(parameterObject);
		Configuration configuration = mappedStatement.getConfiguration();
		String sql = getSql(boundSql, parameterObject, configuration);
		long start = System.currentTimeMillis();
		Object result = null;
		try {
			result = invocation.proceed();
		} catch (Throwable throwable) {
			// 上报数据源不可用
			if(throwable.getCause() instanceof CommunicationsException) {
				DataSourceManager.notifyHystrixDataSource(DynamicDataSourceHolder.getDataSourceName());				
			}
			throw throwable;		
		}
		long end = System.currentTimeMillis();
		long times = end - start;
		String dataSource = DynamicDataSourceHolder.getDataSourceName();
		logger.info("数据源：[" + dataSource + "] - 耗时：" + times + " ms" + " - id:" + statementId + " - Sql:" + sql);
		return result;
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof Executor) {
			return Plugin.wrap(target, this);
		}
		return target;
	}

	@Override
	public void setProperties(Properties properties) {
	}

	private String getSql(BoundSql boundSql, Object parameterObject,
			Configuration configuration) {
		String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
		List<ParameterMapping> parameterMappings = boundSql
				.getParameterMappings();
		TypeHandlerRegistry typeHandlerRegistry = configuration
				.getTypeHandlerRegistry();
		if (parameterMappings != null) {
			for (int i = 0; i < parameterMappings.size(); i++) {
				ParameterMapping parameterMapping = parameterMappings.get(i);
				if (parameterMapping.getMode() != ParameterMode.OUT) {
					Object value;
					String propertyName = parameterMapping.getProperty();
					if (boundSql.hasAdditionalParameter(propertyName)) {
						value = boundSql.getAdditionalParameter(propertyName);
					} else if (parameterObject == null) {
						value = null;
					} else if (typeHandlerRegistry
							.hasTypeHandler(parameterObject.getClass())) {
						value = parameterObject;
					} else {
						MetaObject metaObject = configuration
								.newMetaObject(parameterObject);
						value = metaObject.getValue(propertyName);
					}
					sql = replacePlaceholder(sql, value);
				}
			}
		}
		return sql;
	}

	private String replacePlaceholder(String sql, Object propertyValue) {
		String result;
		if (propertyValue != null) {
			if (propertyValue instanceof String) {
				result = "'" + propertyValue + "'";
			} else if (propertyValue instanceof Date) {
				result = "'" + FORMATER.format(propertyValue) + "'";
			} else {
				result = propertyValue.toString();
			}
		} else {
			result = "null";
		}
		return StringUtils.replaceOnce(sql, "?", result);
	}
}