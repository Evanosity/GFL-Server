package ca.grindforloot.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ca.grindforloot.server.db.Key;

/**
 * Utility methods
 * @author Evan
 *
 */
public class Utils {
	
	/**
	 * 
	 * @param value1
	 * @param value2
	 * @return
	 */
	public static boolean equals(Object value1, Object value2) {
		if(value1 instanceof Key && value2 instanceof Key) {
			Key one = (Key) value1;
			Key two = (Key) value2;
			
			if(one.getType().equals(two.getType()) && one.getId().equals(two.getId()))
				return true;
			
			return false;
		}
		
		return false;
	}
	
	private static Map<String, Constructor<?>> constructorCache = new ConcurrentHashMap<>();	
	/**
	 * Given a path and parameters, reflectively instantiate an object.
	 * This will throw a RuntimeException if the incorrect parameter types are passed in.
	 * TODO IMPORTANT! Deprecate this with switch statements. But for now.... it works....
	 * @param path
	 * @param params
	 * @return
	 */
	public static Object instantiate(String path, Object... params) {
		
		try {
			Constructor<?> cons = null;
			
			if(constructorCache.containsKey(path)) {
				cons = constructorCache.get(path);
			}
			else {
				Class<?>[]clazzes = new Class<?>[params.length];
				
				for(int a = 0; a != params.length; a++) 
					clazzes[a] = params[a].getClass();
				
				Class<?> clazz = Class.forName(path);
				cons = clazz.getConstructor(clazzes);
			}
				
			
			return cons.newInstance(params);
			
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			//TODO log it.
			throw new RuntimeException("Error while instantiating " + path + " Stacktrace: " + e.getStackTrace());
		}
	}
}
