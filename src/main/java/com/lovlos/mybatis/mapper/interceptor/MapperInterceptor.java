package com.lovlos.mybatis.mapper.interceptor;

import java.util.Properties;

import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.lovlos.mybatis.mapper.core.MapperHelper;
import com.lovlos.mybatis.plugin.DynamicDataSourcePlugin;

/**
 * 通用Mapper拦截器
 * <p>
 * 项目地址 : <a href="https://github.com/abel533/Mapper"
 * target="_blank">https://github.com/abel533/Mapper</a>
 * </p>
 *
 * @author liuzh
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class,
                ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {
                MappedStatement.class, Object.class})})
public class MapperInterceptor implements Interceptor {
	
    private final MapperHelper mapperHelper = new MapperHelper();
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] objects = invocation.getArgs();
        MappedStatement ms = (MappedStatement) objects[0];
        String msId = ms.getId();
        // 不需要拦截的方法直接返回
        if (mapperHelper.isMapperMethod(msId)) {
            // 第一次经过处理后，就不会是ProviderSqlSource了，一开始高并发时可能会执行多次，但不影响。以后就不会在执行了
            if (ms.getSqlSource() instanceof ProviderSqlSource) {
                mapperHelper.setSqlSource(ms);
            }
        }
        // 缓存判断	
        Object result = null;
        // result= MapperCacheHelper.getCacheFromRedisIfExistWhiteList(invocation);
        if(result == null) {
	        // 动态分配数据源
	        DynamicDataSourcePlugin.intercept(objects, ms);
	        result = invocation.proceed();
	        // 是否对Map类型的实体处理返回结果，例如USER_NAME=>userName
	        // 这个处理使得通用Mapper可以支持Map类型的实体（实体中的字段必须按常规方式定义，否则无法反射获得列）
	        if (mapperHelper.isCameHumpMap()) {
	            mapperHelper.cameHumpMap(result, ms);
	        }
        }
        return result;
    }
    
    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    @Override
    public void setProperties(Properties properties) {
        String UUID = properties.getProperty("UUID");
        if (UUID != null && UUID.length() > 0) {
            mapperHelper.setUUID(UUID);
        }
        String IDENTITY = properties.getProperty("IDENTITY");
        if (IDENTITY != null && IDENTITY.length() > 0) {
            mapperHelper.setIDENTITY(IDENTITY);
        }
        String seqFormat = properties.getProperty("seqFormat");
        if (seqFormat != null && seqFormat.length() > 0) {
            mapperHelper.setSeqFormat(seqFormat);
        }
        String ORDER = properties.getProperty("ORDER");
        if (ORDER != null && ORDER.length() > 0) {
            mapperHelper.setBEFORE(ORDER);
        }
        String cameHumpMap = properties.getProperty("cameHumpMap");
        if (cameHumpMap != null && cameHumpMap.length() > 0) {
            mapperHelper.setCameHumpMap(cameHumpMap);
        }
        int mapperCount = 0;
        String mapper = properties.getProperty("mappers");
        if (mapper != null && mapper.length() > 0) {
            String[] mappers = mapper.split(",");
            for (String mapperClass : mappers) {
                if (mapperClass.length() > 0) {
                    mapperHelper.registerMapper(mapperClass);
                    mapperCount++;
                }
            }
        }
        if (mapperCount == 0) {
            throw new RuntimeException("通用Mapper没有配置任何有效的通用接口!");
        }
    }
}
