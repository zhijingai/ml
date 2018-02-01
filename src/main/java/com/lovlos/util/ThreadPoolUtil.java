package com.lovlos.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {

	/**
	 * 单线程轮询线程池
	 * @return
	 */
	public static ScheduledExecutorService singleScheduledPool(int thread) {
		return Executors.newScheduledThreadPool(thread);
	}
	
	/**
	 * 单线程可回收线程池
	 * @return
	 */
	public static ThreadPoolExecutor singleCachePool() {
		return new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	}
}
