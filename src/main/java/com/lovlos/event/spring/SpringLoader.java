package com.lovlos.event.spring;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import com.lovlos.mybatis.mapper.interceptor.MapperInterceptor;
import com.lovlos.mybatis.plugin.PerformancePlugin;
import com.lovlos.mybatis.readwrite.base.DataSource;
import com.lovlos.mybatis.readwrite.config.DataSourceConfig;
import com.lovlos.mybatis.readwrite.core.DynamicDataSource;
import com.lovlos.mybatis.readwrite.core.balance.DataSourceBalance;
import com.lovlos.mybatis.readwrite.core.balance.RoundRobinDataSourceBalance;
import com.lovlos.mybatis.readwrite.core.load.DataSourceMapperLoader;
import com.lovlos.mybatis.readwrite.core.load.DataSourceSaxLoader;
import com.lovlos.mybatis.readwrite.monitor.heart.DataSourceHeartBeat;
import com.lovlos.mybatis.readwrite.monitor.heart.DataSourceHeartBeatKeeper;
import com.lovlos.mybatis.readwrite.util.BalanceUtil;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;
import com.lovlos.util.PropertiesUtil;
import com.lovlos.util.SpringUtil;

public class SpringLoader {
	
	public void load(ContextRefreshedEvent event) {
		// 引用容器
		loadSpringUtil(event);
		// 加载读写分离与性能监控插件
		loadPlugins();
		// 加载数据源
		loadDataSource();
		// 装配数据源
		loadDruidDataSource();
		// 缓存mapper路由
		loadDataSourceMapperCache();
		// 心跳检测
		startHeartBeatWorker();
		// 开启负载均衡
		loadBalance();
		// ready
		System.out.println("{ plugins is ready }");
	}

	/**
	 * 引用容器
	 * @param event
	 */
	private void loadSpringUtil(ContextRefreshedEvent event) {
		ApplicationContext applicationContext = event.getApplicationContext();
		SpringUtil.setContext(applicationContext);
	}
	
	/**
	 * 加载数据源
	 */
	private void loadDataSource() {
		InputStream resourceAsStream = null;
		try{
	        SAXParserFactory factory = SAXParserFactory.newInstance(); 
	        SAXParser parser = factory.newSAXParser(); 
	        resourceAsStream = SpringLoader.class.getClassLoader().getResourceAsStream("plugins/data-source.xml");
	        parser.parse(resourceAsStream, new DataSourceSaxLoader());
		} catch(Exception e) {
			System.out.println(e);
			System.out.println("----------------解析XML失败：");
		} finally {
			try {
				resourceAsStream.close();
			} catch (IOException e) {
				System.out.println("----------------释放资源失败：");
			}
		}	
	}
	
	/**
	 * 配置druid-target数据源
	 */
	private void loadDruidDataSource() {
		Map<DataSourceConfig, List<DataSource>> dataSourceMap = DataSourceUtil.getDataSources();
		List<DataSource> masterDataSourceList = dataSourceMap.get(DataSourceConfig.MASTER);
		List<DataSource> slaveDataSourceList = dataSourceMap.get(DataSourceConfig.SLAVE);
		if(masterDataSourceList == null || masterDataSourceList.isEmpty() || slaveDataSourceList == null || slaveDataSourceList.isEmpty()) {
			throw new IllegalArgumentException("dataSource is empty");
		}
		Object defaultDataSource = masterDataSourceList.get(0).getDataSource();
		
		Map<Object, Object> targetDataSources = new HashMap<>();
		for(DataSource _dataSource : masterDataSourceList) {
			targetDataSources.put(_dataSource.getDataSourceName(), _dataSource.getDataSource());
		}
		for(DataSource _dataSource : slaveDataSourceList) {
			targetDataSources.put(_dataSource.getDataSourceName(), _dataSource.getDataSource());
		}
		
		// 装配数据源
		DynamicDataSource dynamicDataSource = (DynamicDataSource) SpringUtil.getBean("dataSource");
		dynamicDataSource.setTargetDataSource(defaultDataSource, targetDataSources);
	}

	/**
	 * 缓存mapper路由
	 */
	private void loadDataSourceMapperCache() {
		DataSourceMapperLoader.loadForMapper();
	}

	/**
	 * 心跳任务
	 */
	private void startHeartBeatWorker() {
		DataSourceHeartBeat heartBeat = new DataSourceHeartBeatKeeper();
		DataSourceUtil.getExecutorService().execute(heartBeat);
	}

	/**
	 * 加载读写分离与性能监控插件
	 */
	private void loadPlugins() {
		DefaultSqlSessionFactory sqlSessionFactory = (DefaultSqlSessionFactory) SpringUtil.getBean("sqlSessionFactory");
		Configuration configuration = sqlSessionFactory.getConfiguration();
		configuration.addInterceptor(new MapperInterceptor());
		configuration.addInterceptor(new PerformancePlugin());
//		sqlSession.setConfigLocation(new PathResource("/Users/lovlos/Documents/workspace/lovlos-pro/apple/src/main/resources/config/mybatis-config.xml"));
//		sqlSession.setDataSource(new DynamicDataSource());
	}

	/**
	 * 开启负载均衡器
	 */
	private void loadBalance() {
		PropertiesConfiguration properties = PropertiesUtil.parseFile("META-INF/balance.properties");
		String defBalance = properties.getString("balance");
		Integer defCount = properties.getInteger("count", 5000000);
		BalanceUtil.setDefaultBalanceCount(defCount);
		BalanceUtil.setDefaultBalanceLocation(defBalance);
		String location = StringUtils.isNotBlank(BalanceUtil.getBalanceLocation()) ? BalanceUtil.getBalanceLocation() : BalanceUtil.getDefaultBalanceLocation();
		try {
			// 类加载器初始化负载策略
			Class<?> clazz = SpringLoader.class.getClassLoader().loadClass(location);
			Object dataSourceBalance = clazz.newInstance();
	    	if (dataSourceBalance instanceof RoundRobinDataSourceBalance &&
	    			DataSourceUtil.getAllDataSources() != null && DataSourceUtil.getAllDataSources().size() > 0) {
	    		for (String dataSource : DataSourceUtil.getAllDataSources().keySet()) {
	    			RoundRobinDataSourceBalance.getCountMap().put(dataSource, new AtomicInteger(0));
	    		}   		
	    	}
			BalanceUtil.setDataSourceBalance((DataSourceBalance) dataSourceBalance);
			DynamicDataSource.setDataSourceBalance((DataSourceBalance) dataSourceBalance);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}
