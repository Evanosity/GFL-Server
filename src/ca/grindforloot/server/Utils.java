package ca.grindforloot.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Utils {
	/**
	 * reflect scares me
	 * @param path
	 * @param clazzParams
	 * @param params
	 * @return
	 */
	public static Object instantiate(String path, Class<?>[] clazzParams, Object[] params) {
		
		try {
			Class<?> clazz = Class.forName(path);
			Constructor<?> cons = clazz.getConstructor(clazzParams);
			
			return cons.newInstance(params);
			
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			//TODO log it.
			throw new RuntimeException("Attempted to instantiate class that does not exist");
		}
	}
}
