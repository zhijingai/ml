package com.lovlos.mybatis.readwrite.monitor.hystrix;

/**
 * 熔断器
 * @author lovlos
 */
abstract class DataSourceHystrix implements Runnable {
	
	@Override
    public void run() {
		close();
    }

	public abstract void close();

}
