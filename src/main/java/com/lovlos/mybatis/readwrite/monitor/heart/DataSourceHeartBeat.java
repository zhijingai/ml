package com.lovlos.mybatis.readwrite.monitor.heart;

/**
 * 数据源心跳机制
 * @author lovlos
 */
public abstract class DataSourceHeartBeat implements Runnable {

	@Override
    public void run() {
       while(true) {
    	   try {
    		   Thread.sleep(1000);
    		   // 检测连接可用性
    		   checkDataSources();
           } catch (Throwable e) {
               e.printStackTrace();
           }
       }
    }

	public abstract void checkDataSources();
}
