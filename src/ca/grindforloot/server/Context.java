package ca.grindforloot.server;

import java.util.HashMap;
import java.util.Map;

import ca.grindforloot.server.db.DBService;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

/**
 * When Json is received by the socket, an instance of this object is created.
 * 
 * Helper methods for dealing with the socket
 *
 * @author Evan
 *
 */
public class Context {
	private Map<String, Object> attributes;
	private final NetSocket socket;
	private final DBService db;
	
	private final JsonObject incoming;
	
	public Context(NetSocket socket, DBService db, JsonObject incoming) {
		if(socket == null)
			throw new IllegalArgumentException("Socket cannot be null.");
		this.socket = socket;
		
		if(db == null)
			throw new IllegalArgumentException("DB cannot be null");
		this.db = db;
		
		if(incoming == null)
			throw new IllegalArgumentException("Incoming cannot be null");
		this.incoming = incoming;
	}
	
	public DBService getDB() {
		return db;
	}
	
	public String getStringProperty(String key) {
		return incoming.getString(key);
	}
	
	public Object getProperty(String key) {
		return incoming.getValue(key);
	}
	
	/**
	 * Push a JsonObject down the socket
	 * @param json
	 */
	public void writeToSocket(JsonObject json) {
		socket.write(Json.encodeToBuffer(json));
	}
	
	
	
	
	public void setAttribute(String key, Object obj) {
		if(attributes == null)
			attributes = new HashMap<>();
		
		attributes.put(key, obj);
	}
	
	public Object getAttribute(String key) {
		if(attributes == null)
			return null;
		
		return attributes.get(key);
	}
	
	/**
	 * Remove an attribute.
	 * @param key
	 * @return if the attribute existed before it was removed
	 */
	public boolean removeAttribute(String key) {
		if(attributes == null)
			return false;
		
		return attributes.remove(key) != null;
	}
}
