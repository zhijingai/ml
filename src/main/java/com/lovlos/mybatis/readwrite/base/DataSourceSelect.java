package com.lovlos.mybatis.readwrite.base;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据源择取列表
 * @author lovlos
 */
public class DataSourceSelect {
	
	// 暂时不考虑重复情况
	private List<String> dateSourceList = new ArrayList<>();

	public List<String> getDateSourceList() {
		return dateSourceList;
	}

	public void addDateSource(String dataSource) {
		this.dateSourceList.add(dataSource);
	}
	
}
