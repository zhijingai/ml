package com.lovlos.mybatis.readwrite.core.balance;

import java.util.List;
import java.util.Random;

import com.lovlos.mybatis.readwrite.core.DynamicDataSourceHolder;

/**
 * 随机负载
 * @author lovlos
 */
public final class RandomDataSourceBalance implements DataSourceBalance {

	private static final Random random = new Random();
	
	@Override
	public String getDataSource(List<String> dataSourceList) {
		if (dataSourceList == null || dataSourceList.isEmpty()) {
			return null;
		}
        int index = random.nextInt(dataSourceList.size());
        String name = dataSourceList.get(index);
        DynamicDataSourceHolder.putDataSourceName(name);
  	    return name;
	}
}
