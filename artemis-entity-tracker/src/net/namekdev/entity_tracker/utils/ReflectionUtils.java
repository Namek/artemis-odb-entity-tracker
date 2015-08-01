package net.namekdev.entity_tracker.utils;

import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Field;
import com.artemis.utils.reflect.Method;
import com.artemis.utils.reflect.ReflectionException;

public class ReflectionUtils {
	public static final Field getHiddenField(Class<?> type, String fieldName) {
		Field field = null;

		try {
			field = ClassReflection.getDeclaredField(type, fieldName);
			field.setAccessible(true);
		}
		catch (ReflectionException e) {
			e.printStackTrace();
		}

		return field;
	}

	public static final Object getHiddenFieldValue(Class<?> type, String fieldName, Object obj) {
		Field field = getHiddenField(type, fieldName);
		try {
			return field.get(obj);
		}
		catch (ReflectionException e) {
			throw new RuntimeException(e);
		}
	}

	public static final Method getHiddenMethod(Class<?> type, String methodName) {
		try {
			Method method = ClassReflection.getDeclaredMethod(type, methodName);
			method.setAccessible(true);
			return method;
		}
		catch (ReflectionException e) {
			throw new RuntimeException(e);
		}
	}

	public static final Object getFieldValue(Field field, Object object) {
		try {
			field.setAccessible(true);
			return field.get(object);
		}
		catch (ReflectionException e) {
			throw new RuntimeException(e);
		}
	}
}
