package ca.grindforloot.server.services;

import java.util.Map;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ScriptService {
	
	private static ScriptEngine engine = null;
	/**
	 * Initialize a JavaScript interpreter. It's a thread-safe resource.
	 * @param contextMap - a name-object map of the global context for the script. These resources HAVE to be threadsafe.
	 */
	public static void init(Map<String, Object> contextMap) {
		
		ScriptEngineManager manager = new ScriptEngineManager();
		
		manager.setBindings(fillBindings(manager.getBindings(), contextMap));
				
		engine = manager.getEngineByName("nashorn");
	}
	
	/**
	 * Interpret javascript. 
	 * @param script - a string of the script to interpret.
	 * @param contextMap - a name-object map of the scope variables for this script. These resources do not have to be threadsafe.
	 * @throws ScriptException - If anything goes wrong.
	 */
	public static void interpret(String script, Map<String, Object> contextMap) throws ScriptException {
		if(engine == null)//alternatively we could just call init
			throw new IllegalStateException("Must call ScriptService.init before interpreting");
		
		if(script == null || script.equals(""))
			throw new IllegalArgumentException("Script cannot be null");
		
		Bindings context = fillBindings(engine.createBindings(), contextMap);
		
		engine.eval(script, context);
	}
	
	private static Bindings fillBindings(Bindings source, Map<String, Object> data) {
		Bindings result = source;
		
		for(Entry<String, Object> entry : data.entrySet())
			result.put(entry.getKey(), entry.getValue());
		
		return result;
	}
}
