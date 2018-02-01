package com.lovlos.mybatis.readwrite.core.load;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.alibaba.druid.pool.DruidDataSource;
import com.lovlos.mybatis.readwrite.base.DataSource;
import com.lovlos.mybatis.readwrite.config.DataSourceConfig;
import com.lovlos.mybatis.readwrite.util.BalanceUtil;
import com.lovlos.mybatis.readwrite.util.DataSourceUtil;
import com.lovlos.mybatis.readwrite.util.HystrixDataSourceUtil;
import com.lovlos.util.SpringUtil;

public class DataSourceSaxLoader extends DefaultHandler {
		
	private DataSource dataSource;
			
	private Map<DataSourceConfig, List<DataSource>> dataSources = new ConcurrentHashMap<>();
	
	private Map<String, DataSource> allDataSources = new ConcurrentHashMap<>();
	
	private Map<String, DataSourceConfig> dataSourceConfigs = new HashMap<>();
	
	private Map<String, DataSource> validDataSources = new ConcurrentHashMap<>();
	
	private Map<String, DataSource> invalidDataSources = new ConcurrentHashMap<>();
	
	private Map<String, Map<String, Object>> dataSourceSubscribes = new ConcurrentHashMap<>();
	
	private Set<String> hasDeal = new HashSet<>();
		
	public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
		if("lovlos.data.sources".equals(name) || "lovlos:setting".equals(name) || "beans".equals(name) || "bean".equals(name)) {
			
		}else if(("lovlos:"+DataSourceConfig.MASTER.name().toLowerCase()).equals(name) || ("lovlos:"+DataSourceConfig.SLAVE.name().toLowerCase()).equals(name)) {
			name = name.substring(7);
			dataSource = new DataSource();			
			boolean isHystrix = false;
			
            for(int i = 0 ; i < attr.getLength(); i++) {
            	if("bean".equals(attr.getQName(i)) && !hasDeal.contains(attr.getValue(i))) {
        			hasDeal.add(attr.getValue(i));
            		// 数据源
            		DataSourceConfig dataSourceConfig = DataSourceConfig.valueOf(name.toUpperCase());
            		dataSource.setDataSourceConfig(dataSourceConfig);
            		dataSource.setDataSourceName(attr.getValue(i));
            		List<DataSource> dataSourceList = dataSources.get(dataSourceConfig);
            		if(dataSourceList == null) {
            			dataSourceList = new ArrayList<>();
            			dataSources.put(dataSourceConfig, dataSourceList);
            			dataSourceList = dataSources.get(dataSourceConfig);
            		}          		
            		if(checkDataSources()) {
            			// 可用数据源
            			dataSourceList.add(dataSource);
            			validDataSources.put(dataSource.getDataSourceName(), dataSource);
            		}else {
            			// 失效数据源
            			invalidDataSources.put(dataSource.getDataSourceName(), dataSource);
            		}
            		allDataSources.put(dataSource.getDataSourceName(), dataSource);
            		dataSourceConfigs.put(dataSource.getDataSourceName(), DataSourceConfig.valueOf(name.toUpperCase()));
            		Map<String, Object> dataSourceSubscribe = dataSourceSubscribes.get(dataSource.getDataSourceName());
            		if(dataSourceSubscribe == null) {
            			dataSourceSubscribe = new ConcurrentHashMap<>();
            			dataSourceSubscribes.put(dataSource.getDataSourceName(), dataSourceSubscribe);
            		}
                }else if("hystrix".equals(attr.getQName(i)) && StringUtils.isNotBlank(attr.getValue(i)) && StringUtils.isNumeric(attr.getValue(i))) {
                	// 熔断阀值
                	isHystrix = true;
                	HystrixDataSourceUtil.getDataSourceErrLimit().put(dataSource.getDataSourceName(), Integer.parseInt(attr.getValue(i)));
                }
            }
            if(!isHystrix) {
            	// 默认熔断阀值
            	HystrixDataSourceUtil.getDataSourceErrLimit().put(dataSource.getDataSourceName(), HystrixDataSourceUtil.DEFAULT_HYSTRIX_NUM);
            }
            HystrixDataSourceUtil.getDataSourceErrTimes().put(dataSource.getDataSourceName(), new AtomicInteger(0));
		}else if("lovlos:mappers".equals(name)){
			// 加载mapper路由缓存
            if(StringUtils.isBlank(DataSourceMapperLoader.getMapperPackages())) {
            	for(int i = 0 ; i < attr.getLength(); i++) {
                	if("packages".equals(attr.getQName(i)) && StringUtils.isNotBlank(attr.getValue(i))) {
                		DataSourceMapperLoader.setMapperPackages(attr.getValue(i));
                		break;
                    }
                }
            }
		}else if("lovlos:balance".equals(name)){
			// 负载均衡器
			for(int i = 0 ; i < attr.getLength(); i++) {
            	if("location".equals(attr.getQName(i)) && StringUtils.isNotBlank(attr.getValue(i))) {
            		BalanceUtil.setBalanceLocation(attr.getValue(i));
                }else if("count".equals(attr.getQName(i)) && StringUtils.isNotBlank(attr.getValue(i)) && StringUtils.isNumeric(attr.getValue(i))) {
                	BalanceUtil.setBalanceCount(Integer.parseInt(attr.getValue(i)));
                }
            }		
		}else{
			throw new IllegalArgumentException("data-source.xml未声明标签");
		}
	}
	
	public void endElement(String uri, String localName, String name) throws SAXException {
		if("lovlos:setting".equals(name)) {
			DataSourceUtil.setDataSources(dataSources);
			DataSourceUtil.setAllDataSources(allDataSources);
			DataSourceUtil.setDataSourceConfigs(dataSourceConfigs);
			DataSourceUtil.setValidDataSources(validDataSources);
			DataSourceUtil.setInvalidDataSources(invalidDataSources);
			DataSourceUtil.setDataSourceSubscribes(dataSourceSubscribes);
		}
	}
	
	/**
	 * 可用性检验
	 * @return
	 */
	private boolean checkDataSources() {
		if(dataSource == null || StringUtils.isBlank(dataSource.getDataSourceName())) {
			return false;
		}
		DruidDataSource druidDataSource = (DruidDataSource) SpringUtil.getBean(dataSource.getDataSourceName());
		dataSource.setDataSource(druidDataSource);
		buffDataSources();
		//return DataSourceHeartBeat.checkDataSource(dataSource);
		return true;
	}
	
	/**
	 * 数据源增强
	 */
	private void buffDataSources() {

	}
	
}
