package com.lovlos.mybatis.readwrite.monitor;

import java.sql.SQLException;

import com.alibaba.druid.pool.DruidAbstractDataSource.PhysicalConnectionInfo;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.util.JdbcUtils;
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
		PhysicalConnectionInfo phy = null;
		try {
			if (druidDataSource != null) {
				if (druidDataSource.isInited()) {
					druidDataSource.restart();
				}
				// 测试链接
				phy = druidDataSource.createPhysicalConnection();
			}
		} catch (Throwable e) {
			// skip
			return false;
		} finally {
			try {
				if (collection != null) {
					// 解决连接池耗尽
					collection.recycle();
				}
				if (phy != null && phy.getPhysicalConnection() != null) {
	            	 JdbcUtils.close(phy.getPhysicalConnection());
	            	 // 初始化数据源
	            	 druidDataSource.init();
	            	 return true;
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
