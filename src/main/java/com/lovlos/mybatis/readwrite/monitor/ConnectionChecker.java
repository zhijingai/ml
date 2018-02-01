package com.lovlos.mybatis.readwrite.monitor;

import java.sql.SQLException;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.lovlos.mybatis.readwrite.base.DataSource;

public class ConnectionChecker {
	
    /**
     * 校验数据源可用
     * @param dataSource
     * @return
     */
	public static boolean checkDataSourceConnection(DataSource dataSource) {
		DruidDataSource druidDataSource = (DruidDataSource) dataSource.getDataSource();
		DruidPooledConnection collection = null;
		try {
			collection = druidDataSource.getConnection();
			druidDataSource.validateConnection(collection);
		} catch (Throwable e) {
			System.out.println(e.getMessage());
			return false;
		} finally {
			try {
				if (collection != null) {
					// 解决连接池耗尽
					collection.recycle();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		// 假定的失效机制
//		if(new Random().nextInt(2) == 0){
//			return false;
//		}
		return true;
    }	

}
