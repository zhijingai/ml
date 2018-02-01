package com.lovlos.mybatis.mapper.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.lovlos.mybatis.mapper.Camel;
import com.lovlos.mybatis.mapper.NotDbField;

/**
 * 实体类工具类
 * <p>
 * 项目地址 : <a href="https://github.com/abel533/Mapper"
 * target="_blank">https://github.com/abel533/Mapper</a>
 * </p>
 * 
 * @author liuzh
 */
public class EntityHelper {
	public static class EntityColumn {
		private String property;
		private String column;
		private Class<?> javaType;
		private String sequenceName;
		private boolean id = false;
		private boolean uuid = false;
		private boolean identity = false;
		private String generator;

		public String getProperty() {
			return property;
		}

		public void setProperty(String property) {
			this.property = property;
		}

		public String getColumn() {
			return column;
		}

		public void setColumn(String column) {
			this.column = column;
		}

		public Class<?> getJavaType() {
			return javaType;
		}

		public void setJavaType(Class<?> javaType) {
			this.javaType = javaType;
		}

		public String getSequenceName() {
			return sequenceName;
		}

		public void setSequenceName(String sequenceName) {
			this.sequenceName = sequenceName;
		}

		public boolean isId() {
			return id;
		}

		public void setId(boolean id) {
			this.id = id;
		}

		public boolean isUuid() {
			return uuid;
		}

		public void setUuid(boolean uuid) {
			this.uuid = uuid;
		}

		public boolean isIdentity() {
			return identity;
		}

		public void setIdentity(boolean identity) {
			this.identity = identity;
		}

		public String getGenerator() {
			return generator;
		}

		public void setGenerator(String generator) {
			this.generator = generator;
		}
	}

	/**
	 * 实体类 => 表名
	 */
	private static final Map<Class<?>, String> entityClassTableName = new HashMap<Class<?>, String>();
	/**
	 * 实体类 => 全部列属性
	 */
	private static final Map<Class<?>, List<EntityColumn>> entityClassColumns = new HashMap<Class<?>, List<EntityColumn>>();
	/**
	 * 实体类 => 主键信息
	 */
	private static final Map<Class<?>, List<EntityColumn>> entityClassPKColumns = new HashMap<Class<?>, List<EntityColumn>>();

	/**
	 * 获取表名
	 * 
	 * @param entityClass
	 * @return
	 */
	public static String getTableName(Class<?> entityClass) {
		String tableName = entityClassTableName.get(entityClass);
		if (tableName == null) {
			initEntityNameMap(entityClass);
			tableName = entityClassTableName.get(entityClass);
		}
		if (tableName == null) {
			throw new RuntimeException("无法获取实体类" + entityClass.getCanonicalName() + "对应的表名!");
		}
		return tableName;
	}

	/**
	 * 获取全部列
	 * 
	 * @param entityClass
	 * @return
	 */
	public static List<EntityColumn> getColumns(Class<?> entityClass) {
		// 可以起到初始化的作用
		getTableName(entityClass);
		return entityClassColumns.get(entityClass);
	}

	/**
	 * 获取主键信息
	 * 
	 * @param entityClass
	 * @return
	 */
	public static List<EntityColumn> getPKColumns(Class<?> entityClass) {
		// 可以起到初始化的作用
		getTableName(entityClass);
		return entityClassPKColumns.get(entityClass);
	}
	/**
	 * 获取查询的Select
	 * 
	 * @param entityClass
	 * @return
	 */
	public static String getSelectColumns(Class<?> entityClass) {
		List<EntityColumn> columnList = getColumns(entityClass);
		StringBuilder selectBuilder = new StringBuilder();
		boolean skipAlias = Map.class.isAssignableFrom(entityClass);
		for (EntityColumn entityColumn : columnList) {
			selectBuilder.append(entityColumn.getColumn());
			if (!skipAlias && !entityColumn.getColumn().equalsIgnoreCase(entityColumn.getProperty())) {
				selectBuilder.append(" ").append(entityColumn.getProperty().toUpperCase()).append(",");
			} else {
				selectBuilder.append(",");
			}
		}
		return selectBuilder.substring(0, selectBuilder.length() - 1);
	}

	/**
	 * 获取查询的Select
	 * 
	 * @param entityClass
	 * @return
	 */
	public static String getAllColumns(Class<?> entityClass) {
		List<EntityColumn> columnList = getColumns(entityClass);
		StringBuilder selectBuilder = new StringBuilder();
		for (EntityColumn entityColumn : columnList) {
			selectBuilder.append(entityColumn.getColumn()).append(",");
		}
		return selectBuilder.substring(0, selectBuilder.length() - 1);
	}

	/**
	 * 获取主键的Where语句
	 * 
	 * @param entityClass
	 * @return
	 */
	public static String getPrimaryKeyWhere(Class<?> entityClass) {
		List<EntityHelper.EntityColumn> entityColumns = EntityHelper.getPKColumns(entityClass);
		StringBuilder whereBuilder = new StringBuilder();
		for (EntityHelper.EntityColumn column : entityColumns) {
			whereBuilder.append(column.getColumn()).append(" = ?").append(" AND ");
		}
		return whereBuilder.substring(0, whereBuilder.length() - 4);
	}

	/**
	 * 初始化实体属性
	 * 
	 * @param entityClass
	 */
	public static synchronized void initEntityNameMap(Class<?> entityClass) {
		if (entityClassTableName.get(entityClass) != null) {
			return;
		}
		// 表名
		if (entityClass.isAnnotationPresent(Table.class)) {
			Table table = entityClass.getAnnotation(Table.class);
			entityClassTableName.put(entityClass, table.name());
		} else {
			entityClassTableName.put(entityClass, camelhumpToUnderline(entityClass.getSimpleName()).toUpperCase());
		}
		boolean isNeedCamel = false;
		// 是否使用驼峰模式进行字段替换 
		if (entityClass.isAnnotationPresent(Camel.class)) {
			isNeedCamel = true;
		}
		// 列
		List<Field> fieldList = getAllField(entityClass, null);
		List<EntityColumn> columnList = new ArrayList<>();
		List<EntityColumn> pkColumnList = new ArrayList<>();
		for (Field field : fieldList) {
			// 排除字段
			if (field.isAnnotationPresent(NotDbField.class)) {
				continue;
			}
			EntityColumn entityColumn = new EntityColumn();
			if (field.isAnnotationPresent(Id.class)) {
				entityColumn.setId(true);
			}
			String columnName = null;
			if (field.isAnnotationPresent(Column.class)) {
				Column column = field.getAnnotation(Column.class);
				columnName = column.name();
			} else {
				if(isNeedCamel){
					//使用驼峰--转下划线模式
					columnName = camelhumpToUnderline(field.getName());
				}else{
					//就是用字段名
					columnName = field.getName();
				}
			}
			entityColumn.setProperty(field.getName());
			entityColumn.setColumn(columnName.toUpperCase());
			entityColumn.setJavaType(field.getType());
			// 主键策略 - Oracle序列，MySql自动增长，UUID
			if (field.isAnnotationPresent(SequenceGenerator.class)) {
				SequenceGenerator sequenceGenerator = field.getAnnotation(SequenceGenerator.class);
				if (sequenceGenerator.sequenceName().equals("")) {
					throw new RuntimeException(entityClass + "字段" + field.getName() + "的注解@SequenceGenerator未指定sequenceName!");
				}
				entityColumn.setSequenceName(sequenceGenerator.sequenceName());
			} else if (field.isAnnotationPresent(GeneratedValue.class)) {
				GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
				if (generatedValue.generator().equals("UUID")) {
					if (field.getType().equals(String.class)) {
						entityColumn.setUuid(true);
					} else {
						throw new RuntimeException(field.getName() + " - 该字段@GeneratedValue配置为UUID，但该字段类型不是String");
					}
				} else {
					// 允许通过generator来设置获取id的sql,例如mysql=CALL
					// IDENTITY(),hsqldb=SELECT SCOPE_IDENTITY()
					// 允许通过拦截器参数设置公共的generator
					if (generatedValue.strategy() == GenerationType.IDENTITY) {
						// mysql的自动增长
						entityColumn.setIdentity(true);
						if (!generatedValue.generator().equals("")) {
							entityColumn.setGenerator(generatedValue.generator());
						}
					} else {
						throw new RuntimeException(field.getName()
								+ " - 该字段@GeneratedValue配置只允许两种形式，全部数据库通用的@GeneratedValue(generator=\"UUID\") 或者 "
								+ "类似mysql数据库的@GeneratedValue(strategy=GenerationType.IDENTITY[,generator=\"CALL IDENTITY()\"])");
					}
				}
			}
			columnList.add(entityColumn);
			if (entityColumn.isId()) {
				pkColumnList.add(entityColumn);
			}
		}
		if (pkColumnList.size() == 0) {
			pkColumnList = columnList;
		}
		
		entityClassColumns.put(entityClass, columnList);
		entityClassPKColumns.put(entityClass, pkColumnList);
	}

	/**
	 * 将驼峰风格替换为下划线风格
	 */
	public static String camelhumpToUnderline(String str) {
		final int size;
		final char[] chars;
		final StringBuilder sb = new StringBuilder((size = (chars = str.toCharArray()).length) * 3 / 2 + 1);
		char c;
		for (int i = 0; i < size; i++) {
			c = chars[i];
			if (isLowercaseAlpha(c)) {
				sb.append(toUpperAscii(c));
			} else {
				sb.append('_').append(c);
			}
		}
		return sb.charAt(0) == '_' ? sb.substring(1) : sb.toString();
	}

	/**
	 * 将下划线风格替换为驼峰风格
	 */
	public static String underlineToCamelhump2(String name) {
		char[] buffer = name.toCharArray();
		int count = 0;
		boolean lastUnderscore = false;
		for (int i = 0; i < buffer.length; i++) {
			char c = buffer[i];
			if (c == '_') {
				lastUnderscore = true;
			} else {
				c = (lastUnderscore && count != 0) ? toUpperAscii(c) : toLowerAscii(c);
				buffer[count++] = c;
				lastUnderscore = false;
			}
		}
		if (count != buffer.length) {
			buffer = subarray(buffer, 0, count);
		}
		return new String(buffer);
	}

	public static char[] subarray(char[] src, int offset, int len) {
		char[] dest = new char[len];
		System.arraycopy(src, offset, dest, 0, len);
		return dest;
	}

	public static boolean isLowercaseAlpha(char c) {
		return (c >= 'a') && (c <= 'z');
	}

	public static char toUpperAscii(char c) {
		if (isLowercaseAlpha(c)) {
			c -= (char) 0x20;
		}
		return c;
	}

	public static char toLowerAscii(char c) {
		if ((c >= 'A') && (c <= 'Z')) {
			c += (char) 0x20;
		}
		return c;
	}

	/**
	 * 获取全部的Field
	 * 
	 * @param entityClass
	 * @param fieldList
	 * @return
	 */
	private static List<Field> getAllField(Class<?> entityClass, List<Field> fieldList) {
		if (fieldList == null) {
			fieldList = new ArrayList<Field>();
		}
		if (entityClass.equals(Object.class)) {
			return fieldList;
		}
		Field[] fields = entityClass.getDeclaredFields();
		for (Field field : fields) {
			// 排除静态字段，解决bug#2
			if (!Modifier.isStatic(field.getModifiers())) {
				fieldList.add(field);
			}
		}
//		if (entityClass.getSuperclass() != null && !entityClass.getSuperclass().equals(Object.class)
//				&& !Map.class.isAssignableFrom(entityClass.getSuperclass())
//				&& !Collection.class.isAssignableFrom(entityClass.getSuperclass())) {
//			return getAllField(entityClass.getSuperclass(), fieldList);
//		}
		if (entityClass.getSuperclass() != null && !entityClass.getSuperclass().equals(Object.class)
				&& !Map.class.isAssignableFrom(entityClass.getSuperclass())
				&& !Collection.class.isAssignableFrom(entityClass.getSuperclass())) {
			// 如果 是 标注 @Inheritance(strategy = InheritanceType.JOINED) 那么需要特殊处理, 将 父类的ID注解的字段补上
			if(entityClass.getSuperclass().isAnnotationPresent(Inheritance.class)){
				Inheritance in = entityClass.getSuperclass().getAnnotation(Inheritance.class);
				if(InheritanceType.JOINED == in.strategy()){
					return addIdField(entityClass.getSuperclass(), fieldList);
				}else{
					return getAllField(entityClass.getSuperclass(), fieldList);
				}
			}
		}
		return fieldList;
	}
	
	/**
	 * 获取全部的Field
	 * 
	 * @param entityClass
	 * @param fieldList
	 * @return
	 */
	private static List<Field> addIdField(Class<?> entityClass, List<Field> fieldList) {
		if (fieldList == null) {
			fieldList = new ArrayList<Field>();
		}
		Field[] fields = entityClass.getDeclaredFields();
		for (Field field : fields) {
			if(field.isAnnotationPresent(Id.class)){
				fieldList.add(field);	
			}
		}
		return fieldList;
	}
}