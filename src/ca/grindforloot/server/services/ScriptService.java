package ca.grindforloot.server.services;

import java.util.Map;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.openjdk.nashorn.api.scripting.ClassFilter;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import ca.grindforloot.server.entities.Script;
import ca.grindforloot.server.errors.UserError;

public class ScriptService {
	
	private static ScriptEngine engine = null;
	/**
	 * Initialize a JavaScript interpreter. It's a thread-safe resource.
	 * @param contextMap - a name-object map of the global context for the script. These resources HAVE to be threadsafe.
	 */
	public static void init(Map<String, Object> contextMap) {
		
		if(engine != null)
			throw new IllegalStateException("Script engine already initialized");
				
		engine = new NashornScriptEngineFactory().getScriptEngine(new ClassFilter() {
			/**
			 * Define the classes that we are allowing script context to instantiate.
			 */
			@Override
			public boolean exposeToScripts(String s) {
				if(s.startsWith("java.lang.") || s.startsWith("java.util."))
					return true;
				
				return false;
			}
		});
		
		Bindings global = fillBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE), contextMap);
		engine.setBindings(global, ScriptContext.GLOBAL_SCOPE);
	}
	
	/**
	 * Interpret a script entity
	 * @param script
	 * @param context
	 * @throws UserError
	 */
	public static void interpret(Script script, Map<String, Object> context) throws UserError {	
		interpret(script.getScript(), context);
	}
	
	/**
	 * Interpret javascript. 
	 * @param script - a string of the script to interpret.
	 * @param contextMap - a name-object map of the scope variables for this script. These resources do not have to be threadsafe.
	 * @throws Throwable 
	 */
	public static void interpret(String script, Map<String, Object> contextMap) throws UserError {
		if(engine == null)
			throw new IllegalStateException("Must call ScriptService.init before interpreting");
		
		if(script == null || script.equals(""))
			throw new IllegalArgumentException("Script cannot be null");
		
		Bindings context = fillBindings(engine.createBindings(), contextMap);
		
		try {
			engine.eval(script, context);
		}
		catch(ScriptException e) {
			Throwable cause = e.getCause();
			
			//TODO java 16
			if(cause instanceof UserError) 
				throw (UserError) cause;
			else
				throw new RuntimeException("Error during script execution: " + cause.toString());
		}
		
	}
	
	/**
	 * Fill a bindings object with data.
	 * @param source
	 * @param data
	 * @return
	 */
	private static Bindings fillBindings(Bindings source, Map<String, Object> data) {		
		for(Entry<String, Object> entry : data.entrySet())
			source.put(entry.getKey(), entry.getValue());
		
		return source;
	}
}
