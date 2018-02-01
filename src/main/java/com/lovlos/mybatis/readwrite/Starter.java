package com.lovlos.mybatis.readwrite;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.lovlos.event.spring.SpringLoader;

public final class Starter implements ApplicationListener<ContextRefreshedEvent> {

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		new SpringLoader().load(event);
	}

}
