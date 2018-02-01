package com.lovlos.mybatis.mapper.cache;
//package com.lovlos.mybatis.mapper;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.ibatis.mapping.BoundSql;
//import org.apache.ibatis.mapping.MappedStatement;
//import org.apache.ibatis.mapping.ParameterMapping;
//import org.apache.ibatis.mapping.ParameterMode;
//import org.apache.ibatis.plugin.Invocation;
//import org.apache.ibatis.reflection.MetaObject;
//import org.apache.ibatis.session.Configuration;
//import org.apache.ibatis.type.TypeHandlerRegistry;
//
//import com.isz.erp.comm.cache.annotation.IszCache;
//import com.isz.erp.common.entity.ResultCacheBean;
//import com.isz.erp.common.entity.TableInfo;
//import com.isz.erp.common.util.ClassUtil;
//import com.isz.erp.common.util.IszLogger;
//import com.isz.erp.common.util.SerializeUtil;
//import com.isz.erp.common.util.SpringContextUtil;
//import com.isz.erp.common.util.SqlBulidUtil;
//import com.isz.erp.common.util.StringUtil;
//import com.isz.erp.facede.cache.service.RedisAnnotationService;
//
///**
// * 
//* @ClassName: MapperCacheHelper 
//* @Description: Mapper接口中设置缓存的中间类
//* @author huajiejun
//* @date 2017年3月20日 下午1:34:16 
//*
// */
//public class MapperCacheHelper {
//	
//    public final static int REFRESH_EXPIRE_TIME =120;//120秒
//    public final static int DEFAULT_EXPIRE_TIME =3600;//一小时
//    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//    public static final String PREFIX = "ishangzu:";
//    
//    private enum cacheStatus{
//    	VALID(0,"有效"),
//    	INVALID(1,"无效"),
//    	FRESHING(2,"刷新中");
//    	
//    	private cacheStatus(int status, String name) {
//			this.status = status;
//			this.name = name;
//		}
//		private int status;
//    	private String name;
//    	
//		public int getStatus() {
//			return status;
//		}
//		
//		@SuppressWarnings("unused")
//		public String getName() {
//			return name;
//		}
//		
//    }
//      /**
//       * 
//      * @Title: getCacheFromRedisIfExist 
//      * @Description: 配置注解的方式
//      * @param @param invocation
//      * @param @return    设定文件 
//      * @return Object    返回类型 
//      * @throws
//       */
//	  public static Object getCacheFromRedisIfExist(Invocation invocation) {
//	    	try {
//				MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
//				Object parameterObject = null;
//				if (invocation.getArgs().length > 1) {
//					parameterObject = invocation.getArgs()[1];
//				} 
//				String statementId = mappedStatement.getId();
//				//获取mybatis的sql模板
//				BoundSql boundSql = mappedStatement.getBoundSql(parameterObject);
//				//主要获取对象化的参数
//				Configuration configuration = mappedStatement.getConfiguration();
//				RedisAnnotationService redisAnnotationService = (RedisAnnotationService) SpringContextUtil.getBean("redisAnnotationService");
//				String className =  ClassUtil.className(statementId);
//				String methodName = ClassUtil.methodName(statementId);
//				IszCache annotation = null;
//				Class<?> clazz = Class.forName(className);
//				Method methodObj = ClassUtil.getMethod(clazz,methodName);
//				if(methodObj!=null){
//					annotation = methodObj.getAnnotation(IszCache.class);	
//					//获取完整的sql
//					Map<String, Object> sqlMap = getSql(boundSql, parameterObject, configuration);
//					//查询缓存的key
//					String tableName=getTableNameFromSqlMap(sqlMap);
//					if(StringUtil.isNullOrEmpty(tableName)){
//						return null;
//					}
//					String queryTableKey =buildQueryKey(sqlMap,tableName);
//					//modelkey;set的名称
//					String dmlKey = PREFIX+tableName;
//					try {
//						//判断资源新旧，判断是否有正在刷新
//						 Object cacheBean = redisAnnotationService.getCacheBean(queryTableKey, dmlKey, annotation.expsecs());
//						 
//						if(null==cacheBean){
//							ResultCacheBean rcb = (ResultCacheBean)setCache(invocation, queryTableKey,redisAnnotationService, annotation, dmlKey);
//							return rcb.getObject();
//						}else{
//							//序列化之前的数据，反序列化
//							if(cacheBean instanceof byte[]){
//								 cacheBean=SerializeUtil.unserialize((byte[])cacheBean);
//							}else{
//								return null;
//							}
//							//判断数据是否旧了
//							ResultCacheBean rcb=(ResultCacheBean)cacheBean;
//							if(rcb.getReFreshFlag()==cacheStatus.VALID.getStatus()){//表示不需要刷新
//								return rcb.getObject();
//								//直接返回
//							}else if(rcb.getReFreshFlag()==cacheStatus.FRESHING.getStatus()){//有人正去刷新数据了，后面请求的人则直接返回旧数据
//								return rcb.getObject();
//							}else{
//								//表示需要刷新，并且没有人做刷新操作,刷新时效为2分钟，2分钟不返回则这个缓存直接清空
//								redisAnnotationService.refreshCacheNeedFresh(queryTableKey, dmlKey, REFRESH_EXPIRE_TIME);
//								rcb = (ResultCacheBean)setCache(invocation, queryTableKey,redisAnnotationService, annotation, dmlKey);
//								return rcb.getObject();
//							}
//						}
//					} catch (Throwable e) {
//						IszLogger.error(e);
//						return null ;
//						//e.printStackTrace();
//					}
//					
//				}
//			} catch (ClassNotFoundException e) {
//				IszLogger.error("ClassNotFoundException",e);
//			} catch (Exception e) {
//				IszLogger.error("Exception",e);
//			}
//			return null;
//			
//		}
//	  
//	  
//	  
//	  /**
//	   * 
//	  * @Title: getCacheFromRedisIfExistWhiteList 
//	  * @Description: 如果存在白名单中，并且是从主键中获取的，则进行缓存操作
//	  * @param @param invocation
//	  * @param @return    设定文件 
//	  * @return Object    返回类型 
//	  * @throws
//	   */
//	  public static Object getCacheFromRedisIfExistWhiteList(Invocation invocation) {
//	    	try {
//				MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
//				Object parameterObject = null;
//				if (invocation.getArgs().length > 1) {
//					parameterObject = invocation.getArgs()[1];
//				} 
//				String statementId = mappedStatement.getId();
//				BoundSql boundSql = mappedStatement.getBoundSql(parameterObject);
//				Configuration configuration = mappedStatement.getConfiguration();
//				
//				if(statementId.endsWith("selectByPrimaryKey")&&SpringContextUtil.getApplicationContext()!=null){//再判断是否在白名单中
//					//IszLogger.info("开始mapper缓存判断.");
//					Object bean = SpringContextUtil.getBean("redisAnnotationService");
//					RedisAnnotationService redisAnnotationService = null;
//					if(bean!=null){
//						 redisAnnotationService = (RedisAnnotationService)bean;
//					}else{
//						return null;
//					}
//					//获取完整的sql
//					Map<String, Object> sqlMap = getSql(boundSql, parameterObject, configuration);
//					//查询缓存的key
//					String tableName=getTableNameFromSqlMap(sqlMap);
//						if(isExistWhite(tableName)){//判断是否存在白名单
//							String queryTableKey =buildQueryKey(sqlMap,tableName);
//							//modelkey;set的名称
//							String dmlKey = PREFIX+tableName;
//							try {
//								//判断资源新旧，判断是否有正在刷新
//								 Object cacheBean = redisAnnotationService.getCacheBean(queryTableKey, dmlKey, DEFAULT_EXPIRE_TIME);
//								if(null==cacheBean){
//									IszLogger.info("mapper缓存击穿.");
//									ResultCacheBean rcb = (ResultCacheBean)setCache(invocation, queryTableKey,redisAnnotationService, dmlKey);
//									return rcb.getObject();
//								}else{
//									//序列化之前的数据，反序列化
//									if(cacheBean instanceof byte[]){
//										 cacheBean=SerializeUtil.unserialize((byte[])cacheBean);
//									}else{
//										return null;
//									}
//									if(!(cacheBean instanceof ResultCacheBean)){
//										return null;
//									}
//									//判断数据是否旧了
//									ResultCacheBean rcb=(ResultCacheBean)cacheBean;
//									if(rcb.getReFreshFlag()==cacheStatus.VALID.getStatus()){//表示不需要刷新
//										//IszLogger.info("mapper缓存获取成功.");
//										return rcb.getObject();
//										//直接返回
//									}else if(rcb.getReFreshFlag()==cacheStatus.FRESHING.getStatus()){//有人正去刷新数据了，后面请求的人则直接返回旧数据
//										IszLogger.info("mapper缓存刷新中，直接返回旧值.");
//										return rcb.getObject();
//									}else{
//										IszLogger.info("mapper缓存已过时，开始刷新.");
//										//表示需要刷新，并且没有人做刷新操作,刷新时效为2分钟，2分钟不返回则这个缓存直接清空
//										redisAnnotationService.refreshCacheNeedFresh(queryTableKey, dmlKey, REFRESH_EXPIRE_TIME);
//										rcb = (ResultCacheBean)setCache(invocation, queryTableKey,redisAnnotationService, dmlKey);
//										return rcb.getObject();
//									}
//								}
//							} catch (Throwable e) {
//								IszLogger.error("缓存异常",e);
//							}
//							
//						}
//					}
//				}catch (Throwable e) {
//					IszLogger.error("缓存异常",e);
//				}
//			return null;
//			
//		}
//
//	   private static boolean isExistWhite(String tableName) {
//		   InputStream resourceAsStream = ClassUtil.class.getClassLoader().getResourceAsStream("cache_table.properties");
//			Properties p = new Properties();
//			try {
//				p.load(resourceAsStream);
//			} catch (IOException e) {
//				IszLogger.error("读取配置文件出错。");
//			}
//			String property = p.getProperty("TABLE_NAME");
//			List<String> buildStrs = StringUtil.buildStrs(property);
//			if(buildStrs!=null&&buildStrs.size()>0){
//				if(buildStrs.contains(tableName)){
//					return true;
//				}
//			}
//			return true; //暂时全部放行
//			//return false
//	   }
//
//
//
//		private static String getTableNameFromSqlMap(Map<String, Object> sqlMap) throws Exception {
//				String tableName = null;
//				if(sqlMap.containsKey("sql")){
//					Map<String, Object> sqlresolve = SqlBulidUtil.sqlresolve((String)sqlMap.get("sql"));
//					//获取table名称
//					if(sqlresolve!=null && sqlresolve.containsKey("SELECT")){
//						@SuppressWarnings("unchecked")
//						List<TableInfo> list=(List<TableInfo>) sqlresolve.get("SELECT");
//						if(list!=null&&list.size()>0){
//							tableName=list.get(0).getTableName();
//						}
//					}
//					return tableName;
//				}else{
//					return null;
//				}
//			
//	    }
//
//		/**
//	     * 
//	    * @Title: buildTableKey 
//	    * @Description: 生成以表名为主加参数的redis的key
//	    * @param @return    设定文件 
//	    * @return String    返回类型 
//	    * @throws
//	     */
//	    public static String buildQueryKey(Map<String, Object> map,String tableName){
//	    	//获得sql的表
//	    	String propertyValue="";
//	    	String propertyName="";
//	    	if(map.containsKey("propertyValue")){
//	    		propertyValue = (String) map.get("propertyValue");
//	    	}
//	    	if(map.containsKey("propertyName")){
//	    		propertyName = (String) map.get("propertyName");
//	    	}
//	    	
//	    	return tableName+"-"+propertyName+"-"+propertyValue;
//	    }
//	    
//	    
//		public static Object setCache(Invocation invocation, String statementId,
//				RedisAnnotationService redisAnnotationService,
//				IszCache annotation, String modelKey)
//				throws InvocationTargetException, IllegalAccessException, Throwable {
//			//缓存封装的对象
//			Object result = null; 
//			//击穿
//			Object proceed = invocation.proceed();//执行过后
//			try {
//				//缓存对象
//				result = redisAnnotationService.setCacheBean(statementId, modelKey, annotation.expsecs(),SerializeUtil.serialize(proceed));
//				if(result instanceof ResultCacheBean){
//					result=(ResultCacheBean)result;
//				}else{
//					result=null;
//				}
//				IszLogger.info("放入缓存成功"+modelKey);
//			} catch (Exception e) {
//				IszLogger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//				IszLogger.error("注解捕获，加入缓存失败"+modelKey);
//				IszLogger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//			}
//			return result;
//		}
//		
//		public static Object setCache(Invocation invocation, String statementId,
//				RedisAnnotationService redisAnnotationService,
//				 String modelKey)
//				throws InvocationTargetException, IllegalAccessException, Throwable {
//			//缓存封装的对象
//			Object result = null; 
//			//击穿
//			Object proceed = invocation.proceed();//执行过后
//			try {
//				//缓存对象
//				result = redisAnnotationService.setCacheBean(statementId, modelKey, DEFAULT_EXPIRE_TIME,SerializeUtil.serialize(proceed));
//				if(result instanceof ResultCacheBean){
//					result=(ResultCacheBean)result;
//				}else{
//					result=null;
//				}
//				IszLogger.info("放入缓存成功"+modelKey);
//			} catch (Exception e) {
//				IszLogger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//				IszLogger.error("注解捕获，加入缓存失败"+modelKey);
//				IszLogger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//			}
//			return result;
//		}
//
//		
//	    private static Map<String, Object> getSql(BoundSql boundSql, Object parameterObject,
//				Configuration configuration) {
//	    	Map<String, Object> returnMap = new HashMap<String, Object>();
//			String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
//			List<ParameterMapping> parameterMappings = boundSql
//					.getParameterMappings();
//			TypeHandlerRegistry typeHandlerRegistry = configuration
//					.getTypeHandlerRegistry();
//			if (parameterMappings != null) {
//				for (int i = 0; i < parameterMappings.size(); i++) {
//					ParameterMapping parameterMapping = parameterMappings.get(i);
//					if (parameterMapping.getMode() != ParameterMode.OUT) {
//						Object value;
//						String propertyName = parameterMapping.getProperty();
//						returnMap.put("propertyName", propertyName);//参数名称
//						if (boundSql.hasAdditionalParameter(propertyName)) {
//							value = boundSql.getAdditionalParameter(propertyName);
//						} else if (parameterObject == null) {
//							value = null;
//						} else if (typeHandlerRegistry
//								.hasTypeHandler(parameterObject.getClass())) {
//							value = parameterObject;
//						} else {
//							MetaObject metaObject = configuration
//									.newMetaObject(parameterObject);
//							value = metaObject.getValue(propertyName);
//							
//							System.err.println(1);
//						}
//						returnMap.put("propertyValue", value);
//						sql = replacePlaceholder(sql, value);
//					}
//				}
//			}
//			returnMap.put("sql", sql);
//			return returnMap;
//		}
//	    
//	    private static String replacePlaceholder(String sql, Object propertyValue) {
//			String result;
//			if (propertyValue != null) {
//				if (propertyValue instanceof String) {
//					result = "'" + propertyValue + "'";
//				} else if (propertyValue instanceof Date) {
//					result = "'" + DATE_FORMAT.format(propertyValue) + "'";
//				} else {
//					result = propertyValue.toString();
//				}
//			} else {
//				result = "null";
//			}
//			return StringUtils.replaceOnce(sql, "?", result);
//		}
//	    
//	    
//}
