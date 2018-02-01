package com.lovlos.util;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class FastJsonUtil {

	public static final SerializerFeature[] serializerFeatures = 
			new SerializerFeature[]{ SerializerFeature.WriteMapNullValue, SerializerFeature.QuoteFieldNames, SerializerFeature.WriteDateUseDateFormat }; 

	public static final Feature[] features = new Feature[]{ Feature.OrderedField };
	
	/**
	 * 将对象写成 json字符串
	 */
	public static String toJSONString(Object o) {
		return JSON.toJSONString(o, serializerFeatures);
	}

	public static <T> List<T> parseArrays(String s, Class<T> clazz) {
		return JSON.parseArray(s, clazz);
	}
	
	public static <T> T parseObject(String s, TypeReference<T> type) {
		return JSON.parseObject(s, type);
	}
	
	public static <T> T parseObject(String text, Class<T> clazz) {
		return JSON.parseObject(text, clazz);
	}
	
}
