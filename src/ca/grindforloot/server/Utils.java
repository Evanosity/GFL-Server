package ca.grindforloot.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Utility methods
 * @author Evan
 *
 */
public class Utils {
	/**
	 * Given a path and parameters, reflectively instantiate an object.
	 * This will throw a RuntimeException if the incorrect parameter types are passed in.
	 * @param path
	 * @param params
	 * @return
	 */
	public static Object instantiate(String path, Object... params) {
		
		try {
			
			Class<?>[]clazzes = new Class<?>[params.length];
			
			for(int a = 0; a != params.length; a++) 
				clazzes[a] = params[a].getClass();
			
			
			Class<?> clazz = Class.forName(path);
			Constructor<?> cons = clazz.getConstructor(clazzes);
			
			return cons.newInstance(params);
			
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			//TODO log it.
			throw new RuntimeException("Attempted to instantiate class that does not exist");
		}
	}
}
