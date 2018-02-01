package com.lovlos.util;

import java.io.IOException;
import java.io.InputStream;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * 参数文件 解析工具
 * 使用apache configuration
 */
public class PropertiesUtil {

	/**
	 * 
	 * @param fileName
	 * @return
	 * @throws ConfigurationException
	 */
	public static PropertiesConfiguration parseFile(String fileName) {
		return parseFile(fileName, "utf-8", ',');
	}

	/**
	 * 
	 * @param fileName
	 * @param encode
	 * @param s
	 * @return
	 * @throws ConfigurationException
	 */
	public static PropertiesConfiguration parseFile(String fileName, String encode, char s) {
		// 生成输入流
		InputStream ins = PropertiesUtil.class.getResourceAsStream("/" + fileName);
		// 生成properties对象
		PropertiesConfiguration p = new PropertiesConfiguration();
		try {
			p.setListDelimiter(s);
			p.load(ins, encode);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ins.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return p;
	}
}
